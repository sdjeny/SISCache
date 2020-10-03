package org.sdjen.download.cache_sis.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.DownloadList;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.http.DefaultCss;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.service.CopyEsToMongo;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.timer.SISDownloadTimer;
import org.sdjen.download.cache_sis.util.EntryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.core.JsonProcessingException;

@Controller
@EnableAutoConfiguration
@RequestMapping("/siscache")
public class Controller_siscache {
	private final static Logger logger = LoggerFactory.getLogger(Controller_siscache.class);
//	@Autowired
//	private HttpUtil httpUtil;
	@Autowired
	private SISDownloadTimer timer;
	@Autowired
	private DownloadList downloadList;
	@Autowired
	private CopyEsToMongo copyEsToMongo;
	@Autowired
	private MongoTemplate mongoTemplate;
	@Resource(name = "${definde.service.name.store}")
	private IStore store;
	@Value("${siscache.conf.can_restart}")
	private boolean can_restart = true;
	@Value("${siscache.conf.can_reload}")
	private boolean can_reload = true;
	@Value("${siscache.conf.can_copy_es_mongo}")
	private boolean can_copy_es_mongo = true;
//	static ConfUtil conf;
//	private String path_es_start;

//	public static ConfUtil getConf() throws IOException {
//		if (null == conf)
//			conf = ConfUtil.getDefaultConf();
//		return conf;
//	}

//	public String getPath_es_start() throws IOException {
//		if (null == path_es_start)
//			path_es_start = getConf().getProperties().getProperty("path_es_start");
//		return path_es_start;
//	}

//	public GetConnection getConnection() throws IOException {
//		if (null == connection) {
//			connection = new GetConnection();
//		}
//		return connection;
//	}

	@RequestMapping("/help")
	@ResponseBody
	String help() {
		StringBuilder rst = new StringBuilder();
		rst.append("</br><table border='0'>");
		{
			rst.append("<tbody><tr>");
			rst.append(String.format("<td>%s</td>", "Fields"));
			rst.append(String.format("<td>%s</td>",
					"id,fid,datetime,type,title,page,context,context_comments,context_zip,author"));
			rst.append(String.format("<td>%s</td>", ""));
			rst.append("</tr></tbody>");
		}
		{
		}
		IStore.FIDDESCES.forEach((k, v) -> {
			rst.append("<tbody><tr>");
			rst.append(String.format(
					"<td><a href='/siscache/list/%s/1/100?debug=true' title='新窗口打开' target='_blank'>%s</a></td>", k,
					v));
			rst.append(String.format("<td>%s</td>", v));
			rst.append(String.format("<td>%s</td>", ""));
			rst.append("</tr></tbody>");
		});
		{
			rst.append("<tbody><tr>");
			rst.append("<td><a href='/siscache/list/all/1/100?debug=true' title='新窗口打开' target='_blank'>list</a></td>");
			rst.append(String.format("<td>%s</td>", "List ALL"));
			rst.append(String.format("<td>%s</td>", ""));
			rst.append("</tr></tbody>");
		}
		{
			rst.append("<tbody><tr>");
			rst.append(
					"<td><a href='/siscache/list/all/1/100?debug=true&q=type:新片;title:~碧 ~筱 -白&order=datetime.keyword:desc id' title='新窗口打开' target='_blank'>Search(eg.)</a></td>");
			rst.append(String.format("<td>%s</td>", "q=type:新片;title:~碧 ~筱 -白"));
			rst.append(String.format("<td>%s</td>", "~:or -:not"));
			rst.append("</tr></tbody>");
		}
		{
			rst.append("<tbody><tr>");
			rst.append(
					"<td><a href='/siscache/list/all/1/100?debug=true&order=datetime.keyword:desc id' title='新窗口打开' target='_blank'>Order</a></td>");
			rst.append(String.format("<td>%s</td>", "order=datetime.keyword:desc id"));
			rst.append(String.format("<td>%s</td>", ""));
			rst.append("</tr></tbody>");
		}
		if (can_copy_es_mongo) {
			Set<String> running = store.getRunnings();
			if (!running.contains("es_mongo_html")) {
				rst.append("<tbody><tr>");
				rst.append(
						"<td><a href='/siscache/copy/es/mongo/html' title='新窗口打开' target='_blank'>es->mongo html</a></td>");
				rst.append(String.format("<td>%s</td>", "copy html"));
				rst.append(String.format("<td>%s</td>", ""));
				rst.append("</tr></tbody>");
			}
			if (!running.contains("es_mongo_url")) {
				rst.append("<tbody><tr>");
				rst.append(
						"<td><a href='/siscache/copy/es/mongo/url' title='新窗口打开' target='_blank'>es->mongo url</a></td>");
				rst.append(String.format("<td>%s</td>", "copy url"));
				rst.append(String.format("<td>%s</td>", ""));
				rst.append("</tr></tbody>");
			}
			if (!running.contains("es_mongo_path")) {
				rst.append("<tbody><tr>");
				rst.append(
						"<td><a href='/siscache/copy/es/mongo/path' title='新窗口打开' target='_blank'>es->mongo path</a></td>");
				rst.append(String.format("<td>%s</td>", "copy path"));
				rst.append(String.format("<td>%s</td>", ""));
				rst.append("</tr></tbody>");
			}
		}
		if (can_restart) {
			rst.append("<tbody><tr>");
			rst.append("<td><a href='/siscache/restart/2' title='新窗口打开' target='_blank'>restart</a></td>");
			rst.append(String.format("<td>%s</td>", "restart/hours"));
			rst.append(String.format("<td>%s</td>", ""));
			rst.append("</tr></tbody>");
		}
		if (can_reload) {
			rst.append("<tbody><tr>");
			rst.append(
					"<td><a href='/siscache/cache/1/30/torrent,image' title='新窗口打开' target='_blank'>reload</a></td>");
			rst.append(String.format("<td>%s</td>", "cache/from/to/[torrent,image,cover]"));
			rst.append(String.format("<td>%s</td>", ""));
			rst.append("</tr></tbody>");
		}
		rst.append("</table>");
		return rst.toString(); // XXX
	}

