package org.sdjen.download.cache_sis.store;

import org.sdjen.download.cache_sis.ESMap;
import org.sdjen.download.cache_sis.json.JsonUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

public class ES {

	public static void main(String[] args) throws JsonProcessingException {
//		rst = connection.doPost(path_es_start + "html/_doc/_mapping/"//
		System.out.println(JsonUtil.toPrettyJson(//
				ESMap.get()//
						.set("properties", ESMap.get()//
								.set("context_zip", ESMap.get()//
										.set("type", "text")//
										.set("index", false)//
										.set("norms", false)//
										.set("fields", ESMap.get()//
												.set("keyword", ESMap.get()//
														.set("type", "keyword")//
														.set("ignore_above", 256)//
												)//
										)//
								)//
						)//
		));
	}

}
