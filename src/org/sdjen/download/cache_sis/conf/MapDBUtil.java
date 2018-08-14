package org.sdjen.download.cache_sis.conf;

import java.io.IOException;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

public class MapDBUtil {
	DB db;
	HTreeMap<String, String> map;

	public static void main(String[] args) throws IOException {
		MapDBUtil dbUtil = new MapDBUtil();
		dbUtil.getMap().put("1", "2");
		dbUtil.finish();
		dbUtil = new MapDBUtil();
		System.out.println(dbUtil.getMap().keySet());
		System.out.println(dbUtil.getMap().values());
		dbUtil.finish();
	}

	public MapDBUtil() throws IOException {
		db = DBMaker.fileDB(ConfUtil.getDefaultConf().getProperties().getProperty("save_path") + "/map.db").make();
		map = db.hashMap("", Serializer.STRING, Serializer.STRING).counterEnable().createOrOpen();
	}

	public HTreeMap<String, String> getMap() {
		return map;
	}

	public DB getDb() {
		return db;
	}

	public void finish() {
		db.close();
	}
}
