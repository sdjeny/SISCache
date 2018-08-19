package org.sdjen.download.cache_sis;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.conf.MapDBUtil;
import org.sdjen.download.cache_sis.http.HttpFactory;
import org.sdjen.download.cache_sis.log.LogUtil;

public class DownloadList {
	private DownloadSingle downloadSingle;

	public static void main(String[] args) throws Throwable {
		new DownloadList();
	}

	public DownloadList() throws Throwable {
		ConfUtil conf = ConfUtil.getDefaultConf();
		LogUtil.init();
		MapDBUtil mapDBUtil = new MapDBUtil();
		downloadSingle = new DownloadSingle().setMapDBUtil(mapDBUtil);
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
			// httpUtil.finish();
			mapDBUtil.finish();
			LogUtil.finishAll();
			// downloadSingle.startDownload("http://www.sexinsex.net/bbs/thread-7701385-1-9.html",
			// "WW.html");
			// new
			// DownloadSingle_HttpClient_T().setConfUtil(confUtil).startDownload("http://tieba.baidu.com/p/3986480945",
			// "WW.html");
			// new
			// DownloadSingle_HttpClient().setConfUtil(confUtil).startDownload("http://tieba.baidu.com/p/3986480945",
			// "EEE.html");
			System.out.println("Íê³É£¡");
		}
	}

	private void list(int from, int to) throws Throwable {
		final HttpFactory httpUtil = new HttpFactory();
		downloadSingle.setHttpUtil(httpUtil);
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			for (int i = from; i <= to; i++) {
				final String uri = MessageFormat
						.format(ConfUtil.getDefaultConf().getProperties().getProperty("list_url"), String.valueOf(i));
				LogUtil.lstLog.showMsg(uri);
				String html = httpUtil.getHTML(uri);
				org.jsoup.nodes.Document doument = Jsoup.parse(html);
				long count = 0, length_download = 0;
				for (final org.jsoup.nodes.Element e : doument.select("tbody").select("tr")) {
					String date = "";
					for (org.jsoup.nodes.Element s : e.select("td.author").select("em")) {
						date = dateFormat.format(dateFormat.parse(s.text()));
					}
					for (org.jsoup.nodes.Element s : e.select("th").select("span").select("a[href]")) {
						try {
							if (downloadSingle//
									// .setHttpUtil(new
									// HttpUtil().setConfUtil(conf))
									.startDownload(httpUtil.joinUrlPath(uri, s.attr("href")), s.text() + ".html",
											date)) {
								length_download += downloadSingle.getLength_download();
								count++;
							}
						} catch (Throwable e1) {
							e1.printStackTrace();
						}

					}
				}
				LogUtil.lstLog.showMsg("	Total:	{0}	{1}(byte)", count, length_download);
//				httpUtil.getPoolConnManager().closeExpiredConnections();
			}
		} finally {
			httpUtil.finish();
		}
	}
}
