package org.sdjen.download.cache_sis.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
//@ConfigurationProperties(prefix = "http.pool.conn")
//@EnableAutoConfiguration
//@Component
public class RestTemplateConfig {
	// https://www.cnblogs.com/javazhiyin/p/9851775.html
	// http://www.mkeeper.club/2018/09/17/SpringBoot%E5%9F%BA%E7%A1%80%E6%95%99%E7%A8%8B2-1-11%20RestTemplate%E6%95%B4%E5%90%88HttpClient/

	@Value("${http.pool.conn.maxTotal}")
	private Integer maxTotal;
	@Value("${http.pool.conn.defaultMaxPerRoute}")
	private Integer defaultMaxPerRoute;
	@Value("${http.pool.conn.connectTimeout}")
	private Integer connectTimeout;
	@Value("${http.pool.conn.connectionRequestTimeout}")
	private Integer connectionRequestTimeout;
	@Value("${http.pool.conn.socketTimeout}")
	private Integer socketTimeout;
	@Value("${http.pool.conn.validateAfterInactivity}")
	private Integer validateAfterInactivity;

	@Bean(name = "proxyRestTemplate")
	public RestTemplate proxyRestTemplate() {
		System.out.println(">>>>>>>>>>>>>>>>>>proxyRestTemplate");
		return new RestTemplate(proxyHttpRequestFactory());
	}

	@Bean(name = "restTemplate")
	public RestTemplate restTemplate() {
		System.out.println(">>>>>>>>>>>>>>>>>>restTemplate");
		return new RestTemplate(httpRequestFactory());
	}

	@Bean
	public ClientHttpRequestFactory httpRequestFactory() {
		return new HttpComponentsClientHttpRequestFactory(httpClient());
	}

	@Bean
	public ClientHttpRequestFactory proxyHttpRequestFactory() {
		return new HttpComponentsClientHttpRequestFactory(proxyHttpClient());
	}

	@Bean
	public HttpClient httpClient() {
		System.out.println(">>>>>>>>>>>>>>>>>>httpClient");
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", SSLConnectionSocketFactory.getSocketFactory()).build();
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
		connectionManager.setMaxTotal(maxTotal);
		connectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
		connectionManager.setValidateAfterInactivity(validateAfterInactivity);
		RequestConfig requestConfig = RequestConfig.custom()
				// 服务器返回数据(response)的时间，超过抛出read timeout
				.setSocketTimeout(socketTimeout)
				// 连接上服务器(握手成功)的时间，超出抛出connect timeout
				.setConnectTimeout(connectTimeout)
				// 从连接池中获取连接的超时时间//超时间未拿到可用连接，会抛出org.apache.http.conn.ConnectionPoolTimeoutException:
				// Timeout waiting for connection from pool
				.setConnectionRequestTimeout(connectionRequestTimeout).build();
		return HttpClientBuilder//
				.create()//
				.setConnectionManager(connectionManager)//
				.setDefaultRequestConfig(requestConfig)//
				.setRetryHandler(getHttpRequestRetryHandler())//
				.build();
	}

	@Bean
	public HttpClient proxyHttpClient() {
		System.out.println(">>>>>>>>>>>>>>>>>>proxyHttpClient");
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", SSLConnectionSocketFactory.getSocketFactory()).build();
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
		connectionManager.setMaxTotal(maxTotal);
		connectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
		connectionManager.setValidateAfterInactivity(validateAfterInactivity);
		RequestConfig requestConfig = RequestConfig.custom()
				// 服务器返回数据(response)的时间，超过抛出read timeout
				.setSocketTimeout(socketTimeout)
				// 连接上服务器(握手成功)的时间，超出抛出connect timeout
				.setConnectTimeout(connectTimeout)
				// 从连接池中获取连接的超时时间//超时间未拿到可用连接，会抛出org.apache.http.conn.ConnectionPoolTimeoutException:
				// Timeout waiting for connection from pool
				.setConnectionRequestTimeout(connectionRequestTimeout)//
				.setProxy(new HttpHost("10.3.5.6", 1080, "http"))//
				.build();
		return HttpClientBuilder//
				.create()//
				.setConnectionManager(connectionManager)//
				.setDefaultRequestConfig(requestConfig)//
				.setRetryHandler(getHttpRequestRetryHandler())//
				.build();
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
}
