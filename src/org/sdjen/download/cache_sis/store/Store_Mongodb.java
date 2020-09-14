package org.sdjen.download.cache_sis.store;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

@Service("Store_Mongodb")
public class Store_Mongodb implements IStore {
	private final static Logger logger = LoggerFactory.getLogger(Store_Mongodb.class);
	private MessageDigest md5Digest;
	private ConfUtil conf;
	@Autowired
	private MongoTemplate mongoTemplate;

	public ConfUtil getConf() throws IOException {
		if (null == conf) {

			conf = ConfUtil.getDefaultConf();
		}
		return conf;
	}

	private Store_Mongodb() throws Exception {
		System.out.println(">>>>>>>>>>>>>>>>>>Store_Mongodb");
		md5Digest = MessageDigest.getInstance("MD5");
	}

	@Override
	public String getLocalHtml(final String id, final String page, final String url, String title, String dateStr)
			throws Throwable {
		return mongoTemplate.findAll(Map.class, "Tas").toString();
	}

	@Override
	public void saveHtml(final String id, final String page, final String url, String title, String dateStr,
			String text) throws Throwable {
	}

	private void saveMD(Map<String, Object> objectToSave, String... keys) {
		Query query = new Query();
		Update update = new Update();
		java.util.List<String> ks = Arrays.asList(keys);
		objectToSave.forEach((k, v) -> {
			if (ks.contains(k)) {
				query.addCriteria(Criteria.where(k).is(v));
			} else {
				update.set(k, v);
			}
		});
		UpdateResult updateResult = mongoTemplate.upsert(query, update, "md");
		logger.debug("Update	>>>>>" + updateResult + ":" + update);
//		if (updateResult.getMatchedCount() < 1) {
////			logger.debug("Save	>>>>>" + mongoTemplate.save(objectToSave, "md"));
//		} else {
//		}
	}

	@Override
	public void saveURL(String url, String path) throws IOException {
		Map<String, Object> objectToSave = new HashMap<>();
		String key = getMD5(url.getBytes("utf8"));
		objectToSave.put("key", key);
		objectToSave.put("url", url);
		objectToSave.put("path", path);
		objectToSave.put("type", "url");
		saveMD(objectToSave, "key", "type");
	}

	@Override
	public void saveMD5(String md5, String path) throws Exception {
		Map<String, Object> objectToSave = new HashMap<>();
		objectToSave.put("key", md5);
		objectToSave.put("path", path);
		objectToSave.put("type", "path");
		saveMD(objectToSave, "key", "type");
	}

	@Override
	public String getMD5_Path(String key) throws Exception {
		Query query = new Query();
		query.addCriteria(Criteria.where("key").is(key));
		query.addCriteria(Criteria.where("type").is("path"));
//		query.skip(skipNumber);
//		query.limit(pageSize);
		query.fields().include("path");
		return (String) mongoTemplate.findOne(query, Map.class, "md").get("path");
	}

	private synchronized String getMD5(byte[] bytes) {
		return new BigInteger(1, md5Digest.digest(bytes)).toString(Character.MAX_RADIX);
	}

	@Override
	public String getURL_Path(String key) throws IOException {
		Query query = new Query();
		query.addCriteria(Criteria.where("key").is(getMD5(key.getBytes("utf8"))));
		query.addCriteria(Criteria.where("type").is("url"));
//		query.skip(skipNumber);
//		query.limit(pageSize);
//		Sort sort = new Sort(Sort.Direction.ASC, "DEVID").and(new Sort(Sort.Direction.ASC, "TIME"));// 多条件DEVID、time
//		query.with(Sort.by(Order.asc("DEVID"), Order.desc("TIME")));
//		query.with(PageRequest.of(page, size, Sort.by(Order.asc("DEVID"), Order.desc("TIME"))));
		query.fields().include("path");
		return (String) mongoTemplate.findOne(query, Map.class, "md").get("path");
	}

	private SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");

	private StringBuilder getMsgText(Object pattern, Object... args) {
		StringBuilder builder = new StringBuilder(dateFormat.format(new Date()));
		builder.append("	,");
		if (null != args && args.length > 0 && pattern instanceof String) {
			builder.append(MessageFormat.format((String) pattern, args));
		} else {
			builder.append(pattern);
		}
		return builder;
	}

	@Override
	public void msg(Object pattern, Object... args) {
		logger.info(getMsgText(pattern, args).toString());
	}

	@Override
	public void err(Object pattern, Object... args) {
		logger.error(getMsgText(pattern, args).toString());
	}

	@Override
	public void refreshMsgLog() {
	}
}
