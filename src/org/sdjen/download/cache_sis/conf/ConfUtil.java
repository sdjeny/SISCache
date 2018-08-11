package org.sdjen.download.cache_sis.conf;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class ConfUtil {
	private Properties properties;
	private File file;

	public ConfUtil(String path) throws Throwable {
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

	public void store() throws Throwable {
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
