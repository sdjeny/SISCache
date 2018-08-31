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
				System.out.println("��ʼ��" + url);
				try {
					Thread.sleep((long) (Math.random() * 1000000 % 1000));
				} catch (InterruptedException e) {
				}
				System.out.println("	�յ���" + url);
				return url;
			}

			private void dueHtml(String h) {
				w.lock();
				html = h;
				System.out.println("		����" + html);
				try {
					Thread.sleep((long) (Math.random() * 1000000 % 5000));
				} catch (InterruptedException e) {
				}
				System.out.println("			��ɣ�" + html);
				w.unlock();
			}

			private void execute(int i) {
				dueHtml(getHtml(i + ""));
			}
		}
		final A a = new A();// �ȴ������ʱ������
		int size = 20;
		final CountDownLatch latch = new CountDownLatch(size);
		ExecutorService executor = Executors.newFixedThreadPool(6);
		List<Future<String>> resultList = new ArrayList<Future<String>>();
		// ����10������ִ��
		for (int i = 0; i < size; i++) {
			// ʹ��ExecutorServiceִ��Callable���͵����񣬲������������future������
			final int d = i;
			Future<String> future = executor.submit(new Callable<String>() {
				public String call() throws Exception {
					a.execute(d);
					latch.countDown();
					return d + "";
				}
			});
			resultList.add(future);// ������ִ�н���洢��List��
		} // ��������Ľ��
		  // ����һ��˳��رգ�ִ����ǰ�ύ�����񣬵�������������
		executor.shutdown();
		if (true) {
			for (Future<String> fs : resultList) {
				try {
					// while (!fs.isDone())
					// ;// Future�������û����ɣ���һֱѭ���ȴ���ֱ��Future�������
					System.out.println(fs.get(2, TimeUnit.SECONDS)); // ��ӡ�����̣߳�����ִ�еĽ��
				} catch (java.util.concurrent.TimeoutException e) {
					fs.cancel(true);
					System.out.println("ͣ��");
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
				}
			}
		} else if (false) {
			// ���̳߳��е���������ִ�����ʱ,�ͻ�ر��̳߳�
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
