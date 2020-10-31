package org.sdjen.download.cache_sis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sdjen.download.cache_sis.configuration.ConfUtil;

public class DownloadSingle_jre {
	private String html = "";// �����ҳHTMLԴ����
	private int cssCount = 0;// ���سɹ�����ʽ���ļ�����
	private int jsCount = 0;// ���سɹ���JavaScript�ļ�����
	private int normalImageCount = 0;// ��ͨͼƬ����
	private int backgroundImageCount = 0;// ����ͼƬ����
	private boolean need_css = true;
	private boolean need_js = true;
	private boolean need_images = true;
	public String chatset = "utf8";
	public String save_path = "C:\\Users\\jimmy.xu\\Downloads\\htmlhelp";
	public String sub_css = "css";
	public String sub_js = "js";
	public String sub_images = "images";
	private Proxy proxy;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");

	public DownloadSingle_jre() {
	}

	public DownloadSingle_jre setIniUtil(ConfUtil iniUtil) {
		chatset = iniUtil.getProperties().getProperty("chatset");
		save_path = iniUtil.getProperties().getProperty("save_path");
		need_css = Boolean.valueOf(iniUtil.getProperties().getProperty("need_css"));
		need_js = Boolean.valueOf(iniUtil.getProperties().getProperty("need_js"));
		need_images = Boolean.valueOf(iniUtil.getProperties().getProperty("need_images"));
		// String proxy = iniUtil.getProperties().getProperty("proxy");
		// if(null != proxy) {
		// showMsg("����{0}", proxy);
		// System.setProperty("http.nonProxyHosts", "127.0.0.1:9666 | 127.0.0.1:8580");
		// String[] s = iniUtil.getProperties().getProperty("proxy").split(":");
		// System.setProperty("http.proxyHost", "127.0.0.1");
		// System.setProperty("http.proxyPort", "9666");
		// }
		try {
			String[] s = iniUtil.getProperties().getProperty("proxy").split(":");
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(s[0], Integer.valueOf(s[1]))); // http ����
			showMsg("����{0}", proxy);
		} catch (Exception e) {
		}
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
	 * ��ʼ����
	 * 
	 * @throws IOException
	 */
	public void startDownload(String url, String save_name) throws IOException {
		File savePath = new File(save_path);
		File newFile = new File(savePath.toString() + "/" + save_name);
		if (newFile.exists()) {
			showMsg("�Ѵ���	{0}", newFile);
			return;
		}
		// ��������
		cssCount = 0;// ���سɹ�����ʽ���ļ�����
		jsCount = 0;// ���سɹ���JavaScript�ļ�����
		normalImageCount = 0;// ��ͨͼƬ����
		backgroundImageCount = 0;// ����ͼƬ����
		// ������Ҫ��һЩ�ļ���
		if (!savePath.exists())
			savePath.mkdir();// ����ļ��в����ڣ��򴴽�
		File css = new File(savePath + "/" + sub_css);
		File js = new File(savePath + "/" + sub_js);
		File images = new File(savePath + "/" + sub_images);
		if (!css.exists()) {
			css.mkdir();
			showMsg("css�ļ��в����ڣ��Ѵ�����");
		}
		if (!js.exists()) {
			js.mkdir();
			showMsg("js�ļ��в����ڣ��Ѵ�����");
		}
		if (!images.exists()) {
			images.mkdir();
			showMsg("images�ļ��в����ڣ��Ѵ�����");
		}
		// ������ҳhtml����
		showMsg("��ʼ������ҳHTMLԴ���룡{0}", url);
		html = getHTML(url, chatset);
		// showMsg("��ҳHTML���سɹ���");
		if (need_css) {
			// showMsg("��ʼ������ʽ���ļ���");
			regx("<link.*type=\"text/css\".*>", "href=\"", url, sub_css);
		}
		if (need_js) {
			// showMsg("��ʼ����JavaScript�ļ���");
			regx("<script.*javascript.*>", "src=\"", url, sub_js);
		}
		if (need_images) {
			// showMsg("��ʼ������ҳǰ��ͼƬ�ļ���");
			regx("<img.*src.*>", "src=\"", url, sub_images);
		}
		// ������ҳHTML���ļ�
		try {
			newFile.createNewFile();
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(newFile), chatset);
			BufferedWriter bw = new BufferedWriter(writer);
			bw.write(html);
			bw.close();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (cssCount > 0)
			showMsg("�ۼ�����css�ļ�	{0}", cssCount);
		if (jsCount > 0)
			showMsg("�ۼ�����JavaScript�ļ�	{0}", jsCount);
		if (normalImageCount > 0)
			showMsg("�ۼ�����ǰ��ͼƬ	{0}", normalImageCount);
		if (backgroundImageCount > 0)
			showMsg("�ۼ����ر���ͼƬ	{0}", backgroundImageCount);
	}

