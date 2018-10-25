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
import org.sdjen.download.cache_sis.http.HttpFactory;
import org.sdjen.download.cache_sis.log.CassandraFactory;
import org.sdjen.download.cache_sis.log.LogUtil;
//import org.sdjen.download.cache_sis.log.MapDBFactory;

public class DownloadSingle {
	private String html = "";
	public String chatset = "utf8";
	private String save_path = "C:\\Users\\jimmy.xu\\Downloads\\htmlhelp";
	// private String sub_images = "images";
	// private String sub_html = "html";
	// private String sub_torrent = "torrent";
	private HttpFactory httpUtil;
	private MessageDigest md5;
	private long length_download;
	private long length_flag_min_byte = 20000;
	private long length_flag_max_byte = 70000;
	private Lock lock_w_replace = new ReentrantReadWriteLock().writeLock();
	// private Lock lock_w_mapdb = new ReentrantReadWriteLock().writeLock();
	private Lock lock_w_html = new ReentrantReadWriteLock().writeLock();
	private int download_threads = 6;
	private org.sdjen.download.cache_sis.log.CassandraFactory cassandraFactory;

	public DownloadSingle() throws Exception {
		ConfUtil conf = ConfUtil.getDefaultConf();
		md5 = MessageDigest.getInstance("MD5");
		chatset = conf.getProperties().getProperty("chatset");
		save_path = conf.getProperties().getProperty("save_path");
		boolean isStore = false;
		try {
			download_threads = Integer.valueOf(conf.getProperties().getProperty("download_threads"));
		} catch (Exception e) {
			conf.getProperties().setProperty("download_threads", String.valueOf(download_threads));
			isStore = true;
		}
		try {
			length_flag_min_byte = Long.valueOf(conf.getProperties().getProperty("length_flag_min_byte"));
		} catch (Exception e) {
			conf.getProperties().setProperty("length_flag_min_byte", String.valueOf(length_flag_min_byte));
			isStore = true;
		}
		try {
			length_flag_max_byte = Long.valueOf(conf.getProperties().getProperty("length_flag_max_byte"));
		} catch (Exception e) {
			conf.getProperties().setProperty("length_flag_max_byte", String.valueOf(length_flag_max_byte));
			isStore = true;
		}
		if (isStore)
			conf.store();
		cassandraFactory = CassandraFactory.getDefaultFactory();
	}

	public DownloadSingle setHttpUtil(HttpFactory httpUtil) {
		this.httpUtil = httpUtil;
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
		ConfUtil.getDefaultConf().getProperties().setProperty("retry_times", "1");
		ConfUtil.getDefaultConf().getProperties().setProperty("retry_time_second", "1");
		// ConfUtil.getDefaultConf().getProperties().setProperty("chatset",
		// "utf8");
		// ConfUtil.getDefaultConf().getProperties().setProperty("list_url",
		// "https://club.autohome.com.cn/bbs/thread/");
		LogUtil.init();
		// MapDBFactory.init();
		HttpFactory httpUtil = new HttpFactory();
		try {
			// System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");
			// Security.setProperty("jdk.tls.disabledAlgorithms","SSLv3, DH
			// keySize < 768");
			DownloadSingle util = new DownloadSingle().setHttpUtil(httpUtil);
			// util.startDownload("http://www.sexinsex.net/bbs/thread-6720446-1-2000.html",
			// "370013862.html", "U");
			// util.downloadFile("http://img599.net/images/2013/06/02/CCe908c.th.jpg",
			// "1.jpg");
			// util.downloadFile("https://www.caribbeancom.com/moviepages/022712-953/images/l_l.jpg",
			// "2.jpg");
			// util.downloadFile("https://www.caribbeancom.com/moviepages/022712-953/images/l_l.jpg",
			// "rr", "2.jpg");
			util.downloadFile("https://e.piclect.com/o180829_110f6.jpg", "rr", "11.jpg");
			// util.downloadFile("https://www1.wi.to/2018/03/29/87188c533dce9cfaa1e34992c693a5d5.jpg",
			// "rr", "11.jpg");
			// https://www1.wi.to/2018/03/29/87188c533dce9cfaa1e34992c693a5d5.jpg
			// https://www1.wi.to/2018/03/29/04f7c405227da092576b127e640d07f8.jpg
		} finally {
			httpUtil.finish();
			// MapDBFactory.finishAll();
			LogUtil.finishAll();
		}
	}

