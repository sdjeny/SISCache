package org.sdjen.download.cache_sis.store;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import org.bson.types.Binary;
import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.tool.ZipUtil;
import org.sdjen.download.cache_sis.util.EntryData;
import org.sdjen.download.cache_sis.util.JsoupAnalysisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.mongodb.client.result.UpdateResult;

@Service("Store_Mongodb")
public class Store_Mongodb implements IStore {
	private static Set<String> proxy_urls;
	private MessageDigest md5Digest;
	private ConfUtil conf;
	@Autowired
	private MongoTemplate mongoTemplate;
	private String[] fbsArr = { "\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|" };
	@Value("${siscache.conf.url_fail_stop}")
	private int url_fail_stop = 10;
	@Value("${siscache.conf.url_fail_retry_begin}")
	private int url_fail_retry_begin = 5;
	@Value("${siscache.conf.url_fail_retry_in_hours}")
	private int url_fail_retry_in_hours = 3;

	private Store_Mongodb() throws Exception {
		System.out.println(">>>>>>>>>>>>>>>>>>Store_Mongodb");
		md5Digest = MessageDigest.getInstance("MD5");
	}

	@Override
	public String getLocalHtml(final String id, final String page) throws Throwable {
		Query query = new Query();
		query.addCriteria(Criteria.where("id").is(Long.valueOf(id)));
		query.addCriteria(Criteria.where("page").is(Long.valueOf(page)));
		query.fields().include("context_zip").include("context_comments").include("context").include("page")
				.include("fid").include("type");
		Map<?, ?> _source = mongoTemplate.findOne(query, Map.class, "htmldoc");
		String result = null;
		if (null != _source) {
			if ("Lost title".equals(_source.get("context")))
				return "Lost title";
			Binary zip = (Binary) _source.get("context_zip");
			if (null != zip) {
				try {
					String context = ZipUtil.uncompress(zip.getData());
					try {
						Map<String, Object> details = JsonUtil.toObject(context, Map.class);
						details.put("fid", (String) _source.get("fid"));
						details.put("type", (String) _source.get("type"));
						details.put("tid", id);
						context = JsoupAnalysisor.restoreToHtml(details);
					} catch (Exception e) {
					}
					return context;
				} catch (DataFormatException e1) {
					e1.printStackTrace();
				}
			}
			result = (String) _source.get("context");
		
		}
		if (null != result) {
			result = replaceLocalHtmlUrl(result).html();
		}
		return result;
	}

	@Override
	public void saveHtml(final String id, final String page, final String url, String title, String dateStr,
			String text) throws Throwable {
		org.jsoup.nodes.Document doument = Jsoup.parse(text);
//		doument.outputSettings(new Document.OutputSettings().prettyPrint(false));
		org.jsoup.nodes.Element h1 = doument.select("div.mainbox").select("h1").first();
		if (null == h1) {
			save("htmldoc", new EntryData()//
					.put("fid", "")//
					.put("id", Long.valueOf(id))//
					.put("page", Long.valueOf(page))//
					.put("context", "Lost title")//
					.getData(), "fid", "id", "page");
			throw new Exception("Lost title");
		}
		String type = h1.select("a").text();
		String dat = null;
		String context = null;
		String author = null;
		String fid = null;
		for (org.jsoup.nodes.Element element : doument.select("#foruminfo").select("a")) {
			String href = element.attr("href");
			if (null != href && href.startsWith("forum-")) {
				href = href.substring("forum-".length());
				fid = href.split("-")[0];
			}
		}
		for (org.jsoup.nodes.Element tbody : doument.select("div.mainbox.viewthread")//// class=mainbox的div
				.select("table")//
				.select("tbody")//
				.select("tr")//
		) {
			String floor = "";
			for (org.jsoup.nodes.Element postinfo : tbody.select("td.postcontent").select("div.postinfo")) {
				org.jsoup.nodes.Element temp = postinfo.select("strong").first();
				if (null != temp) {
					floor = temp.ownText();
					if ("1楼".equals(floor)) {
						dat = postinfo.ownText().replace("发表于 ", "");
						for (org.jsoup.nodes.Element postauthor : tbody.select("td.postauthor").select("cite")
								.select("a[href]")) {
							author = postauthor.text();
						}
					}
				}
			}
			if (null != author)
				break;
		}
		List<ESMap> comments = new ArrayList<>();
		{
			for (org.jsoup.nodes.Element postcontent : doument.select("div.mainbox.viewthread")//// class=mainbox的div
					.select("table")//
					.select("tbody")//
					.select("tr")//
					.select("td.postcontent")//
			) {
				String floor = "";
				for (org.jsoup.nodes.Element postinfo : postcontent.select("div.postinfo")) {
					org.jsoup.nodes.Element temp = postinfo.select("strong").first();
					if (null != temp) {
						floor = temp.ownText();
						if ("1楼".equals(floor)) {
							dat = postinfo.ownText().replace("发表于 ", "");
						}
					}
				}
				if (floor.isEmpty())
					continue;
				context = JsoupAnalysisor.toTextOnly(postcontent.select("div.postmessage.defaultpost"));
			}
		}
		boolean update = false;
		for (org.jsoup.nodes.Element e : doument.select("head").select("style")) {
			update = true;
			e.text("");
		}
		for (org.jsoup.nodes.Element e : doument.select("head").select("script")) {
			update = true;
			e.remove();
		}
		if (update)
			text = doument.html();
		Map<String, Object> json = new HashMap<>();
		json.put("id", Long.valueOf(id));
		json.put("page", Long.valueOf(page));
		json.put("context_comments", comments);
		json.put("title", title);
		json.put("datetime", dat);
		try {
			if (null != dat) {
				SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				Date dt = dtf.parse(dat);
				json.put("datetime", dtf.format(dt));
			}
		} catch (Exception e1) {
		}
		json.put("fid", fid);
		json.put("type", type);
		json.put("context", context);
		json.put("author", author);
		json.put("context_zip", ZipUtil.compress(JsonUtil.toJson(JsoupAnalysisor.split(doument))));
	
		save("htmldoc", json, "fid", "id", "page");
	}

