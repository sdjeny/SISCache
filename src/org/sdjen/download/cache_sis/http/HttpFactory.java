package org.sdjen.download.cache_sis.http;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.log.LogUtil;

public class HttpFactory {
	private CloseableHttpClient proxyClient;
	private CloseableHttpClient client;
	private org.apache.http.client.config.RequestConfig requestConfig;
	private org.apache.http.client.config.RequestConfig proxyRequestConfig;
	private ConfUtil conf;
	private int retry_times = 5;
	private int retry_time_second = 30;
	private int timout_millisecond_connect = 10000;
	private int timout_millisecond_connectionrequest = 10000;
	private Set<String> proxy_urls = new HashSet<String>();
	private static PoolingHttpClientConnectionManager poolConnManager = null;

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
			timout_millisecond_connect = Integer.valueOf(conf.getProperties().getProperty("timout_millisecond_connect"));
		} catch (Exception e) {
			conf.getProperties().setProperty("timout_millisecond_connect", String.valueOf(timout_millisecond_connect));
			isStore = true;
		}
		try {
			timout_millisecond_connectionrequest = Integer.valueOf(conf.getProperties().getProperty("timout_millisecond_connectionrequest"));
		} catch (Exception e) {
			conf.getProperties().setProperty("timout_millisecond_connectionrequest", String.valueOf(timout_millisecond_connectionrequest));
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
			if (false) {
				SSLConnectionSocketFactory sslsf;
				// sslsf = new
				// SSLConnectionSocketFactory(SSLContexts.custom().loadTrustMaterial(null,
				// new
				// TrustSelfSignedStrategy()).build(),
				// SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				String supportedProtocols = conf.getProperties().getProperty("supportedProtocols");
				if (null == supportedProtocols) {
					conf.getProperties().setProperty("supportedProtocols", supportedProtocols = "TLSv1.2,TLSv1.1,TLSv1,SSLv3,SSLv2Hello");
					isStore = true;
				}
				// new String[] { "SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.2" }
				sslsf = new SSLConnectionSocketFactory(SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
						supportedProtocols.split(","), null, NoopHostnameVerifier.INSTANCE);
				// sslsf =
				// org.apache.http.conn.ssl.SSLConnectionSocketFactory.getSocketFactory();
				// sslsf = new
				// SSLConnectionSocketFactory(SSLContexts.custom().loadTrustMaterial(null,
				// new
				// TrustSelfSignedStrategy()).build(),
				// NoopHostnameVerifier.INSTANCE);
				// sslsf = new
				// SSLConnectionSocketFactory(SSLContext.getDefault());
				Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
						.register("http", PlainConnectionSocketFactory.getSocketFactory())//
						.register("https", sslsf)//
						.build();
			}
			requestConfig = getDefaultBuilder().build();
			org.apache.http.client.config.RequestConfig.Builder builder = getDefaultBuilder();
			try {
				String[] s = conf.getProperties().getProperty("proxy").split(":");
				HttpHost proxy = new HttpHost(s[0], Integer.valueOf(s[1]), "http");// ���ô���IP���˿ڡ�Э�飨��ֱ��滻��
				builder.setProxy(proxy);// �Ѵ������õ���������
				// showMsg("����{0}", proxy);
			} catch (Exception e) {
			}
			proxyRequestConfig = builder.build();
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
				// .setSocketTimeout(timout_millisecond_socket)//
				// �����ȡ���ݵĳ�ʱʱ��(����Ӧʱ��)����λ���롣
				// �������һ���ӿڣ�����ʱ�����޷��������ݣ���ֱ�ӷ����˴ε��á�
				.setCookieSpec(CookieSpecs.IGNORE_COOKIES)//
				.setExpectContinueEnabled(true)// �ص��������֪����ɶ�õ�
		// .setStaleConnectionCheckEnabled(true)//�ص������������֮ǰУ�������Ƿ���Ч
		;
	}

	private org.apache.http.client.ServiceUnavailableRetryStrategy getServiceUnavailableRetryStrategy() {
		return new org.apache.http.client.ServiceUnavailableRetryStrategy() {
			/**
			 * retry�߼�
			 */
			@Override
			public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
				if (executionCount <= retry_times)
					return true;
				else
					return false;
			}

			/**
			 * retry���ʱ��
			 */
			@Override
			public long getRetryInterval() {
				return 1000l * retry_time_second;
			}
		};
	}

	private HttpRequestRetryHandler getHttpRequestRetryHandler() {
		return new HttpRequestRetryHandler() {
			public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
				if (exception instanceof java.net.SocketTimeoutException) {
					return false;// Timeout
				} else if (exception instanceof UnknownHostException) {
					return false;// Unknown host
				} else if (exception instanceof org.apache.http.NoHttpResponseException) {
					return false;// Unknown host
				} else if (exception instanceof javax.net.ssl.SSLHandshakeException) {
					return false;// Unknown host
				} else if (exception instanceof java.net.SocketException) {
					return false;// Unknown host
				} else if (executionCount <= 2) {
					// from
					// :https://blog.csdn.net/minicto/article/details/56677420
					try {
						Thread.sleep(1000l * executionCount);
					} catch (InterruptedException e) {
					}
					return !(HttpClientContext.adapt(context).getRequest() instanceof HttpEntityEnclosingRequest);
					// Retry if the request is considered idempotent
					// Do not retry if over max retry count
				} else if (false) {//
					if (exception instanceof InterruptedIOException) {
						return false;// Timeout
					} else if (exception instanceof ConnectTimeoutException) {
						return false;// Connection refused
					} else if (exception instanceof SSLException) {
						return false;// SSL handshake exception
					}
				}
				return false;
			}
		};
	}

	protected synchronized CloseableHttpClient getClient() {
		if (null == client) {
			// ʵ����CloseableHttpClient����

			// CloseableHttpClient httpClient
			// = HttpClients
			// .custom()
			// .setConnectionManager(connManager)
			// .setConnectionManagerShared(true)
			// .build();
			client = HttpClients.custom()//
					.setConnectionManagerShared(true)//
					.setConnectionManager(getPoolConnManager())//
					.setDefaultRequestConfig(requestConfig)//
					.setRetryHandler(getHttpRequestRetryHandler())//
					.build();
		}
		return client;
	}

	private synchronized CloseableHttpClient getProxyClient() {
		if (null == proxyClient) {
			// org.apache.http.client.config.RequestConfig.Builder builder =
			// getDefaultBuilder();
			// try {
			// String[] s =
			// conf.getProperties().getProperty("proxy").split(":");
			// HttpHost proxy = new HttpHost(s[0], Integer.valueOf(s[1]),
			// "http");//
			// ���ô���IP���˿ڡ�Э�飨��ֱ��滻��
			// builder.setProxy(proxy);// �Ѵ������õ���������
			// // showMsg("����{0}", proxy);
			// } catch (Exception e) {
			// }
			// ʵ����CloseableHttpClient����
			proxyClient = HttpClients.custom()//
					.setConnectionManagerShared(true)//
					.setConnectionManager(getPoolConnManager())//
					.setDefaultRequestConfig(proxyRequestConfig)//
					.setRetryHandler(getHttpRequestRetryHandler())//
					.build();
		}
		return proxyClient;
	}

	public void finish() {
		// try {
		// client.close();
		// } catch (Throwable e) {
		// }
		// try {
		// proxyClient.close();
		// } catch (Throwable e) {
		// }
		// client = null;
		// proxyClient = null;
		// ���ӳعر�
		// poolConnManager.close();
	}

	public static PoolingHttpClientConnectionManager getPoolConnManager() {
		if (null == poolConnManager) {
			int timout_millisecond_socket = 10000;
			int pool_max_total = 200;
			int pool_max_per_route = 20;
			try {
				boolean isStore = false;
				ConfUtil conf = ConfUtil.getDefaultConf();
				try {
					timout_millisecond_socket = Integer.valueOf(conf.getProperties().getProperty("timout_millisecond_socket"));
				} catch (Exception e) {
					conf.getProperties().setProperty("timout_millisecond_socket", String.valueOf(timout_millisecond_socket));
					isStore = true;
				}
				try {
					pool_max_total = Integer.valueOf(conf.getProperties().getProperty("pool_max_total"));
				} catch (Exception e) {
					conf.getProperties().setProperty("pool_max_total", String.valueOf(pool_max_total));
					isStore = true;
				}
				try {
					pool_max_per_route = Integer.valueOf(conf.getProperties().getProperty("pool_max_per_route"));
				} catch (Exception e) {
					conf.getProperties().setProperty("pool_max_per_route", String.valueOf(pool_max_per_route));
					isStore = true;
				}
				if (isStore)
					conf.store();
			} catch (IOException e) {
			}
			poolConnManager = new PoolingHttpClientConnectionManager(/* socketFactoryRegistry */);
			// Increase max total connection to 200
			poolConnManager.setMaxTotal(pool_max_total);
			// Increase default max connection per route to 20
			poolConnManager.setDefaultMaxPerRoute(pool_max_per_route);
			poolConnManager.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(timout_millisecond_socket).build());
		}
		return poolConnManager;
	}

	public static abstract class Executor<R> {
		private R result;

		public abstract void execute(InputStream inputStream) throws Throwable;

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

	public void execute(String uri, Executor<?> executor) throws Throwable {
		InputStream in = null;
		HttpGet get = null;
		org.apache.http.client.methods.CloseableHttpResponse response = null;
		HttpEntity entity = null;
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
						throw e1;
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
						conf.getProperties().setProperty("proxy_urls", conf.getProperties().getProperty("proxy_urls") + "," + proxy_url);
						conf.store();
						LogUtil.errLog.showMsg("ADD:	{0}", proxy_url);
					}
				} else {// ����ѽ��Ǵ���ʽ���Ǿ����B�����ˡ�
					throw e;
				}
			}
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				get.abort();
				return;
			}
			entity = response.getEntity();
			if (entity != null) {
				executor.execute(in = entity.getContent());
				// EntityUtils.consumeQuietly(entity);//�˴����ܣ�ͨ��Դ���������EntityUtils�Ƿ����HttpEntity
			}
		} catch (Throwable e) {
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
			if (entity != null) {
				EntityUtils.consumeQuietly(entity);// �˴����ܣ�ͨ��Դ���������EntityUtils�Ƿ����HttpEntity
			}
			if (null != response) {
				try {
					response.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public String getHTML(final String uri) throws Throwable {
		return getHTML(uri, conf.getProperties().getProperty("chatset"));
	}

	public synchronized String getHTML(final String uri, final String chatset) throws Throwable {
		final Executor<String> executor = new Executor<String>() {
			public void execute(InputStream inputStream) throws Throwable {
				setResult(null);
				if (true) {
					ByteArrayOutputStream result = new ByteArrayOutputStream();
					byte[] buffer = new byte[1024];
					int length;
					while ((length = inputStream.read(buffer)) != -1) {
						result.write(buffer, 0, length);
					}
					setResult(result.toString(chatset));
				} else {
					StringBuffer pageHTML = new StringBuffer();
					BufferedReader br;
					br = new BufferedReader(new InputStreamReader(inputStream, chatset));
					String line = null;
					while ((line = br.readLine()) != null) {
						pageHTML.append(line);
						pageHTML.append("\r\n");
					}
					setResult(pageHTML.toString());
				}

			}
		};
		retry(new Retry() {
			public void execute() throws Throwable {
				HttpFactory.this.execute(uri, executor);
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
				LogUtil.errLog.showMsg("Retry	{0}	{1}", count, e);
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
		// return uri.replaceAll("/" + getUrlFileName(uri), "");
		return uri.substring(0, uri.lastIndexOf("/"));
	}

	public static void main(String[] args) throws IOException {
		System.out.println(new HttpFactory().joinUrlPath("http://www.sexinsex.net/bbs/viewthread.php?tid=111&page=11", "呵呵呵"));
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
