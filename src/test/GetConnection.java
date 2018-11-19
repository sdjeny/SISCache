package test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
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
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.sdjen.download.cache_sis.http.HttpFactory;

/**
 * 
 * 
 * @author xbq
 * @version
 * @since JDK 1.8
 */
public class GetConnection extends HttpFactory {
	private static GetConnection connection;

	public static synchronized GetConnection getConnection() throws IOException {
		if (null == connection)
			connection = new GetConnection();
		return connection;
	}

	public GetConnection() throws IOException {
		super();
	}

	static Log logger = LogFactory.getLog(GetConnection.class.getClass());

	/**
	 * 
	 *
	 * @param url
	 * @param bodyJsonParams
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public String doPost(String url, String bodyJsonParams, Map<String, String> headers) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		httpPost.addHeader("Content-Type", "application/json");
		httpPost.setEntity(new StringEntity(bodyJsonParams, Charset.forName("UTF-8")));

		addHeader(httpPost, headers);
		return execute(httpPost);
	}

	/**
	 * 
	 * @param url
	 * @param params
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public String doPost(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
		HttpPost httpPost = new HttpPost(url);
		if (params != null && params.keySet().isEmpty()) {
			httpPost.setEntity(getUrlEncodedFormEntity(params));
		}
		addHeader(httpPost, headers);
		return execute(httpPost);
	}

	/**
	 * 
	 * @param url
	 * @param bodyJsonParams
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public String doPatch(String url, String bodyJsonParams, Map<String, String> headers) throws IOException {
		HttpPatch httpPatch = new HttpPatch(url);
		httpPatch.setEntity(new StringEntity(bodyJsonParams));
		addHeader(httpPatch, headers);
		return execute(httpPatch);
	}

	/**
	 * 
	 * @param url
	 * @param params
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public String doPatch(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
		HttpPatch httpPatch = new HttpPatch(url);
		if (params != null && !params.isEmpty()) {
			httpPatch.setEntity(getUrlEncodedFormEntity(params));
		}
		addHeader(httpPatch, headers);
		return execute(httpPatch);
	}

	@Override
	public void finish() {
		connection = null;
		super.finish();
	}

	/**
	 * 
	 * @param url
	 * @param bodyJsonParams
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public String doPut(String url, String bodyJsonParams, Map<String, String> headers) throws IOException {
		HttpPut httpPut = new HttpPut(url);
		httpPut.addHeader("Content-Type", "application/json");
		httpPut.setEntity(new StringEntity(bodyJsonParams, Charset.forName("UTF-8")));

		addHeader(httpPut, headers);
		return execute(httpPut);
	}

	/**
	 * 
	 * @param url
	 * @param params
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public String doPut(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
		HttpPut httpPut = new HttpPut(url);
		if (params != null && params.keySet().isEmpty()) {
			httpPut.setEntity(getUrlEncodedFormEntity(params));
		}
		addHeader(httpPut, headers);
		return execute(httpPut);
	}

	/**
	 * Delete json ����
	 *
	 * @param url
	 * @param bodyJsonParams
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public String doDeletedoPut(String url, String bodyJsonParams, Map<String, String> headers) throws IOException {
		HttpDeleteWithEntity httpDelete = new HttpDeleteWithEntity(url);
		httpDelete.setEntity(new StringEntity(bodyJsonParams));
		addHeader(httpDelete, headers);
		return execute(httpDelete);
	}

	/**
	 * 
	 * @param url
	 * @param params
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public String doDelete(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
		HttpDeleteWithEntity httpDelete = new HttpDeleteWithEntity(url);
		addHeader(httpDelete, headers);
		if (params != null && !params.isEmpty()) {
			httpDelete.setEntity(getUrlEncodedFormEntity(params));
		}
		return execute(httpDelete);
	}

	/**
	 * options json����
	 *
	 * @param url
	 * @param bodyJsonParams
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public String doOptions(String url, String bodyJsonParams, Map<String, String> headers) throws IOException {
		HttpOptionsWithEntity httpOptions = new HttpOptionsWithEntity(url);
		addHeader(httpOptions, headers);
		httpOptions.setEntity(new StringEntity(bodyJsonParams));
		return execute(httpOptions);
	}

	/**
	 *
	 * @param url
	 * @param params
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public String doOptions(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
		HttpOptionsWithEntity httpOptions = new HttpOptionsWithEntity(url);
		addHeader(httpOptions, headers);
		if (params != null && !params.isEmpty()) {
			httpOptions.setEntity(getUrlEncodedFormEntity(params));
		}
		return execute(httpOptions);
	}

	/**
	 * 
	 * @param url
	 * @param headers
	 * @return
	 * @throws IOException
	 */
	public String doHeader(String url, Map<String, String> headers) throws IOException {
		HttpHead httpHead = new HttpHead(url);
		addHeader(httpHead, headers);
		return execute(httpHead);

	}

