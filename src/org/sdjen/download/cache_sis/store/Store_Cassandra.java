package org.sdjen.download.cache_sis.store;

import java.io.IOException;
import java.util.Map;

import org.sdjen.download.cache_sis.log.CassandraFactory;

public class Store_Cassandra extends Store_File {

	private org.sdjen.download.cache_sis.log.CassandraFactory cassandraFactory;

	public Store_Cassandra() throws IOException {
		super();
		cassandraFactory = CassandraFactory.getDefaultFactory();
	}

	@Override
	public void saveURL(String url, String path) {
		cassandraFactory.saveURL(url, path);
	}

	@Override
	public void saveMD5(String md5, String path) {
		cassandraFactory.saveMD5(md5, path);
	}

	@Override
	public String getMD5_Path(String key) {
		return cassandraFactory.getMD5_Path(key);
	}

	@Override
	public String getURL_Path(String key) {
		return cassandraFactory.getURL_Path(key);
	}

	@Override
	public Map<String, Object> getTitleList(int page, int size, String query, String order) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

}
