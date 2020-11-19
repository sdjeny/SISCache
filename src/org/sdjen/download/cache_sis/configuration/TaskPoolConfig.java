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
	@Autowired
	private ConfigAsync configAsync;

	@Bean("taskExecutor")
	public Executor taskExecutro() {
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
		taskExecutor.setCorePoolSize(configAsync.getNum_single_core());
		taskExecutor.setMaxPoolSize(configAsync.getNum_single_max());
		taskExecutor.setQueueCapacity(configAsync.getQueue_capacity());
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
		taskExecutor.setCorePoolSize(configAsync.getNum_list_core());
		taskExecutor.setMaxPoolSize(configAsync.getNum_list_max());
		taskExecutor.setQueueCapacity(configAsync.getQueue_capacity());
//		taskExecutor.setKeepAliveSeconds(seconds_alive);
		taskExecutor.setThreadNamePrefix("DL-");
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
//		taskExecutor.setAwaitTerminationSeconds(seconds_alive);
		taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		return taskExecutor;
	}

	@Bean("cpExecutor")
	public ThreadPoolTaskExecutor cpExecutor() {
		System.out.println("cpExecutor:	" + configAsync);
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(configAsync.getCp_core());
		taskExecutor.setMaxPoolSize(configAsync.getCp_max());
		taskExecutor.setQueueCapacity(9999);
//		taskExecutor.setKeepAliveSeconds(seconds_alive);
		taskExecutor.setThreadNamePrefix("CP-");
		taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
//		taskExecutor.setAwaitTerminationSeconds(seconds_alive);
		taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		return taskExecutor;
	}

}
