package org.sdjen.download.cache_sis.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "siscache.conf.async") // 需要pojo类
public class ConfigAsync {
	private int num_list_core = 2;
	private int num_list_max = 2;
	private int num_single_core = 6;
	private int num_single_max = 12;
	private int queue_capacity = 50;
	private int seconds_alive = 60;
	private int cp_core = 1;
	private int cp_max = 2;
	private int test = 2;

	@Override
	public String toString() {
		try {
			return org.sdjen.download.cache_sis.json.JsonUtil
					.toPrettyJson(new org.sdjen.download.cache_sis.util.EntryData<>()//
							.put("num_list_core", num_list_core)//
							.put("num_list_max", num_list_max)//
							.put("num_single_core", num_single_core)//
							.put("num_single_max", num_single_max)//
							.put("queue_capacity", queue_capacity)//
							.put("seconds_alive", seconds_alive)//
							.put("cp_core", cp_core)//
							.put("cp_max", cp_max)//
							.put("test", test)//
							.getData());
		} catch (java.lang.Throwable e) {
			return super.toString();
		}
	}

	public int getNum_list_core() {
		return num_list_core;
	}

	public void setNum_list_core(int num_list_core) {
		this.num_list_core = num_list_core;
	}

	public int getNum_list_max() {
		return num_list_max;
	}

	public void setNum_list_max(int num_list_max) {
		this.num_list_max = num_list_max;
	}

	public int getNum_single_core() {
		return num_single_core;
	}

	public void setNum_single_core(int num_single_core) {
		this.num_single_core = num_single_core;
	}

	public int getNum_single_max() {
		return num_single_max;
	}

	public void setNum_single_max(int num_single_max) {
		this.num_single_max = num_single_max;
	}

	public int getQueue_capacity() {
		return queue_capacity;
	}

	public void setQueue_capacity(int queue_capacity) {
		this.queue_capacity = queue_capacity;
	}

	public int getSeconds_alive() {
		return seconds_alive;
	}

	public void setSeconds_alive(int seconds_alive) {
		this.seconds_alive = seconds_alive;
	}

	public int getCp_core() {
		return cp_core;
	}

	public void setCp_core(int cp_core) {
		this.cp_core = cp_core;
	}

	public int getCp_max() {
		return cp_max;
	}

	public void setCp_max(int cp_max) {
		this.cp_max = cp_max;
	}

	public int getTest() {
		return test;
	}

	public void setTest(int test) {
		this.test = test;
	}
}
