package org.sdjen.download.cache_sis.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public abstract class CopyExecutor<T> {
	protected Map<Object, Object> logMsg;
	private List<Future<T>> futures;
	private ThreadPoolTaskExecutor executor;

	public void copy(T from, ThreadPoolTaskExecutor executor) {
		logMsg = new HashMap<Object, Object>();
		this.executor = executor;
		T listResult = from;
		try {
			do {
				listResult = list(from = listResult);
			} while (!isEnd(listResult, from));
		} catch (Exception e) {
			e.printStackTrace();
		}
		exeFutures();
	}

	private List<Future<T>> getFutures() {
		return null == futures ? futures = new ArrayList<>() : futures;
	}

	public abstract void log();

	public abstract boolean isEnd(T rst, T from);

	public abstract Map<String, Object> getListDetail(T from) throws Exception;

	public abstract T getKey(Map<?, ?> map);

	public abstract T getMaxKey(T t1, T t2);

	public abstract T single(Map<?, ?> detail) throws Exception;

	private T list(T from) throws Exception {
		T result = from;
		long tl = System.currentTimeMillis();
		Map<String, Object> detail = getListDetail(from);
		tl = System.currentTimeMillis() - tl;
		long te = System.currentTimeMillis();
		exeFutures();
		te = System.currentTimeMillis() - te;
		logMsg.put("time_exe", te);
		logMsg.put("total", detail.get("total"));
		log();
		logMsg.put("from", from);
		logMsg.put("time_lookup", tl);
		List<Map> list = (List<Map>) detail.get("list");
		for (Map<?, ?> _source : list) {
			result = getMaxKey(getKey(_source), result);
			getFutures().add(executor.submit(new Callable<T>() {
				public T call() throws Exception {
					try {
						return single(_source);
					} catch (Exception e) {
						throw e;
					}
				}
			}));
		}
		return result;
	}

	private T exeFutures() {
		T result = null;
		for (Future<T> fs : getFutures()) {
			try {
				result = getMaxKey(fs.get(30, TimeUnit.MINUTES), result);
			} catch (java.util.concurrent.TimeoutException e) {
				fs.cancel(false);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
		}
		System.out.println("F:	" + result);
		futures = null;
		return result;
	}
}
