package org.sdjen.download.cache_sis.init;

import javax.annotation.Resource;

import org.sdjen.download.cache_sis.configuration.ConfigMain;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.timer.InitStartTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
	private final static Logger logger = LoggerFactory.getLogger(MyApplicationRunner.class);

//	@Autowired
//	WithoutInterfaceService withoutInterfaceService;
//	@Autowired
	@Resource(name = "${definde.service.name.timer}")
	private InitStartTimer timer;
//	@Resource(name = "morebeenB")
//	MoreBeenItfc moreBeenB;
//	@Resource(name = "MorebeenA")
//	MoreBeenItfc moreBeenA;
//	@Resource(name = "${definde.service.name.morebeen}")
//	MoreBeenItfc moreBeen;
	@Value("${siscache.conf.can_copy_es_mongo}")
	private boolean can_copy_es_mongo = true;
	@Resource(name = "${definde.service.name.store}")
	private IStore store;
	@Autowired
	private ConfigMain configMain;
	/**
	 * 会在服务启动完成后立即执行
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		System.out.println("ConfigMain:	" + configMain);
		double hour = configMain.getTimes_period();
		try {
			store.init();
		} catch (Throwable e) {
			logger.error("store.init", e);
		}
		try {
			timer.restart(hour);
		} catch (Throwable e) {
			logger.error("timer", e);
		}
//		System.out.println("~~~~~~~~~~~~~~~~~" + moreBeenA.getInfo());
//		System.out.println("~~~~~~~~~~~~~~~~~" + moreBeenB.getInfo());
//		System.out.println("~~~~~~~~~~~~~~~~~" + moreBeen.getInfo());
//		System.out.println("-----MyApplicationRunner" + Arrays.asList(args));
//		System.out.println("+++++++++++WithoutInterfaceService:	" + withoutInterfaceService.test());
	}

}
