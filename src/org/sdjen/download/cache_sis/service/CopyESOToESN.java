package org.sdjen.download.cache_sis.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;

import javax.annotation.Resource;

import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.http.DefaultCss;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.tool.ZipUtil;
import org.sdjen.download.cache_sis.util.EntryData;
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
public class CopyESOToESN {
	final static Logger logger = LoggerFactory.getLogger(CopyESOToESN.class);
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

	public CopyESOToESN() {
		System.out.println(">>>>>>>>>>>>CopyEsToMongo");
	}

	public String getOldHtml(final String id, final String page) throws Throwable {
		String ss = httpUtil.doLocalGet("http://192.168.0.237:9200/siscache_bak_html/_doc/{key}",
				new EntryData<String, String>().put("key", id + "_" + page).getData());
		ESMap esMap = JsonUtil.toObject(ss, ESMap.class);
		ESMap _source = esMap.get("_source", ESMap.class);
		String result = null;
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
				result = rst.toString();
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
					result = _source.get("context", String.class);
				}
			}
		}
		if (null != result) {
			org.jsoup.nodes.Document document = store_es.replaceLocalHtmlUrl(result);
			for (org.jsoup.nodes.Element e : document.select("head").select("style")) {
				if (e.text().isEmpty()) {
					e.text(DefaultCss.getCss());
				}
			}
			result = document.html();
		}
		return result;
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
