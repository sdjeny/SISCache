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
			};// cassandra主机地址

			// 认证配置
			AuthProvider authProvider = new PlainTextAuthProvider("ershixiong", "123456");

			LoadBalancingPolicy lbp = new TokenAwarePolicy(DCAwareRoundRobinPolicy.builder().withLocalDc("myDC").build());

			// 读超时或连接超时设置
			SocketOptions so = new SocketOptions().setReadTimeoutMillis(3000).setConnectTimeoutMillis(3000);

			// 连接池配置
			// PoolingOptions poolingOptions = new
			// PoolingOptions().setConnectionsPerHost(HostDistance.LOCAL, 2, 3);
			// 集群在同一个机房用HostDistance.LOCAL 不同的机房用HostDistance.REMOTE
			// 忽略用HostDistance.IGNORED
			PoolingOptions poolingOptions = new PoolingOptions().setMaxRequestsPerConnection(HostDistance.LOCAL, 64)// 每个连接最多允许64个并发请求
					.setCoreConnectionsPerHost(HostDistance.LOCAL, 2)// 和集群里的每个机器都至少有2个连接
					.setMaxConnectionsPerHost(HostDistance.LOCAL, 6);// 和集群里的每个机器都最多有6个连接

			// 查询配置
			// 设置一致性级别ANY(0),ONE(1),TWO(2),THREE(3),QUORUM(4),ALL(5),LOCAL_QUORUM(6),EACH_QUORUM(7),SERIAL(8),LOCAL_SERIAL(9),LOCAL_ONE(10);
			// 可以在每次生成查询statement的时候设置，也可以像这样全局设置
			QueryOptions queryOptions = new QueryOptions().setConsistencyLevel(ConsistencyLevel.ONE);

			// 重试策略
			RetryPolicy retryPolicy = DowngradingConsistencyRetryPolicy.INSTANCE;

			int port = 9042;// 端口号

			String keyspace = "keyspacename";// 要连接的库，可以不写

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
				.addContactPoints("192.168.0.231")// addContactPoints:cassandra节点ip
				.withPort(9042)// withPort:cassandra节点端口 默认9042
				// .withCredentials("cassandra", "cassandra")//
				// withCredentials:cassandra用户名密码，如果cassandra.yaml里authenticator：AllowAllAuthenticator可以不用配置
				.withPoolingOptions(new PoolingOptions()
						// 每个连接的最大请求数 2.0的驱动好像没有这个方法
						.setMaxRequestsPerConnection(HostDistance.LOCAL, 32)
						// 表示和集群里的机器至少有2个连接 最多有4个连接
						.setCoreConnectionsPerHost(HostDistance.LOCAL, 2).setMaxConnectionsPerHost(HostDistance.LOCAL, 4)
						.setCoreConnectionsPerHost(HostDistance.REMOTE, 2).setMaxConnectionsPerHost(HostDistance.REMOTE, 4)//
				)//
				.build();
		session = cluster.connect();
		// 创建键空间 单数据中心 复制策略 ：1
		String cql = "CREATE KEYSPACE if not exists mydb WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'}";
		session.close();
		session = cluster.connect("mydb");// 指定键空间，cql里不需要再指明
		session.execute(cql);
		// 创建表a,b为复合主键 a：分区键，b：集群键
		session.execute("CREATE TABLE if not exists test (a text,b int,c text,d int,PRIMARY KEY (a, b))");
		session.execute("CREATE TABLE if not exists url_path (key text,path text, PRIMARY KEY (key))");
		session.execute("CREATE TABLE if not exists md5_path (key text,path text, PRIMARY KEY (key))");
		return this;
	}

	public Session getSession() {
		return session;
	}

	/**
	 * 插入
	 */
	public void insert() {
		String cql = "INSERT INTO mydb.test (a , b , c , d ) VALUES (?,?,?,?);";
		ResultSet resultSet = session.execute(cql, "Hi~ o(*￣▽￣*)ブ", 8, "草", 10);

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
	 * 修改
	 */
	public void update() {
		// a,b是复合主键 所以条件都要带上，少一个都会报错，而且update不能修改主键的值，这应该和cassandra的存储方式有关
		String cql = "UPDATE mydb.test SET d = 1234 WHERE a='aa' and b=2;";
		// 也可以这样 cassandra插入的数据如果主键已经存在，其实就是更新操作
		String cql2 = "INSERT INTO mydb.test (a,b,d) VALUES ( 'aa',2,1234);";
		// cql 和 cql2 的执行效果其实是一样的
		session.execute(cql);
	}

	/**
	 * 删除
	 */
	public void delete() {
		// 删除一条记录里的单个字段 只能删除非主键，且要带上主键条件
		String cql = "DELETE d FROM mydb.test WHERE a='aa' AND b=2;";
		// 删除一张表里的一条或多条记录 条件里必须带上分区键
		String cql2 = "DELETE FROM mydb.test WHERE a='aa';";
		session.execute(cql);
		session.execute(cql2);
	}

	/**
	 * 查询
	 */
	public void query() {
		String cql = "SELECT * FROM test;";
		String cql2 = "SELECT a,b,c,d FROM mydb.test;";

		ResultSet resultSet = session.execute(cql);
		System.out.print("这里是字段名：");
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
