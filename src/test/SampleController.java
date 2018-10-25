package test;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
				params.put("query"//
						,
						ESMap.get().set("bool",
								ESMap.get().set("should",
										Arrays.asList(//
												ESMap.get().set("match"//
														, ESMap.get().set("title", ESMap.get().set("query", search).set("boost", 2))//
												)//
												, ESMap.get().set("match"//
														, ESMap.get().set("context", ESMap.get().set("query", search).set("boost", 1))//
												)//
										))//
						)//
				);
			}
		}
		params.put("size", size);
		params.put("from", (page - 1) * size + 1);
		StringBuffer rst = new StringBuffer();
		try {
			String jsonParams = JsonUtil.toJson(params);
			logger.log(Level.FINE, jsonParams);
			String js = getConnection().doPost("http://192.168.0.237:9200/test_html/_doc/_search", jsonParams, new HashMap<>());
			logger.log(Level.FINE, js);
			Map<String, Object> r = JsonUtil.toObject(js, Map.class);
			List<Map<String, Object>> hits = (List<Map<String, Object>>) ((Map<String, Object>) r.get("hits")).get("hits");
			for (Map<String, Object> hit : hits) {
				Map<String, Object> _source = (Map<String, Object>) hit.get("_source");
				rst.append("<tbody><tr>");
				rst.append(String.format("<td>%s</td>", _source.get("date_str")));
				rst.append(String.format("<td>%s</td>", "    "));
				rst.append(String.format("<td><a href='/siscache/detail/%s' title='新窗口打开' target='_blank'>%s</a></td>", _source.get("id"),
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
