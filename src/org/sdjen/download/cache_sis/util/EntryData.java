package org.sdjen.download.cache_sis.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class EntryData<K, V> {
	Map<K, V> data;
	InitValue<K, V> initValue;

	public interface InitValue<K, V> {
		V get(K key);
	}

	public EntryData<K, V> setInitValue(InitValue<K, V> initValue) {
		this.initValue = initValue;
		return this;
	}

	public EntryData() {
		this(new LinkedHashMap<>());
	}

	public EntryData(Map<K, V> data) {
		this.data = data;
	}

	public V get(K key) {
		return null == initValue ? data.get(key) : get(key, initValue);
	}

	public V get(K key, InitValue<K, V> initValue) {
		if (!data.containsKey(key)) {
			data.put(key, initValue.get(key));
		}
		return data.get(key);
	}

	public EntryData<K, V> put(K key, V value) {
		data.put(key, value);
		return this;
	}

	public Map<K, V> getData() {
		return data;
	}
}
