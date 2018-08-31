package org.sdjen.download.cache_sis.log;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.sdjen.download.cache_sis.conf.ConfUtil;

public class MapDBFactory {
	private String name;
	private DB rdb;
	private DB wdb;
	private HTreeMap<String, String> rMap;
	private HTreeMap<String, String> wMap;
	private static MapDBFactory fileDB;
	private static MapDBFactory urlDB;
	private static MapDBFactory errorDB;

	public static void init() throws IOException {
		fileDB = new MapDBFactory().setName("file-md5-path");
		urlDB = new MapDBFactory().setName("url-path");
		errorDB = new MapDBFactory().setName("error");
	}

	public static void finishAll() {
		fileDB.finish();
		urlDB.finish();
		errorDB.finish();
	}

	public MapDBFactory setName(String name) throws IOException {
		this.name = name;
		String path = ConfUtil.getDefaultConf().getProperties().getProperty("save_path") + "/db/";
		File file = new File(path);
		if (!file.exists())
			file.mkdirs();
		path = path + name + ".db";
		wdb = DBMaker.fileDB(path)//
				.checksumHeaderBypass()//
				.transactionEnable()//
				.closeOnJvmShutdown()//
				.fileLockDisable()//
				.make();
		wMap = wdb.hashMap("map", Serializer.STRING, Serializer.STRING)//
				// .counterEnable()// 实时更新Map.size
				.createOrOpen();
		wdb.commit();
		rdb = DBMaker.fileDB(path)//
				.closeOnJvmShutdown()//
				.readOnly()//
				.make();
		rMap = rdb.hashMap("map", Serializer.STRING, Serializer.STRING).open();
		return this;
	}

	public static MapDBFactory getFileDB() {
		return fileDB;
	}

	public static MapDBFactory getUrlDB() {
		return urlDB;
	}

	public static MapDBFactory getErrorDB() {
		return errorDB;
	}

	public String get(String key) {
		return rMap.get(key);
	}

	public void put(String key, String value) {
		wMap.put(key, value);
		wdb.commit();
	}

	public int size() {
		return rMap.size();
	}

	public static void main(String[] args) throws IOException {
		File file = new File("WEBCACHE");
		if (!file.exists())
			file.mkdirs();
		MapDBFactory.init();
		String path = ConfUtil.getDefaultConf().getProperties().getProperty("save_path") + "/map.db";
		DB db = DBMaker.fileDB(path)//
				.closeOnJvmShutdown()//
				.readOnly()//
				.make();
		// db.hashMap("url-path", Serializer.STRING, Serializer.STRING).open()//
		// .forEach(new BiConsumer<String, String>() {
		//
		// public void accept(String key, String value) {
		// System.out.println("U: " + key + " : " + value);
		// MapDBFactory.getUrlDB().put(key, value);
		// }
		// });
		db.hashMap("file-md5-path", Serializer.STRING, Serializer.STRING).open()//
				.forEach(new BiConsumer<String, String>() {

					public void accept(String key, String value) {
						try {
							System.out.println("F:	" + key + "	:	" + value);
							MapDBFactory.getFileDB().put(key, value);
						} catch (Exception e) {
							MapDBFactory.getErrorDB().put(key, e.toString());
						}
					}
				});
		db.close();
		System.out.println("url:	" + MapDBFactory.getUrlDB().size());
		System.out.println("file:	" + MapDBFactory.getFileDB().size());
		System.out.println("err:	" + MapDBFactory.getErrorDB().size());
		MapDBFactory.finishAll();
	}

	// public void commit() {
	// wdb.commit();
	// }

	public void finish() {
		wdb.close();
		rdb.close();
	}
}
