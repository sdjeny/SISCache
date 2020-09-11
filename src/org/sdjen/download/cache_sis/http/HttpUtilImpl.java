package org.sdjen.download.cache_sis.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;

import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class HttpUtilImpl implements HttpUtil {
	@Resource(name = "restTemplate")
	private RestTemplate restTemplate;
	@Resource(name = "proxyRestTemplate")
	private RestTemplate proxyRestTemplate;

	private ConfUtil conf;
	private Set<String> proxy_urls = new HashSet<String>();
	private int retry_times = 5;
	private int retry_time_second = 30;

	public HttpUtilImpl() {
		System.out.println(">>>>>>>>>>>>Create HttpUtilImpl");
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

	@Override
	public String getHTML(final String uri) throws Throwable {
		return getHTML(uri, conf.getProperties().getProperty("chatset"));
	}

	@Override
	public synchronized String getHTML(final String uri, final String chatset) throws Throwable {
		final Executor<String> executor = new Executor<String>() {
			public void execute(InputStream inputStream) throws Throwable {
				setResult(null);
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int length;
				while ((length = inputStream.read(buffer)) != -1) {
					result.write(buffer, 0, length);
				}
				setResult(result.toString(chatset));
			}
		};
		retry(new Retry() {
			public void execute() throws Throwable {
				HttpUtilImpl.this.execute(uri, executor);
				String result = executor.getResult();
				if (null == result)
					throw new Exception("取不到数据	" + uri);
			}
		});
		return executor.getResult();
	}

	public void retry(Retry retry) throws Throwable {
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

	@Override
	public void execute(String uri, Executor<?> executor) throws Throwable {
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
			executor.execute(new ByteArrayInputStream(rsp.getBody()));
		} catch (Throwable e) {
			throw e;
		} finally {
		}
	}
}
