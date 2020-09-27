package org.sdjen.download.cache_sis.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class CopyEsToMongo {
	final static Logger logger = LoggerFactory.getLogger(CopyEsToMongo.class);
	@Value("${siscache.conf.copy_es_mongo_unit_limit}")
	private int size = 300;
	@Value("${siscache.conf.path_es_start}")
	private String path_es_start;
	@Autowired
	private HttpUtil httpUtil;
	@Autowired
	private MongoTemplate mongoTemplate;

	public CopyEsToMongo() {
		System.out.println(">>>>>>>>>>>>CopyEsToMongo");
	}

	public void copy(String type) throws Throwable {
		Query query = new Query().addCriteria(Criteria.where("type").is("es_mongo_last_" + type));
		Map<?, ?> last = mongoTemplate.findOne(query, Map.class, "es_mongo_last");
		String from = null;
		if (null != last) {
			if (last.containsKey("running") && (Boolean) last.get("running")) {
				logger.info(">>>>>>>>>>>>Copy {} is Running", type);
				return;
			}
			from = (String) last.get("keyword");
		}
		com.mongodb.client.result.UpdateResult updateResult = mongoTemplate.upsert(query, new Update()//
				.set("running", true)//
				.set("time", new Date())//
				.set("msg", " init")//
				, "es_mongo_last");
		logger.info(">>>>>>>>>>>>Copy {} from {}", type, last);
		try {
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
			updateResult = mongoTemplate.updateMulti(query, new Update().set("running", false), "es_mongo_last");
			logger.info(">>>>>>>>>>>>Copy {} finished! {}", type, updateResult);
		} catch (Throwable e) {
			updateResult = mongoTemplate.updateMulti(query, new Update()//
					.set("running", false)//
					.set("time", new Date())//
					.set("msg", e.getMessage())//
					, "es_mongo_last");
			logger.info(">>>>>>>>>>>>Copy {} error! {}", type, updateResult);
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
		params.put("size", size);
		params.put("from", 0);
		long l = System.currentTimeMillis();
		String json = JsonUtil.toJson(params);
//		logger.info("{}	{}", type, json);
		json = httpUtil.doLocalPostUtf8Json(path_es_start + "md/_doc/_search", json);
//		logger.info("{}	{}", type, json);
		l = System.currentTimeMillis() - l;
		ESMap hits = JsonUtil.toObject(json, ESMap.class).get("hits", ESMap.class);
		for (ESMap hit : (List<ESMap>) hits.get("hits")) {
			ESMap _source = hit.get("_source", ESMap.class);
//			logger.info("{}	{}	{}", type, hit.get("_id"), _source);
			result = _source.get("path", String.class);
		}
		long sTime = System.currentTimeMillis() - startTime;
		logger.info("查{}:	{}ms	共:{}ms	Last:{}	total:{}", type, sTime, (System.currentTimeMillis() - startTime),
				result, hits.get("total"));
		logLast("es_mongo_last_" + type, result, hits.get("total").toString());
		int total = (Integer) hits.get("total");
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
		params.put("size", size);
		params.put("from", 0);
		long l = System.currentTimeMillis();
		String js = httpUtil.doLocalPostUtf8Json(path_es_start + "html/_doc/_search", JsonUtil.toJson(params));
		l = System.currentTimeMillis() - l;
		ESMap hits = JsonUtil.toObject(js, ESMap.class).get("hits", ESMap.class);
		ExecutorService executor = Executors.newFixedThreadPool(3);
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
		long sTime = System.currentTimeMillis() - startTime;
		executor.shutdown();
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
		logLast("es_mongo_last_html", String.valueOf(result), hits.get("total").toString());
		logger.info("查:	{}ms	共:{}ms	{}~{}	total:{}", sTime, (System.currentTimeMillis() - startTime), from,
				result, hits.get("total"));
		return result;
	}

	private void single(Long id) throws Throwable {
		Map<Object, Object> params = ESMap.get();
		params.put("query", ESMap.get().set("bool", ESMap.get().set("must",
				Arrays.asList(ESMap.get().set("term", Collections.singletonMap("id", Long.valueOf(id)))))));
//		params.put("query", ESMap.get().set("term", ESMap.get().set("id", Long.valueOf(id))));
		String js = httpUtil.doLocalPostUtf8Json(path_es_start + "html/_doc/_search", JsonUtil.toJson(params));
		ESMap hits = JsonUtil.toObject(js, ESMap.class).get("hits", ESMap.class);
		for (ESMap hit : (List<ESMap>) hits.get("hits")) {
			ESMap _source = hit.get("_source", ESMap.class);
			Long page = Long.valueOf(_source.get("page").toString());
			String title = _source.get("title", String.class);
			if (1l == page) {
				logger.info("{}+{}	{}	{}", id, hits.get("total"), _source.get("datetime", String.class), title);
			}
		}
	}

	private void logLast(String type, String keyword, String msg) {
		mongoTemplate.upsert(new Query().addCriteria(Criteria.where("type").is(type)), new Update()//
				.set("keyword", keyword)//
				.set("running", true)//
				.set("time", new Date())//
				, "es_mongo_last");
	}
}
