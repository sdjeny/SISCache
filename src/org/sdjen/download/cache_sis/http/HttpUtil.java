package org.sdjen.download.cache_sis.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.log.LogUtil;

public class HttpUtil {
	private CloseableHttpClient client;

	private ConfUtil conf;

	private int retry_times = 5;
	private int retry_time_second = 30;
	private int timout_millisecond_connect = 10000;
	private int timout_millisecond_connectionrequest = 10000;
	private int timout_millisecond_socket = 10000;

	public HttpUtil setConfUtil(ConfUtil conf) {
		this.conf = conf;
		try {
			retry_times = Integer.valueOf(conf.getProperties().getProperty("retry_times"));
		} catch (Exception e) {
		}
		try {
			retry_time_second = Integer.valueOf(conf.getProperties().getProperty("retry_time_second"));
		} catch (Exception e) {
		}
		try {
			timout_millisecond_connect = Integer
					.valueOf(conf.getProperties().getProperty("timout_millisecond_connect"));
		} catch (Exception e) {
		}
		try {
			timout_millisecond_connectionrequest = Integer
					.valueOf(conf.getProperties().getProperty("timout_millisecond_connectionrequest"));
		} catch (Exception e) {
		}
		try {
			timout_millisecond_socket = Integer.valueOf(conf.getProperties().getProperty("timout_millisecond_socket"));
		} catch (Exception e) {
		}
		org.apache.http.client.config.RequestConfig.Builder builder = RequestConfig.custom()//
				.setConnectTimeout(timout_millisecond_connect)// 设置连接超时时间，单位毫秒。
				.setConnectionRequestTimeout(timout_millisecond_connectionrequest) // 设置从connect
				// Manager(连接池)获取Connection
				// 超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。
				.setSocketTimeout(timout_millisecond_socket)// 请求获取数据的超时时间(即响应时间)，单位毫秒。
				// 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES);
		try {
			String[] s = conf.getProperties().getProperty("proxy").split(":");
			HttpHost proxy = new HttpHost(s[0], Integer.valueOf(s[1]), "http");// 设置代理IP、端口、协议（请分别替换）
			builder.setProxy(proxy);// 把代理设置到请求配置
			// showMsg("代理：{0}", proxy);
		} catch (Exception e) {
		}
		// 实例化CloseableHttpClient对象
		client = HttpClients.custom().setDefaultRequestConfig(builder.build())//
				.build();
		return this;
	}

	public void finish() {
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static abstract class Executor<R> {
		private R result;

		public abstract void execute(InputStream inputStream);

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

	public void execute(String uri, Executor<?> executor) {
		InputStream in = null;
		HttpGet get = new HttpGet(uri);
		try {
			org.apache.http.client.methods.CloseableHttpResponse response = client.execute(get);
			if (response.getStatusLine().getStatusCode() != 200) {
				get.abort();
				return;
			}
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				executor.execute(in = entity.getContent());
			}
		} catch (Exception e) {
			get.abort();
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private InputStream getInputStream(String uri) throws IOException {
		// 请求返回
		return client.execute(new HttpGet(uri)).getEntity().getContent();
	}

	/**
	 * 根据指定的URL下载html代码
	 * 
	 * @param uri
	 *            网页的地址
	 * @param encoding
	 *            编码方式
	 * @return 返回网页的html内容
	 * @throws IOException
	 */
	public String getHTML(final String uri) throws Throwable {
		final Executor<String> executor = new Executor<String>() {
			public void execute(InputStream inputStream) {
				try {
					setResult(null);
					StringBuffer pageHTML = new StringBuffer();
					BufferedReader br;
					br = new BufferedReader(
							new InputStreamReader(inputStream, conf.getProperties().getProperty("chatset")));
					String line = null;
					while ((line = br.readLine()) != null) {
						pageHTML.append(line);
						pageHTML.append("\r\n");
					}
					setResult(pageHTML.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		retry(new Retry() {

			public void execute() throws Throwable {

				HttpUtil.this.execute(uri, executor);

				String result = executor.getResult();
				if (null == result)
					throw new Exception("取内容失败");
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
				finish();
				count++;
				Thread.sleep(1000l * retry_time_second * count);
				setConfUtil(conf);
				LogUtil.errLog.showMsg("Retry	{0}", count);
				stop = count >= retry_times;
				if (stop)
					throw e;
			}
		} while (!stop);
	}

	/**
	 * 获取URL中最后面的真实文件名
	 * 
	 * @param uri
	 *            如：http://www.hua.com/bg.jpg
	 * @return 返回bg.jpg
	 */
	private String getUrlFileName(String uri) {
		return uri.split("/")[uri.split("/").length - 1];
	}

	/**
	 * 获取URL不带文件名的路径
	 * 
	 * @param uri
	 *            如：http://www.hua.com/bg.jpg
	 * @return 返回 http://www.hua.com
	 */
	private String getUrlPath(String uri) {
		return uri.replaceAll("/" + getUrlFileName(uri), "");
	}

	/**
	 * 拼接URL路径和文件名，注意：以../或者/开头的fileName都要退一层目录
	 * 
	 * @param uri
	 *            如：http://www.hua.com/product/9010753.html
	 * @param fileName
	 *            如：../skins/default/css/base.css
	 * @return http://www.hua.com/skins/default/css/base.css
	 */
	public String joinUrlPath(String uri, String fileName) {
		// showMsg("url:"+url);
		// showMsg("fileName:"+fileName);
		if (fileName.startsWith("http://") || fileName.startsWith("https://"))
			return fileName;
		// 如果去掉“http://”前缀后还包含“/”符，说明要退一层目录，即去掉当前文件名
		if (uri.replaceAll("http://", "").replaceAll("https://", "").contains("/"))
			uri = getUrlPath(uri);
		if (fileName.startsWith("../") || fileName.startsWith("/")) {
			// 只有当前URL包含多层目录才能后退，如果只是http://www.hua.com，想后退都不行
			if (uri.replaceAll("http://", "").replaceAll("https://", "").contains("/"))
				uri = getUrlPath(uri);
			fileName = fileName.substring(fileName.indexOf("/") + 1);
		}
		// showMsg("return:"+url+"/"+fileName);
		return uri + "/" + fileName;
	}
}
