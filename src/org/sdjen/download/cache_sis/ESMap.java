package org.sdjen.download.cache_sis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * [12760]sdjen20171110
 */
public class ESMap extends LinkedHashMap<Object, Object> {
	
//	{
//		  "query" : {
//		    "bool" : {
//		      "filter" : [ {
//		        "range" : {
//		          "departure" : {
//		            "gte" : "2018-07-24T00:00:00",
//		            "lte" : "2018-07-24T00:00:00"
//		          }
//		        }
//		      }, {
//		        "terms" : {
//		          "market.keyword" : [ "BEN", "CAT", "COM" ]
//		        }
//		      }, {
//		        "term" : {
//		          "_ishistory" : false
//		        }
//		      } ],
//		      "must" : [ {
//		        "query_string" : {
//		          "fields" : [ "_all" ],
//		          "query" : "*e*"
//		        }
//		      }, {
//		        "query_string" : {
//		          "fields" : [ "_all" ],
//		          "query" : "��"
//		        }
//		      } ]
//		    }
//		  },
//		  "_source" : {
//		    "includes" : [ "profileid", "groupid" ]
//		  },
//		  "size" : 50,
//		  "from" : 0
//		}

	private static final long serialVersionUID = 1L;
	private static List<String> boolKeys = Arrays.asList("filter", "must", "should", "must_not");// [12871]sdjen20180103
	public static final String DATEFORMAT = "yyyy-MM-dd";

	public static ESMap get() {
		return new ESMap();
	}

	public ESMap set(Object key, Object value) {
		put(key, value);
		return this;
	}

	@Override
	public Object put(Object key, Object value) {
		return super.put(key, format(value));
	}

	private Object format(Object value) {
		if (value instanceof Map && !(value instanceof ESMap)) {
			ESMap node = get();
			node.putAll((Map) value);
			for (Entry<Object, Object> entry : node.entrySet())
				entry.setValue(format(entry.getValue()));
			value = node;
		}
		if (value instanceof List) {
			List<Object> list = (List<Object>) value;
			for (int i = 0; i < list.size(); i++) {
				list.set(i, format(list.get(i)));
			}
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Object key, Class<T> clazz) {
		return (T) super.get(key);
	}

	public ESMap addCondition(ESMap node) {
		if (null == node || node.isEmpty())
			return this;
		boolean isStandard = false;// �Ƿ�Ϊ��׼��ʽ
		for (Object key : node.keySet())
			isStandard = isStandard || boolKeys.contains(key);
		if (!isStandard)
			node = ESMap.get().set("filter", node);// [12871]sdjen20180103Ĭ��filter
		for (Entry<Object, Object> entry : node.entrySet()) {
			List<Object> list = get(entry.getKey(), List.class);
			if (null == list)
				put(entry.getKey(), list = new ArrayList<Object>());
			Object value = entry.getValue();
			if (value instanceof ESMap) {
				list.add((ESMap) value);
			} else if (value instanceof List) {
				for (ESMap item : (List<ESMap>) entry.getValue()) {
					list.add(item);
				}
			}
		}
		return this;
	}
}
