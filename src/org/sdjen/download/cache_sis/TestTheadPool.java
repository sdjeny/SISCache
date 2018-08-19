package org.sdjen.download.cache_sis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
		final A a = new A();
		ExecutorService executorService = Executors.newFixedThreadPool(6);
		List<Future<String>> resultList = new ArrayList<Future<String>>();
		// ����10������ִ��
		for (int i = 0; i < 50; i++) {
			// ʹ��ExecutorServiceִ��Callable���͵����񣬲������������future������
			final int d = i;
			Future<String> future = executorService.submit(new Callable<String>() {

				public String call() throws Exception {
					a.execute(d);
					return null;
				}
			});
			// ������ִ�н���洢��List��
			resultList.add(future);
		} // ��������Ľ��
		for (Future<String> fs : resultList) {
			try {
				while (!fs.isDone())
					;// Future�������û����ɣ���һֱѭ���ȴ���ֱ��Future�������
				// System.out.println(fs.get()); // ��ӡ�����̣߳�����ִ�еĽ��
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				// ����һ��˳��رգ�ִ����ǰ�ύ�����񣬵�������������
				executorService.shutdown();
			}
		}
	}

}
