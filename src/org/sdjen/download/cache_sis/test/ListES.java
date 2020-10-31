package org.sdjen.download.cache_sis.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sdjen.download.cache_sis.DownloadList;
import org.sdjen.download.cache_sis.DownloadSingle;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.configuration.ConfUtil;
import org.sdjen.download.cache_sis.http.HttpFactory;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.log.LogUtil;
import org.sdjen.download.cache_sis.store.IStore;

public class ListES {
	private final static Logger logger = Logger.getLogger(ListES.class.toString());
	GetConnection connection;
	ConfUtil conf;
	private String path_es_start;
	private DownloadSingle downloadSingle;

	private IStore store;
	private static int size = 300;
	private static int limit = 20;
	private static String type = "";

	public static void main(String[] args) throws Throwable {// 6047836
		if (null == args)
			return;
		ListES listES = new ListES();
		String from = "0";
		if (args.length > 0)
			from = args[0];
		if (args.length > 1)
			size = Integer.valueOf(args[1]);
		if (args.length > 2)
			limit = Integer.valueOf(args[2]);
		if (args.length > 3)
			type = args[3];
		try {
			Integer.valueOf(from);
			listES.execute(from);
		} catch (Exception e1) {
			try {
				new DownloadList().execute(type, size, limit + size);
			} catch (Throwable e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}

	private void execute(String mins) throws Throwable {
		LogUtil.init();
		HttpFactory httpUtil = new HttpFactory();
		downloadSingle = new DownloadSingle();
//		downloadSingle.setHttpUtil(httpUtil);
		int i = 0;
		// getStore().refreshMsgLog();
		Long min = 0l;
		try {
			min = Long.valueOf(mins);
		} catch (Exception e) {
		}
		try {
			Long rs = min;
			do {
				min = rs;
				rs = list(min);
			} while (rs.compareTo(min) > 0);
		} finally {
			httpUtil.finish();
			LogUtil.finishAll();
		}
	}

	public IStore getStore() throws Exception {
//		if (null == store)
//			store = Store_ElasticSearch.getStore();
		return store;
	}

	public GetConnection getConnection() throws IOException {
		if (null == connection) {
			connection = new GetConnection();
		}
		return connection;
	}

	public ConfUtil getConf() throws IOException {
		if (null == conf)
			conf = ConfUtil.getDefaultConf();
		return conf;
	}

	public String getPath_es_start() throws IOException {
		if (null == path_es_start)
			path_es_start = getConf().getProperties().getProperty("path_es_start");
		return path_es_start;
	}

	private Long list(Long min) throws Throwable {
		Long result = min, startTime = System.currentTimeMillis();
		Map<Object, Object> params = ESMap.get();
		// params.put("_source",
		// ESMap.get()//
		// .set("includes", Arrays.asList("id"))//
		//// .set("excludes", Arrays.asList("context"))//
		// );
		params.put("query", ESMap.get().set("range"//
				, ESMap.get().set("id"//
						, ESMap.get()//
								.set("gt", min)//
				// .set("lte", "4762486_1")//
				)//
		)//
		);
		params.put("sort", Arrays.asList(//
				ESMap.get()//
						.set("id", ESMap.get().set("order", "asc"))//
		)//
		);
		params.put("size", size);
		params.put("from", 0);
		String jsonParams = JsonUtil.toJson(params);
		long l = System.currentTimeMillis();
		String js = getConnection().doPost(getPath_es_start() + "ids/_doc/_search", jsonParams, new HashMap<>());
		l = System.currentTimeMillis() - l;
		// System.out.println(js);
		ESMap r = JsonUtil.toObject(js, ESMap.class);
		ESMap h = r.get("hits", ESMap.class);
		List<ESMap> hits = (List<ESMap>) h.get("hits");
		ExecutorService executor = Executors.newFixedThreadPool(3);
		List<Future<Long>> resultList = new ArrayList<Future<Long>>();
		for (ESMap hit : hits) {
			// System.out.println(hit.get("_id", String.class));
			ESMap _source = hit.get("_source", ESMap.class);
			Long id = Long.valueOf(_source.get("id").toString());
			result = id.compareTo(result) > 0 ? id : result;
			resultList.add(executor.submit(new Callable<Long>() {
				public Long call() throws Exception {
					try {
						解析(hit.get("_source", ESMap.class));
					} catch (Throwable e) {
						if (e instanceof Exception) {
							throw (Exception) e;
						} else
							throw new Exception(e);
					}
					return null;
				}
			}));
		}
		long sTime = System.currentTimeMillis() - startTime;
		executor.shutdown();
		for (Future<Long> fs : resultList) {
			try {
				fs.get(30, TimeUnit.MINUTES);
			} catch (java.util.concurrent.TimeoutException e) {
				fs.cancel(false);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
		}
		getStore().msg("查:{3}ms	共:{4}ms	{0}~{1}	total:{2}", min, result, h.get("total"), sTime,
				(System.currentTimeMillis() - startTime));
		HttpFactory.getPoolConnManager().closeExpiredConnections();
		return result;
	}

	private void 解析(ESMap _source) throws Throwable {
		Long id = Long.valueOf(_source.get("id").toString());
		Long min = Long.valueOf(_source.get("min").toString());
		Long max = Long.valueOf(_source.get("max").toString());
		String title = _source.get("title", String.class);
		for (Long page = min; page <= Math.min(max, 30); page++) {
			String url = String.format("http://www.sexinsex.net/bbs/viewthread.php?tid=%s&page=%s", id.toString(),
					page.toString());
			// System.out.println(url);torrent cover
			downloadSingle.startDownload(type, "143", id.toString(), page.toString()//
					, url// http://www.sexinsex.net/bbs/thread-%s-%s-300.html
					, title, null);
		}
		// throw new Exception("test");
	}
}
