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
import org.sdjen.download.cache_sis.tool.ZipUtil;
import org.sdjen.download.cache_sis.util.EntryData;
import org.springframework.beans.factory.annotation.Autowired;
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
	private MessageDigest md5Digest;
	private ConfUtil conf;
	@Autowired
	private MongoTemplate mongoTemplate;
	private String[] fbsArr = { "\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|" };
	private static Set<String> proxy_urls;

	public ConfUtil getConf() throws IOException {
		if (null == conf) {
			conf = ConfUtil.getDefaultConf();
		}
		return conf;
	}

	private Store_Mongodb() throws Exception {
		System.out.println(">>>>>>>>>>>>>>>>>>Store_Mongodb");
		md5Digest = MessageDigest.getInstance("MD5");
	}

	@Override
	public String getLocalHtml(final String id, final String page) throws Throwable {
		Query query = new Query();
		query.addCriteria(Criteria.where("id").is(Long.valueOf(id)));
		query.addCriteria(Criteria.where("page").is(Long.valueOf(page)));
		query.fields().include("context_zip").include("context_comments").include("context").include("page");
		Map<?, ?> _source = mongoTemplate.findOne(query, Map.class, "htmldoc");
		if (null != _source) {
			if (Long.valueOf(page) > 1) {
				StringBuffer rst = new StringBuffer();
				rst.append("</br><table border='0'>");
				List<Map<?, ?>> comments = (List<Map<?, ?>>) _source.get("context_comments");
				if (null != comments)
					comments.forEach(m -> {
						rst.append("<tbody><tr>");
						rst.append(String.format("<td>%s</td>", m.get("floor")));
						rst.append(String.format("<td>%s</td>", m.get("context")));
						rst.append("</tr></tbody>");
					});
				rst.append("</table>");
				return rst.toString();
			} else {
				Binary zip = (Binary) _source.get("context_zip");
				if (null != zip) {
					try {
						return ZipUtil.uncompress(zip.getData());
					} catch (DataFormatException e1) {
						e1.printStackTrace();
					}
				}
				return (String) _source.get("context");
			}
		}
		return null;
	}

	@Override
	public void saveHtml(final String id, final String page, final String url, String title, String dateStr,
			String text) throws Throwable {
		org.jsoup.nodes.Document doument = Jsoup.parse(text);
		org.jsoup.nodes.Element h1 = doument.select("div.mainbox").select("h1").first();
		if (null == h1)
			throw new Exception("Lost title");
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
				if ("1楼".equals(floor)) {
					for (org.jsoup.nodes.Element comment : postcontent.select("div.postmessage.defaultpost")) {
//						context = comment.html();
						context = toTextOnly(comment);
					}
				} else {
					String fm = "";
					for (org.jsoup.nodes.Element comment : postcontent.select("div.postmessage.defaultpost")
							.select("div.t_msgfont")) {
						if (!fm.isEmpty())
							fm += ",";
						fm += comment.text();
					}
					comments.add(ESMap.get().set("floor", floor).set("context", fm));
				}
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
		if (page.equals("1")) {
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
			json.put("context_zip", ZipUtil.compress(text));
			json.put("author", author);
		}
		save("htmldoc", json, "fid", "id", "page");
	}

	private synchronized void save(String collectionName, Map<String, Object> objectToSave, String... keys) {
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
	public Map<String, Object> getTitleList(int page, int size, String query_str, String order) throws Throwable {
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
		if (query_str.isEmpty()) {
			query.addCriteria(Criteria.where("page").is(1l));
		} else if (query_str.toUpperCase().startsWith("D:")) {
			query.addCriteria(Criteria.where("datetime").is(query_str.substring(2)));
		} else if (query_str.toUpperCase().startsWith("ALL:")) {
			query_str = query_str.substring(4);
			List<Criteria> mustes = new ArrayList<>();
			mustes.add(Criteria.where("page").is(1l));
			for (String qs : query_str.split(" ")) {
				Pattern pattern = escapeExprSpecialWord.apply(qs.trim());
				mustes.add(new Criteria().orOperator(//
						Criteria.where("title").regex(pattern)//
						, Criteria.where("context").regex(pattern)//
						, Criteria.where("context_comments.context").regex(pattern)//
				));
			}
			if (!mustes.isEmpty())
				query.addCriteria(new Criteria().andOperator(mustes.toArray(new Criteria[] {})));
		} else {
			List<Criteria> shoulds = new ArrayList<>();
			List<Criteria> mustes = new ArrayList<>();
			List<Criteria> mustNots = new ArrayList<>();
			for (String q : query_str.split(";")) {
				if (q.isEmpty())
					continue;
				String[] ss = q.split(":");
				String field = ss[0].replace("'", "^");
				String vs = ss.length > 1 ? ss[1] : "";
				for (String v : vs.split(" ")) {
					if (v.isEmpty())
						continue;
					Pattern pattern = escapeExprSpecialWord.apply(v.trim());
					List<Criteria> list = mustes;// String s = "and";
					if (v.startsWith("~")) {
						v = v.substring(1);
						list = shoulds;// s = "or";
					} else if (v.startsWith("-")) {
						v = v.substring(1);
						list = mustNots;// s = "not";
					}
					list.add(Criteria.where(field).regex(pattern));
				}
			}
			if (!shoulds.isEmpty())
				mustes.add(new Criteria().orOperator(shoulds.toArray(new Criteria[] {})));
			if (!mustNots.isEmpty())
				mustes.add(new Criteria().norOperator(mustNots.toArray(new Criteria[] {})));
			if (!mustes.isEmpty())
				query.addCriteria(new Criteria().andOperator(mustes.toArray(new Criteria[] {})));
		}
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
		query.fields().include("datetime").include("date_str").include("id").include("page").include("type")
				.include("title");
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
	public void addProxyUrl(String url) {
		String proxy_url = cutForProxy(url);
		if (!proxy_url.isEmpty() && !proxy_urls.contains(proxy_url)) {
			proxy_urls.add(proxy_url);
			msg(">>>>>>>>>ADD:	{0}", mongoTemplate
					.insert(new EntryData<String, Object>().put("url", proxy_url).getData(), "urls_proxy"));
		}
	}

	@Override
	public void removeProxyUrl(String url) {
		String proxy_url = cutForProxy(url);
		if (!proxy_url.isEmpty() && proxy_urls.contains(proxy_url)) {
			proxy_urls.remove(proxy_url);
			msg(">>>>>>>>>REMOVE:	{0}",
					mongoTemplate.findAllAndRemove(Query.query(Criteria.where("url").is(proxy_url)), "urls_proxy"));
		}
	}
}
