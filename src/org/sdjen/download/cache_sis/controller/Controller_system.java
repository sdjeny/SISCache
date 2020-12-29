package org.sdjen.download.cache_sis.controller;

import org.sdjen.download.cache_sis.configuration.ConfigAsync;
import org.sdjen.download.cache_sis.configuration.ConfigHttputil;
import org.sdjen.download.cache_sis.configuration.ConfigMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@EnableAutoConfiguration
@RequestMapping("/system")
public class Controller_system {
	private final static Logger logger = LoggerFactory.getLogger(Controller_system.class);
	@Autowired
	private ConfigMain configMain;
	@Autowired
	ConfigAsync configAsync;
	@Autowired
	ConfigHttputil configHttputil;
//	@Autowired
//	private ContextRefresher contextRefresher;

	@RequestMapping("/refresh_config")
	@ResponseBody
	String refresh_config() {
		StringBuilder result = new StringBuilder();
//		result.append(contextRefresher.refresh());
		return result.toString();
	}
}
