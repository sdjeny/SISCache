package org.sdjen.download.cache_sis.残念;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.http.HttpFactory;
import org.sdjen.download.cache_sis.log.CassandraFactory;
import org.sdjen.download.cache_sis.log.LogUtil;

public class DownloadList {
	private DownloadSingle downloadSingle;

	public static void main(String[] args) throws Throwable {
		new DownloadList();
	}

	boolean autoFirst;
	HttpFactory httpUtil;
	String list_url;
	ConfUtil conf;

	public DownloadList() throws Throwable {
		conf = ConfUtil.getDefaultConf();
		LogUtil.init();
		downloadSingle = new DownloadSingle();
		httpUtil = new HttpFactory();
		downloadSingle.setHttpUtil(httpUtil);
		try {
			autoFirst = true;
			try {
				if (conf.getProperties().containsKey("auto_first"))
					autoFirst = Boolean.valueOf(conf.getProperties().getProperty("auto_first"));
				else 
					conf.getProperties().setProperty("auto_first", String.valueOf(autoFirst));
			} catch (Exception e) {
			}
			int from = Integer.valueOf(conf.getProperties().getProperty("list_start"));
			int to = Integer.valueOf(conf.getProperties().getProperty("list_end"));
			int pageU = 50;
			try {
				pageU = Integer.valueOf(conf.getProperties().getProperty("list_page_max"));
			} catch (Exception e) {
			}
			// do {
			// list(page, Math.min(page + pageU - 1, limit));
			// page += pageU;
			// } while (page <= limit);
			try {
				list_url = ConfUtil.getDefaultConf().getProperties().getProperty("list_url");
				for (int i = from; i <= to; i++) {
					if (i != from && ((i - from) % pageU == 0)) {
						for (int j = 1; j < 3; j++) {
							list(j);
						}
						LogUtil.refreshMsgLog();
					}
					list(i);
					if (autoFirst) {
						conf.getProperties().setProperty("list_start", String.valueOf(i));
						conf.store();// �ɹ��򱣴棬�����жϺ����ִ��
					}
				}
			} finally {
			}
		} finally {
			httpUtil.finish();
			LogUtil.finishAll();
			CassandraFactory.getDefaultFactory().finish();
			// downloadSingle.startDownload("http://www.sexinsex.net/bbs/thread-7701385-1-9.html",
			// "WW.html");
			// new
			// DownloadSingle_HttpClient_T().setConfUtil(confUtil).startDownload("http://tieba.baidu.com/p/3986480945",
			// "WW.html");
			// new
			// DownloadSingle_HttpClient().setConfUtil(confUtil).startDownload("http://tieba.baidu.com/p/3986480945",
			// "EEE.html");
		}
	}

	private void list(int i) throws Throwable {
		final String uri = MessageFormat.format(list_url, String.valueOf(i));
		LogUtil.lstLog.showMsg(uri);
		String html = httpUtil.getHTML(uri);
		org.jsoup.nodes.Document doument = Jsoup.parse(html);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		List<Future<Long>> resultList = new ArrayList<Future<Long>>();
		for (final org.jsoup.nodes.Element e : doument.select("tbody").select("tr")) {
			resultList.add(executor.submit(new Callable<Long>() {
				public Long call() throws Exception {
					String date = "";
					for (org.jsoup.nodes.Element s : e.select("td.author")// class=author的td
							.select("em")) {
						String text = s.text();
						try {
							SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
							date = dateFormat.format(dateFormat.parse(text));
							dateFormat = null;
						} catch (Exception e) {
							System.out.println(text + "	" + e);
						}
					}
					Long result = null;
					for (org.jsoup.nodes.Element s : e.select("th").select("span").select("a[href]")) {
						try {
							if (downloadSingle.startDownload(httpUtil.joinUrlPath(uri, s.attr("href")), s.text() + ".html", date)) {
								if (null == result)
									result = 0l;
								result += downloadSingle.getLength_download();
								break;// ֻ��ע��һ������
							}
						} catch (Throwable e1) {
							e1.printStackTrace();
						}
					}
					return result;
				}
			}));// ������ִ�н���洢��List��
		}
		executor.shutdown();
		long count = 0, length_download = 0;
		for (Future<Long> fs : resultList) {
			try {
				// while (!fs.isDone())
				// ;// Future�������û����ɣ���һֱѭ���ȴ���ֱ��Future�������
				Long length = fs.get(30, TimeUnit.MINUTES);// �����̣߳�����ִ�еĽ��
				if (null != length) {
					length_download += length;
					count++;
				}
			} catch (java.util.concurrent.TimeoutException e) {
				fs.cancel(false);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
		}
		// httpUtil.getPoolConnManager().closeExpiredConnections();
	}

	private void list(int from, int to) throws Throwable {
		final HttpFactory httpUtil = new HttpFactory();
		downloadSingle.setHttpUtil(httpUtil);
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		try {
			for (int i = from; i <= to; i++) {
				final String uri = MessageFormat.format(ConfUtil.getDefaultConf().getProperties().getProperty("list_url"), String.valueOf(i));
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
									.startDownload(httpUtil.joinUrlPath(uri, s.attr("href")), s.text() + ".html", date)) {
								length_download += downloadSingle.getLength_download();
								count++;
							}
						} catch (Throwable e1) {
							e1.printStackTrace();
						}
					}
				}
				LogUtil.lstLog.showMsg("	Total:	{0}	{1}(byte)", count, length_download);
				// httpUtil.getPoolConnManager().closeExpiredConnections();
			}
		} finally {
			httpUtil.finish();
			LogUtil.refreshMsgLog();
		}
	}
}
