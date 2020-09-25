package org.sdjen.download.cache_sis.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface InitStartTimer {
	final static Logger logger = LoggerFactory.getLogger(InitStartTimer.class);

	public void restart(double hours) throws Throwable;
}
