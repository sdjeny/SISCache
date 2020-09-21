package org.sdjen.download.cache_sis.service;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("TestThreadsDownloadTimer")
public class TestThreadsDownloadTimer implements InitStartTimer {
	private final static Logger logger = LoggerFactory.getLogger(TestThreadsDownloadTimer.class);
	@Autowired
	private HttpUtil httpUtil;

	public TestThreadsDownloadTimer() {
		System.out.println(">>>>>>>>>>>>TestThreadsDownloadTimer");
	}

	public void restart(double hours) {
		for (String s : Arrays.asList("fd"
//				, "EEEE", "dfafee", "W@!@E", "VRFD", "Dvbtrt", "<L<<P<OMIM", "((*JDKDKD",
//				"MINYUVCRE", "GC YGVUNM", "66KIMU VFC", ":LKKJNN"
				)) {
			try {
				ExecutorService executor = Executors.newFixedThreadPool(6);
				List<Future<Long>> resultList = new ArrayList<Future<Long>>();

				org.jsoup.nodes.Document doument;
				doument = Jsoup
						.parse(httpUtil.getHTML(MessageFormat.format("https://baike.baidu.com/item/{0}", s), "utf8"));
//				System.out.println(doument);
				for (final org.jsoup.nodes.Element element : doument.select("a")) {
					String text = element.text();
					String href = element.attr("href");
					resultList.add(executor.submit(new Callable<Long>() {
						public Long call() throws Exception {
							if (!text.isEmpty()) {
								try {
									if (href.startsWith("http"))
										return (long) httpUtil.getHTML(href).length();
									else if (href.startsWith("/") && !href.startsWith("/item/fd/"))
										return (long) httpUtil.getHTML("https://baike.baidu.com" + href).length();
								} catch (Throwable e) {
								}
							}
							return 0l;
						}
					}));
				}
//			resultList.add(executor.submit(new Callable<Long>() {
//				public Long call() throws Exception {
//					try {// c-container t
//						return (long) httpUtil.getHTML(MessageFormat.format("https://www.baidu.com/baidu?wd={0}", s))
//								.length();
//					} catch (Throwable e) {
//						return -1l;
//					}
//				}
//			}));
				logger.info("resultList.size:	" + resultList.size());
				executor.shutdown();
				for (Future<Long> fs : resultList) {
					try {
						Long length = fs.get(30, TimeUnit.MINUTES);
						logger.info("length:	" + length);
					} catch (java.util.concurrent.TimeoutException e) {
						fs.cancel(false);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
					}
				}
			} catch (Throwable e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
//		HttpFactory.getPoolConnManager().closeExpiredConnections();
		}
	}
}
