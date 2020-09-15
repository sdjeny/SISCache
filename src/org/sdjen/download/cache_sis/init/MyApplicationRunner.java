package org.sdjen.download.cache_sis.init;

import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.service.SISDownloadTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private final static Logger logger = LoggerFactory.getLogger(MyApplicationRunner.class);

//	@Autowired
//	WithoutInterfaceService withoutInterfaceService;
	@Autowired
	private SISDownloadTimer timer;
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
		try {
			Index index = new Index();
			index.unique();
			index.on("fid", Sort.Direction.ASC);
			index.on("id", Sort.Direction.ASC);
			index.on("page", Sort.Direction.ASC);
			logger.info("+++++++++++Index:	" + mongoTemplate.indexOps("htmldoc").ensureIndex(index));
		} catch (Exception e) {
			logger.info("+++++++++++Index:	" + e);
		}
		double hour = 2;
		try {
			hour = Double.valueOf(ConfUtil.getDefaultConf().getProperties().getProperty("times_period"));
		} catch (Exception e) {
			ConfUtil.getDefaultConf().getProperties().setProperty("times_period", String.valueOf(hour));
			ConfUtil.getDefaultConf().store();
		}
		try {
			timer.restart(hour);
		} catch (Exception e) {
			e.printStackTrace();
		}
//		System.out.println("-----MyApplicationRunner" + Arrays.asList(args));
//		System.out.println("+++++++++++WithoutInterfaceService:	" + withoutInterfaceService.test());
	}

}
