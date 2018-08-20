package org.sdjen.download.cache_sis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TestTheadPool {
	public static void main(String[] args) {
		class A {
			private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
			private Lock r = rwl.readLock();
			private Lock w = rwl.writeLock();
			private String html = "";

			private synchronized String getHtml(String url) {
				System.out.println("开始：" + url);
				try {
					Thread.sleep((long) (Math.random() * 1000000 % 1000));
				} catch (InterruptedException e) {
				}
				System.out.println("	收到：" + url);
				return url;
			}

			private void dueHtml(String h) {
				w.lock();
				html = h;
				System.out.println("		处理：" + html);
				try {
					Thread.sleep((long) (Math.random() * 1000000 % 5000));
				} catch (InterruptedException e) {
				}
				System.out.println("			完成：" + html);
				w.unlock();
			}

			private void execute(int i) {
				dueHtml(getHtml(i + ""));
			}
		}
		final A a = new A();// 等待命令倒计时计数器
		int size = 20;
		final CountDownLatch latch = new CountDownLatch(size);
		ExecutorService executor = Executors.newFixedThreadPool(6);
		List<Future<String>> resultList = new ArrayList<Future<String>>();
		// 创建10个任务并执行
		for (int i = 0; i < size; i++) {
			// 使用ExecutorService执行Callable类型的任务，并将结果保存在future变量中
			final int d = i;
			Future<String> future = executor.submit(new Callable<String>() {
				public String call() throws Exception {
					a.execute(d);
					latch.countDown();
					return d + "";
				}
			});
			resultList.add(future);// 将任务执行结果存储到List中
		} // 遍历任务的结果
		  // 启动一次顺序关闭，执行以前提交的任务，但不接受新任务
		executor.shutdown();
		if (true) {
			for (Future<String> fs : resultList) {
				try {
					// while (!fs.isDone())
					// ;// Future返回如果没有完成，则一直循环等待，直到Future返回完成
					System.out.println(fs.get(2, TimeUnit.SECONDS)); // 打印各个线程（任务）执行的结果
				} catch (java.util.concurrent.TimeoutException e) {
					fs.cancel(true);
					System.out.println("停滞");
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
				}
			}
		} else if (false) {
			// 当线程池中的所有任务执行完毕时,就会关闭线程池
			try {
				while (!executor.awaitTermination(2, TimeUnit.SECONDS))
					;
			} catch (InterruptedException e) {
			}
		} else {
			try {
				latch.await(2 * size, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
			}
		}
		System.out.println("finish");
	}
}
