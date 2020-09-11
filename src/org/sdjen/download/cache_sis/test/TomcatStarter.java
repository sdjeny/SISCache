package org.sdjen.download.cache_sis.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.startup.Tomcat;

public class TomcatStarter {
	public static class MessageServlet extends javax.servlet.http.HttpServlet {
		@Override
		protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			req.setCharacterEncoding("utf-8");
			resp.setCharacterEncoding("utf-8");
			PrintWriter out = resp.getWriter();
			out.flush();
			out.close();
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			doPost(req, resp);
		}
	}

	// https://www.cnblogs.com/lmq-1048498039/p/8329481.html
	// https://www.cnblogs.com/mahuan2/p/6733566.html
	public static void main(String[] args) throws LifecycleException {
		// ����Servlet·����http://localhost:8080/myapp/myServlet
		// ����html·����http://localhost:8080(����ʵ�resourcesĿ¼�µ�index.html/jsp)
		// ����html·����http://localhost:8080/test1(����ʵ�resources/test1�µ�index.html/jsp����Ȼ��Ҳ������test1�������ҳ�����֣�����test.html)
		Tomcat tomcat = new Tomcat();// ����tomcatʵ������������tomcat
		// tomcat.setHostname("localhost");// ����������
		// tomcat.setPort(8080);// ���ö˿�
		// tomcat.setBaseDir(".");// tomcat�洢������Ϣ��Ŀ¼��������־����Ϣ����Ŀ¼
		// ���ʶ˿ںͱ���
		Connector connector = new Connector();// ����Э�飬Ĭ�Ͼ������Э��
		connector.setURIEncoding("UTF-8");// ���ñ���
		connector.setPort(8080);// ���ö˿�
		tomcat.getService().addConnector(connector);
		// ����servlet
		org.apache.catalina.Context ctx = tomcat.addContext("myapp", null);// �������·��
		tomcat.addServlet(ctx, "myServlet", new MessageServlet()); // ����servlet
		ctx.addServletMappingDecoded("/messageServlet", "myServlet");// ����servletӳ��·��
		//
		// StandardServer server = (StandardServer) tomcat.getServer();// ��Ӽ���������֪����
		// AprLifecycleListener listener = new AprLifecycleListener();
		// server.addLifecycleListener(listener);
		// ����appBaseΪ��Ŀ����Ŀ¼
		tomcat.getHost().setAppBase(System.getProperty("user.dir") + File.separator + ".");
		// ����WEB-INF�ļ�������Ŀ¼
		// ���ļ����°���web.xml
		// ������localhost:�˿ں�ʱ����Ĭ�Ϸ��ʸ�Ŀ¼�µ�index.html/jspҳ��
		tomcat.addWebapp("", "resources");
		tomcat.start();// ����tomcat
		tomcat.getServer().await();// ά��tomcat���񣬷���tomcatһ�����ͻ�ر�}
	}

	private void abc() throws LifecycleException {
		// ����Servlet·����http://localhost:8080/myapp/myServlet
		// ����html·����http://localhost:8080(����ʵ�resourcesĿ¼�µ�index.html/jsp)
		// ����html·����http://localhost:8080/test1(����ʵ�resources/test1�µ�index.html/jsp����Ȼ��Ҳ������test1�������ҳ�����֣�����test.html)
		Tomcat tomcat = new Tomcat();// ����tomcatʵ������������tomcat
		tomcat.setHostname("localhost");// ����������
		tomcat.setPort(8080);// ���ö˿�
		tomcat.setBaseDir(".");// tomcat�洢������Ϣ��Ŀ¼��������־����Ϣ����Ŀ¼
		String DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";
		Connector connector = new Connector(DEFAULT_PROTOCOL);// ����Э�飬Ĭ�Ͼ������Э��connector.setURIEncoding("UTF-8");//���ñ���
		connector.setPort(8080);// ���ö˿�
		tomcat.getService().addConnector(connector);
		org.apache.catalina.Context ctx = tomcat.addContext("myapp", null);// �������·��
		tomcat.addServlet(ctx, "myServlet", new MessageServlet()); // ����servlet
		ctx.addServletMappingDecoded("/messageServlet", "myServlet");// ����servletӳ��·��
		StandardServer server = (StandardServer) tomcat.getServer();// ��Ӽ���������֪����
		AprLifecycleListener listener = new AprLifecycleListener();
		server.addLifecycleListener(listener);
		// ����appBaseΪ��Ŀ����Ŀ¼
		tomcat.getHost().setAppBase(System.getProperty("user.dir") + File.separator + ".");
		// ����WEB-INF�ļ�������Ŀ¼
		// ���ļ����°���web.xml
		// ������localhost:�˿ں�ʱ����Ĭ�Ϸ��ʸ�Ŀ¼�µ�index.html/jspҳ��
		tomcat.addWebapp("", "webapp");
		tomcat.start();// ����tomcat
		tomcat.getServer().await();// ά��tomcat���񣬷���tomcatһ�����ͻ�ر�}
	}
}
