package org.sdjen.download.cache_sis.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HttpUtil {
	private final static Logger logger = LoggerFactory.getLogger(HttpUtil.class);

	@Resource(name = "restTemplate")
	private RestTemplate restTemplate;
	@Resource(name = "proxyRestTemplate")
	private RestTemplate proxyRestTemplate;

	private ConfUtil conf;
	private Set<String> proxy_urls = new HashSet<String>();
	private int retry_times = 5;
	private int retry_time_second = 30;

	public static abstract class Executor<R> {
		private R result;

		public abstract void execute(byte[] bytes) throws Throwable;

		public void setResult(R result) {
			this.result = result;
		}

		public R getResult() {
			return result;
		}
	}

	public interface Retry {
		void execute() throws Throwable;
	}

	public HttpUtil() {
		System.out.println(">>>>>>>>>>>>Create HttpUtil");
		try {
			conf = ConfUtil.getDefaultConf();
			boolean isStore = false;
			try {
				retry_times = Integer.valueOf(conf.getProperties().getProperty("retry_times"));
			} catch (Exception e) {
				conf.getProperties().setProperty("retry_times", String.valueOf(retry_times));
				isStore = true;
			}
			try {
				retry_time_second = Integer.valueOf(conf.getProperties().getProperty("retry_time_second"));
			} catch (Exception e) {
				conf.getProperties().setProperty("retry_time_second", String.valueOf(retry_time_second));
				isStore = true;
			}
			try {
				for (String s : conf.getProperties().getProperty("proxy_urls").split(",")) {
					proxy_urls.add(s.trim());
				}
				proxy_urls.remove("");
			} catch (Exception e) {
				conf.getProperties().setProperty("proxy_urls", "http://www.sexinsex.net");
				isStore = true;
			}
			if (isStore)
				conf.store();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public String joinUrlPath(String uri, String fileName) {
		if (fileName.startsWith("http://") || fileName.startsWith("https://"))
			return fileName;
		if (uri.replaceAll("http://", "").replaceAll("https://", "").contains("/"))
			uri = getUrlPath(uri);
		if (fileName.startsWith("../") || fileName.startsWith("/")) {
			if (uri.replaceAll("http://", "").replaceAll("https://", "").contains("/"))
				uri = getUrlPath(uri);
			fileName = fileName.substring(fileName.indexOf("/") + 1);
		}
		return uri + "/" + fileName;
	}

	private String getUrlPath(String uri) {
		return uri.substring(0, uri.lastIndexOf("/"));
	}

	public String doLocalPostUtf8Json(String url, String content) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		return restTemplate.postForEntity(url, new HttpEntity<>(content, headers), String.class).getBody();
	}

	public String doLocalGet(String url, Map<String, String> uriVariables) {
//		Map<String, Object> map = new HashMap<String, Object>();
//		map.put("flag", flag);
//		"http://10.145.198.143:8081/ords/data_service/monitor/IntMonitor/{flag}", 
		return restTemplate.getForEntity(url, String.class, uriVariables).getBody();
	}

	public String getHTML(final String uri) throws Throwable {
		return getHTML(uri, conf.getProperties().getProperty("chatset"));
	}

	public synchronized String getHTML(final String uri, final String chatset) throws Throwable {
		final Executor<String> executor = new Executor<String>() {
			public void execute(byte[] bytes) throws Throwable {
				setResult(null);
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				result.write(bytes);
				setResult(result.toString(chatset));
			}
		};
		retry(new Retry() {
			public void execute() throws Throwable {
				HttpUtil.this.execute(uri, executor);
				String result = executor.getResult();
				if (null == result)
					throw new Exception("取不到数据	" + uri);
			}
		});
		return executor.getResult();
	}

	public synchronized void retry(Retry retry) throws Throwable {
		boolean stop = false;
		int count = 0;
		do {
			try {
				retry.execute();
				stop = true;
			} catch (Throwable e) {
				// closeClient();
				count++;
				Thread.sleep(1000l * retry_time_second * count);
				stop = count >= retry_times;
				if (stop)
					throw e;
				System.err.println(MessageFormat.format("Retry	{0}	{1}", count, e));
			}
		} while (!stop);
	}

	private boolean needProxy(String uri) {
		for (String s : proxy_urls) {
			if (uri.startsWith(s))
				return true;
		}
		return false;
	}

	public synchronized void execute(String uri, Executor<?> executor) throws Throwable {
		logger.info(">	"+uri);
		try {
			boolean needProxy = needProxy(uri);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			org.springframework.http.HttpEntity<String> ent = new org.springframework.http.HttpEntity<String>("",
					headers);
			RestTemplate template = needProxy ? proxyRestTemplate : restTemplate;
			ResponseEntity<byte[]> rsp = null;
			try {
				rsp = template.exchange(uri, HttpMethod.GET, ent, byte[].class);
			} catch (Exception e) {
				if (!needProxy) {
					rsp = proxyRestTemplate.exchange(uri, HttpMethod.GET, ent, byte[].class);
					int index = uri.indexOf('/', 9);
					String proxy_url;
					if (-1 != index)
						proxy_url = uri.substring(0, index);// 排除参数部分
					else
						proxy_url = uri;
					if (!proxy_url.isEmpty() && !proxy_urls.contains(proxy_url)) {
						proxy_urls.add(proxy_url);
						conf.getProperties().setProperty("proxy_urls",
								conf.getProperties().getProperty("proxy_urls") + "," + proxy_url);
						conf.store();
						System.out.println(">>>>>>>>>ADD:	" + proxy_url);
					}
				} else {// 已经经过代理的直接终止了
					throw e;
				}
			}
			if (rsp == null || !rsp.getStatusCode().is2xxSuccessful()) {
				return;
			}
			executor.execute(rsp.getBody());
		} catch (Throwable e) {
			throw e;
		} finally {
		}
	}

}
