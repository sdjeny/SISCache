package org.sdjen.download.cache_sis;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.log.LogUtil;

public class DownloadListSubject {

	public static void main(String[] args) throws Throwable {
		new DownloadListSubject();
	}

	public DownloadListSubject() throws Throwable {
		ConfUtil conf = new ConfUtil("conf.ini");
		if (conf.getProperties().isEmpty()) {
			conf.getProperties().setProperty("chatset", "gbk");
			conf.getProperties().setProperty("save_path", "D:/SISCACHE");
			conf.getProperties().setProperty("proxy", "192.168.0.231:9666");
			conf.getProperties().setProperty("list_url", "http://www.sexinsex.net/bbs/forum-143-{0}.html");
			conf.getProperties().setProperty("list_start", "1");
			conf.getProperties().setProperty("list_end", "1");
			conf.store();
		}
		HttpUtil httpUtil = new HttpUtil().setConfUtil(conf);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		LogUtil logUtil = new LogUtil().setLogFile(System.currentTimeMillis() + ".list")
				.setChatset(conf.getProperties().getProperty("chatset"));
		for (int i = Integer.valueOf(conf.getProperties().getProperty("list_start")); //
		i <= Integer.valueOf(conf.getProperties().getProperty("list_end")); //
		i++) {
			String uri = MessageFormat.format(conf.getProperties().getProperty("list_url"), i);
			logUtil.showMsg(uri);
			String html = httpUtil.getHTML(uri);
			org.jsoup.nodes.Document doument = Jsoup.parse(html);
			for (org.jsoup.nodes.Element e : doument.select("tbody").select("tr")) {
				StringBuilder builder = new StringBuilder();
				for (org.jsoup.nodes.Element s : e.select("td.author").select("em")) {
					builder.append("	");
					builder.append(dateFormat.format(dateFormat.parse(s.text())));
				}
				for (org.jsoup.nodes.Element s : e.select("th").select("span").select("a[href]")) {
					builder.append("	");
					builder.append(s.text());
					builder.append("	");
					builder.append(httpUtil.joinUrlPath(uri, s.attr("href")));
				}
				if (builder.length() > 0)
					logUtil.showMsg(builder);
			}
		}
		logUtil.finish();
		httpUtil.finish();
		System.out.println("Íê³É£¡");
	}
}
