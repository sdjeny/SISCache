package org.sdjen.download.cache_sis.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@EnableAutoConfiguration
@RequestMapping("/mongo")
public class TestMongo {

	@RequestMapping("/test")
	@ResponseBody
	String test() {
		try {
//			store.saveMD5("hhehehe", "/efsfds/efefe");
			return "hello"; // XXX
		} catch (Throwable e) {
			e.printStackTrace();
			return "ERR:" + e.getMessage();
		}
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(TestMongo.class, args);
	}
}
