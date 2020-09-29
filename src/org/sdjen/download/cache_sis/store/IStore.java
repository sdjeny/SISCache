package org.sdjen.download.cache_sis.store;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IStore {
	final static Logger logger = LoggerFactory.getLogger(IStore.class);
	SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
//	String getKey(final String id, final String page, final String url, String title, String dateStr) throws Throwable;

	String getLocalHtml(final String id, final String page) throws Throwable;

	void saveURL(String url, String path) throws Throwable;

	void saveMD5(String md5, String path) throws Throwable;

	String getMD5_Path(String key) throws Throwable;

	String getURL_Path(String key) throws Throwable;

	void saveHtml(final String id, final String page, final String url, String title, String dateStr, String html)
			throws Throwable;

	void refreshMsgLog();

	public Map<String, Object> getTitleList(String fid, int page, int size, String query, String order)
			throws Throwable;

	default String cutForProxy(String url) {
		int index = url.indexOf('/', 9);
		String result;
		if (-1 != index)
			result = url.substring(0, index);// 排除参数部分
		else
			result = url;
		return result;
	}

	Set<String> getProxyUrls();

	void addProxyUrl(String url);

	void removeProxyUrl(String url);

	default StringBuilder getMsgText(Object pattern, Object... args) {
		StringBuilder builder = new StringBuilder(dateFormat.format(new Date()));
		builder.append("	,");
		if (null != args && args.length > 0 && pattern instanceof String) {
			builder.append(MessageFormat.format((String) pattern, args));
		} else {
			builder.append(pattern);
		}
		return builder;
	}

	default void msg(Object pattern, Object... args) {
		logger.info(getMsgText(pattern, args).toString());
	}

	default void debug(Object pattern, Object... args) {
		logger.debug(getMsgText(pattern, args).toString());
	}

	default void err(Object pattern, Object... args) {
		logger.error(getMsgText(pattern, args).toString());
	}
}
