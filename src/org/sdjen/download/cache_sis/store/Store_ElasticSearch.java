package org.sdjen.download.cache_sis.store;

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
import java.util.Set;

import javax.annotation.Resource;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.tool.ZipUtil;
import org.sdjen.download.cache_sis.util.EntryData;
import org.sdjen.download.cache_sis.util.JsoupAnalysisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("Store_ElasticSearch")
public class Store_ElasticSearch implements IStore {
	boolean inited = false;
	@Autowired
	private HttpUtil httpUtil;
	@Resource(name = "Store_Mongodb")
	private IStore store;
	@Value("${siscache.conf.path_es_start}")
	private String path_es_start;
	@Value("${siscache.conf.url_fail_stop}")
	private int url_fail_stop = 10;
	@Value("${siscache.conf.url_fail_retry_begin}")
	private int url_fail_retry_begin = 5;
	@Value("${siscache.conf.url_fail_retry_in_hours}")
	private int url_fail_retry_in_hours = 3;
	private MessageDigest md5Digest;
	private static Set<String> proxy_urls;

	@Override
	public synchronized void init() throws Throwable {
		if (inited)
			return;
		md5Digest = MessageDigest.getInstance("MD5");
		{// html.context_zip字段不参与检索
			StringBuilder postStr = new StringBuilder();
			postStr.append(JsonUtil.toJson(ESMap.get().set("index", ESMap.get().set("_id", "test"))));
			postStr.append("\n{}\n");
			postStr.append(JsonUtil.toJson(ESMap.get().set("delete", ESMap.get().set("_id", "test"))));
			postStr.append("\n");
			String rst;
			try {
				rst = httpUtil.doLocalPostUtf8Json(path_es_start + "html/_doc/_bulk/", postStr.toString());
			} catch (Throwable e) {
				msg("ES未启动，10分钟后重试1次");
				Thread.sleep(600000);// 300000
				rst = httpUtil.doLocalPostUtf8Json(path_es_start + "html/_doc/_bulk/", postStr.toString());
			}
			msg(rst);
			msg("html:	" + httpUtil.doLocalPutUtf8Json(path_es_start + "html/_mapping/"//
					, JsonUtil.toJson(//
							ESMap.get()//
									.set("properties", ESMap.get()//
											.set("context_zip", ESMap.get()//
													.set("type", "binary")// text
													.set("index", false)//
//													.set("norms", false)//
//													.set("fields", ESMap.get()//
//															.set("keyword", ESMap.get()//
//																	.set("type", "keyword")//
//																	.set("ignore_above", 256)//
//															)//
//													)//
											)//
									)//
					)//
			));
			for (String index : new String[] { "last", "md", "urls_failed","test" }) {
				msg(index + ":	" + httpUtil.doLocalPostUtf8Json(path_es_start + index + "/_doc/_init_", "{}"));
			}
			String js = httpUtil.doLocalGet(path_es_start + "last/_doc/_search", new HashMap<>());
			ESMap r = JsonUtil.toObject(js, ESMap.class);
			List<ESMap> hits = (List<ESMap>) r.get("hits", ESMap.class).get("hits");
			if (null != hits)
				for (ESMap hit : hits) {
					ESMap _source = hit.get("_source", ESMap.class);
					if (null != _source && _source.containsKey("running") && (Boolean) _source.get("running")) {
						_source.put("running", false);
						String s = httpUtil.doLocalPostUtf8Json(path_es_start + "last/_doc/" + _source.get("type"),
								JsonUtil.toJson(_source));
						logger.info("~~~~~~~~~clean running:{}", s);
					}
				}
			inited = true;
		}
	}

