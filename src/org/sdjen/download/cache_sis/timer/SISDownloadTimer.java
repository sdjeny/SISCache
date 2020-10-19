package org.sdjen.download.cache_sis.timer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.Resource;

import org.sdjen.download.cache_sis.DownloadList;
import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.store.IStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("SISDownloadTimer")
public class SISDownloadTimer implements InitStartTimer {
	private Timer timer;
	@Autowired
	private DownloadList downloadList;
	@Resource(name = "${definde.service.name.store}")
	private IStore store;

	public SISDownloadTimer() {
		System.out.println(">>>>>>>>>>>>SISDownloadTimer");
	}

	public void restart(double hours) {
		if (null != timer) {
			timer.cancel();
			timer.purge();
		}
		(timer = new Timer("定时下载" + System.currentTimeMillis())).schedule(getTimerTask(), 0, (long) (hours * 3600000));// 2hours
	}

	public TimerTask getTimerTask() {

		String rangestr = null;
		try {
			rangestr = ConfUtil.getDefaultConf().getProperties().getProperty("times_ranges");
		} catch (Exception e) {
		}
		if (null == rangestr) {
			rangestr = "~1~30|torrent~1~5|torrent,image~1~5|cover~5~10";
			try {
				ConfUtil.getDefaultConf().getProperties().setProperty("times_ranges", rangestr);
				ConfUtil.getDefaultConf().store();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		List<String[]> ranges = new ArrayList<>();
		for (String s : rangestr.split("\\|"))
			ranges.add(s.split("~"));
		return new TimerTask() {
			Long times = 0l;

			@Override
			public void run() {
				Map<String, Object> last;
				try {
					last = store.getLast("download_list");
				} catch (Throwable e2) {
					last = null;
				}
				if (null != last) {
					if (last.containsKey("running") && (Boolean) last.get("running")) {
						logger.info(">>>>>>>>>>>>download_list is Running");
						return;
					}
				}
				synchronized (times) {
					String[] range = ranges.get((int) (times % ranges.size()));
					String type = (String) range[0];
					int from = 1, to = 30;
					try {
						from = Integer.valueOf(range[1]);
					} catch (Exception e1) {
					}
					try {
						to = Integer.valueOf(range[2]);
					} catch (Exception e1) {
					}
					logger.info(times + "	" + type + "	" + from + "	" + to);
					try {
						downloadList.execute(type, from, to);
						times++;
					} catch (Throwable e) {
						e.printStackTrace();
					} finally {
					}
				}
			}
		};
	}
}
