package org.sdjen.download.cache_sis.service;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.bson.types.Binary;
import org.sdjen.download.cache_sis.configuration.ConfigMain;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.tool.ZipUtil;
import org.sdjen.download.cache_sis.util.CopyExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
			Map<String, Object> last;
			try {
				last = store_es.getLast("copy_m_e_" + type);
			} catch (Throwable e1) {
				last = null;
			}
			String from = null;
			if (null != last) {
				if (last.containsKey("running") && (Boolean) last.get("running")) {
					logger.info(">>>>>>>>>>>>Copy {} is Running", type);
					return;
				}
				from = (String) last.get("keyword");
			}
			store_es.running("copy_m_e_" + type, from, " init");
			logger.info(">>>>>>>>>>>>Copy {} from {}", type, last);
			switch (type) {
			case "html": {
				new CopyExecutor<Long>() {

					@Override
					public void log() throws Throwable {
						String msg = MessageFormat.format("Html	查:	{0}ms	存:{1}ms	自:{2}	余:{3}",
								logMsg.get("time_lookup"), logMsg.get("time_exe"), logMsg.get("from"),
								logMsg.get("total"));
						logger.info(msg);
						store_es.running("copy_m_e_html",
								null == logMsg.get("from") ? null : logMsg.get("from").toString(), msg);
					}

					@Override
					public boolean isEnd(Long rst, Long from) {
						return rst.compareTo(from) <= 0;
					}

					@Override
					public Map<String, Object> getListDetail(Long from) throws Exception {
						Map<String, Object> result = new HashMap<String, Object>();
						Query query = new Query();
						query.addCriteria(Criteria.where("id").gt(from));
						query.addCriteria(Criteria.where("page").is(1l));
						result.put("total", mongoTemplate.count(query, "htmldoc"));
//						query.skip(0);
						query.limit(configMain.getCopy_unit_limit());
						query.with(Sort.by(Order.asc("id")));
//						query.fields().include("id");
						result.put("list", mongoTemplate.find(query, Map.class, "htmldoc"));
						return result;
					}

					@Override
					public Long getKey(Map<?, ?> detail) {
						return (Long) detail.get("id");
					}

					@Override
					public Long getMaxKey(Long t1, Long t2) {
						if (null == t1 && null != t2)
							return t2;
						else if (null != t1 && null == t2)
							return t1;
						else if (null == t1 && null == t2)
							return null;
						else
							return Math.max(t1, t2);
					}

					@Override
					public Long single(Map<?, ?> detail) throws Throwable {
						Map<String, Object> data = new HashMap<>();
						Map<String, Throwable> errs = new HashMap<String, Throwable>();
						detail.forEach((k, v) -> {
							if ("context_zip".equals(k)) {
								try {
									String context = ZipUtil.uncompress(((Binary) v).getData());
									Map<String, Object> details = JsonUtil.toObject(context, Map.class);
									context = ZipUtil.bytesToString(ZipUtil.compress(JsonUtil.toJson(details)));
									data.put("context_zip", context);
								} catch (Exception e) {
									errs.put("zip", e);
								}
							} else if (!k.equals("_id")) {
								data.put((String) k, v);
							}
						});
						if (!errs.isEmpty())
							throw errs.get("zip");
						String r = httpUtil.doLocalPostUtf8Json(
								configMain.getPath_es_start() + "html/_doc/" + data.get("id") + "_" + data.get("page"),
								JsonUtil.toJson(data));
						return getKey(detail);
					}
				}.copy(null == from ? 0l : Long.valueOf(from), executor);
				break;
			}
			case "md": {
				new CopyExecutor<String>() {

					@Override
					public void log() throws Throwable {
						String msg = MessageFormat.format("MD	查:	{0}ms	存:{1}ms	自:{2}	余:{3}",
								logMsg.get("time_lookup"), logMsg.get("time_exe"), logMsg.get("from"),
								logMsg.get("total"));
						logger.info(msg);
						store_es.running("copy_m_e_md",
								null == logMsg.get("from") ? null : logMsg.get("from").toString(), msg);
					}

					@Override
					public boolean isEnd(String rst, String from) {
						return rst.compareTo(from) <= 0;
					}

					@Override
					public Map<String, Object> getListDetail(String from) throws Exception {
						Map<String, Object> result = new HashMap<String, Object>();
						Query query = new Query();
						query.addCriteria(Criteria.where("key").gt(from));
//						query.addCriteria(Criteria.where("type").is("url"));
						result.put("total", mongoTemplate.count(query, "md"));
//						query.skip(0);
						query.limit(configMain.getCopy_unit_limit());
						query.with(Sort.by(Order.asc("key")));
						result.put("list", mongoTemplate.find(query, Map.class, "md"));
						return result;
					}

					@Override
					public String getKey(Map<?, ?> detail) {
						return (String) detail.get("key");
					}

					@Override
					public String getMaxKey(String t1, String t2) {
						if (null == t1 && null != t2)
							return t2;
						else if (null != t1 && null == t2)
							return t1;
						else if (null == t1 && null == t2)
							return null;
						else
							return t1.compareTo(t2) > 0 ? t1 : t2;
					}

					@Override
					public String single(Map<?, ?> detail) throws Throwable {
						Map<String, Object> json = new HashMap<>();
						json.put("key", detail.get("key"));
						json.put("url", detail.get("url"));
						json.put("path", detail.get("path"));
						json.put("type", detail.get("type"));
						httpUtil.doLocalPostUtf8Json(configMain.getPath_es_start() + "md/_doc/" + detail.get("key"),
								JsonUtil.toJson(json));
						return getKey(detail);
					}
				}.copy(null == from ? " " : from, executor);
				break;
			}
			default:
				break;
			}
			logger.info(">>>>>>>>>>>>Copy {} finished! {}", type, store_es.finish("copy_m_e_" + type, "finsh"));
		} catch (Throwable e) {
			logger.info(">>>>>>>>>>>>Copy {} error! {}", type, store_es.finish("copy_m_e_" + type, e.getMessage()));
			throw e;
		}
	}
}
