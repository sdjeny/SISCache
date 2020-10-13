package org.sdjen.download.cache_sis.timer;

import java.util.Collection;

import javax.annotation.Resource;

import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.test.WithoutInterfaceService;
import org.sdjen.download.cache_sis.util.EntryData;
import org.sdjen.download.cache_sis.util.JsoupAnalysisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service("TestTimer")
public class TestTimer implements InitStartTimer {
	final static Logger logger = LoggerFactory.getLogger(TestTimer.class);
	@Autowired
	private HttpUtil httpUtil;
	@Resource(name = "${definde.service.name.store}")
	private IStore store;
	@Resource(name = "downloadListExecutor")
	private ThreadPoolTaskExecutor dlExecutor;
	@Resource(name = "downloadSingleExecutor")
	private ThreadPoolTaskExecutor dsExecutor;
	@Value("${siscache.conf.fids}")
	private Collection<String> fids;
	@Autowired
	WithoutInterfaceService service;

	public TestTimer() {
		System.out.println(">>>>>>>>>>>>TestTimer");
	}

	public void restart(double hours) throws Throwable {
		System.out.println(">>>>>>>>>>>>TestTimer:" + fids);
		String s = httpUtil.getHTML("http://www.sexinsex.net/bbs/thread-8694622-1-1.html");
		System.out.println(s);
		System.out.println(JsonUtil.toPrettyJson(JsoupAnalysisor.split(s)));
//		for (int i = 0; i < 100; i++) {
//			service.async();
//		}
//		List<Future> dl = new ArrayList<>();
//		for (int i = 0; i < 10; i++) {
//			int ii = i;
//			logger.info("begin:	{}", ii);
//			dl.add(dlExecutor.submit(() -> {
//				List<Future> ds = new ArrayList<>();
//				for (int j = 10; j < 60; j++) {
//					int jj = j;
//					logger.info("begin:	{}:{}", ii,jj);
//					ds.add(dsExecutor.submit(() -> {
//						logger.info("finish:	{}:{}", ii,jj);
//						try {
//							Thread.sleep(300l);
//						} catch (InterruptedException e) {
//						}
//					}));
//				}
//				ds.forEach(f -> {
//					try {
//						f.get();
//					} catch (Exception e) {
//					}
//				});
//				logger.info("finish:	{}", ii);
//			}));
//		}
//		dl.forEach(f -> {
//			try {
//				f.get();
//			} catch (Exception e) {
//			}
//		});
//		String s = httpUtil.doLocalGet("http://192.168.0.237:9200/siscache_html/_doc/{key}",
//				new EntryData<String, String>().put("key", "8757080_1").getData());
//		ESMap esMap = JsonUtil.toObject(s, ESMap.class);
//		s = esMap.get("_source", ESMap.class).get("context_zip", String.class);
//		System.out.println(s);
//		System.out.println(ZipUtil.uncompress(s));
//		httpUtil.getHTML("https://www.baidu.com/", "utf8");
//		System.out.println("kais");
//		for (String id : "8720516".split(",")) {//8758015,8758189,8756205,8757822
//			Files.write(httpUtil.getHTML("http://www.sexinsex.net/bbs/thread-"+id
//					+ "-1-1.html").getBytes("GBK"),new File("sisdemo_"+id+".html"));
//			System.out.println(id);
//		}
//		System.out.println(httpUtil.getHTML("http://www.sexinsex.net/bbs/thread-8752715-1-1.html"));
//		for (int i = 2; i < 3; i++) {
//			String uri = MessageFormat.format("http://www.sexinsex.net/bbs/forum-143-{0}.html", String.valueOf(i));
//			org.jsoup.nodes.Document doument = Jsoup.parse(httpUtil.getHTML(uri));
//			List<Map<String, Object>> list = new ArrayList<>();
//			for (final org.jsoup.nodes.Element element : doument.select("tbody").select("tr")) {
//
//				String date = "";
//				for (org.jsoup.nodes.Element s : element.select("td.author")// class=author的td
//						.select("em")) {
//					String text = s.text();
//					try {
//						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
//						date = dateFormat.format(dateFormat.parse(text));
//						dateFormat = null;
//					} catch (Exception e) {
//						store.err("异常	{0}	{1}", text, e);
//					}
//				}
//				String id = null;
//				String title = null;
//				for (org.jsoup.nodes.Element s : element.select("th").select("span")) {//
//					boolean threadpages = s.classNames().contains("threadpages");
//					for (org.jsoup.nodes.Element href : s.select("a[href]")) {
//						if (null == id) {
//							id = s.id();
//							id = id.substring(id.indexOf("_") + 1);
//							title = href.text();
//						}
//						String page = threadpages ? href.text() : "1";
//						String url = httpUtil.joinUrlPath(uri, href.attr("href"));
//						list.add(new EntryData<String,Object>()//
////								.put("type"	, type)//
//								.put("id", id)//
//								.put("page", page)//
//								.put("url", url)//
//								.put("title", title)//
//								.put("date", date)//
//								.getData());
//					}
//				}
//			}
//
//			ExecutorService executor = Executors.newFixedThreadPool(2);
//			List<Future<String>> resultList = new ArrayList<>();
//
//			for (Map<String, Object> map : list) {
//				resultList.add(executor.submit(new Callable<String>() {
//					public String call() throws Exception {
//						try {
//							Thread.sleep(1000);
//							String html = httpUtil.getHTML((String) map.get("url"));
////									downloadSingle.startDownload(type, id, page, url, title, date);
////							if (null != length) {
////								if (null == result)
////									result = 0l;
////								result += length;
////								// break;
////							}
//							return "成功	" + map.get("url");
//						} catch (Throwable e) {
//							return "失败	" + map.get("url");
//						}
//					}
//				}));
//
//			}
//			executor.shutdown();
//			for (Future<String> fs : resultList) {
//				try {
//					logger.info(fs.get(30, TimeUnit.MINUTES));
//				} catch (java.util.concurrent.TimeoutException e) {
//					fs.cancel(false);
//				} catch (Exception e) {
//					e.printStackTrace();
//				} finally {
//				}
//			}
//		}

	}
}
