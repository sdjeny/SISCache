package org.sdjen.download.cache_sis.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.log.LogUtil;

public class HttpFactory {
	private CloseableHttpClient proxyClient;
	private CloseableHttpClient client;
	private ConfUtil conf;
	private int retry_times = 5;
	private int retry_time_second = 30;
	private int timout_millisecond_connect = 10000;
	private int timout_millisecond_connectionrequest = 10000;
	private int timout_millisecond_socket = 10000;
	private Set<String> proxy_urls = new HashSet<String>();
	private PoolingHttpClientConnectionManager poolConnManager = null;
	private int maxTotalPool = 200;
	private int maxConPerRoute = 20;
	private int socketTimeout = 10000;
	private int connectionRequestTimeout = 2000;
	private int connectTimeout = 10000;

	public HttpFactory() throws IOException {
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
			timout_millisecond_connect = Integer
					.valueOf(conf.getProperties().getProperty("timout_millisecond_connect"));
		} catch (Exception e) {
			conf.getProperties().setProperty("timout_millisecond_connect", String.valueOf(timout_millisecond_connect));
			isStore = true;
		}
		try {
			timout_millisecond_connectionrequest = Integer
					.valueOf(conf.getProperties().getProperty("timout_millisecond_connectionrequest"));
		} catch (Exception e) {
			conf.getProperties().setProperty("timout_millisecond_connectionrequest",
					String.valueOf(timout_millisecond_connectionrequest));
			isStore = true;
		}
		try {
			timout_millisecond_socket = Integer.valueOf(conf.getProperties().getProperty("timout_millisecond_socket"));
		} catch (Exception e) {
			conf.getProperties().setProperty("timout_millisecond_socket", String.valueOf(timout_millisecond_socket));
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
		try {
			SSLConnectionSocketFactory sslsf;
			// sslsf = new
			// SSLConnectionSocketFactory(SSLContexts.custom().loadTrustMaterial(null,
			// new
			// TrustSelfSignedStrategy()).build(),
			// SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			String supportedProtocols = conf.getProperties().getProperty("supportedProtocols");
			if (null == supportedProtocols) {
				conf.getProperties().setProperty("supportedProtocols",
						supportedProtocols = "TLSv1.2,TLSv1.1,TLSv1,SSLv3,SSLv2Hello");
				isStore = true;
			}
			// new String[] { "SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.2" }
			sslsf = new SSLConnectionSocketFactory(
					SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
					supportedProtocols.split(","), null, NoopHostnameVerifier.INSTANCE);
			// sslsf =
			// org.apache.http.conn.ssl.SSLConnectionSocketFactory.getSocketFactory();
			// sslsf = new
			// SSLConnectionSocketFactory(SSLContexts.custom().loadTrustMaterial(null,
			// new
			// TrustSelfSignedStrategy()).build(),
			// NoopHostnameVerifier.INSTANCE);
			// sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault());
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())//
					.register("https", sslsf)//
					.build();
			poolConnManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			// Increase max total connection to 200
			poolConnManager.setMaxTotal(maxTotalPool);
			// Increase default max connection per route to 20
			poolConnManager.setDefaultMaxPerRoute(maxConPerRoute);
			poolConnManager
					.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(timout_millisecond_socket).build());
		} catch (Exception e) {
			LogUtil.errLog.showMsg("InterfacePhpUtilManager init Exception" + e.toString());
		}
		if (isStore)
			conf.store();
	}

	private org.apache.http.client.config.RequestConfig.Builder getDefaultBuilder() {
		return RequestConfig.custom()//
				.setConnectTimeout(timout_millisecond_connect)// �������ӳ�ʱʱ�䣬��λ���롣
				.setConnectionRequestTimeout(timout_millisecond_connectionrequest) // ���ô�connect
				// Manager(���ӳ�)��ȡConnection
				// ��ʱʱ�䣬��λ���롣����������¼ӵ����ԣ���ΪĿǰ�汾�ǿ��Թ������ӳصġ�
				.setSocketTimeout(timout_millisecond_socket)// �����ȡ���ݵĳ�ʱʱ��(����Ӧʱ��)����λ���롣
				// �������һ���ӿڣ�����ʱ�����޷��������ݣ���ֱ�ӷ����˴ε��á�
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES)//
		;
	}

	private CloseableHttpClient getClient() {
		if (null == client) {
			// ʵ����CloseableHttpClient����
			client = HttpClients.custom()//
					.setConnectionManager(poolConnManager)//
					.setDefaultRequestConfig(getDefaultBuilder().build())//
					.build();
		}
		return client;
	}

	private CloseableHttpClient getProxyClient() {
		if (null == proxyClient) {
			org.apache.http.client.config.RequestConfig.Builder builder = getDefaultBuilder();
			try {
				String[] s = conf.getProperties().getProperty("proxy").split(":");
				HttpHost proxy = new HttpHost(s[0], Integer.valueOf(s[1]), "http");// ���ô���IP���˿ڡ�Э�飨��ֱ��滻��
				builder.setProxy(proxy);// �Ѵ������õ���������
				// showMsg("����{0}", proxy);
			} catch (Exception e) {
			}
			// ʵ����CloseableHttpClient����
			proxyClient = HttpClients.custom()//
					.setConnectionManager(poolConnManager)//
					.setDefaultRequestConfig(builder.build())//
					.build();
		}
		return proxyClient;
	}

	public void finish() {
		try {
			client.close();
		} catch (Throwable e) {
		}
		try {
			proxyClient.close();
		} catch (Throwable e) {
		}
		client = null;
		proxyClient = null;
		// ���ӳعر�
		poolConnManager.close();
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

	private boolean needProxy(String uri) {
		for (String s : proxy_urls) {
			if (uri.startsWith(s))
				return true;
		}
		return false;
	}

	public void execute(String uri, Executor<?> executor) throws Exception {
		InputStream in = null;
		HttpGet get = null;
		org.apache.http.client.methods.CloseableHttpResponse response = null;
		try {
			get = new HttpGet(uri);
			boolean needProxy = needProxy(uri);
			try {// �Ȳ�Ҫ������һ��
				response = (needProxy ? getProxyClient() : getClient()).execute(get);
			} catch (IOException e) {// ���о�ʹ�ô�������һ��
				get.abort();
				if (!needProxy) {// �����ֱ�B��ʽ���Q����ʽԇһ��
					get = new HttpGet(uri);
					try {
						response = getProxyClient().execute(get);
					} catch (IOException e1) {
						get.abort();
						throw e;
					}
					// Ȼ��ѵ�ַ������Ҫ���������
					int index = uri.indexOf('/', 9);
					String proxy_url;
					if (-1 != index)
						proxy_url = uri.substring(0, index);
					else
						proxy_url = uri;
					if (!proxy_url.isEmpty() && !proxy_urls.contains(proxy_url)) {
						proxy_urls.add(proxy_url);
						conf.getProperties().setProperty("proxy_urls",
								conf.getProperties().getProperty("proxy_urls") + "," + proxy_url);
						conf.store();
						LogUtil.errLog.showMsg("ADD:	{0}", proxy_url);
					}
				} else {// ����ѽ��Ǵ���ʽ���Ǿ����B�����ˡ�
					throw e;
				}
			}
			if (response.getStatusLine().getStatusCode() != 200) {
				get.abort();
				return;
			}
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				executor.execute(in = entity.getContent());
			}
		} catch (Exception e) {
			if (null != get)
				get.abort();
			throw e;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			if (null != response) {
				try {
					response.close();
				} catch (Exception e) {
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
				HttpFactory.this.execute(uri, executor);
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
				// closeClient();
				count++;
				Thread.sleep(1000l * retry_time_second * count);
				LogUtil.errLog.showMsg("Retry	{0}	{1}", count, e);
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
