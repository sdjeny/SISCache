package org.sdjen.download.cache_sis.configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.sdjen.download.cache_sis.json.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.fasterxml.jackson.core.JsonProcessingException;

@Configuration
@EnableAsync
//@ConfigurationProperties(prefix = "siscache.conf.async")// 需要pojo类
public class TaskPoolConfig {
	@Value("${siscache.conf.async.num_list_core}")
	private int num_list_core = 1;
	@Value("${siscache.conf.async.num_list_max}")
	private int num_list_max = 2;
	@Value("${siscache.conf.async.num_single_core}")
	private int num_single_core = 3;
	@Value("${siscache.conf.async.num_single_max}")
	private int num_single_max = 4;
	@Value("${siscache.conf.async.queue_capacity}")
	private int queue_capacity = 5;
	@Value("${siscache.conf.async.seconds_alive}")
	private int seconds_alive = 6;
	@Autowired
	private ConfigMain configMain;

	@Bean("taskExecutor")
	public Executor taskExecutro() {
		System.out.println("taskExecutro:	" + configMain);
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(10);// 配置核心线程数
		taskExecutor.setMaxPoolSize(50);// 配置最大线程数
		taskExecutor.setQueueCapacity(200);// 配置队列大小
		taskExecutor.setKeepAliveSeconds(60);
		taskExecutor.setThreadNamePrefix("async-");// 配置线程池中的线程的名称前缀
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
		taskExecutor.setAwaitTerminationSeconds(60);
		return taskExecutor;
	}

	@Bean("downloadSingleExecutor")
	public ThreadPoolTaskExecutor downloadSingleExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(num_single_core);
		taskExecutor.setMaxPoolSize(num_single_max);
		taskExecutor.setQueueCapacity(queue_capacity);
//		taskExecutor.setKeepAliveSeconds(seconds_alive);
		taskExecutor.setThreadNamePrefix("DS-");
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
//		taskExecutor.setAwaitTerminationSeconds(seconds_alive);
		taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		return taskExecutor;
	}

	@Bean("downloadListExecutor")
	public ThreadPoolTaskExecutor downloadListExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(num_list_core);
		taskExecutor.setMaxPoolSize(num_list_max);
		taskExecutor.setQueueCapacity(queue_capacity);
//		taskExecutor.setKeepAliveSeconds(seconds_alive);
		taskExecutor.setThreadNamePrefix("DL-");
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
//		taskExecutor.setAwaitTerminationSeconds(seconds_alive);
		taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		return taskExecutor;
	}

	@Bean("cpES2MGExecutor")
	public ThreadPoolTaskExecutor cpES2MGExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(3);
		taskExecutor.setMaxPoolSize(5);
		taskExecutor.setQueueCapacity(9999);
//		taskExecutor.setKeepAliveSeconds(seconds_alive);
		taskExecutor.setThreadNamePrefix("CPE2M-");
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
//		taskExecutor.setAwaitTerminationSeconds(seconds_alive);
		taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		return taskExecutor;
	}

}