	/**
	 * ����ĵĴ��룬����ҳhtml�в��ҷ���������ͼƬ��css��js���ļ�����������
	 * 
	 * @param regx
	 *            �������ݵ�һ��������ʽ������Ǻ���ʼ��������ǩ�������ַ��������磺<script.*javascript.*>
	 * @param head
	 *            �Ѿ��������ı�ǩ����Ҫ��ȡ���ַ���ͷ��������ǰ���˫���ţ��磺src="
	 * @param url
	 *            Ҫ���ص���ҳ������ַ���磺http://www.hua.com
	 * @param folderName
	 *            �ļ��������磺css
	 */
	private void regx(String regx, String head, String url, String folderName) {
		// ����ĳ����Դ�ļ�
		Pattern pattern = Pattern.compile(regx);// �½�һ��������ʽ
		Matcher matcher = pattern.matcher(html);// ����ҳԴ������в���ƥ��
		while (matcher.find()) {// �Է��������Ľ������������
			Matcher matcherNew = Pattern.compile(head + ".*\"").matcher(matcher.group());
			if (matcherNew.find()) {
				// ����CSSƥ�䣬���ҳ��Ľ�����磺href="skins/default/css/base.css" rel="stylesheet"
				// type="text/css"
				String myUrl = matcherNew.group();
				myUrl = myUrl.replaceAll(head, "");// ȥ��ǰ���ͷ�����磺href:"
				myUrl = myUrl.substring(0, myUrl.indexOf("\""));// �ӵ�һ�����ſ�ʼ��ȡ���������ݣ��磺skins/default/css/base.css
				String myName = getUrlFileName(myUrl);// ��ȡ��ʽ���ļ����ļ������磺base.css
				html = html.replaceAll(myUrl, folderName + "/" + myName);// �滻html�ļ��е���Դ�ļ�
				myUrl = joinUrlPath(url, myUrl);// �õ����յ���Դ�ļ�URL���磺http://www.hua.com/skins/default/css/base.css
				// showMsg("�����ؽ�����"+myUrl);
				// ȥ���ļ������Ϸ�����������Ϸ����ļ����ַ����кü���������ֻ����������
				if (!myName.contains("?") && !myName.contains("\"") && !myName.contains("/")) {
					if (downloadFile(myUrl, save_path + "/" + folderName + "/" + myName)) {// ��ʼ�����ļ�
						if (regx.startsWith("<img"))// ���������ǰ��ͼƬ�ļ�
							normalImageCount++;
						if (regx.startsWith("<script"))// ���������JS�ļ�
							jsCount++;
						if (regx.startsWith("<link")) {// ���������css�ļ�
							cssCount++;
							// ���ո����ص�CSS�ļ�ʵ����
							File cssFile = new File(save_path + "/" + folderName + "/" + myName);
							String txt = readFile(cssFile, chatset);// ��ȡCSS�ļ������ݣ�������Ĭ�ϵ�gb2312���� XXX
							// ��ʼƥ�䱳��ͼƬ
							Matcher matcherUrl = Pattern.compile("background:url\\(.*\\)").matcher(txt);
							while (matcherUrl.find()) {
								// ȥ��ǰ��ͺ���ı��,�õ��Ľ���磺../images/ico_4.gif
								String temp = matcherUrl.group().replaceAll("background:url\\(", "").replaceAll("\\)", "");
								// ƴ�ӳ�������ͼƬ·�����磺http://www.hua.com/skins/default/images/ico_4.gif
								String backgroundUrl = joinUrlPath(myUrl, temp);
								// ��ȡ����ͼƬ���ļ������磺ico_4.gif
								String backgroundFileName = getUrlFileName(backgroundUrl);
								// ����ͼƬҪ�����·�����磺c:/users\lxa\desktop\��ҳ\images\ico_4.gif
								String backgroundFilePath = save_path + "/" + sub_images + "/" + backgroundFileName;
								if (!new File(backgroundFilePath).exists()) {// ���������ͬ���ļ�
									if (downloadFile(backgroundUrl, backgroundFilePath)) {// ��ʼ���ر���ͼƬ
										backgroundImageCount++;// ������1
										showMsg("�ɹ����ر���ͼƬ��{0}", backgroundFileName);
									}
								} else {
									showMsg("ָ���ļ����Ѵ���ͬ���ļ�����Ϊ���Զ�������{0}", backgroundFilePath);
								}
							}
						}
					}
				}
			}
		}
	}

