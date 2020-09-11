package org.sdjen.download.cache_sis.http;

import java.io.InputStream;

public interface HttpUtil {

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

	void execute(String uri, Executor<?> executor) throws Throwable;

	String getHTML(String uri) throws Throwable;

	String getHTML(String uri, String chatset) throws Throwable;
}