	/**
	 * 
	 * @param url
	 * @param params
	 * @param headers
	 * @return
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public String doGet(String url, Map<String, String> params, Map<String, String> headers) throws IOException {

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
		// ͷ
		addHeader(httpGet, headers);
		return execute(httpGet);
	}

	/**
	 *
	 * @param httpUriRequest
	 * @return
	 * @throws IOException
	 */
	private String execute(HttpUriRequest httpUriRequest) throws IOException {
		try (CloseableHttpClient httpClient = getClient()) {
			if (false) {
				CloseableHttpResponse response = httpClient.execute(httpUriRequest);

				// if (response.getStatusLine().getStatusCode() == 200) {//
				// ����ɹ�״̬
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
				logger.debug("Type:" + type.getValue());
				String defaultCharset = "UTF-8";
				String charset = getCharSet(type.getValue());
				if (null != charset && charset.isEmpty()) {
					defaultCharset = charset;
				}
				return EntityUtils.toString(response.getEntity(), defaultCharset);
			} else {
				InputStream in = null;
				org.apache.http.client.methods.CloseableHttpResponse response = null;
				HttpEntity entity = null;
				try {
					try {
						response = getClient().execute(httpUriRequest);
					} catch (IOException e) {// ���о�ʹ�ô�������һ��
						httpUriRequest.abort();
						throw e;
					}
					// if (response.getStatusLine().getStatusCode() !=
					// HttpStatus.SC_OK) {
					// httpUriRequest.abort();
					// }
					entity = response.getEntity();
					if (entity != null) {
						in = entity.getContent();
						ByteArrayOutputStream result = new ByteArrayOutputStream();
						byte[] buffer = new byte[1024];
						int length;
						while ((length = in.read(buffer)) != -1) {
							result.write(buffer, 0, length);
						}
						Header type = entity.getContentType();
						logger.debug("Type:" + type.getValue());
						String defaultCharset = "UTF-8";
						String charset = getCharSet(type.getValue());
						if (null != charset && charset.isEmpty()) {
							defaultCharset = charset;
						}
						return result.toString(defaultCharset);
					}
					return null;
				} catch (Exception e) {
					if (null != httpUriRequest)
						httpUriRequest.abort();
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
		}
		// return null;
	}

	/**
	 * 
	 * @param httpUriRequest
	 * @param headers
	 */
	private void addHeader(HttpUriRequest httpUriRequest, Map<String, String> headers) {
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
	 * 
	 * @param params
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private UrlEncodedFormEntity getUrlEncodedFormEntity(Map<String, String> params) throws UnsupportedEncodingException {
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
	 * 
	 * @param content
	 * @return
	 */
	private String getCharSet(String content) {
		String regex = ".*charset=([^;]*).*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(content);
		if (matcher.find())
			return matcher.group(1);
		else
			return null;
	}

	/**
	 * 
	 */
	class HttpDeleteWithEntity extends HttpEntityEnclosingRequestBase {
		public final String METHOD_NAME = "DELETE";

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
	 * 
	 */
	class HttpOptionsWithEntity extends HttpEntityEnclosingRequestBase {
		public final String METHOD_NAME = "OPTIONS";

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
		// String rep = doPost("http://127.0.0.1:9200/_search?pretty", new
		// HashMap<>(), Collections.singletonMap("Content-Type",
		// "application/json"));
		GetConnection connection = new GetConnection();
		String rep;
		// rep= connection.doDelete("http://192.168.0.237:9200/test_md/", new
		// HashMap<>(), new HashMap<>());
		// logger.info(rep);
		rep = connection.doDelete("http://192.168.0.237:9200/test_html/", new HashMap<>(), new HashMap<>());
		logger.info(rep);
		connection.finish();
	}
}
