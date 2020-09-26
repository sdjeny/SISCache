package org.sdjen.download.cache_sis.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.springframework.stereotype.Service;

@Service
public class CopyEsToMongo {
	final static Logger logger = LoggerFactory.getLogger(CopyEsToMongo.class);
	@Value("${siscache.conf.copy_es_mongo_unit_limit}")
	private int size = 300;
	@Autowired
	private HttpUtil httpUtil;
	@Value("${siscache.conf.path_es_start}")
	private String path_es_start;

	public CopyEsToMongo() {
		System.out.println(">>>>>>>>>>>>CopyEsToMongo");
	}

	public synchronized void copy(long from) throws Throwable {
		ExecutorService executor = Executors.newFixedThreadPool(3);
		List<Future<Object>> resultList = new ArrayList<>();
		resultList.add(executor.submit(new Callable<Object>() {
			public Object call() throws Exception {
				try {
					copyHtml(from);
				} catch (Throwable e) {
					if (e instanceof Exception) {
						throw (Exception) e;
					} else
						throw new Exception(e);
				}
				return null;
			}
		}));
		resultList.add(executor.submit(new Callable<Object>() {
			public Object call() throws Exception {
				try {
					copyMd("url");
				} catch (Throwable e) {
					if (e instanceof Exception) {
						throw (Exception) e;
					} else
						throw new Exception(e);
				}
				return null;
			}
		}));
		resultList.add(executor.submit(new Callable<Object>() {
			public Object call() throws Exception {
				try {
					copyMd("path");
				} catch (Throwable e) {
					if (e instanceof Exception) {
						throw (Exception) e;
					} else
						throw new Exception(e);
				}
				return null;
			}
		}));
		executor.shutdown();
		for (Future<Object> fs : resultList) {
			try {
				fs.get(30, TimeUnit.MINUTES);
			} catch (java.util.concurrent.TimeoutException e) {
				fs.cancel(false);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
		}
		logger.info(">>>>>>>>>>>>Copy finished!");
	}

	public void copyHtml(long from) throws Throwable {
		Long listResult = from;
		do {
			listResult = listHtml(from = listResult);
		} while (listResult.compareTo(from) > 0);

	}

	void copyMd(String type) {
		String from = " ";
		String listResult = from;
		do {
			listResult = listMd(from = listResult, type);
//			logger.info("{}	{}={}", listResult, from, listResult.equals(from));
		} while (!listResult.equals(from));
	}

	private String listMd(String from, String type) {
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
}