	private void save(String collectionName, Map<String, Object> objectToSave, String... keys) {
		Query query = new Query();
		Update update = new Update();
		java.util.List<String> ks = Arrays.asList(keys);
		objectToSave.forEach((k, v) -> {
			if (ks.contains(k)) {
				query.addCriteria(Criteria.where(k).is(v));
			} else {
				update.set(k, v);
			}
		});
		UpdateResult updateResult = mongoTemplate.upsert(query, update, collectionName);
		logger.debug("Update	>>>>>" + collectionName + ":	" + updateResult);
	}

	@Override
	public void saveURL(String url, String path) throws IOException {
		Map<String, Object> objectToSave = new HashMap<>();
		String key = getMD5(url.getBytes("utf8"));
		objectToSave.put("key", key);
		objectToSave.put("url", url);
		objectToSave.put("path", path);
		objectToSave.put("type", "url");
		save("md", objectToSave, "key", "type");
	}

	@Override
	public void saveMD5(String md5, String path) throws Exception {
		Map<String, Object> objectToSave = new HashMap<>();
		objectToSave.put("key", md5);
		objectToSave.put("path", path);
		objectToSave.put("type", "path");
		save("md", objectToSave, "key", "type");
	}

	@Override
	public String getMD5_Path(String key) throws Exception {
		Query query = new Query();
		query.addCriteria(Criteria.where("key").is(key));
		query.addCriteria(Criteria.where("type").is("path"));
		query.fields().include("path");
		Map<?, ?> rst = mongoTemplate.findOne(query, Map.class, "md");
		return null == rst ? null : (String) rst.get("path");
	}

	private synchronized String getMD5(byte[] bytes) {
		return new BigInteger(1, md5Digest.digest(bytes)).toString(Character.MAX_RADIX);
	}

	@Override
	public String getURL_Path(String key) throws IOException {
		Query query = new Query();
		query.addCriteria(Criteria.where("key").is(getMD5(key.getBytes("utf8"))));
		query.addCriteria(Criteria.where("type").is("url"));
//		query.skip(skipNumber);
//		query.limit(pageSize);
//		Sort sort = new Sort(Sort.Direction.ASC, "DEVID").and(new Sort(Sort.Direction.ASC, "TIME"));// 多条件DEVID、time
//		query.with(Sort.by(Order.asc("DEVID"), Order.desc("TIME")));
//		query.with(PageRequest.of(page, size, Sort.by(Order.asc("DEVID"), Order.desc("TIME"))));
		query.fields().include("path");
		Map<?, ?> rst = mongoTemplate.findOne(query, Map.class, "md");
		return null == rst ? null : (String) rst.get("path");
	}

	@Override
	public void refreshMsgLog() {
	}

