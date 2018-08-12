package org.sdjen.download.cache_sis;

import java.math.BigInteger;
import java.text.MessageFormat;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.log.LogUtil;

public class DownloadList {
	private ConfUtil conf;
	private DownloadSingle downloadSingle;

	public static void main(String[] args) throws Throwable {
		new DownloadList();
	}

	public DownloadList() throws Throwable {
		conf = new ConfUtil("conf.ini");
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
		LogUtil.init(conf);
		downloadSingle = new DownloadSingle().setConfUtil(conf).setHttpUtil(httpUtil);
		try {
			for (int i = Integer.valueOf(conf.getProperties().getProperty("list_start")); //
			i <= Integer.valueOf(conf.getProperties().getProperty("list_end")); //
			i++) {
				String uri = MessageFormat.format(conf.getProperties().getProperty("list_url"), i);
				LogUtil.lstLog.showMsg(uri);
				String html = httpUtil.getHTML(uri);
				org.jsoup.nodes.Document doument = Jsoup.parse(html);
				long count = 0, length_download = 0;
				for (org.jsoup.nodes.Element e : doument.select("tbody").select("th").select("span")
						.select("a[href]")) {
					try {
						if (downloadSingle//
								// .setHttpUtil(new
								// HttpUtil().setConfUtil(conf))
								.startDownload(httpUtil.joinUrlPath(uri, e.attr("href")), e.text() + ".html")) {
							count++;
							length_download += downloadSingle.getLength_download();
						}
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				LogUtil.lstLog.showMsg("	Total:	{0}	{1}(byte)", count, length_download);
			}
		} finally {
			httpUtil.finish();
			LogUtil.finishAll();
			// downloadSingle.startDownload("http://www.sexinsex.net/bbs/thread-7701385-1-9.html",
			// "WW.html");
			// new
			// DownloadSingle_HttpClient_T().setConfUtil(confUtil).startDownload("http://tieba.baidu.com/p/3986480945",
			// "WW.html");
			// new
			// DownloadSingle_HttpClient().setConfUtil(confUtil).startDownload("http://tieba.baidu.com/p/3986480945",
			// "EEE.html");
			System.out.println("��ɣ�");
		}
	}
}
