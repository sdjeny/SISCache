package org.sdjen.download.cache_sis;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.configuration.ConfUtil;
import org.sdjen.download.cache_sis.http.HttpFactory;
import org.sdjen.download.cache_sis.log.LogUtil;

public class DownloadListSubject {
	public static void main(String[] args) throws Throwable {
		new DownloadListSubject();
	}

	LogUtil logUtil;

	public DownloadListSubject() throws Throwable {
		ConfUtil conf = ConfUtil.getDefaultConf();
		logUtil = new LogUtil().setLogFile(System.currentTimeMillis() + ".list").setChatset(conf.getProperties().getProperty("chatset"));
		LogUtil.init();
		try {
			int page = Integer.valueOf(conf.getProperties().getProperty("list_start"));
			int limit = Integer.valueOf(conf.getProperties().getProperty("list_end"));
			int pageU = 50;
			try {
				pageU = Integer.valueOf(conf.getProperties().getProperty("list_page_max"));
			} catch (Exception e) {
			}
			do {
				list(page, Math.min(page + pageU - 1, limit));
				page += pageU;
			} while (page <= limit);
		} finally {
			logUtil.finish();
			LogUtil.finishAll();
			System.out.println("��ɣ�");
		}
	}

	private void list(int from, int to) throws Throwable {
		HttpFactory httpUtil = new HttpFactory();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			for (int i = from; i <= to; i++) {
				String uri = MessageFormat.format(ConfUtil.getDefaultConf().getProperties().getProperty("list_url"), String.valueOf(i));
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
		} finally {
			httpUtil.finish();
		}
	}
}
