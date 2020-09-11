package org.sdjen.download.cache_sis.tool;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.test.GetConnection;

public class File2DB {
	static Log logger = LogFactory.getLog(File2DB.class.getClass());
	// private org.sdjen.download.cache_sis.log.CassandraFactory
	// cassandraFactory;

	public static void main(String[] args) throws Throwable {
		// PropertyConfigurator.configure("config/log4j.properties");
		// System.setProperty("org.apache.commons.logging.Log",
		// "org.apache.commons.logging.impl.SimpleLog");
		// System.setProperty("org.apache.commons.logging.simplelog.showdatetime",
		// "true");
		// System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient",
		// "stdout");
		// System.out.println(logger);
		logger.debug(Arrays.asList(args).toString());
		File root = new File(null != args && args.length > 0 ? args[0] : "\\\\192.168.0.233/d/SISCACHE/html");
		File2DB file2db = new File2DB();
		file2db.listFiles(root);
		file2db.finish();
	}

	GetConnection connection;

	public File2DB() throws IOException {
		connection = new GetConnection();
	}

	private void finish() {
		connection.finish();
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
				logger.error(file.getPath(), e);
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
		logger.info(MessageFormat.format("{0}	{1}	{2}	{3}	{4}", id, dat, type, title, page));
		boolean update = false;
		{

			for (org.jsoup.nodes.Element e : doument.select("a[href]")) {
				String href = e.attr("href");
				if (!href.startsWith("../"))
					continue;
				if (href.startsWith("../../../torrent/m")) {
					update = true;
					href = href.substring(3);
					e.attr("href", href);
				}
				final String t = e.text();
				// logger.info(t + " : " + href + " : " + e);
			}
			for (org.jsoup.nodes.Element e : doument.select("img[src]")) {
				String src = e.attr("src");
				if (!src.startsWith("../"))
					continue;
				if (src.startsWith("../../../images/201")) {
					update = true;
					src = src.substring(3);
					e.attr("src", src);
				}
				String md5 = src.substring(src.lastIndexOf("/") + 1, src.lastIndexOf("."));
				String path = src.replace("../", "");
				Map<String, Object> json = new HashMap<>();
				json.put("key", md5);
				json.put("path", path);
				json.put("type", "path");
				// logger.info(gson.toJson(json));
				String ss = connection.doGet("http://192.168.0.237:9200/test_md/_doc/" + md5, new HashMap<>(), new HashMap<>());
				ESMap esMap = JsonUtil.toObject(ss, ESMap.class);
				if (!esMap.containsKey("found") || !esMap.get("found", Boolean.class)) {
					String s = connection.doPost("http://192.168.0.237:9200/test_md/_doc/" + md5, JsonUtil.toJson(json), new HashMap<>());
					// logger.info(s);
				}
			}
		}
		if (update)
			text = doument.html();
		String key = id + "_" + page;
		Map<String, Object> json = new HashMap<>();
		json.put("id", id);
		if (null != dat) {
			json.put("date_str", dat);
			json.put("date", new SimpleDateFormat("yyyy-MM-dd").parse(dat));
			json.put("datetime", new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(dat));
		}
		json.put("fid", 143);
		json.put("type", type);
		json.put("title", title);
		json.put("page", page);
		json.put("context", text);
		String r = connection.doPost("http://192.168.0.237:9200/test_html/_doc/" + key, JsonUtil.toJson(json), new HashMap<>());
	}

	// public String getURL_Path(String path) {
	// ResultSet resultSet = cassandraFactory.getSession().execute("select key
	// from url_path where path='"
	// +path+ "' allow filtering");
	// for (Row row : resultSet) {
	// return row.getString("key");
	// }
	// return null;
	// }

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
