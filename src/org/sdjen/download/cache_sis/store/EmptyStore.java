package org.sdjen.download.cache_sis.store;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
	public Map<String, Object> saveHtml(String id, String page, String url, String title, String dateStr, String html)
			throws Throwable {
		return null;
	}

	@Override
	public Set<String> getProxyUrls() {
		Set<String> s = new LinkedHashSet<String>();
		s.add("http://www.sexinsex.net");
		return s;
	}

	@Override
	public void addProxyUrl(String url) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeProxyUrl(String url) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, Object> getTitleList(String fid, int page, int size, String query, String order)
			throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void logFailedUrl(String url, Throwable e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void logSucceedUrl(String url) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Object> connectCheck(String url) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getLast(String type) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object running(String type, String keyword, String msg) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object finish(String type, String msg) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

}
