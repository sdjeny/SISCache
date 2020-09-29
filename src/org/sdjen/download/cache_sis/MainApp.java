package org.sdjen.download.cache_sis;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.test.morebeen.MoreBeenItfc;
import org.sdjen.download.cache_sis.test.morebeen.OneBeenItfc;
import org.sdjen.download.cache_sis.util.JsoupAnalysisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.io.CharStreams;

@Controller
@RequestMapping("/")
@SpringBootApplication
@ComponentScan({ //
		"org.sdjen.download.cache_sis"//
		, "org.sdjen.download.cache_sis.store"//
		, "org.sdjen.download.cache_sis.test"//
})
public class MainApp {
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
	@Resource(name = "${definde.service.name.store}")
	private IStore store;
	private String template;

	public static void main(String[] args) {
		SpringApplication.run(MainApp.class, args);
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
//		store.saveURL("url", "path_url");
//		store.saveMD5("md5", "path_md5");
//		System.out.println("get:	" + store.getURL_Path("url"));
//		System.out.println("get:	" + store.getMD5_Path("md5"));
		return httpUtil.getHTML("https://www.baidu.com/", "utf8");
	}

	private String getTemplate() throws IOException {
		if (null == template) {
			this.template = CharStreams.toString(
					new InputStreamReader(this.getClass().getClassLoader().getResource("template.html").openStream(),
							Charset.forName("GBK")));
		}
		return template;
	}

	@RequestMapping("/view/{key}")
	@ResponseBody
	private String view(@PathVariable("key") String key) {
		try {
//			StringBuffer stringBuffer = new StringBuffer();
//			Files.readAllLines(Paths.get(key + ".html"), Charset.forName("GBK"))
//					.forEach(str -> stringBuffer.append(str));
//			Map<String, String> details = TestJsoup.analysis(stringBuffer.toString());
			Map<String, String> details = JsoupAnalysisor.split(CharStreams.toString(
					new InputStreamReader(new FileInputStream(key + ".html"), Charset.forName("GBK"))), false);
			String template = getTemplate();
			for (Entry<String, String> entry : details.entrySet()) {
				template = template.replace(String.format(JsoupAnalysisor.KEYFORMAT, entry.getKey()), entry.getValue());
			}
			return template;
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}
}
