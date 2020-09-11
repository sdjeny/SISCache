package org.sdjen.download.cache_sis.store;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.log.LogUtil;
import org.sdjen.download.cache_sis.test.GetConnection;
import org.sdjen.download.cache_sis.tool.ZipUtil;

public class Store_ElasticSearch implements IStore {

	private static IStore store;

	public static IStore getStore() throws Exception {
		if (null == store)
			store = new Store_ElasticSearch();
		return store;
	}

	GetConnection connection;
	private String path_es_start = "http://192.168.0.237:9200/siscache_";
	private MessageDigest md5Digest;
	public LogUtil msgLog;

	private String logName;
	private String charset;
	private ConfUtil conf;

	public ConfUtil getConf() throws IOException {
		if (null == conf) {
			conf = ConfUtil.getDefaultConf();
		}
		return conf;
	}

	public String getCharset() {
		if (null == charset) {
			try {
				charset = getConf().getProperties().getProperty("chatset");
			} catch (IOException e) {
				logName = "GBK";
			}
		}
		return charset;
	}

	public String getLogName() {
		if (null == logName) {
			try {
				String save_path = getConf().getProperties().getProperty("save_path");
				logName = save_path + "/download.log";
			} catch (IOException e) {
				logName = "/download.log";
			}
		}
		return logName;
	}

	private Store_ElasticSearch() throws Exception {
		connection = GetConnection.getConnection();
		md5Digest = MessageDigest.getInstance("MD5");
		ConfUtil conf = getConf();
		boolean isStore = false;
		String temp = conf.getProperties().getProperty("path_es_start");
		if (null == temp) {
			conf.getProperties().setProperty("path_es_start", path_es_start);
			isStore = true;
		} else {
			path_es_start = temp;
		}
		if (isStore)
			conf.store();
		refreshMsgLog();
		{// html.context_zip字段不参与检索
			StringBuilder postStr = new StringBuilder();
			postStr.append(JsonUtil.toJson(ESMap.get().set("index", ESMap.get().set("_id", "test"))));
			postStr.append("\n{}\n");
			postStr.append(JsonUtil.toJson(ESMap.get().set("delete", ESMap.get().set("_id", "test"))));
			postStr.append("\n");
			String rst;
			try {
				rst = connection.doPost(path_es_start + "html/_doc/_bulk/", postStr.toString(), new HashMap<>());
			} catch (Exception e) {
				msg("ES未启动，5分钟后重试1次");
				Thread.sleep(300000);
				rst = connection.doPost(path_es_start + "html/_doc/_bulk/", postStr.toString(), new HashMap<>());
			}
			msg(rst);
			rst = connection.doPost(path_es_start + "html/_doc/_mapping/"//
					, JsonUtil.toJson(//
							ESMap.get()//
									.set("properties", ESMap.get()//
											.set("context_zip", ESMap.get()//
													.set("type", "text")//
													.set("index", false)//
													.set("norms", false)//
													.set("fields", ESMap.get()//
															.set("keyword", ESMap.get()//
																	.set("type", "keyword")//
																	.set("ignore_above", 256)//
															)//
													)//
											)//
									)//
					)//
					, new HashMap<>());
			msg(rst);
		}
	}

	private String getKey(String id, String page, String url, String title, String dateStr) {
		return id + "_" + page;
	}

	@Override
	public String getLocalHtml(final String id, final String page, final String url, String title, String dateStr)
			throws Throwable {
		String key = getKey(id, page, url, title, dateStr);
		String ss = connection.doGet(path_es_start + "html/_doc/" + key, new HashMap<>(), new HashMap<>());
		ESMap esMap = JsonUtil.toObject(ss, ESMap.class);
		ESMap _source = esMap.get("_source", ESMap.class);
		if (null != _source) {
			if (1 != Integer.valueOf(_source.get("page").toString()))
				return "";
			String text = _source.get("context_zip", String.class);
			if (null != text) {
				try {
					return ZipUtil.uncompress(text);
				} catch (DataFormatException e1) {
					e1.printStackTrace();
					text = null;
				}
			}
			if (null == text) {
				return _source.get("context", String.class);
			}
		}
		return null;
	}

