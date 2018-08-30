package org.sdjen.download.cache_sis;

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
import org.sdjen.download.cache_sis.log.LogUtil;
import org.sdjen.download.cache_sis.log.MapDBFactory;

public class DownloadList {
	private DownloadSingle downloadSingle;

	public static void main(String[] args) throws Throwable {
		new DownloadList();
	}

	public DownloadList() throws Throwable {
		ConfUtil conf = ConfUtil.getDefaultConf();
		LogUtil.init();
		MapDBFactory.init();
		downloadSingle = new DownloadSingle();
		final HttpFactory httpUtil = new HttpFactory();
		downloadSingle.setHttpUtil(httpUtil);
		try {
			boolean autoFirst = true;
			try {
				autoFirst = Boolean.valueOf(conf.getProperties().getProperty("auto_first"));
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
				String list_url = ConfUtil.getDefaultConf().getProperties().getProperty("list_url");
				for (int i = from; i <= to; i++) {
					if ((i - from) % pageU == 0)
						LogUtil.refreshMsgLog();
					final String uri = MessageFormat.format(list_url, String.valueOf(i));
					LogUtil.lstLog.showMsg(uri);
					String html = httpUtil.getHTML(uri);
					org.jsoup.nodes.Document doument = Jsoup.parse(html);
					ExecutorService executor = Executors.newFixedThreadPool(2);
					List<Future<Long>> resultList = new ArrayList<Future<Long>>();
					for (final org.jsoup.nodes.Element e : doument.select("tbody").select("tr")) {
						resultList.add(executor.submit(new Callable<Long>() {
							public Long call() throws Exception {
								final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
								String date = "";
								for (org.jsoup.nodes.Element s : e.select("td.author").select("em")) {
									String text = s.text();
									try {
										date = dateFormat.format(dateFormat.parse(text));
									} catch (Exception e) {
										System.out.println(text + "	" + e);
									}
								}
								Long result = null;
								for (org.jsoup.nodes.Element s : e.select("th").select("span").select("a[href]")) {
									try {
										if (downloadSingle.startDownload(httpUtil.joinUrlPath(uri, s.attr("href")),
												s.text() + ".html", date)) {
											if (null == result)
												result = 0l;
											result += downloadSingle.getLength_download();
											break;// 只关注第一条命中
										}
									} catch (Throwable e1) {
										e1.printStackTrace();
									}
								}
								return result;
							}
						}));// 将任务执行结果存储到List中
					}
					executor.shutdown();
					long count = 0, length_download = 0;
					for (Future<Long> fs : resultList) {
						try {
							// while (!fs.isDone())
							// ;// Future返回如果没有完成，则一直循环等待，直到Future返回完成
							Long length = fs.get(30, TimeUnit.MINUTES);// 各个线程（任务）执行的结果
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
					LogUtil.lstLog.showMsg("	Total:	{0}	{1}(byte)	map_url_size:{2}	map_file_size:{3}", count,
							length_download, MapDBFactory.getUrlDB().size(), MapDBFactory.getFileDB().size());
					// httpUtil.getPoolConnManager().closeExpiredConnections();
					if (autoFirst) {
						conf.getProperties().setProperty("list_start", String.valueOf(i));
						conf.store();// 成功则保存，方便中断后继续执行
					}
				}
			} finally {
			}
		} finally {
			httpUtil.finish();
			MapDBFactory.finishAll();
			LogUtil.finishAll();
			// downloadSingle.startDownload("http://www.sexinsex.net/bbs/thread-7701385-1-9.html",
			// "WW.html");
			// new
			// DownloadSingle_HttpClient_T().setConfUtil(confUtil).startDownload("http://tieba.baidu.com/p/3986480945",
			// "WW.html");
			// new
			// DownloadSingle_HttpClient().setConfUtil(confUtil).startDownload("http://tieba.baidu.com/p/3986480945",
			// "EEE.html");
			System.out.println("完成！");
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
				// httpUtil.getPoolConnManager().closeExpiredConnections();
			}
		} finally {
			httpUtil.finish();
			LogUtil.refreshMsgLog();
		}
	}
}
