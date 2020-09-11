package org.sdjen.download.cache_sis.store;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.sdjen.download.cache_sis.conf.ConfUtil;
import org.sdjen.download.cache_sis.log.LogUtil;

public abstract class Store_File implements IStore {
	public String chatset = "utf8";
	private String save_path = "C:\\Users\\jimmy.xu\\Downloads\\htmlhelp";

	public Store_File() throws IOException {
		ConfUtil conf = ConfUtil.getDefaultConf();
		chatset = conf.getProperties().getProperty("chatset");
		save_path = conf.getProperties().getProperty("save_path");
	}

	private String getKey(final String id, final String page, final String url, String title, String dateStr) {
		String subKey;
		try {
			subKey = dateStr.substring(0, Math.min(7, dateStr.length())) + "/" + dateStr.substring(8, dateStr.length()).replace("-", "");
		} catch (Exception e1) {
			subKey = "unknow";
		}
		String sub_html = "html/" + subKey;
		return save_path + "/" + sub_html + "/" + title;
	}

	@Override
	public String getLocalHtml(final String id, final String page, final String url, String title, String dateStr) throws Throwable {
		File file = new File(getKey(id, page, url, title, dateStr));
		if (!file.exists())
			return null;
		String charset = "GBK";
		long fileByteLength = file.length();
		byte[] content = new byte[(int) fileByteLength];
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(content);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fileInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return new String(content, charset);

	}

	public void saveHtml(final String id, final String page, final String url, String title, String dateStr, String html) throws Throwable {
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(getKey(id, page, url, title, dateStr)),
				ConfUtil.getDefaultConf().getProperties().getProperty("chatset"));
		BufferedWriter bw = new BufferedWriter(writer);
		bw.write(html);
		bw.close();
		writer.close();
	}

	@Override
	public void msg(Object pattern, Object... args) {
		LogUtil.msgLog.showMsg(pattern, args);

	}

	@Override
	public void err(Object pattern, Object... args) {
		LogUtil.errLog.showMsg(pattern, args);
	}

	@Override
	public void refreshMsgLog() {
		try {
			LogUtil.refreshMsgLog();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