	File cacheResultFile;

	public File getCacheResultFile() throws IOException {
		if (null == cacheResultFile) {

			ConfUtil conf = ConfUtil.getDefaultConf();
			String save_path = conf.getProperties().getProperty("save_path");
			String charset = conf.getProperties().getProperty("chatset");
			cacheResultFile = new File(save_path + "/download.log");
		}
		return cacheResultFile;
	}

	@RequestMapping("/copy/es/mongo/{type}")
	@ResponseBody
	private String copyEsToMongo(@PathVariable("type") String type) {
		new Thread(new Runnable() {
			public void run() {
				try {
					copyEsToMongo.copy(type);
				} catch (Throwable e) {
					logger.error(e.getMessage(), e);
				}
			}
		}).start();
		try {
			return JsonUtil.toJson(mongoTemplate.findOne(
					new Query().addCriteria(Criteria.where("type").is("es_mongo_" + type)), Map.class, "last"));
		} catch (JsonProcessingException e) {
			return e.getMessage();
		} // "redirect:/siscache/list/all/1/100?debug=true";
	}

	@RequestMapping("/cache_result")
	@ResponseBody
	String cache_result() {
		StringBuilder result = new StringBuilder(
		// " <meta http-equiv='Content-Type' content='text/html; charset=gb2312'
		// /> "
		);
		BufferedReader reader = null;
		try {
			File file = getCacheResultFile();
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "GBK"));
			String tempString = null;
			while ((tempString = reader.readLine()) != null) {
				result.append(tempString).append("<br>");
			}
			reader.close();
		} catch (IOException e) {
			result.append(e.getMessage());
			// e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}
		return result.toString();
	}

	@RequestMapping("/cache")
	String cache() {
		return cache(1, 1);
	}

	@RequestMapping("/cache/{from}/{to}")
	String cache(@PathVariable("from") int from, @PathVariable("to") int to) {
		return cache(from, to, "");
	}

	@RequestMapping("/cache/{from}/{to}/{type}")
	@ResponseBody
	String cache(@PathVariable("from") int from, @PathVariable("to") int to, @PathVariable("type") final String type) {
		ConfUtil.reload();
		new Thread(new Runnable() {
			public void run() {

				try {
					logger.info(type + "	" + from + "	" + to);
					downloadList.execute(type, from, to);
				} catch (Throwable e) {
					logger.error(e.getMessage(), e);
				}
			}
		}).start();
//		return list(1);
		try {
			return JsonUtil.toJson(store.getLast("download_list"));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return e.getMessage();
		}
		// return "redirect:/siscache/cache_result";
	}

	@RequestMapping("/restart/{hours}")
	@ResponseBody
	String restart(@PathVariable("hours") int hours) {
		timer.restart(hours);
		return list(1);
	}

	@RequestMapping("/list/{page}")
	@ResponseBody
	String list(@PathVariable("page") int page) {
		return list("all", page, 50);
	}

	// @RequestMapping("/list/{search}")
	// @ResponseBody
	// String list(@PathVariable("search") String search) {
	// return list(search, 1, 40);
	// }

	@RequestMapping("/list/{fid}/{page}/{size}")
	@ResponseBody
	String list(@PathVariable("fid") String fid, @PathVariable("page") int page, @PathVariable("size") int size) {
		return list(fid, page, size, "");
	}

	// @RequestMapping("/list/{page}/{size}")
	// @ResponseBody
	// String list(@PathVariable("page") int page, @PathVariable("size") int
	// size) {
	// return list("", page, 40);
	// }

	@RequestMapping("/list/{fid}/{page}/{size}/{search}")
	@ResponseBody
	String list(@PathVariable("fid") String fid, @PathVariable("page") int page, @PathVariable("size") int size,
			@PathVariable("search") String search) {
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes();
		HttpServletRequest request = requestAttributes.getRequest();
		boolean debug = false;
		String query = "";
		String temp;
		if (null != (temp = request.getParameter("debug")))
			debug = Boolean.valueOf(temp);
		if (null != (temp = request.getParameter("q"))) {
			query = temp;
		} else {
			query = search;
			if (null != query && !query.isEmpty() && !query.toUpperCase().startsWith("D:")) {
				query = "ALL:" + query;
			}
		}
		StringBuffer rst = new StringBuffer();
		try {
			Map<String, Object> titleList = store.getTitleList(fid, page, size, query, request.getParameter("order"));
			List<Map<String, Object>> ls = (List<Map<String, Object>>) titleList.get("list");
			long l = System.currentTimeMillis();
			l = System.currentTimeMillis() - l;
			// logger.log(Level.INFO, l + " " + jsonParams);
			rst.append("total:");
			rst.append(titleList.get("total"));
			rst.append("&nbsp;Take:");
			rst.append(l);
			if (debug) {
				rst.append("&nbsp;");
				rst.append(titleList.get("json_params"));
			}
			rst.append("</br><table border='0'>");
			ls.forEach(_source -> {
				rst.append("<tbody><tr>");
				String datestr = (String) _source.get("date"), timestr = (String) _source.get("time");
				if (false) {
					rst.append(String
							.format("<td><a href='/siscache/detail/%s/%s' title='新窗口打开' target='_blank'>%s</a></td>"//
					, _source.get("id"), _source.get("page"), String.format("%s&nbsp;%s&nbsp;%s&nbsp;%s", datestr,
							timestr, _source.get("type"), _source.get("title"))));
				} else {
					if (false) {
						rst.append(String.format("<td>%s</td>", datestr));
						rst.append(String.format("<td>%s</td>", timestr));
						rst.append(String.format("<td align='center'>%s</td>", _source.get("type")));
					} else {
						rst.append(String.format("<td>%s&nbsp;%s&nbsp;%s&nbsp;%s</td>", datestr, timestr,
								"ALL".equalsIgnoreCase(fid) ? IStore.FIDDESCES.get(_source.get("fid")) : "",
								_source.get("type")));
					}
					rst.append("</tr></tbody>");
					rst.append("<tbody><tr>");
					rst.append(String
							.format("<td><a href='/siscache/detail/%s/%s' title='新窗口打开' target='_blank'>%s</a></td>"//
					, _source.get("id"), _source.get("page"), _source.get("title")));
				}
				rst.append("</tr></tbody>");
				// rst.append("</br>");

			});
			rst.append("</table>");
		} catch (Throwable e) {
			rst.append(e.getMessage());
			for (java.lang.StackTraceElement element : e.getStackTrace()) {
				rst.append(element);
				rst.append("</br>");
				rst.append(element.toString());
			}
			e.printStackTrace();
		}
		return rst.toString();
	}

	@RequestMapping("/s/{search}")
	@ResponseBody
	String search(@PathVariable("search") String search) {
		return list("143", 1, 1000, search);
//		Map<String, Object> params = new HashMap<>();
//		Map<String, Object> _source = new HashMap<>();
//		_source.put("includes", Arrays.asList());
//		_source.put("excludes", Arrays.asList("context"));
//		params.put("_source", _source);
//		params.put("query", Collections.singletonMap("match", Collections.singletonMap("title", search)));
//		params.put("sort", Arrays.asList(//
//				Collections.singletonMap("id.keyword", Collections.singletonMap("order", "desc"))//
//		)//
//		);
//		params.put("size", 1000);
//		params.put("from", 1);
//		StringBuffer rst = new StringBuffer();
//		try {
//			String js = httpUtil.doLocalPostUtf8Json(getPath_es_start() + "html/_doc/_search?pretty",
//					JsonUtil.toJson(params));
//			logger.debug(js);
//			Map<String, Object> r = JsonUtil.toObject(js, Map.class);
//
//			List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) r.get("hits"))
//					.get("hits");
//			for (Map<String, Object> hit : hits) {
//				_source = (Map<String, Object>) hit.get("_source");
//
//				rst.append("<tbody><tr>");
//				rst.append(String.format("<td>%s</td>", _source.get("dat")));
//				rst.append(String.format("<td>%s</td>", "    "));
//				rst.append(
//						String.format("<td><a href='../siscache/detail/%s' title='新窗口打开' target='_blank'>%s</a></td>",
//								_source.get("id"), _source.get("title")));
//
//				rst.append("</tr></tbody>");
//				rst.append("</br>");
//			}
//		} catch (IOException e) {
//			rst.append(e.getMessage());
//			for (java.lang.StackTraceElement element : e.getStackTrace()) {
//				rst.append(element.toString());
//			}
//		}
//		return rst.toString();
	}

	@RequestMapping("/detail/{id}")
	@ResponseBody
	String detail(@PathVariable("id") String id) {
		if (id.startsWith("thread")) {
			String[] ss = id.split("-");
			return detail(ss[1], ss[2]);
		} else
			return detail(id, "1");
	}

	@RequestMapping("/detail/{id}/redirect.php")
	String detail_redirect(@PathVariable("id") String id) {
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes();
		HttpServletRequest request = requestAttributes.getRequest();
		String fid = request.getParameter("fid");
		String tid = request.getParameter("tid");
		String gto = request.getParameter("goto");// nextnewset/nextoldset
		return "redirect:/siscache/detail/" + tid;
	}

	@RequestMapping("/detail/{id}/viewthread.php")
	String detail_viewthread(@PathVariable("id") String id) {
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
				.getRequestAttributes();
		HttpServletRequest request = requestAttributes.getRequest();
		String tid = request.getParameter("tid");
		return "redirect:/siscache/detail/" + tid;
	}

	@RequestMapping("/detail/{id}/{page}")
	@ResponseBody
	String detail(@PathVariable("id") String id, @PathVariable("page") String page) {
		if (page.startsWith("thread")) {
			String[] ss = page.split("-");
			if (ss.length > 2)
				page = ss[2];
		}
		Map<String, Object> params = new HashMap<>();
		ESMap _source = ESMap.get();
		_source.put("includes", Arrays.asList("context*"));
		_source.put("excludes", Arrays.asList());
		params.put("_source", _source);
		params.put("query", Collections.singletonMap("bool"//
				, Collections.singletonMap("filter", Arrays.asList(//
						Collections.singletonMap("term", Collections.singletonMap("id", id))//
						, Collections.singletonMap("term", Collections.singletonMap("page", page))//
				))//
		)//
		);
		StringBuffer rst = new StringBuffer();
		try {
			String text = store.getLocalHtml(id, page);
			org.jsoup.nodes.Document doument = Jsoup.parse(text);
			boolean update = false;
			for (org.jsoup.nodes.Element e : doument.select("head").select("style")) {
				if (e.text().isEmpty()) {
					update = true;
					e.text(DefaultCss.getCss());
				}
			}
			for (org.jsoup.nodes.Element e : doument//
					.select("div.mainbox.viewthread")//
					.select("td.postcontent")//
					.select("div.postmessage.defaultpost")//
					.select("div.box.postattachlist")//
					.select("dl.t_attachlist")//
					.select("a[href]")//
			) {
				String href = e.attr("href");
				if (href.startsWith("../../torrent/20")) {
					update = true;
					e.attr("href", "../" + href);
				} else if (href.startsWith("../")) {
					href = href.replace("../", "");
					if (href.startsWith("http")) {
						update = true;
						e.attr("href", href);
					}
				}
			}
			for (org.jsoup.nodes.Element e : doument.select("img[src]")) {
				String src = e.attr("src");
				if (src.startsWith("../../images/20")) {
					update = true;
					e.attr("src", "../" + src);
				}
			}
			if (update) {
				text = doument.html();
			}
			rst.append(text);
		} catch (Throwable e) {
			e.printStackTrace();
			rst.append(e.getMessage());
			for (java.lang.StackTraceElement element : e.getStackTrace()) {
				rst.append(element.toString());
			}
		}
		return rst.toString();
	}
}
