import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.net.ssl.HostnameVerifier;

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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

public class DDD {
	public static void main(String[] args) throws Exception {
		PoolingHttpClientConnectionManager poolConnManager = null;
		SSLConnectionSocketFactory sslsf;
		String supportedProtocols = "TLSv1,TLSv1.2,TLSv1.1,SSLv3,SSLv2Hello";
		HostnameVerifier hostnameVerifier = //
		        // new org.apache.http.conn.ssl.DefaultHostnameVerifier()//
		        org.apache.http.conn.ssl.NoopHostnameVerifier.INSTANCE//
		;
		sslsf = new SSLConnectionSocketFactory(SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build(),
		        supportedProtocols.split(","), null, hostnameVerifier);
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
		        .register("http", PlainConnectionSocketFactory.getSocketFactory())//
		        .register("https", sslsf)//
		        .build();
		poolConnManager = new PoolingHttpClientConnectionManager(/*socketFactoryRegistry*/);
		// Increase max total connection to 200
		poolConnManager.setMaxTotal(200);
		// Increase default max connection per route to 20
		poolConnManager.setDefaultMaxPerRoute(20);
		poolConnManager.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(10000).build());
		org.apache.http.impl.client.CloseableHttpClient client = HttpClients.custom()//
		        .setConnectionManager(poolConnManager)//
		        .setDefaultRequestConfig(RequestConfig.custom()//
		                .setConnectTimeout(10000)// 设置连接超时时间，单位毫秒。
		                .setConnectionRequestTimeout(10000) // 设置从connectManager(连接池)获取Connection超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。
		                .setSocketTimeout(10000)// 请求获取数据的超时时间(即响应时间)，单位毫秒。如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。
		                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)//
//		                .setProxy(new HttpHost("127.0.0.1", 9666, "http"))// 把代理设置到请求配置
		                .build())//
		        .build();
		InputStream in = null;
		HttpGet get = null;
		org.apache.http.client.methods.CloseableHttpResponse response = null;
		String url = //
//		         "https://www.caribbeancom.com/moviepages/022712-953/images/l_l.jpg"//
		        "https://www1.wi.to/2018/03/29/87188c533dce9cfaa1e34992c693a5d5.jpg"//
		;
		get = new HttpGet(url);
		try {// 先不要代理玩一次
			response = client.execute(get);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				try {
					in = entity.getContent();
					FileOutputStream fos = new FileOutputStream("33");
					byte[] buffer = new byte[1024];
					int len = 0;
					while ((len = in.read(buffer)) != -1) {
						fos.write(buffer, 0, len);
					}
					try {
						fos.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (IOException e) {
					try {
						in.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		} catch (IOException e) {// 不行就使用代理再玩一次
			e.printStackTrace();
			get.abort();
		}
		if (null != response)
			try {
				response.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		if (null != client)
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		if (null != poolConnManager)
			try {
				poolConnManager.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
}
