package org.sdjen.download.cache_sis.store.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Urls_proxy {
	@Id
	private String url;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
