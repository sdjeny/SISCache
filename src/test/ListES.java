package test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.store.Store_ElasticSearch;
import org.sdjen.download.cache_sis.tool.ZipUtil;

public class ListES {
	GetConnection connection;
	ConfUtil conf;
	private String path_es_start;

	private IStore store;

	public static void main(String[] args) throws Throwable {
		ListES listES = new ListES();
		String min = "0";
		String rs = min;
		do {
			min = rs;
			rs = listES.list(min);
		} while (rs.compareTo(min) > 0);
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
		params.put("size", 20);
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
			System.out.println(id);
			result = id.compareTo(result) > 0 ? id : result;
			解析(id);
		}
		System.out.println("-----------------------------------");
		return result;
	}

	private void 解析(String id) throws Throwable {
		ESMap params = ESMap.get()//
				// .set("_source", ESMap.get()//
				// // .set("includes",
				// // Arrays.asList("id"))//
				// .set("excludes", Arrays.asList("context"))//
				// )//
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
		if (true || total > 1) {
			for (ESMap hit : hits) {
				ESMap _source = hit.get("_source", ESMap.class);
				String context = _source.get("context", String.class);
				// getStore().saveHtml(hit.get("_id", String.class), context);
				org.jsoup.nodes.Document doument = Jsoup.parse(context);
				ESMap comments = ESMap.get();
				{
					for (org.jsoup.nodes.Element postcontent : doument.select("div.mainbox.viewthread")//// class=mainbox的div
							.select("table")//
							.select("tbody")//
							.select("tr")//
							.select("td.postcontent")//
					//
					) {
						String floor = "";
						for (org.jsoup.nodes.Element postinfo : postcontent.select("div.postinfo")) {
							org.jsoup.nodes.Element temp = postinfo.select("strong").first();
							if (null != temp) {
								floor = temp.ownText();
							}
						}
						if (floor.isEmpty())
							continue;
						String fm = comments.get(floor, String.class);
						if (null == fm)
							fm = "";
						for (org.jsoup.nodes.Element comment : postcontent.select("div.postmessage.defaultpost").select("div.t_msgfont")) {
							if (!fm.isEmpty())
								fm += ",";
							fm += comment.text();
						}
						comments.set(floor, fm);
					}
				}
				String json = JsonUtil.toJson(comments);
				// System.out.println(ZipUtil.uncompress(ZipUtil.compress(json)));deplop_
				System.out.println(context.getBytes().length//
						+ "	VS	" + (ZipUtil.zipString(context).getBytes().length + json.getBytes().length)//
						+ "	VS	" + (ZipUtil.compress(context).getBytes().length + json.getBytes().length)//
						+ "	VS	" + (ZipUtil.compress(context, 1).getBytes().length + json.getBytes().length)//
				);
			}
			// throw new Exception("test");
		}
	}
}
