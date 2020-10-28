package org.sdjen.download.cache_sis.store;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.store.entity.Last;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("Store_Empty")
public class EmptyStore implements IStore {
	@Autowired
	private Dao dao;

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
		System.out.println(dao.getList("from Urls_failed", null));
		System.out.println(dao.getList("select url from Urls_proxy", null));
		Last last = null;//dao.find(Last.class, "init");
		if (null == last) {
			last = new Last();
			last.setType("init");
		}
		last.setKeyword(""//
				+ "0123456789"// 0
				+ "0123456789"// 1
				+ "0123456789"// 2
				+ "0123456789"// 3
				+ "0123456789"// 4
				+ "0123456789"// 5
				+ "0123456789"// 6
				+ "0123456789"// 7
				+ "0123456789"// 8
				+ "0123456789"// 9
				// ~~~~~~~~~~~~~~~~~~
				+ "0123456789"// 0
				+ "0123456789"// 1
				+ "0123456789"// 2
				+ "0123456789"// 3
				+ "0123456789"// 4
				+ "0123456789"// 5
				+ "0123456789"// 6
				+ "0123456789"// 7
				+ "0123456789"// 8
				+ "0123456789"// 9
				// ~~~~~~~~~~~~~~~~~~
				+ "0123456789"// 0
				+ "0123456789"// 1
				+ "0123456789"// 2
				+ "0123456789"// 3
				+ "0123456789"// 4
				+ "0123456789"// 5
				+ "0123456789"// 6
				+ "0123456789"// 7
				+ "0123456789"// 8
				+ "0123456789"// 9
		);
		dao.merge(last);
		System.out.println(dao.getList("select type from Last", null));
	}

	@Override
	public Map<String, Object> connectCheck(String url) throws Throwable {
		Map<String, Object> result = new HashMap<>();
		result.put("found", false);
		result.put("continue", true);
		result.put("msg", "");
		return result;
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

	@Override
	public String logview(String query) {
		try {
			return JsonUtil.toPrettyJson(dao.getList(query, null));
		} catch (Throwable e) {
			return e.getMessage();
		}
	}

	@Override
	public String logexe(String query) {
		try {
			return JsonUtil.toPrettyJson(dao.executeUpdate(query, null));
		} catch (Throwable e) {
			return e.getMessage();
		}
	}

}
