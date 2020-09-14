package org.sdjen.download.cache_sis.store;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service("Store_Mongodb")
public class Store_Mongodb implements IStore {

	private final static Logger logger = Logger.getLogger(Store_Mongodb.class.toString());
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
//		mongoTemplate.insert(ESMap.get().set("field", "abc"), "Tas");
		System.out.println(mongoTemplate);
	}

	@Override
	public String getLocalHtml(final String id, final String page, final String url, String title, String dateStr)
			throws Throwable {
		return "";
	}

	@Override
	public void saveHtml(final String id, final String page, final String url, String title, String dateStr,
			String text) throws Throwable {
	}

	@Override
	public void saveURL(String url, String path) throws IOException {
	}

	@Override
	public void saveMD5(String md5, String path) throws IOException {
		Map<String, Object> json = new HashMap<>();
		json.put("key", md5);
		json.put("path", path);
		json.put("type", "path");
//		mongoTemplate.save(json);
	}

	@Override
	public String getMD5_Path(String key) throws Exception {
		return "";
	}

	private synchronized String getMD5(byte[] bytes) {
		return new BigInteger(1, md5Digest.digest(bytes)).toString(Character.MAX_RADIX);
	}

	@Override
	public String getURL_Path(String key) throws IOException {
		return "";
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
		logger.severe(getMsgText(pattern, args).toString());
	}

	@Override
	public void refreshMsgLog() {
	}
}