	@Override
	public Map<String, Object> getTitleList(String fid, int page, int size, String query_str, String order)
			throws Throwable {
		Query query = new Query();
		Set<Order> orders = new LinkedHashSet<>();
		if (null == query_str)
			query_str = "";
		if (null == order)
			order = "";
		Function<String, Pattern> escapeExprSpecialWord = keyword -> {
			// 完全匹配 ^name$
			// 右匹配 ^.*name$
			// 左匹配 ^name.*$
			// 模糊匹配 ^.*name8.*$"
			if (!StringUtils.isEmpty(keyword)) {
				for (String key : fbsArr) {
					if (keyword.contains(key)) {
						keyword = keyword.replace(key, "\\" + key);
					}
				}
			}
//			return Pattern.compile("^.*" + keyword + ".*$", Pattern.CASE_INSENSITIVE);
			return Pattern.compile(".*" + keyword + ".*", Pattern.CASE_INSENSITIVE);
		};
		List<Criteria> and = new ArrayList<>();
		if (!"ALL".equalsIgnoreCase(fid)) {
			and.add(Criteria.where("fid").is(fid));
		}
		if (query_str.isEmpty()) {
			and.add(Criteria.where("page").is(1l));
		} else if (query_str.toUpperCase().startsWith("D:")) {
			and.add(Criteria.where("datetime").is(query_str.substring(2)));
		} else if (query_str.toUpperCase().startsWith("ALL:")) {
			query_str = query_str.substring(4);
			and.add(Criteria.where("page").is(1l));
			for (String qs : query_str.split(" ")) {
				Pattern pattern = escapeExprSpecialWord.apply(qs.trim());
				and.add(new Criteria().orOperator(//
						Criteria.where("title").regex(pattern)//
						, Criteria.where("context").regex(pattern)//
						, Criteria.where("context_comments.context").regex(pattern)//
				));
			}
		} else {
			for (String q : query_str.split(";")) {
				if (q.isEmpty())
					continue;
				List<Criteria> or = new ArrayList<>();
				List<Criteria> nor = new ArrayList<>();
				String[] ss = q.split(":");
				String field = ss[0].replace("'", "^");
				String vs = ss.length > 1 ? ss[1] : "";
				for (String v : vs.split(" ")) {
					if (v.isEmpty())
						continue;
					List<Criteria> list = and;// String s = "and";
					if (v.startsWith("~")) {
						v = v.substring(1);
						list = or;// s = "or";
					} else if (v.startsWith("-")) {
						v = v.substring(1);
						list = nor;// s = "not";
					}
					Pattern pattern = escapeExprSpecialWord.apply(v.trim());
					list.add(Criteria.where(field).regex(pattern));
				}
				if (!or.isEmpty())
					and.add(new Criteria().orOperator(or.toArray(new Criteria[] {})));
				if (!nor.isEmpty())
					and.add(new Criteria().norOperator(nor.toArray(new Criteria[] {})));
			}
		}
		if (!and.isEmpty())
			query.addCriteria(new Criteria().andOperator(and.toArray(new Criteria[] {})));
		for (String o : order.split(" ")) {
			if (o.isEmpty())
				continue;
			String[] ss = o.split(":");
			orders.add((ss.length > 1 && "asc".equalsIgnoreCase(ss[1])) ? Order.asc(ss[0]) : Order.desc(ss[0]));
		}
		if (orders.isEmpty())
			orders.add(Order.desc("id"));
//		query.addCriteria(Criteria.where("key").is(getMD5(key.getBytes("utf8"))));
//		query.addCriteria(Criteria.where("type").is("url"));
//		query.skip(skipNumber);
//		query.limit(pageSize);
//		Sort sort = new Sort(Sort.Direction.ASC, "DEVID").and(new Sort(Sort.Direction.ASC, "TIME"));// 多条件DEVID、time
//		query.with(Sort.by(Order.asc("DEVID"), Order.desc("TIME")));
		query.fields().include("datetime").include("date_str").include("fid").include("id").include("page")
				.include("type").include("title");
		List<Map<String, Object>> ls = new ArrayList<>();
		Map<String, Object> result = new HashMap<>();
		result.put("list", ls);
		result.put("total", mongoTemplate.count(query, "htmldoc"));
		query.skip((page - 1) * size);
		query.limit(size);
		query.with(Sort.by(orders.toArray(new Order[] {})));
//		query.with(PageRequest.of(page, size, Sort.by(orders.toArray(new Order[] {}))));
		result.put("json_params", query.toString());
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		SimpleDateFormat dformat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat tformat = new SimpleDateFormat("HH:mm");
		for (Map<?, ?> _source : mongoTemplate.find(query, Map.class, "htmldoc")) {
			String datestr, timestr;
			try {
				Date date = format.parse((String) _source.get("datetime"));
				datestr = dformat.format(date);
				timestr = tformat.format(date);
			} catch (Exception e) {
				datestr = (String) _source.get("date_str");
				timestr = "    ";
			}
			ls.add(new EntryData<String, Object>()//
					.put("date", datestr)//
					.put("time", timestr)//
					.put("fid", _source.get("fid"))//
					.put("id", _source.get("id"))//
					.put("page", _source.get("page"))//
					.put("type", _source.get("type"))//
					.put("title", _source.get("title"))//
					.getData());
		}
		return result;
	}

