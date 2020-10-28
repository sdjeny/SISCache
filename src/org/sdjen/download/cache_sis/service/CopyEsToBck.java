package org.sdjen.download.cache_sis.service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class CopyEsToBck {
	final static Logger logger = LoggerFactory.getLogger(CopyEsToBck.class);
	@Value("${siscache.conf.copy_es_mongo_unit_limit}")
	private int size = 300;
	@Autowired
	private HttpUtil httpUtil;
	@Resource(name = "Store_ElasticSearch")
	private IStore store_es;
	@Resource(name = "cpES2MGExecutor")
	private ThreadPoolTaskExecutor executor;

	public CopyEsToBck() {
		System.out.println(">>>>>>>>>>>>CopyEsToMongo");
	}

	@Async("taskExecutor")
	public void copy(String type) throws Throwable {
		Map<String, Object> last = store_es.getLast("es_bck_" + type);
		String from = null;
		if (null != last) {
			if (last.containsKey("running") && (Boolean) last.get("running")) {
				logger.info(">>>>>>>>>>>>Copy {} is Running", type);
				return;
			}
			from = (String) last.get("keyword");
		}
		try {
			store_es.running("es_bck_" + type, from, " init");
			store_es.init();
			logger.info(">>>>>>>>>>>>Copy {} from {}", type, last);
			switch (type) {
			case "html": {
				copyHtml(null == from ? 0l : Long.valueOf(from));
				break;
			}
			case "url": {
				copyUrl(null == from ? " " : from);
				break;
			}
			default:
				break;
			}
			logger.info(">>>>>>>>>>>>Copy {} finished! {}", type, store_es.finish("es_bck_" + type, "finsh"));
		} catch (Throwable e) {
			logger.info(">>>>>>>>>>>>Copy {} error! {}", type, store_es.finish("es_bck_" + type, e.getMessage()));
			throw e;
		}
	}

	public void copyHtml(long from) throws Throwable {
		Long listResult = from;
		do {
			listResult = listHtml(from = listResult);
		} while (listResult.compareTo(from) > 0);

	}

	void copyUrl(String from) throws Throwable {
		String listResult = from;
		do {
			listResult = listUrl(from = listResult);
//			logger.info("{}	{}={}", listResult, from, listResult.equals(from));
		} while (!listResult.equals(from));
	}

	private String listUrl(String from) throws Throwable {
		String result = from;
		long startTime = System.currentTimeMillis();
		Map<Object, Object> params = ESMap.get();
		params.put("query", ESMap.get().set("bool", ESMap.get().set("must", Arrays.asList(//
				ESMap.get().set("term", Collections.singletonMap("type", "url")), //
				ESMap.get().set("range", ESMap.get().set("path.keyword", ESMap.get().set("gt", from)))//
		)))//
		);
		params.put("sort", Arrays.asList(//
				ESMap.get().set("path.keyword", ESMap.get().set("order", "asc"))//
		));
		params.put("size", size);
		params.put("from", 0);
		String json = JsonUtil.toJson(params);
		json = httpUtil.doLocalPostUtf8Json("http://192.168.0.237:9200/siscache_md/_doc/_search", json);
		long l = System.currentTimeMillis() - startTime;
		ESMap hits = JsonUtil.toObject(json, ESMap.class).get("hits", ESMap.class);
		int count = 0;
		for (ESMap hit : (List<ESMap>) hits.get("hits")) {
			ESMap _source = hit.get("_source", ESMap.class);
//			logger.info("{}	{}	{}", type, hit.get("_id"), _source);
			Object _id = hit.get("_id");
			String url = (String) _source.get("url");
			String path = (String) _source.get("path");
			String key = (String) _source.get("key");
			if (!url.equalsIgnoreCase(path) && !_id.equals(key)) {
				String r = httpUtil.doLocalPostUtf8Json("http://192.168.0.237:9200/siscache_bck_md/_doc/" + _id,
						JsonUtil.toJson(_source));
				_source.put("key", _id);
				r = httpUtil.doLocalPostUtf8Json("http://192.168.0.237:9200/siscache_md/_doc/" + _id,
						JsonUtil.toJson(_source));
				count++;
			}
			result = _source.get("path", String.class);
		}
		logger.info(MessageFormat.format("查URL	{4}:	{0}ms	共:{1}ms	Last:{2}	total:{3}", l,
				(System.currentTimeMillis() - startTime), result, hits.get("total"), count));
		store_es.running("es_bck_url", result, MessageFormat.format("查URL	{3}:	{0}ms	共:{1}ms	total:{2}", l,
				(System.currentTimeMillis() - startTime), hits.get("total"), count));
//		int total = (Integer) hits.get("total");
		return result;
	}

	private Long listHtml(Long from) throws Throwable {
		Long result = from, startTime = System.currentTimeMillis();
		Map<Object, Object> params = ESMap.get();
		params.put("query", ESMap.get().set("bool", ESMap.get().set("must", Arrays.asList(//
				ESMap.get().set("term", Collections.singletonMap("page", 1)), //
				ESMap.get().set("range", ESMap.get().set("id", ESMap.get().set("gt", from)))//
		)))//
		);
//		params.put("_source", ESMap.get()//
//				.set("includes", Arrays.asList("id"))//
//				.set("excludes", Arrays.asList())//
//		);
		params.put("sort", Arrays.asList(//
				ESMap.get().set("id", ESMap.get().set("order", "asc"))//
		));
		params.put("size", size);
		params.put("from", 0);
		String js = httpUtil.doLocalPostUtf8Json("http://192.168.0.237:9200/siscache_html/_doc/_search",
				JsonUtil.toJson(params));
		long l = System.currentTimeMillis() - startTime;
		ESMap hits = JsonUtil.toObject(js, ESMap.class).get("hits", ESMap.class);
//		List<Future<Long>> resultList = new ArrayList<Future<Long>>();
		for (ESMap hit : (List<ESMap>) hits.get("hits")) {
			ESMap _source = hit.get("_source", ESMap.class);
			Long id = Long.valueOf(_source.get("id").toString());
			String r = httpUtil.doLocalPostUtf8Json("http://192.168.0.237:9200/siscache_bck_html/_doc/" + id + "_1",
					JsonUtil.toJson(_source));
			result = Math.max(id, result);
//					id.compareTo(result) > 0 ? id : result;
//			resultList.add(executor.submit(new Callable<Long>() {
//				public Long call() throws Exception {
//					try {
//						single(id);
//					} catch (Throwable e) {
//						if (e instanceof Exception) {
//							throw (Exception) e;
//						} else
//							throw new Exception(e);
//					}
//					return null;
//				}
//			}));
		}
//		for (Future<Long> fs : resultList) {
//			try {
//				fs.get(30, TimeUnit.MINUTES);
//			} catch (java.util.concurrent.TimeoutException e) {
//				fs.cancel(false);
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//			}
//		}
		String msg = MessageFormat.format("查html:	{0}ms	共:{1}ms	{2}~{3}	total:{4}", l,
				(System.currentTimeMillis() - startTime), from, result, hits.get("total"));
		logger.info(msg);
		store_es.running("es_bck_html", String.valueOf(result), msg);
		return result;
	}

	private void single(Long id) throws Throwable {
		Map<Object, Object> params = ESMap.get();
		params.put("query", ESMap.get().set("bool", ESMap.get().set("must",
				Arrays.asList(ESMap.get().set("term", Collections.singletonMap("id", Long.valueOf(id)))))));
//		params.put("query", ESMap.get().set("term", ESMap.get().set("id", Long.valueOf(id))));
		String js = httpUtil.doLocalPostUtf8Json("http://192.168.0.237:9200/siscache_html/_doc/_search",
				JsonUtil.toJson(params));
		ESMap hits = JsonUtil.toObject(js, ESMap.class).get("hits", ESMap.class);
		for (ESMap hit : (List<ESMap>) hits.get("hits")) {
			ESMap _source = hit.get("_source", ESMap.class);
			Long page = Long.valueOf(_source.get("page").toString());
			String r = httpUtil.doLocalPostUtf8Json(
					"http://192.168.0.237:9200/siscache_bck_html/_doc/" + id + "_" + page, JsonUtil.toJson(_source));
//			if (1l == page) {
//			String title = _source.get("title", String.class);
//				logger.info("{}+{}	{}	{}", id, hits.get("total"), _source.get("datetime", String.class), title);
//			}
		}
	}
}
