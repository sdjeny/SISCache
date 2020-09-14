package org.sdjen.download.cache_sis;

import javax.annotation.Resource;

import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.test.morebeen.MoreBeenItfc;
import org.sdjen.download.cache_sis.test.morebeen.OneBeenItfc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
@SpringBootApplication
@ComponentScan({ //
		"org.sdjen.download.cache_sis"//
		, "org.sdjen.download.cache_sis.store"//
		, "org.sdjen.download.cache_sis.test"//
})
public class MainTest {
//	@Autowired
//	@Qualifier("morebeenB")
	@Resource()
	OneBeenItfc oneBeen;
	@Resource(name = "morebeenB")
	MoreBeenItfc moreBeenB;
	@Resource(name = "MorebeenA")
	MoreBeenItfc moreBeenA;
	@Autowired
	private HttpUtil httpUtil;
	@Resource(name = "Store_Mongodb")
	private IStore store;

	public static void main(String[] args) {

		SpringApplication.run(MainTest.class, args);
	}

	@RequestMapping("/")
	@ResponseBody
	private String hello() {
		System.out.println(oneBeen.getInfo());
		System.out.println(moreBeenA.getInfo());
		System.out.println(moreBeenB.getInfo());
		return "hello";
	}

	@RequestMapping("/rest")
	@ResponseBody
	private String rest() throws Throwable {
		store.saveURL("url", "path_url");
		store.saveMD5("md5", "path_md5");
//		System.out.println("get:	" + store.getURL_Path("url"));
//		System.out.println("get:	" + store.getMD5_Path("md5"));
		return httpUtil.getHTML("https://www.baidu.com/", "utf8");
	}
}
