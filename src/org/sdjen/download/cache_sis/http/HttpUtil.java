package org.sdjen.download.cache_sis.http;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import javax.annotation.Resource;

import org.sdjen.download.cache_sis.configuration.ConfigHttputil;
import org.sdjen.download.cache_sis.store.IStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
	@Resource(name = "${definde.service.name.store}")
	private IStore store;
	@Autowired
	private ConfigHttputil config;
	private Object lock_html = new Object();

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

	public interface Retry<R> {
		R execute() throws Throwable;
	}

	public HttpUtil() {
		System.out.println(">>>>>>>>>>>>Create HttpUtil");
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

	public String doLocalPostUtf8Json(String url, String content) throws Throwable {
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		return retry(() -> {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
			return restTemplate.postForEntity(url, new HttpEntity<>(content, headers), String.class).getBody();
		});
//		return restTemplate.postForEntity(url, new HttpEntity<>(content, headers), String.class).getBody();
	}

	public String doLocalPutUtf8Json(String url, String content) throws Throwable {
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		return retry(() -> {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
			return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(content, headers), String.class)
					.getBody();
		});
//		return restTemplate.postForEntity(url, new HttpEntity<>(content, headers), String.class).getBody();
	}

	public String doLocalGet(String url, Map<String, String> uriVariables) throws Throwable {
//		Map<String, Object> map = new HashMap<String, Object>();
//		map.put("flag", flag);
//		"http://10.145.198.143:8081/ords/data_service/monitor/IntMonitor/{flag}", 
		return retry(() -> restTemplate.getForEntity(url, String.class, uriVariables).getBody());
//		return restTemplate.getForEntity(url, String.class, uriVariables).getBody();
	}

	public String getHTML(final String uri) throws Throwable {
		return getHTML(uri, config.getChatset());
	}

	public String getHTML(final String uri, final String chatset) throws Throwable {
		final Executor<String> executor = new Executor<String>() {
			public void execute(byte[] bytes) throws Throwable {
				setResult(null);
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				result.write(bytes);
				setResult(result.toString(chatset));
			}
		};
		return retry(() -> {
			synchronized (lock_html) {
				HttpUtil.this.execute(uri, executor);
				String result = executor.getResult();
				if (null == result)
					throw new Exception("取不到数据	" + uri);
				else
					return result;
			}
		});
	}

	public <R> R retry(Retry<R> retry) throws Throwable {
		R result = null;
		boolean stop = false;
		int count = 0;
		do {
			try {
				result = retry.execute();
				stop = true;
			} catch (Throwable e) {
				// closeClient();
				count++;
				Thread.sleep(1000l * config.getRetry_time_second() * count);
				if (stop = count >= config.getRetry_times())
					throw e;
				logger.debug("Retry	{}	{}", count, e);
			}
		} while (!stop);
		return result;
	}

	private boolean needProxy(String uri) {
		for (String s : store.getProxyUrls()) {
			if (uri.startsWith(s))
				return true;
		}
		return false;
	}

	public void execute(String uri, Executor<?> executor) throws Throwable {
		Map<String, Object> connectCheck = store.connectCheck(uri);
		if (!(boolean) connectCheck.get("continue"))
			throw new Exception((String) connectCheck.get("msg"));
		logger.debug(">	" + uri);
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
					store.addProxyUrl(uri);
				} else {
					rsp = restTemplate.exchange(uri, HttpMethod.GET, ent, byte[].class);
					store.removeProxyUrl(uri);
//					throw e;// 已经经过代理的直接终止了
				}
			}
			if ((boolean) connectCheck.get("found"))
				store.logSucceedUrl(uri);
			if (rsp == null || !rsp.getStatusCode().is2xxSuccessful()) {
				return;
			}
			try {
				executor.execute(rsp.getBody());
			} catch (Throwable ue) {
				store.err(ue.getMessage(), ue);
			}
		} catch (Throwable e) {
			try {
				store.logFailedUrl(uri, e);
			} catch (Throwable ue) {
				store.err(ue.getMessage(), ue);
			}
			throw e;
		} finally {
		}
	}

}
