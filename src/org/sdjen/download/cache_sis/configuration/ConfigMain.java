package org.sdjen.download.cache_sis.configuration;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "siscache.conf") // 需要pojo类
public class ConfigMain implements Serializable {
	private static final long serialVersionUID = 1L;
	private int url_fail_stop = 0;
	private int url_fail_retry_begin = 0;
	private int url_fail_retry_in_hours = 0;
	private List<String> fids = Arrays.asList("143".split(","));
	private List<String> fids_unreplace_img = Arrays.asList("".split(","));
	private String path_es_start = "http://192.168.0.237:9200/siscache_";
	private boolean can_restart = false;
	private boolean can_reload = false;
	private boolean can_copy_es_mongo = false;
	private boolean can_copy_mongo_es = false;
	private int copy_unit_limit = 500;
	private List<String> connect_uncheck = Arrays.asList("sexinsex".split(","));
	private String list_url = "http://www.sexinsex.net/bbs/forum-{0}-{1}.html";
	private int list_page_middle = 50;
	private int list_page_middle_begin = 1;
	private int list_page_middle_end = 2;
	private int times_period = 2;
	private String times_ranges = "~1~10~143|torrent~1~5|torrent,image~1~5|cover~5~10~143,25";

	@Override
	public String toString() {
		try {
			return org.sdjen.download.cache_sis.json.JsonUtil
					.toPrettyJson(new org.sdjen.download.cache_sis.util.EntryData<>()//
							.put("url_fail_stop", url_fail_stop)//
							.put("url_fail_retry_begin", url_fail_retry_begin)//
							.put("url_fail_retry_in_hours", url_fail_retry_in_hours)//
							.put("fids", fids)//
							.put("fids_unreplace_img", fids_unreplace_img)//
							.put("path_es_start", path_es_start)//
							.put("can_restart", can_restart)//
							.put("can_reload", can_reload)//
							.put("can_copy_es_mongo", can_copy_es_mongo)//
							.put("can_copy_mongo_es", can_copy_mongo_es)//
							.put("copy_unit_limit", copy_unit_limit)//
							.put("connect_uncheck", connect_uncheck)//
							.put("list_url", list_url)//
							.put("list_page_middle", list_page_middle)//
							.put("list_page_middle_begin", list_page_middle_begin)//
							.put("list_page_middle_end", list_page_middle_end)//
							.put("times_period", times_period)//
							.put("times_ranges", times_ranges)//
							.getData());
		} catch (java.lang.Throwable e) {
			return super.toString();
		}
	}

	public int getUrl_fail_stop() {
		return url_fail_stop;
	}

	public void setUrl_fail_stop(int url_fail_stop) {
		this.url_fail_stop = url_fail_stop;
	}

	public int getUrl_fail_retry_begin() {
		return url_fail_retry_begin;
	}

	public void setUrl_fail_retry_begin(int url_fail_retry_begin) {
		this.url_fail_retry_begin = url_fail_retry_begin;
	}

	public int getUrl_fail_retry_in_hours() {
		return url_fail_retry_in_hours;
	}

	public void setUrl_fail_retry_in_hours(int url_fail_retry_in_hours) {
		this.url_fail_retry_in_hours = url_fail_retry_in_hours;
	}

	public List<String> getFids() {
		return fids;
	}

	public void setFids(List<String> fids) {
		this.fids = fids;
	}

	public List<String> getFids_unreplace_img() {
		return fids_unreplace_img;
	}

	public void setFids_unreplace_img(List<String> fids_unreplace_img) {
		this.fids_unreplace_img = fids_unreplace_img;
	}

	public String getPath_es_start() {
		return path_es_start;
	}

	public void setPath_es_start(String path_es_start) {
		this.path_es_start = path_es_start;
	}

	public boolean isCan_restart() {
		return can_restart;
	}

	public void setCan_restart(boolean can_restart) {
		this.can_restart = can_restart;
	}

	public boolean isCan_reload() {
		return can_reload;
	}

	public void setCan_reload(boolean can_reload) {
		this.can_reload = can_reload;
	}

	public boolean isCan_copy_es_mongo() {
		return can_copy_es_mongo;
	}

	public void setCan_copy_es_mongo(boolean can_copy_es_mongo) {
		this.can_copy_es_mongo = can_copy_es_mongo;
	}

	public boolean isCan_copy_mongo_es() {
		return can_copy_mongo_es;
	}

	public void setCan_copy_mongo_es(boolean can_copy_mongo_es) {
		this.can_copy_mongo_es = can_copy_mongo_es;
	}

	public int getCopy_unit_limit() {
		return copy_unit_limit;
	}

	public void setCopy_unit_limit(int copy_unit_limit) {
		this.copy_unit_limit = copy_unit_limit;
	}

	public List<String> getConnect_uncheck() {
		return connect_uncheck;
	}

	public void setConnect_uncheck(List<String> connect_uncheck) {
		this.connect_uncheck = connect_uncheck;
	}

	public String getList_url() {
		return list_url;
	}

	public void setList_url(String list_url) {
		this.list_url = list_url;
	}

	public int getList_page_middle() {
		return list_page_middle;
	}

	public void setList_page_middle(int list_page_max) {
		this.list_page_middle = list_page_max;
	}

	public int getList_page_middle_begin() {
		return list_page_middle_begin;
	}

	public void setList_page_middle_begin(int list_page_max_begin) {
		this.list_page_middle_begin = list_page_max_begin;
	}

	public int getList_page_middle_end() {
		return list_page_middle_end;
	}

	public void setList_page_middle_end(int list_page_max_end) {
		this.list_page_middle_end = list_page_max_end;
	}

	public int getTimes_period() {
		return times_period;
	}

	public void setTimes_period(int times_period) {
		this.times_period = times_period;
	}

	public String getTimes_ranges() {
		return times_ranges;
	}

	public void setTimes_ranges(String times_ranges) {
		this.times_ranges = times_ranges;
	}
}
