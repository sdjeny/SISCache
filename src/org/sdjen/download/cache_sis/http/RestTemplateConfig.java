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
import org.apache.http.conn.HttpClientConnectionManager;
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

	@Value("${http.pool.conn.proxyHost}")
	private String proxyHost;
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

//    /**
//     * 客户端请求链接策略
//     * 
//     * @return
//     */
//    @Bean
//    public ClientHttpRequestFactory clientHttpRequestFactory() {
//        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
//        clientHttpRequestFactory.setHttpClient(httpClientBuilder().build());
//        clientHttpRequestFactory.setConnectTimeout(6000); // 连接超时时间/毫秒
//        clientHttpRequestFactory.setReadTimeout(6000); // 读写超时时间/毫秒
//        clientHttpRequestFactory.setConnectionRequestTimeout(5000);// 请求超时时间/毫秒
//        return clientHttpRequestFactory;
//    }
//
//    /**
//     * 设置HTTP连接管理器,连接池相关配置管理
//     * 
//     * @return 客户端链接管理器
//     */
//    @Bean
//    public HttpClientBuilder httpClientBuilder() {
//        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
//        httpClientBuilder.setConnectionManager(poolingConnectionManager());
//        return httpClientBuilder;
//    }
//
//    /**
//     * 链接线程池管理,可以keep-alive不断开链接请求,这样速度会更快 MaxTotal 连接池最大连接数 DefaultMaxPerRoute
//     * 每个主机的并发 ValidateAfterInactivity
//     * 可用空闲连接过期时间,重用空闲连接时会先检查是否空闲时间超过这个时间，如果超过，释放socket重新建立
//     * 
//     * @return
//     */
//    @Bean
//    public HttpClientConnectionManager poolingConnectionManager() {
//        PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager();
//        poolingConnectionManager.setMaxTotal(1000);
//        poolingConnectionManager.setDefaultMaxPerRoute(5000);
//        poolingConnectionManager.setValidateAfterInactivity(30000);
//        return poolingConnectionManager;
//    }

	@Bean
	public ClientHttpRequestFactory httpRequestFactory() {
		return new HttpComponentsClientHttpRequestFactory(httpClient());
	}

	@Bean
	public ClientHttpRequestFactory proxyHttpRequestFactory() {
		return new HttpComponentsClientHttpRequestFactory(proxyHttpClient());
	}

	@Bean
	public HttpClientConnectionManager connectionManager() {
		System.out.println(">>>>>>>>>>>>>>>>>>connectionManager");
		Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", SSLConnectionSocketFactory.getSocketFactory()).build();
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
		connectionManager.setMaxTotal(maxTotal);
		connectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
		connectionManager.setValidateAfterInactivity(validateAfterInactivity);
		return connectionManager;
	}

	@Bean
	public HttpClient httpClient() {
		System.out.println(">>>>>>>>>>>>>>>>>>httpClient");
		RequestConfig requestConfig = RequestConfig.custom()//
				.setSocketTimeout(socketTimeout)// 服务器返回数据(response)的时间，超过抛出read timeout
				.setConnectTimeout(connectTimeout)// 连接上服务器(握手成功)的时间，超出抛出connect timeout
				.setConnectionRequestTimeout(connectionRequestTimeout)// 从连接池中获取连接的超时时间//超时间未拿到可用连接，会抛出org.apache.http.conn.ConnectionPoolTimeoutException:Timeout
																		// waiting for connection from pool
				.build();
		return HttpClientBuilder//
				.create()//
				.setConnectionManager(connectionManager())//
				.setDefaultRequestConfig(requestConfig)//
				.setRetryHandler(getHttpRequestRetryHandler())//
				.build();
	}

	@Bean
	public HttpClient proxyHttpClient() {
		System.out.println(">>>>>>>>>>>>>>>>>>proxyHttpClient");
		String[] host = this.proxyHost.split("://");
		String[] proxy = host[1].split(":");
		RequestConfig requestConfig = RequestConfig.custom()//
				.setSocketTimeout(socketTimeout)// 服务器返回数据(response)的时间，超过抛出read timeout
				.setConnectTimeout(connectTimeout)// 连接上服务器(握手成功)的时间，超出抛出connect timeout
				.setConnectionRequestTimeout(connectionRequestTimeout)// 从连接池中获取连接的超时时间//超时间未拿到可用连接，会抛出org.apache.http.conn.ConnectionPoolTimeoutException:Timeout
																		// waiting for connection from pool
				.setProxy(new HttpHost(proxy[0], Integer.valueOf(proxy[1]), host[0]))//
				.build();
		return HttpClientBuilder//
				.create()//
				.setConnectionManager(connectionManager())//
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
