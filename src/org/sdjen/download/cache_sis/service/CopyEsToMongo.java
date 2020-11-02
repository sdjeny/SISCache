package org.sdjen.download.cache_sis.service;

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
import org.sdjen.download.cache_sis.configuration.ConfigMain;
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
public class CopyEsToMongo {
	final static Logger logger = LoggerFactory.getLogger(CopyEsToMongo.class);
	@Autowired
	private ConfigMain configMain;
	@Autowired
	private HttpUtil httpUtil;
	@Resource(name = "Store_Mongodb")
	private IStore store_mongo;
	@Resource(name = "Store_ElasticSearch")
	private IStore store_es;
	@Resource(name = "cpExecutor")
	private ThreadPoolTaskExecutor executor;

	public CopyEsToMongo() {
		System.out.println(">>>>>>>>>>>>CopyEsToMongo");
	}

	@Async("taskExecutor")
	public void copy(String type) throws Throwable {
		Map<String, Object> last = store_mongo.getLast("es_mongo_" + type);
		String from = null;
		if (null != last) {
			if (last.containsKey("running") && (Boolean) last.get("running")) {
				logger.info(">>>>>>>>>>>>Copy {} is Running", type);
				return;
			}
			from = (String) last.get("keyword");
		}
		try {
			store_mongo.running("es_mongo_" + type, from, " init");
			store_es.init();
			store_mongo.init();
			logger.info(">>>>>>>>>>>>Copy {} from {}", type, last);
			switch (type) {
			case "html": {
				copyHtml(null == from ? 0l : Long.valueOf(from));
				break;
			}
			case "url":
			case "path": {
				copyMd(type, null == from ? " " : from);
				break;
			}
			default:
				break;
			}
			logger.info(">>>>>>>>>>>>Copy {} finished! {}", type, store_mongo.finish("es_mongo_" + type, "finsh"));
		} catch (Throwable e) {
			logger.info(">>>>>>>>>>>>Copy {} error! {}", type, store_mongo.finish("es_mongo_" + type, e.getMessage()));
			throw e;
		}
	}

	public void copyHtml(long from) throws Throwable {
		Long listResult = from;
		do {
			listResult = listHtml(from = listResult);
		} while (listResult.compareTo(from) > 0);

	}

	void copyMd(String type, String from) throws Throwable {
		String listResult = from;
		do {
			listResult = listMd(type, from = listResult);
//			logger.info("{}	{}={}", listResult, from, listResult.equals(from));
		} while (!listResult.equals(from));
	}

	private String listMd(String type, String from) throws Throwable {
		String result = from;
		long startTime = System.currentTimeMillis();
		Map<Object, Object> params = ESMap.get();
		params.put("query", ESMap.get().set("bool", ESMap.get().set("must", Arrays.asList(//
				ESMap.get().set("term", Collections.singletonMap("type", type)), //
				ESMap.get().set("range", ESMap.get().set("path.keyword", ESMap.get().set("gt", from)))//
		)))//
		);
		params.put("sort", Arrays.asList(//
				ESMap.get().set("path.keyword", ESMap.get().set("order", "asc"))//
		));
		params.put("size", configMain.getCopy_unit_limit());
		params.put("from", 0);
		String json = JsonUtil.toJson(params);
//		logger.info("{}	{}", type, json);
		json = httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + "md/_doc/_search", json);
//		logger.info("{}	{}", type, json);
		long l = System.currentTimeMillis() - startTime;
		ESMap hits = JsonUtil.toObject(json, ESMap.class).get("hits", ESMap.class);
		for (ESMap hit : (List<ESMap>) hits.get("hits")) {
			ESMap _source = hit.get("_source", ESMap.class);
//			logger.info("{}	{}	{}", type, hit.get("_id"), _source);
			if ("url".equals(type))
				store_mongo.saveURL((String) _source.get("url"), (String) _source.get("path"));
			if ("path".equals(type))
				store_mongo.saveMD5((String) _source.get("key"), (String) _source.get("path"));
			result = _source.get("path", String.class);
		}
		logger.info("查{}:	{}ms	共:{}ms	Last:{}	total:{}", type, l, (System.currentTimeMillis() - startTime),
				result, hits.get("total"));
		store_mongo.running("es_mongo_" + type, result, hits.get("total").toString());
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
		params.put("_source", ESMap.get()//
				.set("includes", Arrays.asList("id"))//
				.set("excludes", Arrays.asList())//
		);
		params.put("sort", Arrays.asList(//
				ESMap.get().set("id", ESMap.get().set("order", "asc"))//
		));
		params.put("size", configMain.getCopy_unit_limit());
		params.put("from", 0);
		String js = httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + "html/_doc/_search", JsonUtil.toJson(params));
		long l = System.currentTimeMillis() - startTime;
		ESMap hits = JsonUtil.toObject(js, ESMap.class).get("hits", ESMap.class);
		List<Future<Long>> resultList = new ArrayList<Future<Long>>();
		for (ESMap hit : (List<ESMap>) hits.get("hits")) {
			ESMap _source = hit.get("_source", ESMap.class);
			Long id = Long.valueOf(_source.get("id").toString());
			result = Math.max(id, result);
//					id.compareTo(result) > 0 ? id : result;
			resultList.add(executor.submit(new Callable<Long>() {
				public Long call() throws Exception {
					try {
						single(id);
					} catch (Throwable e) {
						if (e instanceof Exception) {
							throw (Exception) e;
						} else
							throw new Exception(e);
					}
					return null;
				}
			}));
		}
		for (Future<Long> fs : resultList) {
			try {
				fs.get(30, TimeUnit.MINUTES);
			} catch (java.util.concurrent.TimeoutException e) {
				fs.cancel(false);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
		}
		store_mongo.running("es_mongo_html", String.valueOf(result), hits.get("total").toString());
		logger.info("查:	{}ms	共:{}ms	{}~{}	total:{}", l, (System.currentTimeMillis() - startTime), from, result,
				hits.get("total"));
		return result;
	}

	private void single(Long id) throws Throwable {
		store_mongo.saveHtml(String.valueOf(id), "1", "url", "", "dateStr", store_es.getLocalHtml(id.toString(), "1"));
//		Map<Object, Object> params = ESMap.get();
//		params.put("query", ESMap.get().set("bool", ESMap.get().set("must",
//				Arrays.asList(ESMap.get().set("term", Collections.singletonMap("id", Long.valueOf(id)))))));
////		params.put("query", ESMap.get().set("term", ESMap.get().set("id", Long.valueOf(id))));
//		String js = httpUtil.doLocalPostUtf8Json(path_es_start + "html/_doc/_search", JsonUtil.toJson(params));
//		ESMap hits = JsonUtil.toObject(js, ESMap.class).get("hits", ESMap.class);
//		for (ESMap hit : (List<ESMap>) hits.get("hits")) {
//			ESMap _source = hit.get("_source", ESMap.class);
//			Long page = Long.valueOf(_source.get("page").toString());
//			String title = _source.get("title", String.class);
//			if (1l == page) {
//				logger.info("{}+{}	{}	{}", id, hits.get("total"), _source.get("datetime", String.class), title);
//			}
//		}
	}
}
