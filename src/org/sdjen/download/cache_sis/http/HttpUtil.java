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
				.setConnectTimeout(timout_millisecond_connect)// �������ӳ�ʱʱ�䣬��λ���롣
				.setConnectionRequestTimeout(timout_millisecond_connectionrequest) // ���ô�connect
				// Manager(���ӳ�)��ȡConnection
				// ��ʱʱ�䣬��λ���롣����������¼ӵ����ԣ���ΪĿǰ�汾�ǿ��Թ������ӳصġ�
				.setSocketTimeout(timout_millisecond_socket)// �����ȡ���ݵĳ�ʱʱ��(����Ӧʱ��)����λ���롣
				// �������һ���ӿڣ�����ʱ�����޷��������ݣ���ֱ�ӷ����˴ε��á�
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES);
		try {
			String[] s = conf.getProperties().getProperty("proxy").split(":");
			HttpHost proxy = new HttpHost(s[0], Integer.valueOf(s[1]), "http");// ���ô���IP���˿ڡ�Э�飨��ֱ��滻��
			builder.setProxy(proxy);// �Ѵ������õ���������
			// showMsg("����{0}", proxy);
		} catch (Exception e) {
		}
		// ʵ����CloseableHttpClient����
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
		// ���󷵻�
		return client.execute(new HttpGet(uri)).getEntity().getContent();
	}

	/**
	 * ����ָ����URL����html����
	 * 
	 * @param uri
	 *            ��ҳ�ĵ�ַ
	 * @param encoding
	 *            ���뷽ʽ
	 * @return ������ҳ��html����
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
					throw new Exception("ȡ����ʧ��");
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
	 * ��ȡURL����������ʵ�ļ���
	 * 
	 * @param uri
	 *            �磺http://www.hua.com/bg.jpg
	 * @return ����bg.jpg
	 */
	private String getUrlFileName(String uri) {
		return uri.split("/")[uri.split("/").length - 1];
	}

	/**
	 * ��ȡURL�����ļ�����·��
	 * 
	 * @param uri
	 *            �磺http://www.hua.com/bg.jpg
	 * @return ���� http://www.hua.com
	 */
	private String getUrlPath(String uri) {
		return uri.replaceAll("/" + getUrlFileName(uri), "");
	}

	/**
	 * ƴ��URL·�����ļ�����ע�⣺��../����/��ͷ��fileName��Ҫ��һ��Ŀ¼
	 * 
	 * @param uri
	 *            �磺http://www.hua.com/product/9010753.html
	 * @param fileName
	 *            �磺../skins/default/css/base.css
	 * @return http://www.hua.com/skins/default/css/base.css
	 */
	public String joinUrlPath(String uri, String fileName) {
		// showMsg("url:"+url);
		// showMsg("fileName:"+fileName);
		if (fileName.startsWith("http://") || fileName.startsWith("https://"))
			return fileName;
		// ���ȥ����http://��ǰ׺�󻹰�����/������˵��Ҫ��һ��Ŀ¼����ȥ����ǰ�ļ���
		if (uri.replaceAll("http://", "").replaceAll("https://", "").contains("/"))
			uri = getUrlPath(uri);
		if (fileName.startsWith("../") || fileName.startsWith("/")) {
			// ֻ�е�ǰURL�������Ŀ¼���ܺ��ˣ����ֻ��http://www.hua.com������˶�����
			if (uri.replaceAll("http://", "").replaceAll("https://", "").contains("/"))
				uri = getUrlPath(uri);
			fileName = fileName.substring(fileName.indexOf("/") + 1);
		}
		// showMsg("return:"+url+"/"+fileName);
		return uri + "/" + fileName;
	}
}
