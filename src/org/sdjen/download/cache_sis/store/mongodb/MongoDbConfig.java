package org.sdjen.download.cache_sis.store.mongodb;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

/**
 * @author by abbot
 * @create 2018-08-24 17:19
 **/

@Configuration
@EnableConfigurationProperties({ MongoDbProperties.class })
public class MongoDbConfig {

	@Autowired
	MongoDbProperties properties;

	@Bean
	public MongoDbFactory mongoDbFactory(MongoDbProperties properties) {

		MongoClientOptions.Builder builder = new MongoClientOptions.Builder();

		builder.connectionsPerHost(properties.getMaxConnectionsPerHost());
		builder.minConnectionsPerHost(properties.getMinConnectionsPerHost());
		if (null != properties.getReplicaSet() && !properties.getReplicaSet().isEmpty()) {
			builder.requiredReplicaSetName(properties.getReplicaSet());
		}
		builder.threadsAllowedToBlockForConnectionMultiplier(
				properties.getThreadsAllowedToBlockForConnectionMultiplier());
		builder.serverSelectionTimeout(properties.getServerSelectionTimeout());
		builder.maxWaitTime(properties.getMaxWaitTime());
		builder.maxConnectionIdleTime(properties.getMaxConnectionIdleTime());
		builder.maxConnectionLifeTime(properties.getMaxConnectionLifeTime());
		builder.connectTimeout(properties.getConnectTimeout());
		builder.socketTimeout(properties.getSocketTimeout());
//        builder.socketKeepAlive(properties.getSocketKeepAlive());
		builder.sslEnabled(properties.getSslEnabled());
		builder.sslInvalidHostNameAllowed(properties.getSslInvalidHostNameAllowed());
		builder.alwaysUseMBeans(properties.getAlwaysUseMBeans());
		builder.heartbeatFrequency(properties.getHeartbeatFrequency());
		builder.minHeartbeatFrequency(properties.getMinHeartbeatFrequency());
		builder.heartbeatConnectTimeout(properties.getHeartbeatConnectTimeout());
		builder.heartbeatSocketTimeout(properties.getHeartbeatSocketTimeout());
		builder.localThreshold(properties.getLocalThreshold());

		MongoClientOptions mongoClientOptions = builder.build();

		// MongoDB地址列表
		List<ServerAddress> serverAddresses = new ArrayList<>();
		for (String address : properties.getAddress()) {
			String[] hostAndPort = address.split(":");
			String host = hostAndPort[0];
			Integer port = Integer.parseInt(hostAndPort[1]);
			ServerAddress serverAddress = new ServerAddress(host, port);
			serverAddresses.add(serverAddress);
		}
		System.out.println(">>>>>>>>>>>MongoDB(" + properties.getDatabase() + ")地址列表serverAddresses:"
				+ serverAddresses.toString());

		// 连接认证
		List<MongoCredential> mongoCredentialList = new ArrayList<>();
		if (null != properties.getUsername() && !properties.getUsername().isEmpty()) {
			mongoCredentialList.add(MongoCredential.createScramSha1Credential(properties.getUsername(),
					(null != properties.getAuthenticationDatabase()
							&& !properties.getAuthenticationDatabase().isEmpty())
									? properties.getAuthenticationDatabase()
									: properties.getDatabase(),
					properties.getPassword().toCharArray()));
			System.out.println(">>>>>>>>>>>连接认证mongoCredentialList:" + mongoCredentialList.toString());
		}

		// 创建客户端和Factory
		MongoClient mongoClient = new MongoClient(serverAddresses, mongoCredentialList, mongoClientOptions);
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(mongoClient, properties.getDatabase());

		return mongoDbFactory;
	}

	@Bean
	public MappingMongoConverter mappingMongoConverter(MongoDbFactory factory, MongoMappingContext context,
			BeanFactory beanFactory) {
		DbRefResolver dbRefResolver = new DefaultDbRefResolver(factory);
		MappingMongoConverter mappingConverter = new MappingMongoConverter(dbRefResolver, context);
		try {
			mappingConverter.setCustomConversions(beanFactory.getBean(CustomConversions.class));
		} catch (NoSuchBeanDefinitionException ignore) {
		}
		// 不保存save _class
		mappingConverter.setTypeMapper(new DefaultMongoTypeMapper(null));
		return mappingConverter;
	}
}
