package org.sdjen.download.cache_sis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.conf.ConfUtil;
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
	private MessageDigest md5;
	private long length_download;

	public DownloadSingle() throws Exception {
		ConfUtil conf = ConfUtil.getDefaultConf();
		md5 = MessageDigest.getInstance("MD5");
		chatset = conf.getProperties().getProperty("chatset");
		save_path = conf.getProperties().getProperty("save_path");
	}

	public DownloadSingle setHttpUtil(HttpUtil httpUtil) {
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
		// ConfUtil.getDefaultConf().getProperties().setProperty("chatset", "utf8");
		// ConfUtil.getDefaultConf().getProperties().setProperty("list_url",
		// "https://club.autohome.com.cn/bbs/thread/");
		LogUtil.init();
		HttpUtil httpUtil = new HttpUtil();
		try {
			DownloadSingle util = new DownloadSingle().setHttpUtil(httpUtil);
			util.startDownload("http://www.sexinsex.net/bbs/thread-7705114-1-1.html", "370013862.html");
			// util.downloadFile("http://img599.net/images/2013/06/02/CCe908c.th.jpg",
			// "1.jpg");
			// util.downloadFile("https://www.caribbeancom.com/moviepages/022712-953/images/l_l.jpg",
			// "2.jpg");
		} finally {
			httpUtil.finish();
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
		for (String sub : new String[] { sub_images, sub_torrent, sub_html }) {
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
			String newName = getMD5(downloadUrl.getBytes("utf-8"));
			if (src.contains("."))
				newName += getFileName(src.substring(src.lastIndexOf("."), src.length()));
			downloadFile(downloadUrl, save_path + "/" + sub_images + "/" + newName);
			replaceAll(src, sub_images + "/" + newName);
		}
		for (org.jsoup.nodes.Element e : doument.select("a[href]")) {
			String href = e.attr("href");
			if (href.startsWith("attachment.php?aid=")) {
				String text = getFileName("(" + href.substring(href.lastIndexOf("=") + 1, href.length()) + ")" + e.text());
				downloadFile(httpUtil.joinUrlPath(url, href), save_path + "/" + sub_torrent + "/" + text);
				replaceAll(href, sub_torrent + "/" + text);
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
	 * @param fileURL
	 *            下载地址
	 * @param filePath
	 *            存放的路径
	 * @throws Exception
	 */
	private boolean downloadFile(final String fileURL, final String filePath) throws Exception {
		try {
			final File file = new File(filePath);
			if (file.exists()) {
				// showMsg("文件已存在： {0}", filePath);
				return false;
			} else {
				HttpUtil.Executor<Boolean> executor = new HttpUtil.Executor<Boolean>() {
					public void execute(InputStream inputStream) {
						byte[] buffer = new byte[1024];
						FileOutputStream fos;
						try {
							fos = new FileOutputStream(file);
							int len = 0;
							while ((len = inputStream.read(buffer)) != -1) {
								fos.write(buffer, 0, len);
								length_download += len;
							}
							fos.close();
							LogUtil.msgLog.showMsg("+	{0}	{1}", filePath, fileURL);
							setResult(true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				executor.setResult(false);
				httpUtil.execute(fileURL, executor);
				return executor.getResult();
			}
		} catch (Exception e) {
			e.printStackTrace();
			LogUtil.errLog.showMsg("	异常：	{0}	{1}		{2}", fileURL, filePath, e);
			// throw e;// 异常则终止本网页生产
			return false;
		}
	}
}
