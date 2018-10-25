package org.sdjen.download.cache_sis.log;

import java.io.IOException;
import java.util.function.BiConsumer;

import org.sdjen.download.cache_sis.conf.ConfUtil;

import com.datastax.driver.core.AuthProvider;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ColumnDefinitions.Definition;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PlainTextAuthProvider;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.LoadBalancingPolicy;
import com.datastax.driver.core.policies.RetryPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;

public class CassandraFactory {
	public static void main(String[] args) throws IOException {
		ConfUtil.getDefaultConf().getProperties().setProperty("cassandra_addresses", "192.168.0.233");
		CassandraFactory factory = new CassandraFactory().connect();
		// {
		// MapDBFactory.init();
		// String path =
		// ConfUtil.getDefaultConf().getProperties().getProperty("save_path") +
		// "/map.db";
		// DB db = DBMaker.fileDB(path)//
		// .closeOnJvmShutdown()//
		// .readOnly()//
		// .make();
		// db.hashMap("url-path", Serializer.STRING, Serializer.STRING).open()//
		// .forEach(new BiConsumer<String, String>() {
		//
		// public void accept(String key, String value) {
		// try {
		// System.out.println("U: " + key + " : " + value);
		// factory.getSession().execute("INSERT INTO url_path(key,path) VALUES
		// (?,?);",key,value);
		// } catch (Exception e) {
		// MapDBFactory.getErrorDB().put(key, e.toString());
		// }
		// }
		// });
		// db.hashMap("file-md5-path", Serializer.STRING,
		// Serializer.STRING).open()//
		// .forEach(new BiConsumer<String, String>() {
		//
		// public void accept(String key, String value) {
		// try {
		// System.out.println("F: " + key + " : " + value);
		// factory.getSession().execute("INSERT INTO md5_path(key,path) VALUES
		// (?,?);",key,value);
		// } catch (Exception e) {
		// MapDBFactory.getErrorDB().put(key, e.toString());
		// }
		// }
		// });
		// db.close();
		// MapDBFactory.finishAll();
		// }
		long l = System.currentTimeMillis();
		try {
			for (String key : (//
			"1m3rco0n43fg3ev63gz5u5dxg"//
					+ ",1anc6byrmqnrvsd7s20ark1zl"//
					+ ",ez9acoxzotp6btmtr93877f8m"//
					+ ",apw4k2k821uk0ishkga1m8fu8"//
			).split(",")) {
				ResultSet resultSet = factory.getSession().execute("select path from md5_path where key=?", key);
				for (Row row : resultSet) {
					System.out.println((System.currentTimeMillis() - l) + " " + row.getString("path"));
				}
			}
//			ResultSet resultSet = factory.getSession().execute("select count(key) from md5_path");
			ResultSet resultSet = factory.getSession().execute("select key from url_path where path=? allow filtering",
					"images/2018-08/max/4frj05m0pojt47uo090n026qo.gif");
			for (Row row : resultSet) {
				System.out.println(row.getString("key"));
			}
			// for (Row row : resultSet) {
			// System.out.println((System.currentTimeMillis() - l) + " " +
			// row.getLong(0));
			// }
		} finally {
			factory.session.close();
			factory.cluster.close();
		}
	}

	public Cluster cluster;

	public Session session;
	private static CassandraFactory factory;

	private String addresses = "192.168.0.233";
	private int port = 9042;
	private String keyspace = "mydb";

	public CassandraFactory() throws IOException {
		ConfUtil conf = ConfUtil.getDefaultConf();
		boolean isStore = false;
		String value = conf.getProperties().getProperty("cassandra_addresses");
		if (null == value || value.isEmpty()) {
			conf.getProperties().setProperty("cassandra_addresses", addresses);
			isStore = true;
		} else
			addresses = value;
		value = conf.getProperties().getProperty("cassandra_keyspace");
		if (null == value || value.isEmpty()) {
			conf.getProperties().setProperty("cassandra_keyspace", keyspace);
			isStore = true;
		} else
			keyspace = value;
		try {
			port = Integer.valueOf(conf.getProperties().getProperty("cassandra_port"));
		} catch (Exception e) {
			conf.getProperties().setProperty("cassandra_port", String.valueOf(port));
			isStore = true;
		}
		if (isStore)
			conf.store();
	}

	public static synchronized CassandraFactory getDefaultFactory() throws IOException {
		if (null == factory)
			factory = new CassandraFactory().connect();
		return factory;
	}

	public void finish() {
		session.close();
		cluster.close();
	}

