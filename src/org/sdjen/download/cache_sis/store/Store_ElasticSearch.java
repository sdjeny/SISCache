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
import java.util.Set;

import javax.annotation.Resource;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.configuration.ConfigMain;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.store.entity.Last;
import org.sdjen.download.cache_sis.store.entity.Urls_failed;
import org.sdjen.download.cache_sis.store.entity.Urls_proxy;
import org.sdjen.download.cache_sis.tool.ZipUtil;
import org.sdjen.download.cache_sis.util.EntryData;
import org.sdjen.download.cache_sis.util.JsoupAnalysisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException.NotFound;

import com.fasterxml.jackson.core.JsonProcessingException;

@Service("Store_ElasticSearch")
public class Store_ElasticSearch implements IStore {
	boolean inited = false;
	@Autowired
	private HttpUtil httpUtil;
	@Resource(name = "Store_Mongodb")
	private IStore store;
	@Autowired
	private ConfigMain configMain;
	private MessageDigest md5Digest;
	private static Set<String> proxy_urls;
	private Map<String, Object> checkConnectUrls;
	@Autowired
	private Dao dao;

	@Override
	public synchronized void init() throws Throwable {
		if (inited)
			return;
		proxy_urls = new HashSet<>();
		dao.getList("select url from Urls_proxy", new HashMap<>()).forEach(u -> proxy_urls.add((String) u));
		logger.info("~~~~~~~~~clean running:{}", dao.executeUpdate("update Last set running=:false where running=:true",
				new EntryData<String, Object>().put("true", true).put("false", false).getData()));
		checkConnectUrls = new HashMap<>();
		md5Digest = MessageDigest.getInstance("MD5");
		{// html.context_zip字段不参与检索
			StringBuilder postStr = new StringBuilder();
			postStr.append(JsonUtil.toJson(ESMap.get().set("index", ESMap.get().set("_id", "test"))));
			postStr.append("\n{}\n");
			postStr.append(JsonUtil.toJson(ESMap.get().set("delete", ESMap.get().set("_id", "test"))));
			postStr.append("\n");
			String rst;
			try {
				rst = httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + "html/_doc/_bulk/", postStr.toString());
			} catch (Throwable e) {
				msg("ES未启动，10分钟后重试1次");
				Thread.sleep(600000);// 300000
				rst = httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + "html/_doc/_bulk/", postStr.toString());
			}
			msg(rst);
			for (String index : new String[] { "md", "html"
//					, "last", "urls_failed", "test"
			}) {
				msg(index + ":	" + httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + index + "/_doc/_init_", "{}"));
			}
			try {
				msg("number_of_replicas:	" + httpUtil.doLocalPutUtf8Json("http://192.168.0.237:9200/_settings",
						JsonUtil.toJson(Collections.singletonMap("number_of_replicas", 0))));
			} catch (Throwable e) {
				e.printStackTrace();
			}
			try {
				msg("html:	" + httpUtil.doLocalPutUtf8Json(configMain.getPath_es_start() + "html/_doc/_mapping/"//
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
			} catch (Throwable e) {
				msg("html:	" + httpUtil.doLocalPutUtf8Json(configMain.getPath_es_start() + "html/_doc/_mapping/?include_type_name=true"//
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

	private String returnToHtml(String id, ESMap _source) throws IOException {
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
					if (StringUtils.isEmpty(details.get("title")))
						details.put("title", (String) _source.get("title"));
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
	public String getLocalHtml(final String id, final String page) throws Throwable {
		long l = System.currentTimeMillis();
		String key = getKey(id, page);
		ESMap _source;
		try {
			_source = JsonUtil
					.toObject(httpUtil.doLocalGet(configMain.getPath_es_start() + "html/_doc/{key}",
							new EntryData<String, String>().put("key", key).getData()), ESMap.class)
					.get("_source", ESMap.class);
		} catch (NotFound notFound) {
			_source = null;
		}
		l = System.currentTimeMillis() - l;
		long r = System.currentTimeMillis();
		String result = returnToHtml(id, _source);
		r = System.currentTimeMillis() - r;
		msg("getLocalHtml lookup:{0}	return:{1}	({2}_{3}){4}", l, r, id, page,
				null == _source ? "Find nothing" : _source.get("title"));
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
		String r = httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + "html/_doc/" + key, JsonUtil.toJson(data));
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
		String s = httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + "md/_doc/" + md5, JsonUtil.toJson(json));
	}

	@Override
	public void saveMD5(String md5, String path) throws Throwable {
		init();
		Map<String, Object> json = new HashMap<>();
		json.put("key", md5);
		json.put("path", path);
		json.put("type", "path");
		String s = httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + "md/_doc/" + md5, JsonUtil.toJson(json));
	}

	@Override
	public String getMD5_Path(String key) throws Throwable {
		ESMap _source;
		try {
			_source = JsonUtil.toObject(
					httpUtil.doLocalGet(configMain.getPath_es_start() + "md/_doc/{key}", Collections.singletonMap("key", key)),
					ESMap.class).get("_source", ESMap.class);
		} catch (NotFound notFound) {
			_source = null;
		}
		if (null != _source) {
			return _source.get("path", String.class);
		}
		return null;
	}

	private synchronized String getMD5(byte[] bytes) {
		return new BigInteger(1, md5Digest.digest(bytes)).toString(Character.MAX_RADIX);
	}

	@Override
	public String getURL_Path(String url) throws Throwable {
		ESMap _source;
		try {
			_source = JsonUtil.toObject(
					httpUtil.doLocalGet(configMain.getPath_es_start() + "md/_doc/{key}",
							new EntryData<String, String>().put("key", getMD5(url.getBytes("utf8"))).getData()),
					ESMap.class).get("_source", ESMap.class);
		} catch (NotFound notFound) {
			_source = null;
		}
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
		if (!"ALL".equalsIgnoreCase(fid))
			mustes.add(ESMap.get().set("term", Collections.singletonMap("fid", fid)));
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
		String js = httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + "html/_doc/_search", jsonParams);
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
					.put("fid", _source.get("fid"))//
					.put("id", _source.get("id"))//
					.put("page", _source.get("page"))//
					.put("type", _source.get("type"))//
					.put("title", _source.get("title"))//
					.getData());
		}
		return result;
	}

	public Set<String> getProxyUrls() {
		synchronized (proxy_urls) {
			return proxy_urls;
		}
	}

	public void addProxyUrl(String url) {
		long l = System.currentTimeMillis();
		synchronized (proxy_urls) {
			try {
				String proxy_url = cutForProxy(url);
				if (!proxy_url.isEmpty() && !proxy_urls.contains(proxy_url)) {
					proxy_urls.add(proxy_url);
					Urls_proxy urls_proxy = new Urls_proxy();
					urls_proxy.setUrl(proxy_url);
					dao.merge(urls_proxy);
					msg(">>>>>>>>>Proxy add:	{0}	Takes:{1}", proxy_url, (System.currentTimeMillis() - l));
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void removeProxyUrl(String url) {
		long l = System.currentTimeMillis();
		synchronized (proxy_urls) {
			try {
				String proxy_url = cutForProxy(url);
				if (!proxy_url.isEmpty() && proxy_urls.contains(proxy_url)) {
					proxy_urls.remove(proxy_url);
					int count = dao.executeUpdate("delete from Urls_proxy where url=:url",
							Collections.singletonMap("url", proxy_url));
					msg(">>>>>>>>>Proxy remove:	{0}	{1}	Takes:{2}", proxy_url, count, (System.currentTimeMillis() - l));
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}

	private Object getLockObject(String url) {
		Object result;
		synchronized (checkConnectUrls) {
			result = checkConnectUrls.get(url);
			if (null == result)
				checkConnectUrls.put(url, result = new Object());
		}
		return result;
	}

	@Override
	public void logFailedUrl(String url, Throwable e) throws Throwable {
		long l = System.currentTimeMillis();
		url = cutForProxy(url);
		synchronized (getLockObject(url)) {
			Urls_failed findOne = dao.find(Urls_failed.class, url);
			if (null == findOne) {
				findOne = new Urls_failed();
				findOne.setCount(0);
				findOne.setUrl(url);
				findOne.setMsg(e.getMessage());
				findOne.setTime(new Date());
			}
			findOne.setCount(findOne.getCount() + 1);
			dao.merge(findOne);
			msg(">>>>>>>>>logFailedUrl:	{0}	{1}	Takes:{2}", url, findOne.getCount(), (System.currentTimeMillis() - l));
		}
	}

	@Override
	public void logSucceedUrl(String url) throws Throwable {
		long l = System.currentTimeMillis();
		url = cutForProxy(url);
		synchronized (getLockObject(url)) {
			int count = dao.executeUpdate("delete from Urls_failed where url=:url",
					Collections.singletonMap("url", url));
			msg(">>>>>>>>>logSucceedUrl:	{0}	{1}	Takes:{2}", url, count, (System.currentTimeMillis() - l));
		}
	}

	@Override
	public Map<String, Object> connectCheck(String url) throws Throwable {
		init();
		Map<String, Object> result = new HashMap<>();
		result.put("found", false);
		result.put("continue", true);
		result.put("msg", "");
		try {
			url = cutForProxy(url);
			Urls_failed findOne = dao.find(Urls_failed.class, url);
			if (null != findOne) {
				int count = findOne.getCount();
				result.put("found", count > 0);
				if (count > configMain.getUrl_fail_stop()) {
					if (System.currentTimeMillis() - findOne.getTime().getTime() < 3600000 * configMain.getUrl_fail_retry_in_hours()) {
						result.put("continue", false);
						result.put("msg", configMain.getUrl_fail_retry_in_hours() + "小时内禁止连接：" + findOne.getMsg());
					} else {
						synchronized (getLockObject(url)) {
							findOne.setCount(configMain.getUrl_fail_retry_begin());
							findOne.setTime(new Date());
							dao.merge(findOne);
							msg(">>>>>>>>>connectCheck:	{0}	{1}", url, findOne.getCount());
						}
					}
				}
			}
		} catch (Throwable ue) {
			err(ue.getMessage(), ue);
		}
		return result;
	}

	@Override
	public synchronized Map<String, Object> getLast(String type) throws Throwable {
		Last last = dao.find(Last.class, type);
		if (null != last) {
			Map<String, Object> json = new HashMap<>();
			json.put("type", type);
			json.put("keyword", last.getKeyword());
			json.put("running", last.isRunning());
			json.put("msg", last.getMsg());
			json.put("time", last.getTime());
			return json;
		}
		return null;
	}

	@Override
	public synchronized Object running(String type, String keyword, String msg) throws Throwable {
		Last last = dao.find(Last.class, type);
		if (null == last) {
			last = new Last();
			last.setType(type);
		}
		last.setKeyword(keyword);
		last.setRunning(true);
		last.setMsg(msg);
		last.setTime(new Date());
		dao.merge(last);
		return last;
	}

	@Override
	public synchronized Object finish(String type, String msg) throws Throwable {
		Last last = dao.find(Last.class, type);
		if (null == last) {
			last = new Last();
			last.setType(type);
		}
		last.setRunning(false);
		last.setMsg(msg);
		last.setTime(new Date());
		dao.merge(last);
		return last;
	}

	@Override
	public String logview(String query) {
		try {
			return JsonUtil.toPrettyJson(dao.getList(query, null));
		} catch (Throwable e) {
			return e.getMessage();
		}
	}

	@Override
	public String logexe(String query) {
		try {
			return JsonUtil.toPrettyJson(dao.executeUpdate(query, null));
		} catch (Throwable e) {
			return e.getMessage();
		}
	}

	public static ESMap getSource(String text) throws JsonProcessingException {
		List<ESMap> hits = (List<ESMap>) JsonUtil.toObject(text, ESMap.class).get("hits", ESMap.class).get("hits");
		for (ESMap hit : hits)
			return hit.get("_source", ESMap.class);
		return null;
	}

	@Override
	public String lookupLocalHtml(String id, String page) throws Throwable {
		long l = System.currentTimeMillis();
		ESMap _source;
		try {
			Map<Object, Object> params = ESMap.get();
			params.put("size", 1);
			params.put("from", 0);
			params.put("query"//
					, ESMap.get().set("bool", ESMap.get()//
							.set("must", Arrays.asList(
									ESMap.get().set("term", Collections.singletonMap("id", Long.valueOf(id))),
									ESMap.get().set("term", Collections.singletonMap("page", Long.valueOf(page)))))//
					)//
			);
//			params.put("_source", ESMap.get().set("includes", Arrays.asList("path")));
			_source = getSource(
					httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + "html/_doc/_search", JsonUtil.toJson(params)));
		} catch (NotFound notFound) {
			_source = null;
		}
		l = System.currentTimeMillis() - l;
		long r = System.currentTimeMillis();
		String result = returnToHtml(id, _source);
		r = System.currentTimeMillis() - r;
		msg("lookupLocalHtml lookup:{0}	return:{1}	({2}_{3}){4}", l, r, id, page,
				null == _source ? "Find nothing" : _source.get("title"));
		return result;
	}

	@Override
	public String lookupMD5_Path(String key) throws Throwable {
		ESMap _source;
		try {
			Map<Object, Object> params = ESMap.get();
			params.put("size", 1);
			params.put("from", 0);
			params.put("query"//
					, ESMap.get().set("bool", ESMap.get()//
							.set("must",
									Arrays.asList(ESMap.get().set("term", Collections.singletonMap("type", "path")),
											ESMap.get().set("term", Collections.singletonMap("key", key))))//
					)//
			);
			params.put("_source", ESMap.get().set("includes", Arrays.asList("path")));
			_source = getSource(
					httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + "md/_doc/_search", JsonUtil.toJson(params)));
		} catch (NotFound notFound) {
			_source = null;
		}
		if (null != _source) {
			return _source.get("path", String.class);
		}
		return null;

	}

	@Override
	public String lookupURL_Path(String url) throws Throwable {
		ESMap _source;
		try {
			Map<Object, Object> params = ESMap.get();
			params.put("size", 1);
			params.put("from", 0);
			params.put("query"//
					, ESMap.get().set("bool", ESMap.get()//
							.set("must",
									Arrays.asList(ESMap.get().set("term", Collections.singletonMap("type", "url")),
											ESMap.get().set("term",
													Collections.singletonMap("key", getMD5(url.getBytes("utf8"))))))//
					)//
			);
			params.put("_source", ESMap.get().set("includes", Arrays.asList("path")));
			_source = getSource(
					httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + "md/_doc/_search", JsonUtil.toJson(params)));
		} catch (NotFound notFound) {
			_source = null;
		}
		if (null != _source) {
			return _source.get("path", String.class);
		}
		return null;

	}
}
