package org.sdjen.download.cache_sis.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class CopyMongoToES {
	final static Logger logger = LoggerFactory.getLogger(CopyMongoToES.class);
	@Value("${siscache.conf.copy_es_mongo_unit_limit}")
	private int size = 300;
	@Value("${siscache.conf.path_es_start}")
	private String path_es_start;
	@Autowired
	private HttpUtil httpUtil;
	@Resource(name = "Store_Mongodb")
	private IStore store_mongo;
	@Resource(name = "Store_ElasticSearch")
	private IStore store_es;
	@Resource(name = "cpES2MGExecutor")
	private ThreadPoolTaskExecutor executor;
	@Autowired
	private MongoTemplate mongoTemplate;

	public CopyMongoToES() {
		System.out.println(">>>>>>>>>>>>CopyEsToMongo");
	}

	@Async("taskExecutor")
	public void copy(String type) throws Throwable {
		try {
			store_es.init();
			store_mongo.init();
			Map<String, Object> last = store_es.getLast("copy_" + type);
			String from = null;
			if (null != last) {
				if (last.containsKey("running") && (Boolean) last.get("running")) {
					logger.info(">>>>>>>>>>>>Copy {} is Running", type);
					return;
				}
				from = (String) last.get("keyword");
			}
			store_es.running("copy_" + type, from, " init");
			logger.info(">>>>>>>>>>>>Copy {} from {}", type, last);
			switch (type) {
			case "html": {
				copyHtml(null == from ? 0l : Long.valueOf(from));
				break;
			}
			case "md": {
				copyMd(null == from ? " " : from);
				break;
			}
			default:
				break;
			}
			logger.info(">>>>>>>>>>>>Copy {} finished! {}", type, store_es.finish("copy_" + type, "finsh"));
		} catch (Throwable e) {
			logger.info(">>>>>>>>>>>>Copy {} error! {}", type, store_es.finish("copy_" + type, e.getMessage()));
			throw e;
		}
	}

	public void copyHtml(long from) throws Throwable {
		Long listResult = from;
		do {
			listResult = listHtml(from = listResult);
		} while (listResult.compareTo(from) > 0);

	}

	void copyMd(String from) throws Throwable {
		String listResult = from;
		do {
			listResult = listMd(from = listResult);
//			logger.info("{}	{}={}", listResult, from, listResult.equals(from));
		} while (!listResult.equals(from));
	}

	private String listMd(String from) throws Throwable {
		String result = from;
		long startTime = System.currentTimeMillis();
		Query query = new Query();
		query.addCriteria(Criteria.where("key").gt(from));
		Object total = mongoTemplate.count(query, "md");
//		query.skip(0);
		query.limit(size);
		query.with(Sort.by(Order.asc("key")));
		List<Map> hits = mongoTemplate.find(query, Map.class, "md");
		long l = System.currentTimeMillis() - startTime;
		for (Map<?, ?> _source : hits) {
			String type = (String) _source.get("type");
//			if ("url".equals(type))
//				store_es.saveURL((String) _source.get("url"), (String) _source.get("path"));
//			if ("path".equals(type))
//				store_es.saveMD5((String) _source.get("key"), (String) _source.get("path"));
			result = (String) _source.get("key");
		}
		store_es.running("copy_md", result, total.toString());
		logger.info("查:	{}ms	共:{}ms	Last:{}	total:{}", l, (System.currentTimeMillis() - startTime), result, total);
		return result;
	}

	private Long listHtml(Long from) throws Throwable {
		Long result = from, startTime = System.currentTimeMillis();
		Query query = new Query();
		query.addCriteria(Criteria.where("id").gt(from));
		Object total = mongoTemplate.count(query, "htmldoc");
//		query.skip(0);
		query.limit(size);
		query.with(Sort.by(Order.asc("id")));
		query.fields().include("id");
		List<Map> ms = mongoTemplate.find(query, Map.class, "htmldoc");
		long l = System.currentTimeMillis() - startTime;
		List<Future<Long>> resultList = new ArrayList<Future<Long>>();
		for (Map<?, ?> _source : ms) {
			Long id = Long.valueOf(_source.get("id").toString());
			result = Math.max(id, result);
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
		store_es.running("copy_html", String.valueOf(result), total.toString());
		logger.info("查:	{}ms	共:{}ms	{}~{}	total:{}", l, (System.currentTimeMillis() - startTime), from, result,
				total);
		return result;
	}

	private void single(Long id) throws Throwable {
//		store_es.saveHtml(String.valueOf(id), "1", "url", "", "dateStr", store_mongo.getLocalHtml(id.toString(), "1"));
	}
}
