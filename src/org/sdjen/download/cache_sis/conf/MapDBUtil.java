package org.sdjen.download.cache_sis.conf;

import java.io.IOException;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

public class MapDBUtil {
	DB db;
	HTreeMap<String, String> fileMap;
	HTreeMap<String, String> urlMap;

	public MapDBUtil() throws IOException {
		db = DBMaker.fileDB(ConfUtil.getDefaultConf().getProperties().getProperty("save_path") + "/map.db").make();
		fileMap = db.hashMap("file-md5-path", Serializer.STRING, Serializer.STRING).counterEnable().createOrOpen();
		urlMap = db.hashMap("url-path", Serializer.STRING, Serializer.STRING).counterEnable().createOrOpen();
	}

	public HTreeMap<String, String> getFileMap() {
		return fileMap;
	}

	public HTreeMap<String, String> getUrlMap() {
		return urlMap;
	}

	public DB getDb() {
		return db;
	}

	public void finish() {
		db.close();
	}
}
