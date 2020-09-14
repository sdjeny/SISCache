package org.sdjen.download.cache_sis.init;

import java.util.Arrays;

import org.sdjen.download.cache_sis.test.WithoutInterfaceService;
import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	WithoutInterfaceService withoutInterfaceService;

	/**
	 * 会在服务启动完成后立即执行
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		System.out.println("-----MyApplicationRunner" + Arrays.asList(args));
		System.out.println("+++++++++++WithoutInterfaceService:	" + withoutInterfaceService.test());
	}

}
