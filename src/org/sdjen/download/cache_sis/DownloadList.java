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
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.store.Store_ElasticSearch;

import test.GetConnection;

public class DownloadList {
	private DownloadSingle downloadSingle;
	private IStore store = null;

	public static void main(String[] args) throws Throwable {
		ConfUtil conf = ConfUtil.getDefaultConf();
		String type = args.length > 0 ? args[0] : "torrent";
		int from = Integer.valueOf(args.length > 1 ? args[1] : conf.getProperties().getProperty("list_start"));
		int to = Integer.valueOf(args.length > 1 ? args[1] : conf.getProperties().getProperty("list_end"));
		new DownloadList(type).execute(from, to);
		HttpFactory.getPoolConnManager().close();
	}

	boolean autoFirst;
	HttpFactory httpUtil;
	String list_url;
	ConfUtil conf;
	String type;

	public DownloadList(String type) throws Throwable {
		this.type = null == type ? "" : type;
		conf = ConfUtil.getDefaultConf();
		LogUtil.init();
		downloadSingle = new DownloadSingle();
		httpUtil = new HttpFactory();
		downloadSingle.setHttpUtil(httpUtil);
		list_url = conf.getProperties().getProperty("list_url");
		store = Store_ElasticSearch.getStore();
	}

	public void execute(int from, int to) throws Throwable {
		try {
			autoFirst = true;
			try {
				if (conf.getProperties().containsKey("auto_first"))
					autoFirst = Boolean.valueOf(conf.getProperties().getProperty("auto_first"));
				else
					conf.getProperties().setProperty("auto_first", String.valueOf(autoFirst));
			} catch (Exception e) {
			}
			int pageU = 50;
			try {
				pageU = Integer.valueOf(conf.getProperties().getProperty("list_page_max"));
			} catch (Exception e) {
			}
			try {
				for (int i = from; i <= to; i++) {
					if (i != from && ((i - from) % pageU == 0)) {// 执行到一定数量重新下载3页，保证齐全
						for (int j = 2; j < 3; j++) {
							list(j, "");
						}
						store.refreshMsgLog();
					}
					list(i, type);
					if (autoFirst) {
						conf.getProperties().setProperty("list_start", String.valueOf(i));
						conf.store();// 自动记录最后一次执行完成
					}
				}
			} finally {
			}
		} catch (Throwable e) {
			store.err("异常终止	{0}", e);
			throw e;
		} finally {
			store.msg("Finish");
			httpUtil.finish();
			LogUtil.finishAll();
			// CassandraFactory.getDefaultFactory().finish();
			// GetConnection.getConnection().finish();
		}
	}

	protected String getHTML(String uri) throws Throwable {
		return httpUtil.getHTML(uri);
	}

	protected void list(final int i, String type) throws Throwable {
		long t = System.currentTimeMillis();
		String uri = MessageFormat.format(list_url, String.valueOf(i));
		String html = getHTML(uri);
		org.jsoup.nodes.Document doument = Jsoup.parse(html);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		List<Future<Long>> resultList = new ArrayList<Future<Long>>();
		for (final org.jsoup.nodes.Element element : doument.select("tbody").select("tr")) {
			resultList.add(executor.submit(new Callable<Long>() {
				public Long call() throws Exception {
					String date = "";
					for (org.jsoup.nodes.Element s : element.select("td.author")// class=author的td
							.select("em")) {
						String text = s.text();
						try {
							SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
							date = dateFormat.format(dateFormat.parse(text));
							dateFormat = null;
						} catch (Exception e) {
							store.err("异常	{0}	{1}", text, e);
						}
					}
					Long result = null;
					String id = null;
					String title = null;
					for (org.jsoup.nodes.Element s : element.select("th").select("span")) {//
						boolean threadpages = s.classNames().contains("threadpages");
						for (org.jsoup.nodes.Element href : s.select("a[href]")) {
							if (null == id) {
								id = s.id();
								id = id.substring(id.indexOf("_") + 1);
								title = href.text();
							}
							String page = threadpages ? href.text() : "1";
							String url = httpUtil.joinUrlPath(uri, href.attr("href"));
							try {
								Long length = downloadSingle.startDownload(type, id, page, url, title, date);
								if (null != length) {
									if (null == result)
										result = 0l;
									result += length;
									// break;
								}
							} catch (Throwable e) {
								store.err("异常	{0}	{1}", url, e);
								e.printStackTrace();
							}
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
		HttpFactory.getPoolConnManager().closeExpiredConnections();
		store.msg("耗时{3}	下载	{0} byte,	{1} 项	{2}", length_download, count, uri, (System.currentTimeMillis() - t));
		// httpUtil.getPoolConnManager().closeExpiredConnections();
	}
}
