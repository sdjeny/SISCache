package org.sdjen.download.cache_sis.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.sdjen.download.cache_sis.conf.ConfUtil;

public class LogUtil {
	public static LogUtil errLog;
	public static LogUtil lstLog;
	public static LogUtil msgLog;

	public static void init() throws IOException {
		ConfUtil conf = ConfUtil.getDefaultConf();
		String save_path = conf.getProperties().getProperty("save_path");
		String charset = conf.getProperties().getProperty("chatset");
		File savePath = new File(save_path);
		if (!savePath.exists())
			savePath.mkdirs();
		lstLog = new LogUtil().setLogFile(save_path + "/list.csv").setChatset(charset);
		errLog = new LogUtil().setLogFile(save_path + "/err.log").setChatset(charset);
		refreshMsgLog();

//		msgLog = new LogUtil().setLogFile(save_path + "/download.log").setChatset(charset);
	}

	public static void refreshMsgLog() throws IOException {
		if (null != msgLog)
			msgLog.finish();
		ConfUtil conf = ConfUtil.getDefaultConf();
		String save_path = conf.getProperties().getProperty("save_path");
		String charset = conf.getProperties().getProperty("chatset");
		msgLog = new LogUtil().setLogFile(save_path + "/download_" + System.currentTimeMillis() + ".log")
				.setChatset(charset);
	}

	public static void finishAll() {
		errLog.finish();
		lstLog.finish();
		msgLog.finish();
	}

	private SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
	private FileOutputStream logOutputStream;
	private String chatset = "utf8";

	public LogUtil setChatset(String chatset) {
		this.chatset = chatset;
		return this;
	}

	public LogUtil setLogFile(String filePath) throws IOException {
		File log = new File(filePath);
		if (!log.exists())
			log.createNewFile();
		logOutputStream = new FileOutputStream(log, true);
		return this;
	}

	public void finish() {
		try {
			if (null != logOutputStream)
				logOutputStream.close();
		} catch (IOException e) {
		}
	}

	// @Override
	// protected void finalize() throws Throwable {
	// finish();
	// super.finalize();
	// }
	public void showMsg(Object pattern, Object... args) {
		StringBuilder builder = new StringBuilder(dateFormat.format(new Date()));
		builder.append("	,");
		if (null != args && args.length > 0 && pattern instanceof String) {
			builder.append(MessageFormat.format((String) pattern, args));
		} else {
			builder.append(pattern);
		}
		System.out.println(builder);
		if (null != logOutputStream) {
			try {
				logOutputStream.write(builder.toString().getBytes(chatset));
				logOutputStream.write('\n');
				logOutputStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
