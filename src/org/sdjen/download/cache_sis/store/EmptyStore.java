package org.sdjen.download.cache_sis.store;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service("Store_Empty")
public class EmptyStore implements IStore {

	@Override
	public String getLocalHtml(String id, String page) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveURL(String url, String path) throws Throwable {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveMD5(String md5, String path) throws Throwable {
		// TODO Auto-generated method stub

	}

	@Override
	public String getMD5_Path(String key) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getURL_Path(String key) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveHtml(String id, String page, String url, String title, String dateStr, String html)
			throws Throwable {
		// TODO Auto-generated method stub

	}

	@Override
	public void refreshMsgLog() {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, Object> getTitleList(int page, int size, String query, String order) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

}
