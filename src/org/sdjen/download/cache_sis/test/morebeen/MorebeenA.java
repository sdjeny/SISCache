package org.sdjen.download.cache_sis.test.morebeen;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("MorebeenA")
@Transactional
public class MorebeenA implements MoreBeenItfc {

	@Override
	public String getInfo() {
		return "A";
	}

}
