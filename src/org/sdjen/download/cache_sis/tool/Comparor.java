package org.sdjen.download.cache_sis.tool;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Comparor {
	private final static Logger logger = Logger.getLogger(Comparor.class.toString());

	public static synchronized boolean equals(String t1, String t2) {
		boolean result = t1.equals(t2);
		if (result)
			return result;
		if (!result) {
			List<String> l1 = _2List(t1);
			List<String> l2 = _2List(t2);
			result = l1.size() == l2.size();
			if (!result)
				return result;
			int count = Math.min(l1.size(), l2.size());
			for (int i = 0; i < count; i++) {
				String a1 = l1.get(i);
				String a2 = l2.get(i);
				if (!a1.equals(a2)) {
					result = false;
					logger.fine("[" + a1 + "]\n[" + a2 + "]\n--------------------------------------");
				}
			}
		}
		return result;
	}

	private static List<String> _2List(String text) {
		List<String> result = new ArrayList<>();
		for (String row : text.split("\n")) {
			row = row.trim();
			if (!row.isEmpty())
				result.add(row);
		}
		return result;
	}
}
