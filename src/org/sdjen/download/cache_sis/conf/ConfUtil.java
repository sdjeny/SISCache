package org.sdjen.download.cache_sis.conf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class ConfUtil {
	private Properties properties;
	private File file;
	private static ConfUtil defaultConf;

	public static ConfUtil getDefaultConf() throws IOException {
		if (null == defaultConf) {
			defaultConf = new ConfUtil("conf.ini");
			if (defaultConf.getProperties().isEmpty()) {
				defaultConf.getProperties().setProperty("chatset", "gbk");
				defaultConf.getProperties().setProperty("save_path", "WEBCACHE");
				defaultConf.getProperties().setProperty("proxy", "127.0.0.1:9666");
				defaultConf.getProperties().setProperty("list_url", "http://www.sexinsex.net/bbs/forum-143-{0}.html");
				defaultConf.getProperties().setProperty("list_start", "10");
				defaultConf.getProperties().setProperty("list_end", "10");
				defaultConf.getProperties().setProperty("list_page_max", "50");
				defaultConf.getProperties().setProperty("retry_times", "5");
				defaultConf.getProperties().setProperty("retry_time_second", "30");
				defaultConf.getProperties().setProperty("timout_millisecond_connect", "10000");
				defaultConf.getProperties().setProperty("timout_millisecond_connectionrequest", "10000");
				defaultConf.getProperties().setProperty("timout_millisecond_socket", "10000");
				defaultConf.getProperties().setProperty("proxy_urls", "http://www.sexinsex.net");
				defaultConf.store();
			}
		}
		return defaultConf;
	}

	public ConfUtil(String path) throws IOException {
		properties = new Properties();
		file = new File(path);
		if (file.exists()) {
			FileReader fileReader = new FileReader(file);
			try {
				properties.load(fileReader);
			} finally {
				fileReader.close();
			}
		} else {
			file.createNewFile();
		}
	}

	public Properties getProperties() {
		return properties;
	}

	public void store() throws IOException {
		FileWriter writer = new FileWriter(file);
		try {
			properties.store(writer, "");
		} finally {
			try {
				writer.flush();
			} finally {
				writer.close();
			}
		}
	}
}
