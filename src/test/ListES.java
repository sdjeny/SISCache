package test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.DownloadSingle;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.http.HttpFactory;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.log.LogUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.store.Store_ElasticSearch;
import org.sdjen.download.cache_sis.tool.ZipUtil;

public class ListES {
	GetConnection connection;
	ConfUtil conf;
	private String path_es_start;
	private DownloadSingle downloadSingle;

	private IStore store;

	public static void main(String[] args) throws Throwable {
		ListES listES = new ListES();
		listES.execute();
	}

	private void execute() throws Throwable {
		LogUtil.init();
		HttpFactory httpUtil = new HttpFactory();
		downloadSingle = new DownloadSingle();
		downloadSingle.setHttpUtil(httpUtil);
		int i = 0;
		getStore().refreshMsgLog();
		try {
			String min = "0";
			String rs = min;
			do {
				min = rs;
				rs = list(min);
				getStore().msg("{0}	~	{1}", min, rs);
				if (i++ % 20 == 0)
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

	private String list(String min) throws Throwable {
		String result = min;
		Map<Object, Object> params = ESMap.get();
		params.put("_source",
				ESMap.get()//
						.set("includes", Arrays.asList("id"))//
						.set("excludes", Arrays.asList("context"))//
		);
		// params.put("query", ESMap.get().set("term", ESMap.get().set("_id",
		// "4762486_1")));
		params.put("query",
				ESMap.get().set("bool",
						ESMap.get().set("must",
								Arrays.asList(//
										ESMap.get().set("range"//
												, ESMap.get().set("id"//
														, ESMap.get()//
																.set("gt", min)//
												// .set("lte", "4762486_1")//
												)//
										)//
										, ESMap.get().set("term", ESMap.get().set("page", "1"))//
								)))

		);
		params.put("sort",
				Arrays.asList(//
						ESMap.get()//
								.set("id.keyword", ESMap.get().set("order", "asc"))//
				// .set("page.keyword", ESMap.get().set("order", "asc"))//
				)//
		);
		params.put("size", 50);
		params.put("from", 0);
		String jsonParams = JsonUtil.toJson(params);
		// System.out.println(jsonParams);
		long l = System.currentTimeMillis();
		String js = getConnection().doPost(getPath_es_start() + "html/_doc/_search", jsonParams, new HashMap<>());
		l = System.currentTimeMillis() - l;
		// System.out.println(js);
		ESMap r = JsonUtil.toObject(js, ESMap.class);
		List<ESMap> hits = (List<ESMap>) r.get("hits", ESMap.class).get("hits");
		for (ESMap hit : hits) {
			// System.out.println(hit.get("_id", String.class));
			ESMap _source = hit.get("_source", ESMap.class);
			String id = _source.get("id", String.class);
			result = id.compareTo(result) > 0 ? id : result;
			getStore().msg(id);
			解析(id);
		}
		return result;
	}

	private void 解析(String id) throws Throwable {
		ESMap params = ESMap.get()//
				.set("_source", ESMap.get()//
						.set("includes", Arrays.asList("page", "title", "date_str"))//
				// .set("excludes", Arrays.asList("context"))//
				)//
				.set("query", ESMap.get().set("term", ESMap.get().set("id", id)))//
				.set("sort", Arrays.asList(ESMap.get().set("page.keyword", ESMap.get().set("order", "asc"))))//
				.set("size", 100)//
				.set("from", 0)//
		;
		String jsonParams = JsonUtil.toJson(params);
		long l = System.currentTimeMillis();
		String js = getConnection().doPost(getPath_es_start() + "html/_doc/_search", jsonParams, new HashMap<>());
		l = System.currentTimeMillis() - l;
		// System.out.println(js);
		ESMap r = JsonUtil.toObject(js, ESMap.class);
		ESMap h = r.get("hits", ESMap.class);
		int total = (int) h.get("total");
		// System.out.println(total);
		List<ESMap> hits = (List<ESMap>) h.get("hits");
		for (ESMap hit : hits) {
			ESMap _source = hit.get("_source", ESMap.class);
			String page = _source.get("page", String.class);
			String title = _source.get("title", String.class);
			String dateStr = _source.get("date_str", String.class);
			if (null != dateStr && dateStr.contains(" "))
				dateStr = dateStr.substring(0, dateStr.indexOf(" ")).trim();
			// String context = _source.get("context", String.class);
			// String context_zip = _source.get("context_zip",
			// String.class);
			// ESMap context_comments = _source.get("context_comments",
			// ESMap.class);
			downloadSingle.startDownload("torrent", id, page//
					, String.format("http://www.sexinsex.net/bbs/viewthread.php?tid=%s&page=%s", id, page)//
					// http://www.sexinsex.net/bbs/thread-%s-%s-300.html
					, title, dateStr);
		}
		// throw new Exception("test");

	}
}
