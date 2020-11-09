package org.sdjen.download.cache_sis.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "siscache.conf.httputil") // 需要pojo类
public class ConfigHttputil {
	private int retry_times = 5;
	private int retry_time_second = 30;
	private String chatset = "GBK";

	@Override
	public String toString() {
		try {
			return org.sdjen.download.cache_sis.json.JsonUtil
					.toPrettyJson(new org.sdjen.download.cache_sis.util.EntryData<>()//
							.put("retry_times", retry_times)//
							.put("retry_time_second", retry_time_second)//
							.put("chatset", chatset)//
							.getData());
		} catch (java.lang.Throwable e) {
			return super.toString();
		}
	}

	public int getRetry_times() {
		return retry_times;
	}

	public void setRetry_times(int retry_times) {
		this.retry_times = retry_times;
	}

	public int getRetry_time_second() {
		return retry_time_second;
	}

	public void setRetry_time_second(int retry_time_second) {
		this.retry_time_second = retry_time_second;
	}

	public String getChatset() {
		return chatset;
	}

	public void setChatset(String chatset) {
		this.chatset = chatset;
	}
}
