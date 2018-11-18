package org.sdjen.download.cache_sis.store;

public interface IStore {

	String getKey(final String id, final String page, final String url, String title, String dateStr) throws Throwable;

	String getLocalHtml(String key) throws Throwable;

	void saveURL(String url, String path) throws Throwable;

	void saveMD5(String md5, String path) throws Throwable;

	String getMD5_Path(String key) throws Throwable;

	String getURL_Path(String key) throws Throwable;

	void saveHtml(String key, String html) throws Throwable;

	void msg(Object obj, Object... params);

	void err(Object obj, Object... params);
	
	void refreshMsgLog();
}
