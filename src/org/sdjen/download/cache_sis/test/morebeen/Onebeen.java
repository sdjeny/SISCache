package org.sdjen.download.cache_sis.test.morebeen;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Onebeen implements OneBeenItfc {

	@Override
	public String getInfo() {
		return "One";
	}

}
