package org.sdjen.download.cache_sis.init;

import java.util.Arrays;
import java.util.logging.Logger;

import org.sdjen.download.cache_sis.test.WithoutInterfaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * 初始化类
 */
@Order(1) // @Order注解可以改变执行顺序，越小越先执行
@Component
public class MyApplicationRunner implements ApplicationRunner {
	private final static Logger logger = Logger.getLogger(MyApplicationRunner.class.toString());

//	@Autowired
//	WithoutInterfaceService withoutInterfaceService;
	@Autowired
	private MongoTemplate mongoTemplate;

	/**
	 * 会在服务启动完成后立即执行
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		try {
			Index index = new Index();
			index.unique();
			index.on("type", Sort.Direction.ASC);
			index.on("key", Sort.Direction.ASC);
			logger.info("+++++++++++Index:	" + mongoTemplate.indexOps("md").ensureIndex(index));
		} catch (Exception e) {
			logger.info("+++++++++++Index:	" + e);
		}
//		System.out.println("-----MyApplicationRunner" + Arrays.asList(args));
//		System.out.println("+++++++++++WithoutInterfaceService:	" + withoutInterfaceService.test());
	}

}
