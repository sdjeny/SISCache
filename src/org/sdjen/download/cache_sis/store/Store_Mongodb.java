package org.sdjen.download.cache_sis.store;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.tool.ZipUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.mongodb.client.result.UpdateResult;

@Service("Store_Mongodb")
public class Store_Mongodb implements IStore {
	private final static Logger logger = LoggerFactory.getLogger(Store_Mongodb.class);
	private MessageDigest md5Digest;
	private ConfUtil conf;
	@Autowired
	private MongoTemplate mongoTemplate;

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
	public String getLocalHtml(final String id, final String page, final String url, String title, String dateStr)
			throws Throwable {
		return null;//mongoTemplate.findAll(Map.class, "Tas").toString();
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
						context = comment.html();
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
			json.put("fid", 143);
			json.put("type", type);
			json.put("context", context);
			json.put("context_zip", ZipUtil.compress(text));
			json.put("author", author);
		}
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
		return (String) mongoTemplate.findOne(query, Map.class, "md").get("path");
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
		return (String) mongoTemplate.findOne(query, Map.class, "md").get("path");
	}

	private SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");

	private StringBuilder getMsgText(Object pattern, Object... args) {
		StringBuilder builder = new StringBuilder(dateFormat.format(new Date()));
		builder.append("	,");
		if (null != args && args.length > 0 && pattern instanceof String) {
			builder.append(MessageFormat.format((String) pattern, args));
		} else {
			builder.append(pattern);
		}
		return builder;
	}

	@Override
	public void msg(Object pattern, Object... args) {
		logger.info(getMsgText(pattern, args).toString());
	}

	@Override
	public void err(Object pattern, Object... args) {
		logger.error(getMsgText(pattern, args).toString());
	}

	@Override
	public void refreshMsgLog() {
	}

	@Override
	public Map<String, Object> getTitleList(int page, int size, String query, String order) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
}