	private synchronized String getMD5(byte[] bytes) {
		return new BigInteger(1, md5.digest(bytes)).toString(Character.MAX_RADIX);
	}

	/**
	 * ��ʼ����
	 * 
	 * @throws Throwable
	 * 
	 * @throws IOException
	 */
	public boolean startDownload(final String url, String save_name, String dateStr) throws Throwable {
		String subKey;
		try {
			subKey = dateStr.substring(0, Math.min(7, dateStr.length())) + "/" + dateStr.substring(8, dateStr.length()).replace("-", "");
		} catch (Exception e1) {
			subKey = "unknow";
		}
		final String sub_images = "images/" + subKey;
		String sub_html = "html/" + subKey;
		final String sub_torrent = "torrent/" + subKey;
		save_name = getFileName(save_name);
		File newFile = new File(save_path + "/" + sub_html + "/" + save_name);
		if (newFile.exists()) {
			return false;
		}
		String tmp_html = httpUtil.getHTML(url);
		lock_w_html.lock();
		File savePath = new File(save_path);
		if (!savePath.exists())
			savePath.mkdirs();
		for (String sub : new String[] { sub_images, sub_images + "/min", sub_images + "/mid", sub_images + "/max"//
				, sub_torrent, sub_torrent + "/min", sub_torrent + "/mid", sub_torrent + "/max"//
				, sub_html }) {
			File f = new File(savePath + "/" + sub);
			if (!f.exists()) {
				f.mkdirs();
				LogUtil.msgLog.showMsg("{0}�ļ��� {0}	�����ڣ��Ѵ�����", f);
			}
		}
		length_download = 0;
		LogUtil.msgLog.showMsg("{0} {1}	{2}", dateStr, save_name, url);
		html = tmp_html;
		org.jsoup.nodes.Document doument = Jsoup.parse(html);
		ExecutorService executor = Executors.newFixedThreadPool(download_threads);
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
						return new String[] { href, newName };
					}
				}));
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
			}));// ������ִ�н���洢��List��
		}
		executor.shutdown();
		for (Future<String[]> fs : resultList) {
			try {
				// while (!fs.isDone())
				// ;// Future�������û����ɣ���һֱѭ���ȴ���ֱ��Future�������
				String[] names = fs.get(1, TimeUnit.MINUTES);// �����̣߳�����ִ�еĽ��
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
		// ������ҳHTML���ļ�
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
				LogUtil.errLog.showMsg("X	���ȹ���	{0}	{1}	{2}", length, save_name, url);
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			LogUtil.errLog.showMsg("	�쳣��	{0}	{1}		{2}", save_name, url, e);
			return false;
		} finally {
			httpUtil.getPoolConnManager().closeExpiredConnections();
			LogUtil.msgLog.showMsg("	��������	{0}���ֽڣ�", length_download);
			lock_w_html.unlock();
		}
		return true;
	}

	public long getLength_download() {
		return length_download;
	}

	private void replaceAll(String src, String targ) {
		lock_w_replace.lock();
		html = html.replace("\"" + src + "\"", "\"../../../" + targ + "\"");
		lock_w_replace.unlock();
	}

	/**
	 * ����URL����ĳ���ļ�
	 * 
	 * @param url
	 *            ���ص�ַ
	 * @param path
	 *            ��ŵ�·��
	 * @throws IOException
	 * @throws Exception
	 */
	private String downloadFile(final String url, final String path, final String name) {
		String result = cassandraFactory.getURL_Path(url);// MapDBFactory.getUrlDB().get(url);
		if (null == result) {
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
						String result = cassandraFactory.getMD5_Path(md5);// MapDBFactory.getFileDB().get(md5);
						if (null == result) {
							result = path;
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
							// lock_w_mapdb.lock();
							// MapDBFactory.getFileDB().put(md5, result);
							// mapDBUtil.commit();
							// lock_w_mapdb.unlock();
							cassandraFactory.saveMD5(md5, result);
						}
						setResult(result);
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
				LogUtil.errLog.showMsg("	�쳣��	{0}	{1}", url, e);
				e.printStackTrace();
			}
			if (null == result)
				result = url;
			// else {// �����쳣δ���
			// lock_w_mapdb.lock();
			// MapDBFactory.getUrlDB().put(url, result);
			// mapDBUtil.commit();
			// lock_w_mapdb.unlock();
			cassandraFactory.saveURL(url, result);
			// }
		}
		LogUtil.msgLog.showMsg("+	{0}	{1}", result, url);
		return result;
	}
}