	@Override
	public void saveHtml(final String id, final String page, final String url, String title, String dateStr,
			String text) throws Throwable {
		String key = getKey(id, page, url, title, dateStr);
		org.jsoup.nodes.Document doument = Jsoup.parse(text);
		org.jsoup.nodes.Element h1 = doument.select("div.mainbox").select("h1").first();
		if (null == h1)
			throw new Exception("Lost title");
//		org.jsoup.nodes.Element pages = doument.select("div.pages_btns").select("div.pages").first();
		String type = h1.select("a").text();
//		String title = h1.ownText();
//		String id = doument.select("div.mainbox").select("span.headactions").select("a[href]").first().attr("href")
//				.replace("viewthread.php?action=printable&tid=", "");
//		String page = null == pages ? "1" : pages.select("strong").text();
		String dat = null;
		String context = null;
		String author = null;
		if (false) {
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
		}
		for (org.jsoup.nodes.Element tbody : doument.select("div.mainbox.viewthread")//// class=mainbox的div
				.select("table")//
				.select("tbody")//
				.select("tr")//
		//
		) {

			String floor = "";
			for (org.jsoup.nodes.Element postinfo : tbody.select("td.postcontent").select("div.postinfo")) {
				org.jsoup.nodes.Element temp = postinfo.select("strong").first();
				if (null != temp) {
					floor = temp.ownText();
					if ("1楼".equals(floor)) {
						dat = postinfo.ownText().replace("发表于 ", "");
						for (org.jsoup.nodes.Element postauthor : tbody.select("td.postauthor").select("cite")
								.select("a[href]")) {
							author = postauthor.text();
						}
					}
				}
			}
			if (null != author)
				break;
		}
		ESMap comments = ESMap.get();
		{
			for (org.jsoup.nodes.Element postcontent : doument.select("div.mainbox.viewthread")//// class=mainbox的div
					.select("table")//
					.select("tbody")//
					.select("tr")//
					.select("td.postcontent")//
			//
			) {
				String floor = "";
				for (org.jsoup.nodes.Element postinfo : postcontent.select("div.postinfo")) {
					org.jsoup.nodes.Element temp = postinfo.select("strong").first();
					if (null != temp) {
						floor = temp.ownText();
						if ("1楼".equals(floor)) {
							dat = postinfo.ownText().replace("发表于 ", "");
						}
					}
				}
				if (floor.isEmpty())
					continue;
				if ("1楼".equals(floor)) {
					for (org.jsoup.nodes.Element comment : postcontent.select("div.postmessage.defaultpost")) {
						context = comment.html();
					}
				} else {
					String fm = comments.get(floor, String.class);
					if (null == fm)
						fm = "";
					for (org.jsoup.nodes.Element comment : postcontent.select("div.postmessage.defaultpost")
							.select("div.t_msgfont")) {
						if (!fm.isEmpty())
							fm += ",";
						fm += comment.text();
					}
					comments.set(floor, fm);
				}
			}
		}

		boolean update = false;

		for (org.jsoup.nodes.Element e : doument.select("head").select("style")) {
			// if (!e.text().isEmpty()) {
			update = true;
			e.text("");
			// }
		}
		for (org.jsoup.nodes.Element e : doument.select("head").select("script")) {
			update = true;
			e.remove();
		}
		if (update)
			text = doument.html();
		key = id + "_" + page;
		Map<String, Object> json = new HashMap<>();
		json.put("id", Long.valueOf(id));
		json.put("page", Long.valueOf(page));
		json.put("context_comments", comments);
		json.put("title", title);
		if (page.equals("1")) {
			json.put("datetime", dat);
			try {
				if (null != dat) {
					SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
					Date dt = dtf.parse(dat);
					json.put("datetime", dtf.format(dt));
					// json.put("date", new
					// SimpleDateFormat("yyyy-MM-dd").parse(dat));
					// json.put("datetime", dt);
				}
			} catch (Exception e1) {
			}
			json.put("fid", 143);
			json.put("type", type);
			json.put("context", context);
			json.put("context_zip", ZipUtil.compress(text));
			json.put("author", author);
		}
		String r = connection.doPost(path_es_start + "html/_doc/" + key, JsonUtil.toJson(json), new HashMap<>());
	}

	@Override
	public void saveURL(String url, String path) throws IOException {
		Map<String, Object> json = new HashMap<>();
		json.put("url", url);
		json.put("path", path);
		json.put("type", "url");
		String s = connection.doPost(path_es_start + "md/_doc/" + getMD5(url.getBytes("utf8")), JsonUtil.toJson(json),
				new HashMap<>());
	}

	@Override
	public void saveMD5(String md5, String path) throws IOException {
		Map<String, Object> json = new HashMap<>();
		json.put("key", md5);
		json.put("path", path);
		json.put("type", "path");
		String s = connection.doPost(path_es_start + "md/_doc/" + md5, JsonUtil.toJson(json), new HashMap<>());
	}

	@Override
	public String getMD5_Path(String key) throws Exception {
		String ss = connection.doGet(path_es_start + "md/_doc/" + key, new HashMap<>(), new HashMap<>());
		ESMap esMap = JsonUtil.toObject(ss, ESMap.class);
		ESMap _source = esMap.get("_source", ESMap.class);
		if (null != _source) {
			return _source.get("path", String.class);
		}
		return null;
	}

	private synchronized String getMD5(byte[] bytes) {
		return new BigInteger(1, md5Digest.digest(bytes)).toString(Character.MAX_RADIX);
	}

	@Override
	public String getURL_Path(String key) throws IOException {
		String ss = connection.doGet(path_es_start + "md/_doc/" + getMD5(key.getBytes("utf8")), new HashMap<>(),
				new HashMap<>());
		ESMap esMap = JsonUtil.toObject(ss, ESMap.class);
		ESMap _source = esMap.get("_source", ESMap.class);
		if (null != _source) {
			return _source.get("path", String.class);
		}
		return null;
	}

	@Override
	public void msg(Object pattern, Object... args) {
		msgLog.showMsg(pattern, args);
	}

	@Override
	public void err(Object pattern, Object... args) {
		msg(pattern, args);
	}

	@Override
	public void refreshMsgLog() {
		try {
			File file = new File(getLogName());
			if (file.exists()) {
				if (file.length() > 0) {// 有内容才备份
					File dest = new File(
							getLogName().replace("/download.log", "/download_" + System.currentTimeMillis() + ".log"));
					if (!dest.exists())
						dest.createNewFile();
					file.renameTo(dest);
					file.delete();
				}
			}
			if (null != msgLog) {
				try {
					Thread.sleep(30000l);// 休息半分钟够了，有重试机制
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				msgLog.finish();
			}
			msgLog = new LogUtil().setLogFile(getLogName()).setChatset(getCharset());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
