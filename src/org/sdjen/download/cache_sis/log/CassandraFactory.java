package org.sdjen.download.cache_sis.log;

import java.io.IOException;
import java.util.function.BiConsumer;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
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
		if (false) {
			String[] hosts = new String[] { "192.168.0.231"
					// "192.168.1.1", "192.168.1.2", "192.168.1.3"//
			};// cassandra������ַ

			// ��֤����
			AuthProvider authProvider = new PlainTextAuthProvider("ershixiong", "123456");

			LoadBalancingPolicy lbp = new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().withLocalDc("myDC").build());

			// ����ʱ�����ӳ�ʱ����
			SocketOptions so = new SocketOptions().setReadTimeoutMillis(3000).setConnectTimeoutMillis(3000);

			// ���ӳ�����
			// PoolingOptions poolingOptions = new
			// PoolingOptions().setConnectionsPerHost(HostDistance.LOCAL, 2, 3);
			// ��Ⱥ��ͬһ��������HostDistance.LOCAL ��ͬ�Ļ�����HostDistance.REMOTE
			// ������HostDistance.IGNORED
			PoolingOptions poolingOptions = new PoolingOptions().setMaxRequestsPerConnection(HostDistance.LOCAL, 64)// ÿ�������������64����������
					.setCoreConnectionsPerHost(HostDistance.LOCAL, 2)// �ͼ�Ⱥ���ÿ��������������2������
					.setMaxConnectionsPerHost(HostDistance.LOCAL, 6);// �ͼ�Ⱥ���ÿ�������������6������

			// ��ѯ����
			// ����һ���Լ���ANY(0),ONE(1),TWO(2),THREE(3),QUORUM(4),ALL(5),LOCAL_QUORUM(6),EACH_QUORUM(7),SERIAL(8),LOCAL_SERIAL(9),LOCAL_ONE(10);
			// ������ÿ�����ɲ�ѯstatement��ʱ�����ã�Ҳ����������ȫ������
			QueryOptions queryOptions = new QueryOptions().setConsistencyLevel(ConsistencyLevel.ONE);

			// ���Բ���
			RetryPolicy retryPolicy = DowngradingConsistencyRetryPolicy.INSTANCE;

			int port = 9042;// �˿ں�

			String keyspace = "keyspacename";// Ҫ���ӵĿ⣬���Բ�д

			Cluster cluster = Cluster.builder().addContactPoints(hosts).withAuthProvider(authProvider).withLoadBalancingPolicy(lbp)
					.withSocketOptions(so).withPoolingOptions(poolingOptions).withQueryOptions(queryOptions).withRetryPolicy(retryPolicy)
					.withPort(port).build();
			Session session = cluster.connect(keyspace);
		}
		CassandraFactory factory = new CassandraFactory().connect();
		{
			MapDBFactory.init();
			String path = ConfUtil.getDefaultConf().getProperties().getProperty("save_path") + "/map.db";
			DB db = DBMaker.fileDB(path)//
					.closeOnJvmShutdown()//
					.readOnly()//
					.make();
			db.hashMap("url-path", Serializer.STRING, Serializer.STRING).open()//
					.forEach(new BiConsumer<String, String>() {

						public void accept(String key, String value) {
							try {
								System.out.println("U: " + key + " : " + value);
								factory.getSession().execute("INSERT INTO url_path(key,path) VALUES (?,?);",key,value);
							} catch (Exception e) {
								MapDBFactory.getErrorDB().put(key, e.toString());
							}
						}
					});
			db.hashMap("file-md5-path", Serializer.STRING, Serializer.STRING).open()//
					.forEach(new BiConsumer<String, String>() {

						public void accept(String key, String value) {
							try {
								System.out.println("F:	" + key + "	:	" + value);
								factory.getSession().execute("INSERT INTO md5_path(key,path) VALUES (?,?);",key,value);
							} catch (Exception e) {
								MapDBFactory.getErrorDB().put(key, e.toString());
							}
						}
					});
			db.close();
			MapDBFactory.finishAll();
		}
		factory.session.close();
		factory.cluster.close();
	}

	public Cluster cluster;

	public Session session;

	public CassandraFactory connect() {
		cluster = Cluster.builder()//
				.addContactPoints("192.168.0.231")// addContactPoints:cassandra�ڵ�ip
				.withPort(9042)// withPort:cassandra�ڵ�˿� Ĭ��9042
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
		String cql = "CREATE KEYSPACE if not exists mydb WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}";
		session.close();
		session = cluster.connect("mydb");// ָ�����ռ䣬cql�ﲻ��Ҫ��ָ��
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

	/**
	 * ����
	 */
	public void insert() {
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
	public void update() {
		// a,b�Ǹ������� ����������Ҫ���ϣ���һ�����ᱨ������update�����޸�������ֵ����Ӧ�ú�cassandra�Ĵ洢��ʽ�й�
		String cql = "UPDATE mydb.test SET d = 1234 WHERE a='aa' and b=2;";
		// Ҳ�������� cassandra�����������������Ѿ����ڣ���ʵ���Ǹ��²���
		String cql2 = "INSERT INTO mydb.test (a,b,d) VALUES ( 'aa',2,1234);";
		// cql �� cql2 ��ִ��Ч����ʵ��һ����
		session.execute(cql);
	}

	/**
	 * ɾ��
	 */
	public void delete() {
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
	public void query() {
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
