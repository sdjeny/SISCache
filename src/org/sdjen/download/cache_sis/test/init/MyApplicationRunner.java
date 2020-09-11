package org.sdjen.download.cache_sis.test.init;

import java.util.Arrays;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 初始化类
 */
@Order(1) // @Order注解可以改变执行顺序，越小越先执行
@Component
public class MyApplicationRunner implements ApplicationRunner {

	/**
	 * 会在服务启动完成后立即执行
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		System.out.println("MyApplicationRunner----" + Arrays.asList(args));
	}

}
