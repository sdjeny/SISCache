package org.sdjen.download.cache_sis;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.configuration.ConfUtil;
import org.sdjen.download.cache_sis.configuration.ConfigMain;
import org.sdjen.download.cache_sis.http.HttpFactory;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.log.LogUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.util.EntryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class DownloadList {
	private final static Logger logger = LoggerFactory.getLogger(DownloadList.class);
	@Autowired
	private DownloadSingle downloadSingle;
	@Resource(name = "${definde.service.name.store}")
	private IStore store;
	@Autowired
	private ConfigMain configMain;
	@Resource(name = "downloadListExecutor")
	private ThreadPoolTaskExecutor executor;
	@Autowired
	private HttpUtil httpUtil;
	private String lastMsg = "";

	boolean autoFirst;
//	ConfUtil conf;

	public static void main(String[] args) throws Throwable {
		ConfUtil conf = ConfUtil.getDefaultConf();
		String type = args.length > 0 ? args[0] : "torrent";
		int from = Integer.valueOf(args.length > 1 ? args[1] : conf.getProperties().getProperty("list_start"));
		int to = Integer.valueOf(args.length > 2 ? args[2] : conf.getProperties().getProperty("list_end"));
		new DownloadList().execute(type, from, to);
		HttpFactory.getPoolConnManager().close();
	}

	public DownloadList() throws Throwable {
		System.out.println(">>>>>>>>>>>>>>>>>>DownloadList");
//		conf = ConfUtil.getDefaultConf();
		LogUtil.init();
	}

	@Async("taskExecutor")
	public void execute_async(String type, int from, int to) throws Throwable {
		execute(type, from, to);
	}

	public void execute(String type, int from, int to) throws Throwable {
		synchronized (this) {
			Map<String, Object> last = store.getLast("download_list");
			if (null != last) {
				if (last.containsKey("running") && (Boolean) last.get("running")) {
					logger.info(">>>>>>>>>>>>download_list is Running");
					return;
				}
			}
			store.running("download_list",
					JsonUtil.toJson(new EntryData<>().put("type", type).put("from", from).put("to", to).getData()),
					"init");
			lastMsg = "";
			downloadSingle.init();
		}
		try {
//			autoFirst = true;
//			try {
//				if (conf.getProperties().containsKey("auto_first"))
//					autoFirst = Boolean.valueOf(conf.getProperties().getProperty("auto_first"));
//				else
//					conf.getProperties().setProperty("auto_first", String.valueOf(autoFirst));
//			} catch (Exception e) {
//			}
//			int pageU = 50;
//			try {
//				pageU = Integer.valueOf(conf.getProperties().getProperty("list_page_max"));
//			} catch (Exception e) {
//			}
			try {
				for (int i = from; i <= to; i++) {
					if (i != from && ((i - from) % configMain.getList_page_middle() == 0)) {// 执行到一定数量重新下载3页，保证齐全
						for (int j = configMain.getList_page_middle_begin(); j < configMain.getList_page_middle_end(); j++) {
							list(j, type, JsonUtil.toJson(new EntryData<>().put("type", type).put("from", i)
									.put("to", to).put("middle", j).getData()));
						}
//						store.refreshMsgLog();
					}
					list(i, type, JsonUtil
							.toJson(new EntryData<>().put("type", type).put("from", i).put("to", to).getData()));
//					if (autoFirst) {
//						conf.getProperties().setProperty("list_start", String.valueOf(i));
//						conf.store();// 自动记录最后一次执行完成
//					}
				}
				store.finish("download_list", "finish");
			} finally {
			}
		} catch (Throwable e) {
			store.finish("download_list", e.getMessage());
			store.err("异常终止	{0}", e);
			throw e;
		} finally {
			store.msg("Finish");
			// CassandraFactory.getDefaultFactory().finish();
			// GetConnection.getConnection().finish();
		}
	}

	protected String getHTML(String uri) throws Throwable {
		return httpUtil.getHTML(uri);
	}

	protected void list(final int i, String type, String logMsg) throws Throwable {
		for (String fid : configMain.getFids()) {
			if (IStore.FIDDESCES.containsKey(fid)) {
				store.running("download_list", logMsg,
						lastMsg + " Running:" + fid + "[" + IStore.FIDDESCES.get(fid) + "]");
				long t = System.currentTimeMillis();
				String uri = MessageFormat.format(configMain.getList_url(), fid, String.valueOf(i));
				String html = getHTML(uri);
				org.jsoup.nodes.Document doument = Jsoup.parse(html);
				Map<String, Map<String, String>> map = new LinkedHashMap<>();
				for (final org.jsoup.nodes.Element element : doument.select("tbody").select("tr")) {
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
							map.put(url, new EntryData<String, String>()//
									.put("id", id)//
									.put("page", page)//
									.put("url", url)//
									.put("title", title)//
									.put("date", date)//
									.getData());
						}
					}
				}
				List<Future<Long>> resultList = new ArrayList<Future<Long>>();
				for (Entry<String, Map<String, String>> entry : map.entrySet()) {
					resultList.add(executor.submit(new Callable<Long>() {
						public Long call() throws Exception {
							Long result = 0l;
							try {
								Long length = downloadSingle.startDownload(type, fid, entry.getValue().get("id"),
										entry.getValue().get("page"), entry.getValue().get("url"),
										entry.getValue().get("title"), entry.getValue().get("date"));
								if (null != length) {
									result += length;
								}
							} catch (Throwable e) {
								store.err("异常	{0}	{1}", entry.getValue().get("url"), e);
								e.printStackTrace();
							}
							return result;
						}
					}));// ������ִ�н���洢��List��
				}
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
//				HttpFactory.getPoolConnManager().closeExpiredConnections();
				store.msg(lastMsg = MessageFormat.format("耗时{3}	下载	{0} byte,	{1} 项	{4}[{5}]	{2}",
						length_download, count, uri, (System.currentTimeMillis() - t), fid, IStore.FIDDESCES.get(fid)));
				// httpUtil.getPoolConnManager().closeExpiredConnections();
			}
		}
	}
}