	@Override
	public synchronized Set<String> getProxyUrls() {
		if (null == proxy_urls) {
			proxy_urls = new HashSet<>();
			mongoTemplate.findAll(Map.class, "urls_proxy").forEach(m -> proxy_urls.add((String) m.get("url")));
			proxy_urls.remove("");
		}
		return proxy_urls;
	}

	@Override
	public synchronized void addProxyUrl(String url) {
		url = cutForProxy(url);
		if (!url.isEmpty() && !proxy_urls.contains(url)) {
			proxy_urls.add(url);
			Map<String, Object> result = mongoTemplate.insert(new EntryData<String, Object>().put("url", url).getData(),
					"urls_proxy");
			msg(">>>>>>>>>ADD:	{0}", result);
		}
	}

	@Override
	public synchronized void removeProxyUrl(String url) {
		url = cutForProxy(url);
		if (!url.isEmpty() && proxy_urls.contains(url)) {
			proxy_urls.remove(url);
			List<Object> result = mongoTemplate.findAllAndRemove(Query.query(Criteria.where("url").is(url)),
					"urls_proxy");
			msg(">>>>>>>>>REMOVE:	{0}", result);
		}
	}

	@Override
	public Map<String, Object> getLast(String type) {
		Query query = new Query().addCriteria(Criteria.where("type").is(type));
		return mongoTemplate.findOne(query, Map.class, "last");
	}

	@Override
	public Object running(String type, String keyword, String msg) {
		return mongoTemplate.upsert(new Query().addCriteria(Criteria.where("type").is(type)), new Update()//
				.set("keyword", keyword)//
				.set("running", true)//
				.set("msg", msg)//
				.set("time", new Date())//
				, "last");
	}

	@Override
	public Set<String> getRunnings() {
		Set<String> running = new HashSet<>();
		mongoTemplate.find(new Query().addCriteria(Criteria.where("running").is(true)), Map.class, "last")
				.forEach(m -> running.add((String) m.get("type")));
		return running;
	}

	@Override
	public Object finish(String type, String msg) {
		return mongoTemplate.updateMulti(new Query().addCriteria(Criteria.where("type").is(type)), new Update()//
				.set("running", false)//
				.set("time", new Date())//
				.set("msg", msg)//
				, "last");
	}

	@Override
	public void logFailedUrl(String url, Throwable e) {
		UpdateResult updateResult = mongoTemplate.upsert(
				new Query().addCriteria(Criteria.where("url").is(cutForProxy(url))),
				new Update().inc("count", 1).set("msg", e.getMessage()).set("time", new Date()), "urls_failed");
		logger.debug("logFailedUrl	>>>>>failurl:{}", updateResult);
	}

	@Override
	public Map<String, Object> connectCheck(String url) {
		Map<String, Object> result = new HashMap<>();
		result.put("found", false);
		result.put("continue", true);
		result.put("msg", "");
		url = cutForProxy(url);
		Map<String, Object> findOne = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("url").is(url)),
				Map.class, "urls_failed");
		if (null != findOne) {
			result.put("found", true);
			if ((int) findOne.get("count") > url_fail_stop) {
				if (System.currentTimeMillis() - ((Date) findOne.get("time")).getTime() < 3600000
						* url_fail_retry_in_hours) {
					result.put("continue", false);
					result.put("msg", url_fail_retry_in_hours + "小时内禁止连接：" + findOne.get("msg"));
				} else {
					mongoTemplate.updateMulti(new Query().addCriteria(Criteria.where("url").is(url)), new Update()//
							.set("count", url_fail_retry_begin)//
							, "urls_failed");
				}
			}
		}
		return result;
	}

	@Override
	public void logSucceedUrl(String url) {
		mongoTemplate.findAllAndRemove(Query.query(Criteria.where("url").is(cutForProxy(url))), "urls_failed");
	}
}
