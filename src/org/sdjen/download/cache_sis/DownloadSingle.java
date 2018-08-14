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

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.conf.MapDBUtil;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.log.LogUtil;

public class DownloadSingle {
	private String html = "";// 存放网页HTML源代码
	public String chatset = "utf8";
	private String save_path = "C:\\Users\\jimmy.xu\\Downloads\\htmlhelp";
	// private String sub_css = "css";
	// private String sub_js = "js";
	private String sub_images = "images";
	private String sub_html = "html";
	private String sub_torrent = "torrent";
	private HttpUtil httpUtil;
	private MapDBUtil mapDBUtil;
	private MessageDigest md5;
	private long length_download;
	private long length_flag_min_byte = 20000;
	private long length_flag_max_byte = 70000;

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

	public DownloadSingle setHttpUtil(HttpUtil httpUtil) {
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
		// ConfUtil.getDefaultConf().getProperties().setProperty("chatset", "utf8");
		// ConfUtil.getDefaultConf().getProperties().setProperty("list_url",
		// "https://club.autohome.com.cn/bbs/thread/");
		LogUtil.init();
		HttpUtil httpUtil = new HttpUtil();
		try {
			DownloadSingle util = new DownloadSingle().setHttpUtil(httpUtil).setMapDBUtil(mapDBUtil);
			util.startDownload("http://www.sexinsex.net/bbs/thread-7705114-1-1.html", "370013862.html");
			// util.downloadFile("http://img599.net/images/2013/06/02/CCe908c.th.jpg",
			// "1.jpg");
			// util.downloadFile("https://www.caribbeancom.com/moviepages/022712-953/images/l_l.jpg",
			// "2.jpg");
		} finally {
			httpUtil.finish();
			mapDBUtil.finish();
			LogUtil.finishAll();
		}
	}

	private String getMD5(byte[] bytes) {
		return new BigInteger(1, md5.digest(bytes)).toString(Character.MAX_RADIX);
	}

	/**
	 * 开始下载
	 * 
	 * @throws Throwable
	 * 
	 * @throws IOException
	 */
	public boolean startDownload(String url, String save_name) throws Throwable {
		length_download = 0;
		File savePath = new File(save_path);
		if (!savePath.exists())
			savePath.mkdirs();
		save_name = getFileName(save_name);
		File newFile = new File(savePath.toString() + "/" + sub_html + "/" + save_name);
		if (newFile.exists()) {
			// showMsg("已存在 {0}", newFile);
			return false;
		}
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
		// 下载网页html代码
		LogUtil.msgLog.showMsg("{0}	{1}", save_name, url);
		html = httpUtil.getHTML(url);
		org.jsoup.nodes.Document doument = Jsoup.parse(html);
		for (org.jsoup.nodes.Element e : doument.select("img[src]")) {
			String src = e.attr("src");
			String downloadUrl = httpUtil.joinUrlPath(url, src);
			String name;// = getMD5(downloadUrl.getBytes("utf-8"));
			if (src.contains("."))
				name = getFileName(src.substring(src.lastIndexOf("."), src.length()));
			else {
				name = ".jpg";
			}
			name = downloadFile(downloadUrl, sub_images, name);
			replaceAll(src, name);
		}
		for (org.jsoup.nodes.Element e : doument.select("a[href]")) {
			String href = e.attr("href");
			if (href.startsWith("attachment.php?aid=")) {
				String name = getFileName("(" + href.substring(href.lastIndexOf("=") + 1, href.length()) + ")" + e.text());
				name = downloadFile(httpUtil.joinUrlPath(url, href), sub_torrent, name);
				replaceAll(href, name);
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
			LogUtil.msgLog.showMsg("	本次下载	{0}（字节）", length_download);
		}
		return true;
	}

	public long getLength_download() {
		return length_download;
	}

	private void replaceAll(String src, String targ) {
		html = html.replace("\"" + src + "\"", "\"../" + targ + "\"");
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
	private String downloadFile(final String url, final String path, final String name) throws Exception {
		String result;
		if (mapDBUtil.getUrlMap().containsKey(url)) {
			result = mapDBUtil.getUrlMap().get(url);
		} else {
			HttpUtil.Executor<String> executor = new HttpUtil.Executor<String>() {
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
							mapDBUtil.getFileMap().put(md5, result);
							setResult(result);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			executor.setResult(null);
			httpUtil.execute(url, executor);
			result = executor.getResult();
			mapDBUtil.getUrlMap().put(url, result);
		}
		LogUtil.msgLog.showMsg("+	{0}	{1}", result, url);
		return result;
	}
}
