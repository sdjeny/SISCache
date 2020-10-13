package org.sdjen.download.cache_sis.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class WithoutInterfaceService {
	final static Logger logger = LoggerFactory.getLogger(WithoutInterfaceService.class);

	public String test() {
		return "WithoutInterfaceService+++";
	}

	@Async("taskExecutor")
	public void async() {// 调用者必须是其他类
		try {
			Thread.sleep(1000l);
			logger.info("dd");
		} catch (InterruptedException e) {
		}
	}
}
