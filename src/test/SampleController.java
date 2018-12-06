package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;

import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.DownloadList;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.http.DefaultCss;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.tool.ZipUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Controller
@EnableAutoConfiguration
@RequestMapping("/siscache")
public class SampleController {
	private final static Logger logger = Logger.getLogger(SampleController.class.toString());
	GetConnection connection;
	ConfUtil conf;

	private String path_es_start;

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

	public GetConnection getConnection() throws IOException {
		if (null == connection) {
			connection = new GetConnection();
		}
		return connection;
	}

	@RequestMapping("/hello")
	@ResponseBody
	String home() {
		return "Hello World!";
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
	String cache(@PathVariable("from") int from, @PathVariable("to") int to, @PathVariable("type") final String type) {
		try {
			new Thread(new Runnable() {
				public void run() {

					try {
						new DownloadList(type).execute(from, to);
					} catch (Throwable e) {
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}).start();
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return "redirect:/siscache/cache_result";
	}

	@RequestMapping("/list")
	@ResponseBody
	String list() {
		return list(1);
	}

	@RequestMapping("/list/{page}")
	@ResponseBody
	String list(@PathVariable("page") int page) {
		return list(page, 50, "");
	}

	// @RequestMapping("/list/{search}")
	// @ResponseBody
	// String list(@PathVariable("search") String search) {
	// return list(search, 1, 40);
	// }

	@RequestMapping("/list/{page}/{size}")
	@ResponseBody
	String list(@PathVariable("page") int page, @PathVariable("size") int size) {
		return list(page, size, "");
	}

	// @RequestMapping("/list/{page}/{size}")
	// @ResponseBody
	// String list(@PathVariable("page") int page, @PathVariable("size") int
	// size) {
	// return list("", page, 40);
	// }

	@RequestMapping("/list/{page}/{size}/{search}")
	@ResponseBody
	String list(@PathVariable("page") int page, @PathVariable("size") int size, @PathVariable("search") String search) {
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = requestAttributes.getRequest();
		boolean debug = false;
		String query = "";
		// String fieldstr = "";
		String temp;
		if (null != (temp = request.getParameter("debug")))
			debug = Boolean.valueOf(temp);
		if (null != (temp = request.getParameter("q"))) {
			query = temp;
		}
		String order = "";
		Map<Object, Object> params = ESMap.get();
		params.put("_source",
				ESMap.get()//
						.set("includes", Arrays.asList())//
						.set("excludes", Arrays.asList("context*"))//
		);
		List<ESMap> shoulds = new ArrayList<>();
		List<ESMap> mustes = new ArrayList<>();
		List<ESMap> mustNots = new ArrayList<>();
		if (null != query && !query.isEmpty()) {
			listAdv(query, shoulds, mustes, mustNots);
		} else if (search == null || search.trim().isEmpty()) {
			listAll(shoulds, mustes, mustNots);
			order = "id:desc";
		} else {
			try {
				listDate(search, shoulds, mustes, mustNots);
				order = "id:desc";
			} catch (Exception e) {
				listDef(search, shoulds, mustes, mustNots);
			}
		}
		params.put("query"//
				,
				ESMap.get().set("bool",
						ESMap.get()//
								.set("must", mustes)//
								.set("should", shoulds)//
								.set("must_not", mustNots)//
				)//
		);
		params.put("size", size);
		params.put("from", (page - 1) * size);
		if (null != (temp = request.getParameter("order"))) {
			order = temp;
		}
		List<ESMap> orders = new ArrayList<>();
		for (String o : order.split(" ")) {
			if (o.isEmpty())
				continue;
			String[] ss = o.split(":");
			orders.add(//
					ESMap.get().set(ss[0], Collections.singletonMap("order", ss.length > 1 ? ss[1] : "desc"))//
			);
		}
		if (!orders.isEmpty())
			params.put("sort", orders);
		StringBuffer rst = new StringBuffer();
		String jsonParams = JsonUtil.toJson(params);
		try {
			long l = System.currentTimeMillis();
			String js = getConnection().doPost(getPath_es_start() + "html/_doc/_search", jsonParams, new HashMap<>());
			l = System.currentTimeMillis() - l;
			// logger.log(Level.INFO, l + " " + jsonParams);
			ESMap r = JsonUtil.toObject(js, ESMap.class);
			rst.append("total:");
			rst.append(r.get("hits", ESMap.class).get("total"));
			rst.append("&nbsp;Take:");
			rst.append(l);
			if (debug) {
				rst.append("&nbsp;");
				rst.append(jsonParams);
			}
			rst.append("</br><table border='0'>");
			List<ESMap> hits = (List<ESMap>) r.get("hits", ESMap.class).get("hits");
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			SimpleDateFormat dformat = new SimpleDateFormat("yyyy-MM-dd");
			SimpleDateFormat tformat = new SimpleDateFormat("HH:mm");
			for (ESMap hit : hits) {
				ESMap _source = hit.get("_source", ESMap.class);
				rst.append("<tbody><tr>");
				try {
					Date date = format.parse((String) _source.get("datetime"));
					rst.append(String.format("<td>%s</td>", dformat.format(date)));
					rst.append(String.format("<td>%s</td>", tformat.format(date)));
				} catch (Exception e) {
					rst.append(String.format("<td>%s</td>", _source.get("date_str")));
					rst.append(String.format("<td>%s</td>", "    "));
				}
				rst.append(String.format("<td align='center'>%s</td>", _source.get("type")));
				rst.append(String.format("<td><a href='/siscache/detail/%s/%s' title='新窗口打开' target='_blank'>%s</a></td>"//
						, _source.get("id"), _source.get("page"), _source.get("title")));
				rst.append("</tr></tbody>");
				// rst.append("</br>");
			}
			rst.append("</table>");
		} catch (IOException e) {
			rst.append(e.getMessage());
			for (java.lang.StackTraceElement element : e.getStackTrace()) {
				rst.append(jsonParams);
				rst.append("</br>");
				rst.append(element.toString());
			}
		}
		return rst.toString();
	}

	private void listAll(List<ESMap> shoulds, List<ESMap> mustes, List<ESMap> mustNots) {
		mustes.add(ESMap.get().set("term", Collections.singletonMap("page", 1)));
	}

	private void listDate(String search, List<ESMap> shoulds, List<ESMap> mustes, List<ESMap> mustNots) throws ParseException {
		try {
			new SimpleDateFormat("yyyy-MM-dd").parse(search);
		} catch (Exception e) {
			new SimpleDateFormat("yyyy-MM").parse(search);
		}
		mustes.add(ESMap.get().set("match_phrase", Collections.singletonMap("datetime", search)));
	}

	private void listAdv(String query, List<ESMap> shoulds, List<ESMap> mustes, List<ESMap> mustNots) {
		// System.out.println("query:" + query);
		for (String q : query.split(";")) {
			if (q.isEmpty())
				continue;
			String[] ss = q.split(":");
			String field = ss[0].replace("'", "^");
			String vs = ss.length > 1 ? ss[1] : "";
			for (String v : vs.split(" ")) {
				if (v.isEmpty())
					continue;
				// String s = "and";
				List<ESMap> list = mustes;
				if (v.startsWith("~")) {
					v = v.substring(1);
					// s = "or";
					list = shoulds;
				} else if (v.startsWith("-")) {
					v = v.substring(1);
					// s = "not";
					list = mustNots;
				}
				// System.out.println(s + " " + field + " " + v);
				ESMap item = ESMap.get()//
						.set("fields", Arrays.asList(field.split(",")));
				item.set("query", v);
				item.set("type", "phrase");
				list.add(ESMap.get().set("multi_match", item));
			}
		}
		// mustes.add(ESMap.get().set("term", Collections.singletonMap("page",
		// "1")));
	}

	private void listDef(String search, List<ESMap> shoulds, List<ESMap> mustes, List<ESMap> mustNots) {
		mustes.add(ESMap.get().set("term", ESMap.get().set("page", 1)));
		for (String type : new String[] { "best_fields", "most_fields", "cross_fields" }) {
			ESMap item = ESMap.get()//
					.set("fields", Arrays.asList("title^3", "context"));
			item.set("query", search);
			item.set("boost", 1);
			item.set("type", type);
			shoulds.add(ESMap.get().set("multi_match", item));
		}
		if (false) {
			shoulds.add(ESMap.get().set("match"//
					, ESMap.get().set("title", ESMap.get().set("query", search).set("boost", 2))//
			)//
			);
			shoulds.add(ESMap.get().set("match"//
					, ESMap.get().set("context", ESMap.get().set("query", search).set("boost", 1))//
			));
		}
		if (false) {
			String fieldstr = "", opt = "";
			// if (null != (temp = request.getParameter("fields"))) {
			// adv = true;
			// fieldstr = temp;
			// fieldstr = fieldstr.replace('$', '^');
			// }
			Set<String> siFields = new HashSet<>();
			Set<String> seFields = new HashSet<>();
			for (String f : fieldstr.split(",")) {
				if (f.startsWith("-")) {
					seFields.add(f.substring(1));
				} else {
					siFields.add(f);
				}
			}
			for (Collection[] cs : new Collection[][] { { "and".equalsIgnoreCase(opt) ? mustes : shoulds, siFields }, { mustNots, seFields } }) {
				List<ESMap> list = (List<ESMap>) cs[0];
				Set<String> set = (Set<String>) cs[1];
				if (set.isEmpty())
					continue;
				for (String s : search.split(" ")) {
					int boost = 1;
					ESMap item = ESMap.get()//
							.set("fields", set);
					if (s.contains("^")) {
						String[] ss = s.split("\\^");
						try {
							boost += Integer.valueOf(ss[1]);
							s = ss[0];
						} catch (NumberFormatException e1) {
						}
					}
					item.set("query", s);
					item.set("boost", boost);
					item.set("type", "phrase");
					list.add(ESMap.get().set("multi_match", item));
				}
			}
			String order = "id:desc";
		}
	}

	@RequestMapping("/s/{search}")
	@ResponseBody
	String search(@PathVariable("search") String search) {
		Map<String, Object> params = new HashMap<>();
		Map<String, Object> _source = new HashMap<>();
		_source.put("includes", Arrays.asList());
		_source.put("excludes", Arrays.asList("context"));
		params.put("_source", _source);
		params.put("query", Collections.singletonMap("match", Collections.singletonMap("title", search)));
		params.put("sort", Arrays.asList(//
				Collections.singletonMap("id.keyword", Collections.singletonMap("order", "desc"))//
		)//
		);
		params.put("size", 1000);
		params.put("from", 1);
		StringBuffer rst = new StringBuffer();
		try {
			String jsonParams = JsonUtil.toJson(params);
			logger.log(Level.FINE, jsonParams);
			String js = getConnection().doPost(getPath_es_start() + "html/_doc/_search?pretty", jsonParams, new HashMap<>());
			logger.log(Level.FINE, js);
			Map<String, Object> r = JsonUtil.toObject(js, Map.class);

			List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) r.get("hits")).get("hits");
			for (Map<String, Object> hit : hits) {
				_source = (Map<String, Object>) hit.get("_source");

				rst.append("<tbody><tr>");
				rst.append(String.format("<td>%s</td>", _source.get("dat")));
				rst.append(String.format("<td>%s</td>", "    "));
				rst.append(String.format("<td><a href='../siscache/detail/%s' title='新窗口打开' target='_blank'>%s</a></td>", _source.get("id"),
						_source.get("title")));

				rst.append("</tr></tbody>");
				rst.append("</br>");
			}
		} catch (IOException e) {
			rst.append(e.getMessage());
			for (java.lang.StackTraceElement element : e.getStackTrace()) {
				rst.append(element.toString());
			}
		}
		return rst.toString();
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

	@RequestMapping("/detail/{id}/{page}")
	@ResponseBody
	String detail(@PathVariable("id") String id, @PathVariable("page") String page) {
		if (page.startsWith("thread")) {
			String[] ss = page.split("-");
			if (ss.length > 2)
				page = ss[2];
		}
		Map<String, Object> params = new HashMap<>();
		Map<String, Object> _source = new HashMap<>();
		_source.put("includes", Arrays.asList("context*"));
		_source.put("excludes", Arrays.asList());
		params.put("_source", _source);
		params.put("query",
				Collections.singletonMap("bool"//
						, Collections.singletonMap("filter",
								Arrays.asList(//
										Collections.singletonMap("term", Collections.singletonMap("id", id))//
										, Collections.singletonMap("term", Collections.singletonMap("page", page))//
								))//
				)//
		);
		StringBuffer rst = new StringBuffer();
		try {
			String jsonParams = JsonUtil.toJson(params);
			logger.log(Level.FINE, jsonParams);
			String js = getConnection().doPost(getPath_es_start() + "html/_doc/_search", jsonParams, new HashMap<>());
			logger.log(Level.FINE, js);
			Map<String, Object> r = JsonUtil.toObject(js, Map.class);

			List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) r.get("hits")).get("hits");
			for (Map<String, Object> hit : hits) {
				_source = (Map<String, Object>) hit.get("_source");
				String text = (String) _source.get("context_zip");
				if (null != text) {
					try {
						text = ZipUtil.uncompress(text);
					} catch (DataFormatException e1) {
						e1.printStackTrace();
						text = null;
					}
				}
				if (null == text) {
					text = (String) _source.get("context");
				}
				long length_org = text.getBytes().length;
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
			}
		} catch (IOException e) {
			rst.append(e.getMessage());
			for (java.lang.StackTraceElement element : e.getStackTrace()) {
				rst.append(element.toString());
			}
		}
		return rst.toString();
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleController.class, args);
	}
}
