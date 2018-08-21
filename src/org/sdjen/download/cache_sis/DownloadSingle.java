package org.sdjen.download.cache_sis;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.conf.MapDBUtil;
import org.sdjen.download.cache_sis.http.HttpFactory;
import org.sdjen.download.cache_sis.log.LogUtil;

public class DownloadSingle {
	private String html = "";// 存放网页HTML源代码
	public String chatset = "utf8";
	private String save_path = "C:\\Users\\jimmy.xu\\Downloads\\htmlhelp";
	// private String sub_images = "images";
	// private String sub_html = "html";
	// private String sub_torrent = "torrent";
	private HttpFactory httpUtil;
	private MapDBUtil mapDBUtil;
	private MessageDigest md5;
	private long length_download;
	private long length_flag_min_byte = 20000;
	private long length_flag_max_byte = 70000;
	private Lock lock_w_replace = new ReentrantReadWriteLock().writeLock();
	private Lock lock_w_mapdb = new ReentrantReadWriteLock().writeLock();
	private Lock lock_w_html = new ReentrantReadWriteLock().writeLock();

	public DownloadSingle() throws Exception {
		ConfUtil conf = ConfUtil.getDefaultConf();
		md5 = MessageDigest.getInstance("MD5");
		chatset = conf.getProperties().getProperty("chatset");
		save_path = conf.getProperties().getProperty("save_path");
		try {
			length_flag_min_byte = Long.valueOf(conf.getProperties().getProperty("length_flag_min_byte"));
		} catch (Exception e) {
		}
		try {
			length_flag_max_byte = Long.valueOf(conf.getProperties().getProperty("length_flag_max_byte"));
		} catch (Exception e) {
		}
	}

	public DownloadSingle setHttpUtil(HttpFactory httpUtil) {
		this.httpUtil = httpUtil;
		return this;
	}

	public DownloadSingle setMapDBUtil(MapDBUtil mapDBUtil) {
		this.mapDBUtil = mapDBUtil;
		return this;
	}

	private String getFileName(String name) {
		return name//
		        .replace('\\', ' ')//
		        .replace('/', ' ')//
		        .replace(':', ' ')//
		        .replace('*', ' ')//
		        .replace('?', ' ')//
		        .replace('<', ' ')//
		        .replace('>', ' ')//
		        .replace('|', ' ')//
		        .replace('"', ' ')//
		;
	}

	public static void main(String[] args) throws Throwable {
		MapDBUtil mapDBUtil = new MapDBUtil();
		ConfUtil.getDefaultConf().getProperties().setProperty("retry_times", "1");
		ConfUtil.getDefaultConf().getProperties().setProperty("retry_time_second", "1");
		// ConfUtil.getDefaultConf().getProperties().setProperty("chatset",
		// "utf8");
		// ConfUtil.getDefaultConf().getProperties().setProperty("list_url",
		// "https://club.autohome.com.cn/bbs/thread/");
		LogUtil.init();
		HttpFactory httpUtil = new HttpFactory();
		try {
			// System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");
			// Security.setProperty("jdk.tls.disabledAlgorithms","SSLv3, DH
			// keySize < 768");
			DownloadSingle util = new DownloadSingle().setHttpUtil(httpUtil).setMapDBUtil(mapDBUtil);
			// util.startDownload("http://www.sexinsex.net/bbs/thread-6720446-1-2000.html",
			// "370013862.html", "U");
			// util.downloadFile("http://img599.net/images/2013/06/02/CCe908c.th.jpg",
			// "1.jpg");
			// util.downloadFile("https://www.caribbeancom.com/moviepages/022712-953/images/l_l.jpg",
			// "2.jpg");
			// util.downloadFile("https://www.caribbeancom.com/moviepages/022712-953/images/l_l.jpg",
			// "rr", "2.jpg");
			util.downloadFile("https://www1.wi.to/2018/03/29/87188c533dce9cfaa1e34992c693a5d5.jpg", "rr", "11.jpg");
			// https://www1.wi.to/2018/03/29/87188c533dce9cfaa1e34992c693a5d5.jpg
			// https://www1.wi.to/2018/03/29/04f7c405227da092576b127e640d07f8.jpg
		} finally {
			httpUtil.finish();
			mapDBUtil.finish();
			LogUtil.finishAll();
		}
	}

