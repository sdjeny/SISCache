package org.sdjen.download.cache_sis.tool;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.es.GetConnection;

import com.google.gson.Gson;

public class File2DB {
	static Logger logger = Logger.getLogger(File2DB.class.getName());

	public static void main(String[] args) throws Throwable {
		logger.log(Level.INFO, Arrays.asList(args).toString());
		File root = new File(null != args && args.length > 0 ? args[0] : "\\\\192.168.0.233/d/SISCACHE/html");
		File2DB file2db = new File2DB();
		file2db.listFiles(root);
	}

	private void listFiles(File file) throws Throwable {
		if (file.isDirectory()) {
			for (File subFile : file.listFiles(new FileFilter() {

				@Override
				public boolean accept(File path) {
					return path.isDirectory() || path.getName().toLowerCase().endsWith(".html");
				}
			})) {
				listFiles(subFile);
			}
		} else {
			String text = getContext(file);
			try {
				analyticalHtml(text);
			} catch (Exception e) {
				logger.log(Level.SEVERE, file.getPath(), e);
			}
			// System.out.println(text);
		}
	}

	private void analyticalHtml(String text) throws Exception {
		org.jsoup.nodes.Document doument = Jsoup.parse(text);

		// for (org.jsoup.nodes.Element e : doument.select("title")) {
		// System.out.println(e.text());
		// break;
		// }
		// try {
		// System.out.println(doument.select("div#foruminfo").select("div#nav").first().text());
		// } catch (Exception e1) {
		// }
		org.jsoup.nodes.Element h1 = doument.select("div.mainbox").select("h1").first();
		if (null == h1)
			throw new Exception("Lost title");
		org.jsoup.nodes.Element pages = doument.select("div.pages_btns").select("div.pages").first();
		String type = h1.select("a").text();
		String title = h1.ownText();
		String id = doument.select("div.mainbox").select("span.headactions").select("a[href]").first().attr("href")
				.replace("viewthread.php?action=printable&tid=", "");
		String page = null == pages ? "1" : pages.select("strong").text();
		String dat = null;
		String f = "1";
		for (org.jsoup.nodes.Element e : doument.select("div.mainbox")//// class=mainbox的div
				.select("table")//
				.select("tbody")//
				.select("tr")//
				.select("td.postcontent")//
				.select("div.postinfo")//
		) {
			f = e.select("strong").first().ownText().replace("楼", "");
			if ("1".equals(f))
				dat = e.ownText().replace("发表于 ", "");
		}
		logger.log(Level.INFO, MessageFormat.format("{0}	{1}	{2}	{3}	{4}", id, dat, type, title, page));
		String key = id+"_"+page;
		Gson gson = new Gson();
		Map<String, Object> json = new HashMap<>();
		json.put("id", id);
		json.put("dat", dat);
		json.put("type", type);
		json.put("title", title);
		json.put("page", page);
		json.put("context", text);
		GetConnection.doPost("http://192.168.0.237:9200/test/a/"+key, gson.toJson(json), Collections.singletonMap("Content-Type", "application/json"));
	}

	private String getContext(File file) throws Throwable {
		String charset = "GBK";
		long fileByteLength = file.length();
		byte[] content = new byte[(int) fileByteLength];
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(content);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fileInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new String(content, charset);
	}
}
