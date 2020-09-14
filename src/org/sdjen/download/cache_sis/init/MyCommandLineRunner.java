package org.sdjen.download.cache_sis.init;

import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1) // @Order注解可以改变执行顺序，越小越先执行
@Component
public class MyCommandLineRunner implements CommandLineRunner {
//	@Autowired
//	A em;

	/**
	 * 会在服务启动完成后立即执行
	 */
	@Override
	public void run(String... args) throws Exception {
//		try {
//			em.init();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		System.out.println("-----MyCommandLineRunner" + Arrays.asList(args));
	}

}