	private synchronized String getMD5(byte[] bytes) {
		return new BigInteger(1, md5.digest(bytes)).toString(Character.MAX_RADIX);
	}

	/**
	 * 开始下载
	 * 
	 * @throws Throwable
	 * 
	 * @throws IOException
	 */
	public boolean startDownload(final String url, String save_name, String dateStr) throws Throwable {
		String subKey;
		if (null == dateStr || dateStr.isEmpty())
			subKey = "unknow";
		else
			subKey = dateStr.substring(0, Math.min(7, dateStr.length()));
		final String sub_images = "images/" + subKey;
		String sub_html = "html/" + subKey;
		final String sub_torrent = "torrent";
		save_name = getFileName(save_name);
		File newFile = new File(save_path + "/" + sub_html + "/" + save_name);
		if (newFile.exists()) {
			// showMsg("已存在 {0}", newFile);
			return false;
		}
		// 下载网页html代码
		String tmp_html = httpUtil.getHTML(url);
		lock_w_html.lock();
		File savePath = new File(save_path);
		if (!savePath.exists())
			savePath.mkdirs();
		// 创建必要的一些文件夹
		for (String sub : new String[] { sub_images, sub_images + "/min", sub_images + "/mid", sub_images + "/max"//
		        , sub_torrent, sub_torrent + "/min", sub_torrent + "/mid", sub_torrent + "/max"//
		        , sub_html }) {
			File f = new File(savePath + "/" + sub);
			if (!f.exists()) {
				f.mkdirs();
				LogUtil.msgLog.showMsg("{0}文件夹 {0}	不存在，已创建！", f);
			}
		}
		length_download = 0;
		LogUtil.msgLog.showMsg("{0} {1}	{2}", dateStr, save_name, url);
		html = tmp_html;
		org.jsoup.nodes.Document doument = Jsoup.parse(html);
		ExecutorService executor = Executors.newFixedThreadPool(6);
		List<Future<String[]>> resultList = new ArrayList<Future<String[]>>();
		for (org.jsoup.nodes.Element e : doument.select("a[href]")) {
			final String href = e.attr("href");
			final String text = e.text();
			if (href.startsWith("attachment.php?aid=")) {
				resultList.add(executor.submit(new Callable<String[]>() {
					public String[] call() throws Exception {
						String newName = getFileName("(" + href.substring(href.lastIndexOf("=") + 1, href.length()) + ")" + text);
						String downloadUrl = httpUtil.joinUrlPath(url, href);
						newName = downloadFile(downloadUrl, sub_torrent, newName);
						// if (!name.equals(downloadUrl))
						// replaceAll(href, name);
						return new String[] { href, newName };
					}
				}));// 将任务执行结果存储到List中
			}
		}
		for (org.jsoup.nodes.Element e : doument.select("img[src]")) {
			final String src = e.attr("src");
			resultList.add(executor.submit(new Callable<String[]>() {
				public String[] call() throws Exception {
					String downloadUrl = httpUtil.joinUrlPath(url, src);
					String newName;// = getMD5(downloadUrl.getBytes("utf-8"));
					if (src.contains("."))
						newName = getFileName(src.substring(src.lastIndexOf("."), src.length()));
					else {
						newName = ".jpg";
					}
					newName = downloadFile(downloadUrl, sub_images, newName);
					// if (!newName.equals(downloadUrl))
					// replaceAll(src, newName);
					return new String[] { src, newName };
				}
			}));// 将任务执行结果存储到List中
		}
		executor.shutdown();
		for (Future<String[]> fs : resultList) {
			try {
				// while (!fs.isDone())
				// ;// Future返回如果没有完成，则一直循环等待，直到Future返回完成
				String[] names = fs.get(1, TimeUnit.MINUTES);// 各个线程（任务）执行的结果
				if (null != names && !names[0].equals(names[1])) {
					replaceAll(names[0], names[1]);
				}
			} catch (java.util.concurrent.TimeoutException e) {
				fs.cancel(false);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
		}
		// 保存网页HTML到文件
		try {
			int length = html.length();
			length_download += html.length();
			if (html.length() > 10000) {
				// newFile.createNewFile();
				OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(newFile), chatset);
				BufferedWriter bw = new BufferedWriter(writer);
				bw.write(html);
				bw.close();
				writer.close();
			} else {
				LogUtil.errLog.showMsg("X	长度过短	{0}	{1}	{2}", length, save_name, url);
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			LogUtil.errLog.showMsg("	异常：	{0}	{1}		{2}", save_name, url, e);
			return false;
		} finally {
			mapDBUtil.getDb().commit();
			httpUtil.getPoolConnManager().closeExpiredConnections();
			LogUtil.msgLog.showMsg("	本次下载	{0}（字节）", length_download);
			lock_w_html.unlock();
		}
		return true;
	}

	public long getLength_download() {
		return length_download;
	}

	private void replaceAll(String src, String targ) {
		lock_w_replace.lock();
		html = html.replace("\"" + src + "\"", "\"../../" + targ + "\"");
		lock_w_replace.unlock();
	}

	/**
	 * 根据URL下载某个文件
	 * 
	 * @param url
	 *            下载地址
	 * @param path
	 *            存放的路径
	 * @throws Exception
	 */
	private String downloadFile(final String url, final String path, final String name) {
		String result = null;
		if (mapDBUtil.getUrlMap().containsKey(url)) {
			result = mapDBUtil.getUrlMap().get(url);
		} else {
			HttpFactory.Executor<String> executor = new HttpFactory.Executor<String>() {
				public void execute(InputStream inputStream) {
					setResult(null);
					ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
					byte[] buffer = new byte[1024];
					try {
						int len = 0;
						while ((len = inputStream.read(buffer)) != -1) {
							arrayOutputStream.write(buffer, 0, len);
						}
						byte[] bytes = arrayOutputStream.toByteArray();
						try {
							arrayOutputStream.close();
						} catch (Exception e) {
						}
						String md5 = getMD5(bytes);
						if (mapDBUtil.getFileMap().containsKey(md5)) {
							String result = mapDBUtil.getFileMap().get(md5);
							setResult(result);
						} else {
							String result = path;
							if (bytes.length < length_flag_min_byte)
								result += "/min";
							else if (bytes.length > length_flag_max_byte)
								result += "/max";
							else
								result += "/mid";
							result += "/";
							if (name.startsWith("."))
								result += md5;
							result += name;
							File file = new File(save_path + "/" + result);
							if (!file.exists()) {
								FileOutputStream fos = new FileOutputStream(file);
								try {
									fos.write(bytes);
								} finally {
									try {
										fos.close();
									} catch (Exception e) {
									}
								}
								length_download += bytes.length;
							}
							lock_w_mapdb.lock();
							mapDBUtil.getFileMap().put(md5, result);
							lock_w_mapdb.unlock();
							setResult(result);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			executor.setResult(null);
			try {
				httpUtil.execute(url, executor);
				result = executor.getResult();
			} catch (Exception e) {
				LogUtil.errLog.showMsg("	异常：	{0}	{1}", url, e);
				e.printStackTrace();
			}
			if (null == result)
				result = url;
			// else {// 握手异常未解决
			lock_w_mapdb.lock();
			mapDBUtil.getUrlMap().put(url, result);
			lock_w_mapdb.unlock();
			// }
		}
		LogUtil.msgLog.showMsg("+	{0}	{1}", result, url);
		return result;
	}
}
