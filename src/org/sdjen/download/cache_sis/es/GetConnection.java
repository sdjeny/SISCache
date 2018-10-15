package org.sdjen.download.cache_sis.es;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * ClassName:GetConnection Date: 2018年3月20日 下午8:12:07
 * 
 * @author xbq
 * @version
 * @since JDK 1.8
 */
public class GetConnection {
	private final static Logger logger = Logger.getLogger(GetConnection.class.toString());

	/**
	 * post请求 json参数
	 *
	 * @param url
	 * @param bodyJsonParams
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public static String doPost(String url, String bodyJsonParams, Map<String, String> headers) throws IOException {
		HttpPost httpPost = new HttpPost(url);
//		httpPost.addHeader("Content-Type", "application/json");
		httpPost.setEntity(new StringEntity(bodyJsonParams, Charset.forName("UTF-8")));

		addHeader(httpPost, headers);
		return execute(httpPost);
	}

	/**
	 * post k-v参数
	 *
	 * @param url
	 * @param params
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public static String doPost(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		if (params != null && params.keySet().isEmpty()) {
			httpPost.setEntity(getUrlEncodedFormEntity(params));
		}
		addHeader(httpPost, headers);
		return execute(httpPost);
	}

	/**
	 * patch json参数
	 *
	 * @param url
	 * @param bodyJsonParams
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public static String doPatch(String url, String bodyJsonParams, Map<String, String> headers) throws IOException {
		HttpPatch httpPatch = new HttpPatch(url);
		httpPatch.setEntity(new StringEntity(bodyJsonParams));
		addHeader(httpPatch, headers);
		return execute(httpPatch);
	}

	/**
	 * patch k-v参数
	 *
	 * @param url
	 * @param params
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public static String doPatch(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
		HttpPatch httpPatch = new HttpPatch(url);
		if (params != null && !params.isEmpty()) {
			httpPatch.setEntity(getUrlEncodedFormEntity(params));
		}
		addHeader(httpPatch, headers);
		return execute(httpPatch);
	}

	/**
	 * PUT JSON参数
	 *
	 * @param url
	 * @param bodyJsonParams
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public static String doPut(String url, String bodyJsonParams, Map<String, String> headers) throws IOException {
		HttpPut httpPut = new HttpPut(url);
		httpPut.addHeader("Content-Type", "application/json");
		httpPut.setEntity(new StringEntity(bodyJsonParams, Charset.forName("UTF-8")));

		addHeader(httpPut, headers);
		return execute(httpPut);
	}

	/**
	 * put k-v参数
	 *
	 * @param url
	 * @param params
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public static String doPut(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
		HttpPut httpPut = new HttpPut(url);
		if (params != null && params.keySet().isEmpty()) {
			httpPut.setEntity(getUrlEncodedFormEntity(params));
		}
		addHeader(httpPut, headers);
		return execute(httpPut);
	}

	/**
	 * Delete json 参数
	 *
	 * @param url
	 * @param bodyJsonParams
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public static String doDeletedoPut(String url, String bodyJsonParams, Map<String, String> headers) throws IOException {
		HttpDeleteWithEntity httpDelete = new HttpDeleteWithEntity(url);
		httpDelete.setEntity(new StringEntity(bodyJsonParams));
		addHeader(httpDelete, headers);
		return execute(httpDelete);
	}

	/**
	 * delete k-v参数
	 *
	 * @param url
	 * @param params
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public static String doDelete(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
		HttpDeleteWithEntity httpDelete = new HttpDeleteWithEntity(url);
		addHeader(httpDelete, headers);
		if (params != null && !params.isEmpty()) {
			httpDelete.setEntity(getUrlEncodedFormEntity(params));
		}
		return execute(httpDelete);
	}

	/**
	 * options json参数
	 *
	 * @param url
	 * @param bodyJsonParams
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public static String doOptions(String url, String bodyJsonParams, Map<String, String> headers) throws IOException {
		HttpOptionsWithEntity httpOptions = new HttpOptionsWithEntity(url);
		addHeader(httpOptions, headers);
		httpOptions.setEntity(new StringEntity(bodyJsonParams));
		return execute(httpOptions);
	}

	/**
	 * options k-v参数
	 *
	 * @param url
	 * @param params
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public static String doOptions(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
		HttpOptionsWithEntity httpOptions = new HttpOptionsWithEntity(url);
		addHeader(httpOptions, headers);
		if (params != null && !params.isEmpty()) {
			httpOptions.setEntity(getUrlEncodedFormEntity(params));
		}
		return execute(httpOptions);
	}

	/**
	 * head请求
	 *
	 * @param url
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public static String doHeader(String url, Map<String, String> headers) throws IOException {
		HttpHead httpHead = new HttpHead(url);
		addHeader(httpHead, headers);
		return execute(httpHead);

	}

	/**
	 * get请求
	 *
	 * @param url
	 * @param params
	 * @param headers
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public static String doGet(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
		// 参数
		StringBuilder paramsBuilder = new StringBuilder(url);

		if (params != null && params.keySet().isEmpty()) {
			if (url.indexOf("?") == -1) {
				paramsBuilder.append("?");
			}
			List<NameValuePair> list = new ArrayList<>();

			Set<String> keySet = headers.keySet();
			Iterator<String> iterator = keySet.iterator();
			while (iterator.hasNext()) {
				String key = iterator.next();
				String value = headers.get(key);
				list.add(new BasicNameValuePair(key, value));
			}
			String paramsStr = EntityUtils.toString(new UrlEncodedFormEntity(list));
			paramsBuilder.append(paramsStr);
		}
		HttpGet httpGet = new HttpGet(paramsBuilder.toString());
		// 头
		addHeader(httpGet, headers);
		return execute(httpGet);
	}

	/**
	 * 执行请求并返回string值
	 *
	 * @param httpUriRequest
	 * @return
	 * @throws IOException
	 */
	private static String execute(HttpUriRequest httpUriRequest) throws IOException {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			CloseableHttpResponse response = httpClient.execute(httpUriRequest);

			// if (response.getStatusLine().getStatusCode() == 200) {// 请求成功状态
			// try (BufferedReader bufferedReader = new BufferedReader(
			// new InputStreamReader(response.getEntity().getContent()))) {
			// StringBuilder sb = new StringBuilder();
			// String tmp;
			// while ((tmp = bufferedReader.readLine()) != null) {
			// sb.append(tmp);
			// }
			// return sb.toString();
			// }
			// }
			Header type = response.getEntity().getContentType();
			logger.log(Level.INFO, "Type:" + type.getValue());
			String defaultCharset = "UTF-8";
			String charset = getCharSet(type.getValue());
			if (null != charset && charset.isEmpty()) {
				defaultCharset = charset;
			}
			return EntityUtils.toString(response.getEntity(), defaultCharset);
		}
		// return null;
	}

	/**
	 * 添加请求头部
	 *
	 * @param httpUriRequest
	 * @param headers
	 */
	private static void addHeader(HttpUriRequest httpUriRequest, Map<String, String> headers) {
		if (httpUriRequest != null) {
			if (headers != null && !headers.keySet().isEmpty()) {
				Set<String> keySet = headers.keySet();
				Iterator<String> iterator = keySet.iterator();
				while (iterator.hasNext()) {
					String key = iterator.next();
					String value = headers.get(key);
					httpUriRequest.addHeader(key, value);
				}
			}
		}
	}

	/**
	 * 获取 UrlEncodedFormEntity 参数实体
	 *
	 * @param params
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private static UrlEncodedFormEntity getUrlEncodedFormEntity(Map<String, String> params) throws UnsupportedEncodingException {
		if (params != null && params.keySet().isEmpty()) {
			List<NameValuePair> list = new ArrayList<>();

			Set<String> keySet = params.keySet();
			Iterator<String> iterator = keySet.iterator();
			while (iterator.hasNext()) {
				String key = iterator.next();
				String value = params.get(key);
				list.add(new BasicNameValuePair(key, value));
			}
			return new UrlEncodedFormEntity(list);
		}
		return null;
	}

	/**
	 * 根据HTTP 响应头部的content type抓取响应的字符集编码
	 *
	 * @param content
	 * @return
	 */
	private static String getCharSet(String content) {
		String regex = ".*charset=([^;]*).*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(content);
		if (matcher.find())
			return matcher.group(1);
		else
			return null;
	}

	/**
	 * 解决httpclient 的DELETE默认不支持setEntity
	 */
	static class HttpDeleteWithEntity extends HttpEntityEnclosingRequestBase {
		public static final String METHOD_NAME = "DELETE";

		@Override
		public String getMethod() {
			return METHOD_NAME;
		}

		public HttpDeleteWithEntity(final String uri) {
			super();
			setURI(URI.create(uri));
		}

		public HttpDeleteWithEntity(final URI uri) {
			super();
			setURI(uri);
		}

		public HttpDeleteWithEntity() {
			super();
		}
	}

	/**
	 * 解决httpclient 的OPTIONS默认不支持setEntity
	 */
	static class HttpOptionsWithEntity extends HttpEntityEnclosingRequestBase {
		public static final String METHOD_NAME = "OPTIONS";

		@Override
		public String getMethod() {
			return METHOD_NAME;
		}

		public HttpOptionsWithEntity() {
			super();
		}

		public HttpOptionsWithEntity(final String uri) {
			super();
			setURI(URI.create(uri));
		}

		public HttpOptionsWithEntity(final URI uri) {
			super();
			setURI(uri);
		}
	}

	public static void main(String[] args) throws IOException {
		
		// Map<String, String >headers = new HashMap<>();
		// "Content-Type", "application/json"
//		String rep = doPost("http://127.0.0.1:9200/_search?pretty", new HashMap<>(), Collections.singletonMap("Content-Type", "application/json"));
		String rep = doPost("http://192.168.0.237:9200/test/a", "{\"k\":\"v\"}", Collections.singletonMap("Content-Type", "application/json"));
		logger.log(Level.INFO, rep);
	}
}
