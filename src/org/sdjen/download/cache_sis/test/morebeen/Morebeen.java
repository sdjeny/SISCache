package org.sdjen.download.cache_sis.test.morebeen;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/test/morebeen")
public class Morebeen {

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
}