	public CassandraFactory connect() {
		cluster = Cluster.builder()//
				.addContactPoints(addresses.split(","))// addContactPoints:cassandra�ڵ�ip
				.withPort(port)// withPort:cassandra�ڵ�˿� Ĭ��9042
				// .withCredentials("cassandra", "cassandra")//
				// withCredentials:cassandra�û������룬���cassandra.yaml��authenticator��AllowAllAuthenticator���Բ�������
				.withPoolingOptions(new PoolingOptions()
						// ÿ�����ӵ���������� 2.0����������û���������
						.setMaxRequestsPerConnection(HostDistance.LOCAL, 32)
						// ��ʾ�ͼ�Ⱥ��Ļ���������2������ �����4������
						.setCoreConnectionsPerHost(HostDistance.LOCAL, 2).setMaxConnectionsPerHost(HostDistance.LOCAL, 4)
						.setCoreConnectionsPerHost(HostDistance.REMOTE, 2).setMaxConnectionsPerHost(HostDistance.REMOTE, 4)//
				)//
				.build();
		session = cluster.connect();
		// �������ռ� ���������� ���Ʋ��� ��1
		String cql = "CREATE KEYSPACE if not exists " + keyspace + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}";
		session.close();
		session = cluster.connect(keyspace);// ָ�����ռ䣬cql�ﲻ��Ҫ��ָ��
		session.execute(cql);
		// ������a,bΪ�������� a����������b����Ⱥ��
		session.execute("CREATE TABLE if not exists test (a text,b int,c text,d int,PRIMARY KEY (a, b))");
		session.execute("CREATE TABLE if not exists url_path (key text,path text, PRIMARY KEY (key))");
		session.execute("CREATE TABLE if not exists md5_path (key text,path text, PRIMARY KEY (key))");
		return this;
	}

	public Session getSession() {
		return session;
	}

	public String getMD5_Path(String key) {
		ResultSet resultSet = getSession().execute("select path from md5_path where key=?", key);
		for (Row row : resultSet) {
			return row.getString("path");
		}
		return null;
	}

	public String getURL_Path(String key) {
		ResultSet resultSet = getSession().execute("select path from url_path where key=?", key);
		for (Row row : resultSet) {
			return row.getString("path");
		}
		return null;
	}

	public void saveMD5(String key, String path) {
		getSession().execute("INSERT INTO md5_path(key,path) VALUES(?,?);", key, path);
	}

	public void saveURL(String key, String path) {
		getSession().execute("INSERT INTO url_path(key,path) VALUES(?,?);", key, path);
	}

	/**
	 * ����
	 */
	private void insert() {
		String cql = "INSERT INTO mydb.test (a , b , c , d ) VALUES (?,?,?,?);";
		ResultSet resultSet = session.execute(cql, "Hi~ o(*������*)��", 8, "��", 10);

		for (Definition definition : resultSet.getColumnDefinitions()) {
			System.out.print(definition.getName() + " ");
		}
		int size = resultSet.getColumnDefinitions().size();
		System.out.println(size);
		for (Row row : resultSet) {
			for (int i = 0; i < size; i++) {
				System.out.println(row.getObject(i));
			}
		}
	}

	/**
	 * �޸�
	 */
	private void update() {
		// a,b�Ǹ�������
		// ����������Ҫ���ϣ���һ�����ᱨ������update�����޸�������ֵ����Ӧ�ú�cassandra�Ĵ洢��ʽ�й�
		String cql = "UPDATE mydb.test SET d = 1234 WHERE a='aa' and b=2;";
		// Ҳ�������� cassandra�����������������Ѿ����ڣ���ʵ���Ǹ��²���
		String cql2 = "INSERT INTO mydb.test (a,b,d) VALUES ( 'aa',2,1234);";
		// cql �� cql2 ��ִ��Ч����ʵ��һ����
		session.execute(cql);
	}

	/**
	 * ɾ��
	 */
	private void delete() {
		// ɾ��һ����¼��ĵ����ֶ� ֻ��ɾ������������Ҫ������������
		String cql = "DELETE d FROM mydb.test WHERE a='aa' AND b=2;";
		// ɾ��һ�ű����һ���������¼ �����������Ϸ�����
		String cql2 = "DELETE FROM mydb.test WHERE a='aa';";
		session.execute(cql);
		session.execute(cql2);
	}

	/**
	 * ��ѯ
	 */
	private void query() {
		String cql = "SELECT * FROM test;";
		String cql2 = "SELECT a,b,c,d FROM mydb.test;";

		ResultSet resultSet = session.execute(cql);
		System.out.print("�������ֶ�����");
		for (Definition definition : resultSet.getColumnDefinitions()) {
			System.out.print(definition.getName() + " ");
		}
		System.out.println();
		System.out.println(String.format("%s\t%s\t%s\t%s\t\n%s", "a", "b", "c", "d",
				"--------------------------------------------------------------------------"));
		for (Row row : resultSet) {
			System.out.println(String.format("%s\t%d\t%s\t%d\t", row.getString("a"), row.getInt("b"), row.getString("c"), row.getInt("d")));
		}
	}
}
