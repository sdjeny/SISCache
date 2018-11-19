package org.sdjen.download.cache_sis;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Test;

public class DownloadListTest {

	@Test
	public void test() throws Throwable {
		new DownloadList("") {
			protected String getHTML(String uri) throws Throwable {
				return getContext(new File("b.htm"));
			};
		}.execute(1, 2);
	}

	private String getContext(File file) throws Throwable {
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
}
