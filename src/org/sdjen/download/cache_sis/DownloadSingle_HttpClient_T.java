package org.sdjen.download.cache_sis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.http.HttpUtil;

public class DownloadSingle_HttpClient_T {
	private String html = "";// 存放网页HTML源代码
	private int cssCount = 0;// 下载成功的样式表文件个数
	private int jsCount = 0;// 下载成功的JavaScript文件个数
	private int normalImageCount = 0;// 普通图片数量
	private int backgroundImageCount = 0;// 背景图片数量
	private boolean need_css = true;
	private boolean need_js = true;
	private boolean need_images = true;
	public String chatset = "utf8";
	public String save_path = "C:\\Users\\jimmy.xu\\Downloads\\htmlhelp";
	public String sub_css = "css";
	public String sub_js = "js";
	public String sub_images = "images";
	private HttpUtil httpUtil;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");

	public DownloadSingle_HttpClient_T() {
	}

	public DownloadSingle_HttpClient_T setConfUtil(ConfUtil conf) {
		chatset = conf.getProperties().getProperty("chatset");
		save_path = conf.getProperties().getProperty("save_path");
		need_css = Boolean.valueOf(conf.getProperties().getProperty("need_css"));
		need_js = Boolean.valueOf(conf.getProperties().getProperty("need_js"));
		need_images = Boolean.valueOf(conf.getProperties().getProperty("need_images"));
		httpUtil = new HttpUtil().setConfUtil(conf);
		return this;
	}

	private void showMsg(Object pattern, Object... args) {
		System.out.print(dateFormat.format(new Date()));
		System.out.print("	");
		if (null != args && args.length > 0 && pattern instanceof String) {
			System.out.println(MessageFormat.format((String) pattern, args));
		} else {
			System.out.println(pattern);
		}
	}

	/**
	 * 开始下载
	 * 
	 * @throws IOException
	 */
	public void startDownload(String url, String save_name) throws IOException {
		File savePath = new File(save_path);
		File newFile = new File(savePath.toString() + "/" + save_name);
		if (newFile.exists()) {
			showMsg("已存在	{0}", newFile);
			return;
		}
		// 计数清零
		cssCount = 0;// 下载成功的样式表文件个数
		jsCount = 0;// 下载成功的JavaScript文件个数
		normalImageCount = 0;// 普通图片数量
		backgroundImageCount = 0;// 背景图片数量
		// 创建必要的一些文件夹
		if (!savePath.exists())
			savePath.mkdir();// 如果文件夹不存在，则创建
		File css = new File(savePath + "/" + sub_css);
		File js = new File(savePath + "/" + sub_js);
		File images = new File(savePath + "/" + sub_images);
		File t = new File(savePath + "/t");
		if (!css.exists()) {
			css.mkdir();
			showMsg("css文件夹不存在，已创建！");
		}
		if (!js.exists()) {
			js.mkdir();
			showMsg("js文件夹不存在，已创建！");
		}
		if (!images.exists()) {
			images.mkdir();
			showMsg("images文件夹不存在，已创建！");
		}
		if (!t.exists()) {
			t.mkdir();
			showMsg("T文件夹不存在，已创建！");
		}
		// 下载网页html代码
		showMsg("开始下载网页HTML源代码！{0}", url);
		html = getHTML(url, chatset);
		// showMsg("网页HTML下载成功！");
		if (need_css) {
			 showMsg("开始下载样式表文件！");
			regx("<link.*type=\"text/css\".*>", "href=\"", url, sub_css);
		}
		if (need_js) {
			 showMsg("开始下载JavaScript文件！");
			regx("<script.*javascript.*>", "src=\"", url, sub_js);
		}
		if (need_images) {
			 showMsg("开始下载网页前景图片文件！");
			regx("<img.*src.*>", "src=\"", url, sub_images);
		}
		if (true) {
			showMsg("开始下载文件！");
			regx("<a.*href=\"attachment.php?.*>", "href=\"", url, "t");
		}
		// 保存网页HTML到文件
		// try {
		// newFile.createNewFile();
		// OutputStreamWriter writer = new OutputStreamWriter(new
		// FileOutputStream(newFile), chatset);
		// BufferedWriter bw = new BufferedWriter(writer);
		// bw.write(html);
		// bw.close();
		// writer.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		if (cssCount > 0)
			showMsg("累计下载css文件	{0}", cssCount);
		if (jsCount > 0)
			showMsg("累计下载JavaScript文件	{0}", jsCount);
		if (normalImageCount > 0)
			showMsg("累计下载前景图片	{0}", normalImageCount);
		if (backgroundImageCount > 0)
			showMsg("累计下载背景图片	{0}", backgroundImageCount);
	}

