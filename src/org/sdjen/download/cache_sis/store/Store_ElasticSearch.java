package org.sdjen.download.cache_sis.store;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.DataFormatException;

import javax.annotation.Resource;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.tool.ZipUtil;
import org.sdjen.download.cache_sis.util.EntryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("Store_ElasticSearch")
public class Store_ElasticSearch implements IStore {
	private final static Logger logger = LoggerFactory.getLogger(Store_ElasticSearch.class);
	@Autowired
	private HttpUtil httpUtil;
	@Resource(name = "Store_Mongodb")
	private IStore store;
	private String path_es_start = "http://192.168.0.237:9200/siscache_";
	private MessageDigest md5Digest;
	private String logName;
	private String charset;
	private ConfUtil conf;
	private static Set<String> proxy_urls;

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
		System.out.println(">>>>>>>>>>>>>>>>>>Store_ElasticSearch");
//		connection = GetConnection.getConnection();
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
				try {
					rst = httpUtil.doLocalPostUtf8Json(path_es_start + "html/_doc/_bulk/", postStr.toString());
				} catch (Exception e) {
					msg("ES未启动，5分钟后重试1次");
					Thread.sleep(300);// 300000
					rst = httpUtil.doLocalPostUtf8Json(path_es_start + "html/_doc/_bulk/", postStr.toString());
				}
				msg(rst);
				rst = httpUtil.doLocalPostUtf8Json(path_es_start + "html/_doc/_mapping/"//
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
				);
				msg(rst);
			} catch (Exception e) {
				msg("ES启动失败");
			}
		}
	}

	private String getKey(String id, String page) {
		return id + "_" + page;
	}

	@Override
	public String getLocalHtml(final String id, final String page) throws Throwable {
		String key = getKey(id, page);
		String ss = httpUtil.doLocalGet(path_es_start + "html/_doc/{key}",
				new EntryData<String, String>().put("key", key).getData());
		ESMap esMap = JsonUtil.toObject(ss, ESMap.class);
		ESMap _source = esMap.get("_source", ESMap.class);
		if (null != _source) {
			if (page.compareTo("1") > 0) {
				StringBuffer rst = new StringBuffer();
				rst.append("</br><table border='0'>");
				for (Entry<Object, Object> e : _source.get("context_comments", ESMap.class).entrySet()) {
					rst.append("<tbody><tr>");
					rst.append(String.format("<td>%s</td>", e.getKey()));
					rst.append(String.format("<td>%s</td>", e.getValue()));
					rst.append("</tr></tbody>");
				}
				rst.append("</table>");
				return rst.toString();
			} else {
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
		}
		return null;
	}

	@Override
	public void saveHtml(final String id, final String page, final String url, String title, String dateStr,
			String text) throws Throwable {
		String key = getKey(id, page);
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
			json.put("context_zip", ZipUtil.bytesToString(ZipUtil.compress(text)));
			json.put("author", author);
		}
		String r = httpUtil.doLocalPostUtf8Json(path_es_start + "html/_doc/" + key, JsonUtil.toJson(json));
	}

	@Override
	public void saveURL(String url, String path) throws IOException {
		Map<String, Object> json = new HashMap<>();
		json.put("url", url);
		json.put("path", path);
		json.put("type", "url");
		String s = httpUtil.doLocalPostUtf8Json(path_es_start + "md/_doc/" + getMD5(url.getBytes("utf8")),
				JsonUtil.toJson(json));
	}

	@Override
	public void saveMD5(String md5, String path) throws IOException {
		Map<String, Object> json = new HashMap<>();
		json.put("key", md5);
		json.put("path", path);
		json.put("type", "path");
		String s = httpUtil.doLocalPostUtf8Json(path_es_start + "md/_doc/" + md5, JsonUtil.toJson(json));
	}

	@Override
	public String getMD5_Path(String key) throws Exception {
		String ss = httpUtil.doLocalGet(path_es_start + "md/_doc/{key}",
				new EntryData<String, String>().put("key", key).getData());
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
		String ss = httpUtil.doLocalGet(path_es_start + "md/_doc/{key}",
				new EntryData<String, String>().put("key", getMD5(key.getBytes("utf8"))).getData());
		ESMap esMap = JsonUtil.toObject(ss, ESMap.class);
		ESMap _source = esMap.get("_source", ESMap.class);
		if (null != _source) {
			return _source.get("path", String.class);
		}
		return null;
	}

	@Override
	public Map<String, Object> getTitleList(int page, int size, String query, String order) throws Throwable {
		List<Map<String, Object>> ls = new ArrayList<>();
		Map<String, Object> result = new HashMap<>();
		result.put("list", ls);
		Map<Object, Object> params = ESMap.get();
		params.put("_source", ESMap.get()//
				.set("includes", Arrays.asList())//
				.set("excludes", Arrays.asList("context*"))//
		);
		if (null == query)
			query = "";
		if (null == order)
			order = "";
		List<ESMap> shoulds = new ArrayList<>();
		List<ESMap> mustes = new ArrayList<>();
		List<ESMap> mustNots = new ArrayList<>();
		if (query.isEmpty()) {
			mustes.add(ESMap.get().set("term", Collections.singletonMap("page", 1)));
			order = "id:desc";
		} else if (query.toUpperCase().startsWith("D:")) {
			query = query.substring(2);
			mustes.add(ESMap.get().set("match_phrase", Collections.singletonMap("datetime", query)));
			order = "id:desc";
		} else if (query.toUpperCase().startsWith("ALL:")) {
			query = query.substring(4);
			mustes.add(ESMap.get().set("term", ESMap.get().set("page", 1)));
			for (String type : new String[] { "best_fields", "most_fields", "cross_fields" }) {
				ESMap item = ESMap.get()//
						.set("fields", Arrays.asList("title^3", "context"));
				item.set("query", query);
				item.set("boost", 1);
				item.set("type", type);
				shoulds.add(ESMap.get().set("multi_match", item));
			}
		} else {
			for (String q : query.split(";")) {
				if (q.isEmpty())
					continue;
				String[] ss = q.split(":");
				String field = ss[0].replace("'", "^");
				String vs = ss.length > 1 ? ss[1] : "";
				for (String v : vs.split(" ")) {
					if (v.isEmpty())
						continue;
					// String s = "and";
					List<ESMap> list = mustes;
					if (v.startsWith("~")) {
						v = v.substring(1);
						// s = "or";
						list = shoulds;
					} else if (v.startsWith("-")) {
						v = v.substring(1);
						// s = "not";
						list = mustNots;
					}
					// System.out.println(s + " " + field + " " + v);
					ESMap item = ESMap.get()//
							.set("fields", Arrays.asList(field.split(",")));
					item.set("query", v);
					item.set("type", "phrase");
					list.add(ESMap.get().set("multi_match", item));
				}
			}
		}
		params.put("query"//
				, ESMap.get().set("bool", ESMap.get()//
						.set("must", mustes)//
						.set("should", shoulds)//
						.set("must_not", mustNots)//
				)//
		);
		params.put("size", size);
		params.put("from", (page - 1) * size);
		List<ESMap> orders = new ArrayList<>();
		for (String o : order.split(" ")) {
			if (o.isEmpty())
				continue;
			String[] ss = o.split(":");
			orders.add(//
					ESMap.get().set(ss[0], Collections.singletonMap("order", ss.length > 1 ? ss[1] : "desc"))//
			);
		}
		if (!orders.isEmpty())
			params.put("sort", orders);
		String jsonParams = JsonUtil.toJson(params);
		result.put("json_params", jsonParams);
		String js = httpUtil.doLocalPostUtf8Json(path_es_start + "html/_doc/_search", jsonParams);
		ESMap r = JsonUtil.toObject(js, ESMap.class);
		result.put("total", r.get("hits", ESMap.class).get("total"));
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		SimpleDateFormat dformat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat tformat = new SimpleDateFormat("HH:mm");
		List<ESMap> hits = (List<ESMap>) r.get("hits", ESMap.class).get("hits");
		for (ESMap hit : hits) {
			ESMap _source = hit.get("_source", ESMap.class);
			String datestr, timestr;
			try {
				Date date = format.parse((String) _source.get("datetime"));
				datestr = dformat.format(date);
				timestr = tformat.format(date);
			} catch (Exception e) {
				datestr = (String) _source.get("date_str");
				timestr = "    ";
			}
			ls.add(new EntryData<String, Object>()//
					.put("date", datestr)//
					.put("time", timestr)//
					.put("id", _source.get("id"))//
					.put("page", _source.get("page"))//
					.put("type", _source.get("type"))//
					.put("title", _source.get("title"))//
					.getData());
		}
		return result;
	}

	@Override
	public void refreshMsgLog() {
	}

	public synchronized Set<String> getProxyUrls() {
		if (null == proxy_urls) {
			proxy_urls = new HashSet<>();
			try {
				ConfUtil conf = ConfUtil.getDefaultConf();
				for (String s : conf.getProperties().getProperty("proxy_urls").split(",")) {
					proxy_urls.add(s.trim());
				}
				proxy_urls.remove("");
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return proxy_urls;
	}

	public void addProxyUrl(String url) {
		try {
			ConfUtil conf = ConfUtil.getDefaultConf();
			String proxy_url = cutForProxy(url);
			if (!proxy_url.isEmpty() && !proxy_urls.contains(proxy_url)) {
				proxy_urls.add(proxy_url);
				conf.getProperties().setProperty("proxy_urls",
						conf.getProperties().getProperty("proxy_urls") + "," + proxy_url);
				conf.store();
				msg(">>>>>>>>>ADD:	{0}", proxy_url);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public void removeProxyUrl(String url) {
		try {
			ConfUtil conf = ConfUtil.getDefaultConf();
			String proxy_url = cutForProxy(url);
			if (!proxy_url.isEmpty() && proxy_urls.contains(proxy_url)) {
				proxy_urls.remove(proxy_url);
				conf.getProperties().setProperty("proxy_urls",
						conf.getProperties().getProperty("proxy_urls") + "," + proxy_url);
				conf.store();
				msg(">>>>>>>>>remove:	{0}", proxy_url);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