	private HttpURLConnection getURLConnection(String spec) throws IOException {
		HttpURLConnection result;
		if (null != proxy) {
			result = (HttpURLConnection) new URL(spec).openConnection(proxy);
			result.setRequestProperty("Content-Type", "text/html; charset=UTF-8"); // ���ô��ݵı��뷽ʽ
			result.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		} else
			result = (HttpURLConnection) new URL(spec).openConnection();
		// result.setRequestProperty("User-Agent", "MSIE 9.0");// ���ÿͻ��������ΪIE9
		return result;
	}

	/**
	 * ����ָ����URL����html����
	 * 
	 * @param pageURL
	 *            ��ҳ�ĵ�ַ
	 * @param encoding
	 *            ���뷽ʽ
	 * @return ������ҳ��html����
	 * @throws IOException
	 */
	private String getHTML(String pageURL, String encoding) throws IOException {
		StringBuffer pageHTML = new StringBuffer();
		HttpURLConnection connection = getURLConnection(pageURL);
		BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));
		String line = null;
		while ((line = br.readLine()) != null) {
			pageHTML.append(line);
			pageHTML.append("\r\n");
		}
		connection.disconnect();
		return pageHTML.toString();
	}

	/**
	 * ����URL����ĳ���ļ�
	 * 
	 * @param fileURL
	 *            ���ص�ַ
	 * @param filePath
	 *            ��ŵ�·��
	 */
	private boolean downloadFile(String fileURL, String filePath) {
		try {
			File file = new File(filePath);
			if (file.exists()) {
				// showMsg("�ļ��Ѵ��ڣ� {0}", filePath);
				return false;
			} else {
				file.createNewFile();
				// StringBuffer sb = new StringBuffer();
				HttpURLConnection connection = getURLConnection(fileURL);
				byte[] buffer = new byte[1024];
				InputStream is = connection.getInputStream();
				FileOutputStream fos = new FileOutputStream(file);
				int len = 0;
				while ((len = is.read(buffer)) != -1)
					fos.write(buffer, 0, len);
				fos.close();
				is.close();
				connection.disconnect();
				showMsg("�ɹ������ļ���	{0}", filePath);
				return true;
			}
		} catch (IOException e) {
			showMsg("���ļ������ڣ�	{0}	{1}", fileURL, e);
			return false;
		}
	}

	/**
	 * ��ȡĳ���ı��ļ�������
	 * 
	 * @param file
	 *            Ҫ��ȡ���ļ�
	 * @param encode
	 *            ��ȡ�ļ��ı��뷽ʽ
	 * @return ���ض�ȡ��������
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
	 * ��ȡURL����������ʵ�ļ���
	 * 
	 * @param url
	 *            �磺http://www.hua.com/bg.jpg
	 * @return ����bg.jpg
	 */
	private String getUrlFileName(String url) {
		return url.split("/")[url.split("/").length - 1];
	}

	/**
	 * ��ȡURL�����ļ�����·��
	 * 
	 * @param url
	 *            �磺http://www.hua.com/bg.jpg
	 * @return ���� http://www.hua.com
	 */
	private String getUrlPath(String url) {
		return url.replaceAll("/" + getUrlFileName(url), "");
	}

	/**
	 * ƴ��URL·�����ļ�����ע�⣺��../����/��ͷ��fileName��Ҫ��һ��Ŀ¼
	 * 
	 * @param url
	 *            �磺http://www.hua.com/product/9010753.html
	 * @param fileName
	 *            �磺../skins/default/css/base.css
	 * @return http://www.hua.com/skins/default/css/base.css
	 */
	private String joinUrlPath(String url, String fileName) {
		// showMsg("url:"+url);
		// showMsg("fileName:"+fileName);
		if (fileName.startsWith("http://") || fileName.startsWith("https://"))
			return fileName;
		// ���ȥ����http://��ǰ׺�󻹰�����/������˵��Ҫ��һ��Ŀ¼����ȥ����ǰ�ļ���
		if (url.replaceAll("http://", "").replaceAll("https://", "").contains("/"))
			url = getUrlPath(url);
		if (fileName.startsWith("../") || fileName.startsWith("/")) {
			// ֻ�е�ǰURL�������Ŀ¼���ܺ��ˣ����ֻ��http://www.hua.com������˶�����
			if (url.replaceAll("http://", "").replaceAll("https://", "").contains("/"))
				url = getUrlPath(url);
			fileName = fileName.substring(fileName.indexOf("/") + 1);
		}
		// showMsg("return:"+url+"/"+fileName);
		return url + "/" + fileName;
	}
}
