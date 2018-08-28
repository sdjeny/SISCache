package org.sdjen.download.cache_sis.conf;

import java.io.File;
import java.io.IOException;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

public class MapDBUtil {
	DB rdb;
	DB wdb;
	HTreeMap<String, String> readFileMap;
	HTreeMap<String, String> readUrlMap;
	HTreeMap<String, String> writeFileMap;
	HTreeMap<String, String> writeUrlMap;

	public static void main(String[] args) throws IOException {
		File file = new File("WEBCACHE");
		if (!file.exists())
			file.mkdirs();
		MapDBUtil mapDBUtil = new MapDBUtil();
		mapDBUtil.getWriteFileMap().put("hehe", "RREEE");
		System.out.println(mapDBUtil.getReadFileMap().get("hehe"));
		System.out.println(mapDBUtil.getReadFileMap().size());
		mapDBUtil.commit();
		System.out.println(mapDBUtil.getReadFileMap().get("hehe"));
		System.out.println(mapDBUtil.getReadFileMap().size());
	}

	public MapDBUtil() throws IOException {
		String path = ConfUtil.getDefaultConf().getProperties().getProperty("save_path") + "/map.db";
		wdb = DBMaker.fileDB(path)//
				.checksumHeaderBypass()//
				.transactionEnable()//
				.closeOnJvmShutdown()//
				.fileLockDisable()//
				.make();
		writeFileMap = wdb.hashMap("file-md5-path", Serializer.STRING, Serializer.STRING)//
				// .counterEnable()// 实时更新Map.size
				.createOrOpen();
		writeUrlMap = wdb.hashMap("url-path", Serializer.STRING, Serializer.STRING).createOrOpen();
		commit();
		rdb = DBMaker.fileDB(path)//
				.closeOnJvmShutdown()//
				.readOnly()//
				.make();
		readFileMap = rdb.hashMap("file-md5-path", Serializer.STRING, Serializer.STRING).open();
		readUrlMap = rdb.hashMap("url-path", Serializer.STRING, Serializer.STRING).open();
	}

	public HTreeMap<String, String> getReadFileMap() {
		return readFileMap;
	}

	public HTreeMap<String, String> getReadUrlMap() {
		return readUrlMap;
	}

	public HTreeMap<String, String> getWriteFileMap() {
		return writeFileMap;
	}

	public HTreeMap<String, String> getWriteUrlMap() {
		return writeUrlMap;
	}

	public void commit() {
		wdb.commit();
	}

	public void finish() {
		rdb.close();
		wdb.close();
	}
}
