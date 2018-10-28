package test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.json.JsonUtil;
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

	public GetConnection getConnection() throws IOException {
		if (null == connection) {
			connection = new GetConnection();
		}
		return connection;
	}

	@RequestMapping("/")
	@ResponseBody
	String home() {
		return "Hello World!";
	}

	@RequestMapping("/list")
	@ResponseBody
	String list() {
		return list(1);
	}

	@RequestMapping("/list/{page}")
	@ResponseBody
	String list(@PathVariable("page") int page) {
		return list("", page, 40);
	}

	// @RequestMapping("/list/{search}")
	// @ResponseBody
	// String list(@PathVariable("search") String search) {
	// return list(search, 1, 40);
	// }

	@RequestMapping("/list/{search}/{page}")
	@ResponseBody
	String list(@PathVariable("search") String search, @PathVariable("page") int page) {
		return list(search, page, 40);
	}

	// @RequestMapping("/list/{page}/{size}")
	// @ResponseBody
	// String list(@PathVariable("page") int page, @PathVariable("size") int
	// size) {
	// return list("", page, 40);
	// }

	@RequestMapping("/list/{search}/{page}/{size}")
	@ResponseBody
	String list(@PathVariable("search") String search, @PathVariable("page") int page, @PathVariable("size") int size) {
		Map<Object, Object> params = ESMap.get();
		params.put("_source",
				ESMap.get()//
						.set("includes", Arrays.asList())//
						.set("excludes", Arrays.asList("context"))//
		);
		if (search == null || search.trim().isEmpty()) {
			params.put("query", ESMap.get().set("term", Collections.singletonMap("page", "1")));
			params.put("sort", Arrays.asList(//
					ESMap.get().set("id.keyword", Collections.singletonMap("order", "desc"))//
			)//
			);
		} else {
			try {
				Date date = new SimpleDateFormat("yyyy-MM-dd").parse(search);
				params.put("query", ESMap.get().set("term", Collections.singletonMap("date", date)));
				params.put("sort", Arrays.asList(//
						ESMap.get().set("datetime", Collections.singletonMap("order", "desc"))//
				)//
				);
			} catch (Exception e) {
				// Collections.singletonMap("match",
				// Collections.singletonMap("title", search))
				List<ESMap> shoulds = new ArrayList<>();
				for (java.util.Map.Entry<Object, Object> entry : ESMap.get().set(10, "phrase").set(5, "most_fields").entrySet()) {
					for (String s : search.split(" ")) {
						int boost = (Integer) entry.getKey();
						ESMap item = ESMap.get()//
								.set("fields", Arrays.asList("title^5", "context"));
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
						item.set("type", entry.getValue());
						shoulds.add(ESMap.get().set("multi_match", item));
					}
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
				params.put("query"//
						, ESMap.get().set("bool", ESMap.get().set("should", shoulds)//
						)//
				);
			}
		}
		params.put("size", size);
		params.put("from", (page - 1) * size + 1);
		StringBuffer rst = new StringBuffer();
		String jsonParams = JsonUtil.toJson(params);
		try {
			long l = System.currentTimeMillis();
			String js = getConnection().doPost("http://192.168.0.237:9200/test_html/_doc/_search", jsonParams, new HashMap<>());
			// logger.log(Level.INFO, (l = System.currentTimeMillis() - l) + " "
			// + jsonParams);
			ESMap r = JsonUtil.toObject(js, ESMap.class);
			rst.append("total:");
			rst.append(r.get("hits", ESMap.class).get("total"));
			rst.append("&nbsp;Take:");
			rst.append(l);
			try {
				ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
				HttpServletRequest request = requestAttributes.getRequest();
				if (Boolean.valueOf(request.getParameter("debug"))) {
					rst.append("&nbsp;");
					rst.append(jsonParams);
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}
			rst.append("</br><table border='0'>");
			List<ESMap> hits = (List<ESMap>) r.get("hits", ESMap.class).get("hits");
			for (ESMap hit : hits) {
				ESMap _source = hit.get("_source", ESMap.class);
				rst.append("<tbody><tr>");
				rst.append(String.format("<td>%s</td>", _source.get("date_str")));
				rst.append(String.format("<td>%s</td>", "    "));
				rst.append(String.format("<td><a href='/siscache/detail/%s' title='新窗口打开' target='_blank'>%s</a></td>", _source.get("id"),
						_source.get("title")));
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
			String js = getConnection().doPost("http://192.168.0.237:9200/test_html/_doc/_search?pretty", jsonParams, new HashMap<>());
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
		return detail(id, "1");
	}

	@RequestMapping("/detail/{id}/{page}")
	@ResponseBody
	String detail(@PathVariable("id") String id, @PathVariable("page") String page) {
		Map<String, Object> params = new HashMap<>();
		Map<String, Object> _source = new HashMap<>();
		_source.put("includes", Arrays.asList("context"));
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
			String js = getConnection().doPost("http://192.168.0.237:9200/test_html/_doc/_search", jsonParams, new HashMap<>());
			logger.log(Level.FINE, js);
			Map<String, Object> r = JsonUtil.toObject(js, Map.class);

			List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) r.get("hits")).get("hits");
			for (Map<String, Object> hit : hits) {
				_source = (Map<String, Object>) hit.get("_source");
				rst.append(((String) _source.get("context"))
				// .replace("<img src=\"../../images/", "<img
				// src=\"../../../images/")
				);
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
