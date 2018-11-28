package org.sdjen.download.cache_sis.http;

import java.io.IOException;

public class DefaultCss {
	static String css = null;
	static int len = -1;

	public synchronized static String getCss() throws IOException {
		if (null == css) {
			java.io.InputStream inputStream = DefaultCss.class.getClassLoader().getResourceAsStream("default.css");
			byte[] bytes = new byte[inputStream.available()];
			inputStream.read(bytes);
			css = new String(bytes, "GBK");
		}
		return css;
	}

	public synchronized static int getLength() throws IOException {
		if (-1 == len) {
			len = getCss().getBytes().length;
		}
		return len;
	}
}