	private Store_ElasticSearch() throws Exception {
		System.out.println(">>>>>>>>>>>>>>>>>>Store_ElasticSearch");
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
		String result = null;
		if (null != _source) {
			String zip = _source.get("context_zip", String.class);
			if (null != zip) {
				try {
					String context = ZipUtil.uncompress(ZipUtil.stringToBytes(zip));
					Map<String, Object> details = JsonUtil.toObject(context, Map.class);
					details.put("fid", (String) _source.get("fid"));
					details.put("type", (String) _source.get("type"));
					details.put("tid", id);
					details.put("id", id);
					result = JsoupAnalysisor.restoreToHtml(details);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (null == zip) {
				result = _source.get("context", String.class);
			}
		}
		if (null != result) {
			result = replaceLocalHtmlUrl(result).html();
		}
		return result;
	}

	@Override
	public Map<String, Object> saveHtml(final String id, final String page, final String url, String title,
			String dateStr, String text) throws Throwable {
		init();
		String key = getKey(id, page);
		org.jsoup.nodes.Document doument = Jsoup.parse(text);
		Map<String, Object> details = JsoupAnalysisor.split(doument);
		details.remove("id");
		details.remove("tid");
		Map<String, Object> data = new HashMap<>();
		data.put("id", Long.valueOf(id));
		data.put("page", Long.valueOf(page));
		if (StringUtils.isEmpty(title)) {
			title = (String) details.get("title");
			for (int i = 0; i < 2; i++) {
				int index = title.lastIndexOf(" - ");
				if (index > -1)
					title = title.substring(0, index);
			}
		}
		data.put("title", title);
		new EntryData<String, String>()//
				.put("fid", "fid")//
				.put("type", "type")//
				.put("context", "content_txt")//
				.getData().forEach((k_data, k_detail) -> {
					data.put(k_data, (String) details.get(k_detail));
					details.remove(k_detail);
				});
		for (Map<String, String> map : (List<Map<String, String>>) details.get("contents")) {
			if ("1楼".equals(map.get("floor"))) {
				Arrays.asList("datetime", "author", "level").forEach(k -> data.put(k, (String) map.get(k)));
				break;
			}
		}
		data.put("context_zip", ZipUtil.bytesToString(ZipUtil.compress(JsonUtil.toJson(details))));
		String r = httpUtil.doLocalPostUtf8Json(path_es_start + "html/_doc/" + key, JsonUtil.toJson(data));
		return data;
	}

	@Override
	public void saveURL(String url, String path) throws Throwable {
		init();
		Map<String, Object> json = new HashMap<>();
		String md5 = getMD5(url.getBytes("utf8"));
		json.put("key", md5);
		json.put("url", url);
		json.put("path", path);
		json.put("type", "url");
		String s = httpUtil.doLocalPostUtf8Json(path_es_start + "md/_doc/" + md5, JsonUtil.toJson(json));
	}

	@Override
	public void saveMD5(String md5, String path) throws Throwable {
		init();
		Map<String, Object> json = new HashMap<>();
		json.put("key", md5);
		json.put("path", path);
		json.put("type", "path");
		String s = httpUtil.doLocalPostUtf8Json(path_es_start + "md/_doc/" + md5, JsonUtil.toJson(json));
	}

	@Override
	public String getMD5_Path(String key) throws Throwable {
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
	public String getURL_Path(String key) throws Throwable {
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
	public Map<String, Object> getTitleList(String fid, int page, int size, String query, String order)
			throws Throwable {
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
		try {
			mustes.add(ESMap.get().set("term", Collections.singletonMap("fid", Integer.valueOf(fid))));
		} catch (NumberFormatException e1) {
		}
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
				List<ESMap> and = new ArrayList<>();
				List<ESMap> or = new ArrayList<>();
				List<ESMap> nor = new ArrayList<>();
				String[] ss = q.split(":");
				String field = ss[0].replace("'", "^");
				String vs = ss.length > 1 ? ss[1] : "";
				for (String v : vs.split(" ")) {
					if (v.isEmpty())
						continue;
					// String s = "and";
					List<ESMap> list = and;
					if (v.startsWith("~")) {
						v = v.substring(1);
						// s = "or";
						list = or;
					} else if (v.startsWith("-")) {
						v = v.substring(1);
						// s = "not";
						list = nor;
					}
					// System.out.println(s + " " + field + " " + v);
					ESMap item = ESMap.get()//
							.set("fields", Arrays.asList(field.split(",")));
					item.set("query", v);
					item.set("type", "phrase");
					list.add(ESMap.get().set("multi_match", item));
				}
				ESMap bool = ESMap.get();
				if (!and.isEmpty())
					bool.set("must", and);
				if (!or.isEmpty())
					bool.set("should", or);
				if (!nor.isEmpty())
					bool.set("must_not", nor);
				if (!bool.isEmpty())
					mustes.add(ESMap.get().set("bool", bool));
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

	public synchronized void addProxyUrl(String url) {
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

	public synchronized void removeProxyUrl(String url) {
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

	@Override
	public synchronized void logFailedUrl(String url, Throwable e) throws Throwable {
		init();
		url = cutForProxy(url);
		ESMap _source = JsonUtil.toObject(
				httpUtil.doLocalGet(path_es_start + "urls_failed/_doc/{key}",
						new EntryData<String, String>().put("key", getMD5(url.getBytes("utf8"))).getData()),
				ESMap.class).get("_source", ESMap.class);
		if (null == _source) {
			_source = ESMap.get();
			_source.put("count", 0);
		} else {
			_source.put("count", Integer.valueOf(_source.get("count").toString()) + 1);
		}
		_source.put("url", url);
		_source.put("msg", e.getMessage());
		_source.put("time", dateFormat.format(new Date()));
		httpUtil.doLocalPostUtf8Json(path_es_start + "urls_failed/_doc/" + getMD5(url.getBytes("utf8")),
				JsonUtil.toJson(_source));
	}

	@Override
	public synchronized void logSucceedUrl(String url) throws Throwable {
		init();
		url = cutForProxy(url);
		Map<String, Object> json = new HashMap<>();
		json.put("count", 0);
		json.put("url", url);
		json.put("time", dateFormat.format(new Date()));
		httpUtil.doLocalPostUtf8Json(path_es_start + "urls_failed/_doc/" + getMD5(url.getBytes("utf8")),
				JsonUtil.toJson(json));
	}

	@Override
	public synchronized Map<String, Object> connectCheck(String url) throws Throwable {
		init();
		Map<String, Object> result = new HashMap<>();
		result.put("found", false);
		result.put("continue", true);
		result.put("msg", "");
		url = cutForProxy(url);
		ESMap findOne = JsonUtil.toObject(
				httpUtil.doLocalGet(path_es_start + "urls_failed/_doc/{key}",
						new EntryData<String, String>().put("key", getMD5(url.getBytes("utf8"))).getData()),
				ESMap.class).get("_source", ESMap.class);
		if (null != findOne) {
			int count = Integer.valueOf(findOne.get("count").toString());
			result.put("found", count > 0);
			if (count > url_fail_stop) {
				if (System.currentTimeMillis() - ((Date) findOne.get("time")).getTime() < 3600000
						* url_fail_retry_in_hours) {
					result.put("continue", false);
					result.put("msg", url_fail_retry_in_hours + "小时内禁止连接：" + findOne.get("msg"));
				} else {
					Map<String, Object> json = new HashMap<>();
					json.put("count", url_fail_retry_begin);
					json.put("url", url);
					json.put("time", dateFormat.format(new Date()));
					httpUtil.doLocalPostUtf8Json(path_es_start + "urls_failed/_doc/" + getMD5(url.getBytes("utf8")),
							JsonUtil.toJson(json));
				}
			}
		}
		return result;
	}

	@Override
	public synchronized Map<String, Object> getLast(String type) throws Throwable {
		ESMap _source = JsonUtil
				.toObject(httpUtil.doLocalGet(path_es_start + "last/_doc/{type}",
						new EntryData<String, String>().put("type", type).getData()), ESMap.class)
				.get("_source", ESMap.class);
		if (null != _source) {
			Map<String, Object> json = new HashMap<>();
			json.put("type", type);
			json.put("keyword", _source.get("keyword"));
			json.put("running", _source.get("running"));
			json.put("msg", _source.get("msg"));
			json.put("time", dateFormat.parse(_source.get("time", String.class)));
			return json;
		}
		return null;
	}

	@Override
	public synchronized Object running(String type, String keyword, String msg) throws Throwable {
		init();
		Map<String, Object> json = new HashMap<>();
		json.put("type", type);
		json.put("keyword", keyword);
		json.put("running", true);
		json.put("msg", msg);
		json.put("time", dateFormat.format(new Date()));
		String s = httpUtil.doLocalPostUtf8Json(path_es_start + "last/_doc/" + type, JsonUtil.toJson(json));
		return s;
	}

	@Override
	public synchronized Object finish(String type, String msg) throws Throwable {
		init();
		Map<String, Object> json = new HashMap<>();
		json.put("type", type);
		json.put("running", false);
		json.put("msg", msg);
		json.put("time", dateFormat.format(new Date()));
		String s = httpUtil.doLocalPostUtf8Json(path_es_start + "last/_doc/" + type, JsonUtil.toJson(json));
		return s;
	}
}
