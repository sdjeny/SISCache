package org.sdjen.download.cache_sis.store;

import java.util.List;
import java.util.Map;

public interface IStore {
//	String getKey(final String id, final String page, final String url, String title, String dateStr) throws Throwable;

	String getLocalHtml(final String id, final String page, final String url, String title, String dateStr) throws Throwable;

	void saveURL(String url, String path) throws Throwable;

	void saveMD5(String md5, String path) throws Throwable;

	String getMD5_Path(String key) throws Throwable;

	String getURL_Path(String key) throws Throwable;

	void saveHtml(final String id, final String page, final String url, String title, String dateStr, String html) throws Throwable;

	void msg(Object obj, Object... params);

	void err(Object obj, Object... params);

	void refreshMsgLog();

	public Map<String, Object> getTitleList(int page, int size, String query, String order) throws Throwable;
}
