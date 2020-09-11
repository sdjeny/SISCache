package org.sdjen.download.cache_sis.test.morebeen;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MorebeenB implements MoreBeenItfc {

	@Override
	public String getInfo() {
		return "B";
	}

}