	/**
	 * 最核心的代码，从网页html中查找符合条件的图片、css、js等文件并批量下载
	 * 
	 * @param regx
	 *            检索内容的一级正则表达式，结果是含开始、结束标签的整个字符串，例如：<script.*javascript.*>
	 * @param head
	 *            已经检索出的标签块中要提取的字符的头部，包含前面的双引号，如：src="
	 * @param url
	 *            要下载的网页完整地址，如：http://www.hua.com
	 * @param folderName
	 *            文件夹名，如：css
	 */
	private void regx(String regx, String head, String url, String folderName) {
		// 下载某种资源文件
		Pattern pattern = Pattern.compile(regx);// 新建一个正则表达式
		Matcher matcher = pattern.matcher(html);// 对网页源代码进行查找匹配
		while (matcher.find()) {// 对符合条件的结果逐条做处理
			Matcher matcherNew = Pattern.compile(head + ".*\"").matcher(matcher.group());
			if (matcherNew.find()) {
				// 对于CSS匹配，查找出的结果形如：href="skins/default/css/base.css" rel="stylesheet"
				// type="text/css"
				String myUrl = matcherNew.group();
				myUrl = myUrl.replaceAll(head, "");// 去掉前面的头部，如：href:"
				myUrl = myUrl.substring(0, myUrl.indexOf("\""));// 从第一个引号开始截取真正的内容，如：skins/default/css/base.css
				String myName = getUrlFileName(myUrl);// 获取样式表文件的文件名，如：base.css
				html = html.replaceAll(myUrl, folderName + "/" + myName);// 替换html文件中的资源文件
				myUrl = joinUrlPath(url, myUrl);// 得到最终的资源文件URL，如：http://www.hua.com/skins/default/css/base.css
				showMsg(myUrl);
				if (true)
					continue;
				// showMsg("发生地健康："+myUrl);
				// 去掉文件名不合法的情况，不合法的文件名字符还有好几个，这里只随便举例几个
				if (!myName.contains("?") && !myName.contains("\"") && !myName.contains("/")) {
					if (downloadFile(myUrl, save_path + "/" + folderName + "/" + myName)) {// 开始下载文件
						if (regx.startsWith("<img"))// 如果是下载前景图片文件
							normalImageCount++;
						if (regx.startsWith("<script"))// 如果是下载JS文件
							jsCount++;
						if (regx.startsWith("<link")) {// 如果是下载css文件
							cssCount++;
							// 将刚刚下载的CSS文件实例化
							File cssFile = new File(save_path + "/" + folderName + "/" + myName);
							String txt = readFile(cssFile, chatset);// 读取CSS文件的内容，这里用默认的gb2312编码 XXX
							// 开始匹配背景图片
							Matcher matcherUrl = Pattern.compile("background:url\\(.*\\)").matcher(txt);
							while (matcherUrl.find()) {
								// 去掉前面和后面的标记,得到的结果如：../images/ico_4.gif
								String temp = matcherUrl.group().replaceAll("background:url\\(", "").replaceAll("\\)", "");
								// 拼接出真正的图片路径，如：http://www.hua.com/skins/default/images/ico_4.gif
								String backgroundUrl = joinUrlPath(myUrl, temp);
								// 获取背景图片的文件名，如：ico_4.gif
								String backgroundFileName = getUrlFileName(backgroundUrl);
								// 背景图片要保存的路径，如：c:/users\lxa\desktop\网页\images\ico_4.gif
								String backgroundFilePath = save_path + "/" + sub_images + "/" + backgroundFileName;
								if (!new File(backgroundFilePath).exists()) {// 如果不存在同名文件
									if (downloadFile(backgroundUrl, backgroundFilePath)) {// 开始下载背景图片
										backgroundImageCount++;// 计数加1
										showMsg("成功下载背景图片：{0}", backgroundFileName);
									}
								} else {
									showMsg("指定文件夹已存在同名文件，已为您自动跳过：{0}", backgroundFilePath);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 根据指定的URL下载html代码
	 * 
	 * @param pageURL
	 *            网页的地址
	 * @param encoding
	 *            编码方式
	 * @return 返回网页的html内容
	 * @throws IOException
	 */
	private String getHTML(String pageURL, String encoding) throws IOException {
		StringBuffer pageHTML = new StringBuffer();
//		BufferedReader br = new BufferedReader(new InputStreamReader(httpUtil.getInputStream(pageURL), encoding));
//		String line = null;
//		while ((line = br.readLine()) != null) {
//			pageHTML.append(line);
//			pageHTML.append("\r\n");
//		}
		return pageHTML.toString();
	}

	/**
	 * 根据URL下载某个文件
	 * 
	 * @param fileURL
	 *            下载地址
	 * @param filePath
	 *            存放的路径
	 */
	private boolean downloadFile(String fileURL, String filePath) {
		try {
			File file = new File(filePath);
			if (file.exists()) {
				// showMsg("文件已存在： {0}", filePath);
				return false;
			} else {
				file.createNewFile();
//				byte[] buffer = new byte[1024];
//				InputStream is = httpUtil.getInputStream(fileURL);
//				FileOutputStream fos = new FileOutputStream(file);
//				int len = 0;
//				while ((len = is.read(buffer)) != -1)
//					fos.write(buffer, 0, len);
//				fos.close();
//				is.close();
				showMsg("成功下载文件：	{0}	{1}", filePath, fileURL);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			showMsg("该文件不存在：	{0}	{1}		{2}", fileURL, filePath, e);
			return false;
		}
	}

	/**
	 * 读取某个文本文件的内容
	 * 
	 * @param file
	 *            要读取的文件
	 * @param encode
	 *            读取文件的编码方式
	 * @return 返回读取到的内容
	 */
	private String readFile(File file, String encode) {
		try {
			InputStreamReader read = new InputStreamReader(new FileInputStream(file), encode);
			BufferedReader bufread = new BufferedReader(read);
			StringBuffer sb = new StringBuffer();
			String str = "";
			while ((str = bufread.readLine()) != null)
				sb.append(str + "\n");
			String txt = new String(sb);
			return txt;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 获取URL中最后面的真实文件名
	 * 
	 * @param url
	 *            如：http://www.hua.com/bg.jpg
	 * @return 返回bg.jpg
	 */
	private String getUrlFileName(String url) {
		String result = url.split("/")[url.split("/").length - 1];
		if (result.contains("?"))
			result = result.substring(result.lastIndexOf("="), result.length()) + ".torrent";
		return result;
	}

	/**
	 * 获取URL不带文件名的路径
	 * 
	 * @param url
	 *            如：http://www.hua.com/bg.jpg
	 * @return 返回 http://www.hua.com
	 */
	private String getUrlPath(String url) {
		return url.replaceAll("/" + getUrlFileName(url), "");
	}

	/**
	 * 拼接URL路径和文件名，注意：以../或者/开头的fileName都要退一层目录
	 * 
	 * @param url
	 *            如：http://www.hua.com/product/9010753.html
	 * @param fileName
	 *            如：../skins/default/css/base.css
	 * @return http://www.hua.com/skins/default/css/base.css
	 */
	private String joinUrlPath(String url, String fileName) {
		// showMsg("url:"+url);
		// showMsg("fileName:"+fileName);
		if (fileName.startsWith("http://") || fileName.startsWith("https://"))
			return fileName;
		// 如果去掉“http://”前缀后还包含“/”符，说明要退一层目录，即去掉当前文件名
		if (url.replaceAll("http://", "").replaceAll("https://", "").contains("/"))
			url = getUrlPath(url);
		if (fileName.startsWith("../") || fileName.startsWith("/")) {
			// 只有当前URL包含多层目录才能后退，如果只是http://www.hua.com，想后退都不行
			if (url.replaceAll("http://", "").replaceAll("https://", "").contains("/"))
				url = getUrlPath(url);
			fileName = fileName.substring(fileName.indexOf("/") + 1);
		}
		// showMsg("return:"+url+"/"+fileName);
		return url + "/" + fileName;
	}
}
