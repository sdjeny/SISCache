package org.sdjen.download.cache_sis.log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.Server;
import org.hsqldb.server.ServerConfiguration;

public class HsqldbFactory {
	public static void main(String[] args) {
		HsqldbFactory.start();
	}

	public static void start() {
		int port = 18463;
		boolean flag = false;
		try {
			Socket socket = new Socket(InetAddress.getByName("127.0.0.1"), port); // 建立一个Socket连接
			flag = socket.isBound();
			socket.close();
		} catch (IOException e) {
		}
		if (flag) {
			System.out.println("Port has been binded:" + port);
			return;
		}
		// org.hsqldb.util.DatabaseManagerSwing.main(new String[] {});
		// new org.hsqldb.util.DatabaseManager();
		// org.hsqldb.Server.main("-database.0 mydb -dbname.0 demoDB".split(" "));
		HsqlProperties props = new HsqlProperties();
		props.setProperty("server.database.0", "mydb");
		props.setProperty("server.dbname.0", "demoDB");
		props.setProperty("server.port", String.valueOf(port));
		ServerConfiguration.translateDefaultDatabaseProperty(props);
		// Standard behaviour when started from the command line
		// is to halt the VM when the server shuts down. This may, of
		// course, be overridden by whatever, if any, security policy
		// is in place.
		ServerConfiguration.translateDefaultNoSystemExitProperty(props);
		ServerConfiguration.translateAddressProperty(props);
		// finished setting up properties;
		Server server = new Server();
		try {
			server.setProperties(props);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		server.start();
	}
}
