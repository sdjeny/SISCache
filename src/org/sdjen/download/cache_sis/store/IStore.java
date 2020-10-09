package org.sdjen.download.cache_sis.store;

import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.http.DefaultCss;
import org.sdjen.download.cache_sis.util.EntryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface IStore {
	final static Map<String, String> FIDDESCES = new EntryData<String, String>()//
			.put("143", "亚洲无码原创")//
			.put("318", "第六天魔王区")//
			.put("230", "亚洲有码原创")//
			.put("229", "欧美无码原创")//
			.put("122", "标准自压原创")//
			.put("463", "自拍精品区")//
//			.put("341", "WMV验证病毒")//
			.put("25", "亚洲无码区")//
			.put("58", "亚有码薄码区")//
			.put("77", "欧美无码区")//
///////////////////////////////////////////////////////////
			.put("430", "新手原创发布")//
			.put("252", "唯美图文")//
			.put("186", "东方唯美图坊")//
			.put("253", "西方唯美图坊")//
			.put("254", "景致唯美区")//
			.put("64", "东方靓女区")//
			.put("68", "西洋靓女骚妹")//
			.put("61", "星梦奇缘")//
			.put("249", "高跟美腿丝袜")//
			.put("62", "网友自拍贴")//
			.put("411", "原创自拍区")//
			.put("459", "自拍视频区")//
			.put("441", "俱乐部")//
//			.put("297", "原创精品套图")//
//			.put("250", "原创图帖超市")//
//			.put("277", "成人图片打包")//
//			.put("184", "精品套图鉴赏")//
//			.put("242", "熟女乱伦图区")//
//			.put("63", "另类奇怪图区")//
//			.put("419", "SM贴图区")//
//			.put("183", "同性贴图区")//
			.getData();
	final static Logger logger = LoggerFactory.getLogger(IStore.class);
	SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
//	String getKey(final String id, final String page, final String url, String title, String dateStr) throws Throwable;

	String getLocalHtml(final String id, final String page) throws Throwable;

	void saveURL(String url, String path) throws Throwable;

	void saveMD5(String md5, String path) throws Throwable;

	String getMD5_Path(String key) throws Throwable;

	String getURL_Path(String key) throws Throwable;

	void saveHtml(final String id, final String page, final String url, String title, String dateStr, String html)
			throws Throwable;

	void refreshMsgLog();

	Map<String, Object> getTitleList(String fid, int page, int size, String query, String order) throws Throwable;

	default String cutForProxy(String url) {
		int index = url.indexOf('/', 9);
		String result;
		if (-1 != index)
			result = url.substring(0, index);// 排除参数部分
		else
			result = url;
		return result;
	}

	Set<String> getProxyUrls();

	void addProxyUrl(String url);

	void removeProxyUrl(String url);

	void logFailedUrl(String url, Throwable e);

	void logSucceedUrl(String url);

	default org.jsoup.nodes.Document replaceLocalHtmlUrl(String text) throws IOException {
		org.jsoup.nodes.Document doument = Jsoup.parse(text);
		boolean update = false;
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
		return doument;
//		if (update) {
//			text = doument.html();
//		}
	}

	default Map<String, Object> connectCheck(String url) {
		Map<String, Object> result = new HashMap<>();
		result.put("found", false);
		result.put("continue", true);
		result.put("msg", "");
		return result;
	}

	default Map<String, Object> getLast(String type) {
		return null;
	}

	default Object running(String type, String keyword, String msg) {
		return null;
	}

	default Object finish(String type, String msg) {
		return null;
	}

	default Set<String> getRunnings() {
		return new HashSet<String>();
	}

	default StringBuilder getMsgText(Object pattern, Object... args) {
		StringBuilder builder = new StringBuilder(dateFormat.format(new Date()));
		builder.append("	,");
		if (null != args && args.length > 0 && pattern instanceof String) {
			builder.append(MessageFormat.format((String) pattern, args));
		} else {
			builder.append(pattern);
		}
		return builder;
	}

	default void msg(Object pattern, Object... args) {
		logger.info(getMsgText(pattern, args).toString());
	}

	default void debug(Object pattern, Object... args) {
		logger.debug(getMsgText(pattern, args).toString());
	}

	default void err(Object pattern, Object... args) {
		logger.error(getMsgText(pattern, args).toString());
	}
}
