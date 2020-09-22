package org.sdjen.download.cache_sis.service;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.util.EntryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("TestThreadsDownloadTimer")
public class TestThreadsDownloadTimer implements InitStartTimer {
	private final static Logger logger = LoggerFactory.getLogger(TestThreadsDownloadTimer.class);
	@Autowired
	private HttpUtil httpUtil;
	@Resource(name = "${definde.service.name.store}")
	private IStore store;

	public TestThreadsDownloadTimer() {
		System.out.println(">>>>>>>>>>>>TestThreadsDownloadTimer");
	}

	public void restart(double hours) throws Throwable {
//		System.out.println(httpUtil.getHTML("http://www.sexinsex.net/bbs/thread-8752715-1-1.html"));
		for (int i = 2; i < 3; i++) {
			String uri = MessageFormat.format("http://www.sexinsex.net/bbs/forum-143-{0}.html", String.valueOf(i));
			org.jsoup.nodes.Document doument = Jsoup.parse(httpUtil.getHTML(uri));
			List<Map<String, Object>> list = new ArrayList<>();
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
						list.add(new EntryData<String,Object>()//
//								.put("type"	, type)//
								.put("id", id)//
								.put("page", page)//
								.put("url", url)//
								.put("title", title)//
								.put("date", date)//
								.getData());
					}
				}
			}

			ExecutorService executor = Executors.newFixedThreadPool(2);
			List<Future<String>> resultList = new ArrayList<>();

			for (Map<String, Object> map : list) {
				resultList.add(executor.submit(new Callable<String>() {
					public String call() throws Exception {
						try {
							Thread.sleep(1000);
							String html = httpUtil.getHTML((String) map.get("url"));
//									downloadSingle.startDownload(type, id, page, url, title, date);
//							if (null != length) {
//								if (null == result)
//									result = 0l;
//								result += length;
//								// break;
//							}
							return "成功	" + map.get("url");
						} catch (Throwable e) {
							return "失败	" + map.get("url");
						}
					}
				}));

			}
			executor.shutdown();
			for (Future<String> fs : resultList) {
				try {
					logger.info(fs.get(30, TimeUnit.MINUTES));
				} catch (java.util.concurrent.TimeoutException e) {
					fs.cancel(false);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
				}
			}
		}

	}
}