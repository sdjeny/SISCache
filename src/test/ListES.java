package test;

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

import org.sdjen.download.cache_sis.DownloadSingle;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.http.HttpFactory;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.log.LogUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.store.Store_ElasticSearch;

public class ListES {
	GetConnection connection;
	ConfUtil conf;
	private String path_es_start;
	private DownloadSingle downloadSingle;

	private IStore store;

	public static void main(String[] args) throws Throwable {// 6047836
		ListES listES = new ListES();
		listES.execute((null == args || args.length < 1) ? "0" : Integer.valueOf(args[0]).toString());
	}

	private void execute(String mins) throws Throwable {
		LogUtil.init();
		HttpFactory httpUtil = new HttpFactory();
		downloadSingle = new DownloadSingle();
		downloadSingle.setHttpUtil(httpUtil);
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
				if (++i % 20 == 0)
					getStore().refreshMsgLog();
			} while (rs.compareTo(min) > 0);
		} finally {
			httpUtil.finish();
			LogUtil.finishAll();
		}
	}

	public IStore getStore() throws Exception {
		if (null == store)
			store = Store_ElasticSearch.getStore();
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
		params.put("query",
				ESMap.get().set("range"//
						, ESMap.get().set("id"//
								, ESMap.get()//
										.set("gt", min)//
						// .set("lte", "4762486_1")//
						)//
				)//
		);
		params.put("sort",
				Arrays.asList(//
						ESMap.get()//
								.set("id", ESMap.get().set("order", "asc"))//
				)//
		);
		params.put("size", 100);
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
		getStore().msg("耗时:{3}ms	{0}~{1}	total:{2}", min, result, h.get("total"), (System.currentTimeMillis() - startTime));
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
		HttpFactory.getPoolConnManager().closeExpiredConnections();
		return result;
	}

	private void 解析(ESMap _source) throws Throwable {
		Long id = Long.valueOf(_source.get("id").toString());
		Long min = Long.valueOf(_source.get("min").toString());
		Long max = Long.valueOf(_source.get("max").toString());
		String title = _source.get("title", String.class);
		for (Long page = min; page <= Math.min(max, 30); page++) {
			String url = String.format("http://www.sexinsex.net/bbs/viewthread.php?tid=%s&page=%s", id.toString(), page.toString());
			// System.out.println(url);torrent cover
			downloadSingle.startDownload("torrent", id.toString(), page.toString()//
					, url// http://www.sexinsex.net/bbs/thread-%s-%s-300.html
					, title, null);
		}
		// throw new Exception("test");
	}
}
