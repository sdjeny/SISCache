package test;

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
		// 访问Servlet路径：http://localhost:8080/myapp/myServlet
		// 访问html路径：http://localhost:8080(会访问到resources目录下的index.html/jsp)
		// 访问html路径：http://localhost:8080/test1(会访问到resources/test1下的index.html/jsp，当然，也可以在test1后面加上页面名字，比如test.html)
		Tomcat tomcat = new Tomcat();// 创建tomcat实例，用来启动tomcat
		// tomcat.setHostname("localhost");// 设置主机名
		// tomcat.setPort(8080);// 设置端口
		// tomcat.setBaseDir(".");// tomcat存储自身信息的目录，比如日志等信息，根目录
		// 访问端口和编码
		Connector connector = new Connector();// 设置协议，默认就是这个协议
		connector.setURIEncoding("UTF-8");// 设置编码
		connector.setPort(8080);// 设置端口
		tomcat.getService().addConnector(connector);
		// 配置servlet
		org.apache.catalina.Context ctx = tomcat.addContext("myapp", null);// 网络访问路径
		tomcat.addServlet(ctx, "myServlet", new MessageServlet()); // 配置servlet
		ctx.addServletMappingDecoded("/messageServlet", "myServlet");// 配置servlet映射路径
		//
		// StandardServer server = (StandardServer) tomcat.getServer();// 添加监听器，不知何用
		// AprLifecycleListener listener = new AprLifecycleListener();
		// server.addLifecycleListener(listener);
		// 设置appBase为项目所在目录
		tomcat.getHost().setAppBase(System.getProperty("user.dir") + File.separator + ".");
		// 设置WEB-INF文件夹所在目录
		// 该文件夹下包含web.xml
		// 当访问localhost:端口号时，会默认访问该目录下的index.html/jsp页面
		tomcat.addWebapp("", "resources");
		tomcat.start();// 启动tomcat
		tomcat.getServer().await();// 维持tomcat服务，否则tomcat一启动就会关闭}
	}

	private void abc() throws LifecycleException {
		// 访问Servlet路径：http://localhost:8080/myapp/myServlet
		// 访问html路径：http://localhost:8080(会访问到resources目录下的index.html/jsp)
		// 访问html路径：http://localhost:8080/test1(会访问到resources/test1下的index.html/jsp，当然，也可以在test1后面加上页面名字，比如test.html)
		Tomcat tomcat = new Tomcat();// 创建tomcat实例，用来启动tomcat
		tomcat.setHostname("localhost");// 设置主机名
		tomcat.setPort(8080);// 设置端口
		tomcat.setBaseDir(".");// tomcat存储自身信息的目录，比如日志等信息，根目录
		String DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";
		Connector connector = new Connector(DEFAULT_PROTOCOL);// 设置协议，默认就是这个协议connector.setURIEncoding("UTF-8");//设置编码
		connector.setPort(8080);// 设置端口
		tomcat.getService().addConnector(connector);
		org.apache.catalina.Context ctx = tomcat.addContext("myapp", null);// 网络访问路径
		tomcat.addServlet(ctx, "myServlet", new MessageServlet()); // 配置servlet
		ctx.addServletMappingDecoded("/messageServlet", "myServlet");// 配置servlet映射路径
		StandardServer server = (StandardServer) tomcat.getServer();// 添加监听器，不知何用
		AprLifecycleListener listener = new AprLifecycleListener();
		server.addLifecycleListener(listener);
		// 设置appBase为项目所在目录
		tomcat.getHost().setAppBase(System.getProperty("user.dir") + File.separator + ".");
		// 设置WEB-INF文件夹所在目录
		// 该文件夹下包含web.xml
		// 当访问localhost:端口号时，会默认访问该目录下的index.html/jsp页面
		tomcat.addWebapp("", "webapp");
		tomcat.start();// 启动tomcat
		tomcat.getServer().await();// 维持tomcat服务，否则tomcat一启动就会关闭}
	}
}
