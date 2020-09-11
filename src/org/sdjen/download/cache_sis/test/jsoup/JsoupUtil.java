package org.sdjen.download.cache_sis.test.jsoup;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * JSoup �������湤����
 * 
 * @author Cloud
 * @data 2016-11-21 JsoupUtil
 */
public class JsoupUtil {
	public static void main(String[] args) throws Exception {
		System.out.println("--start--");
		JsoupUtil.getNetworkImage("http://www.tooopen.com/img/87.aspx", "E://");
		System.out.println("--end--");
	}

	/**
	 * <span style="color:red;font-size:18px;">��ȡ��վͼƬ</span>
	 * 
	 * @param networkUrl
	 *            ��վ·��
	 * @param outPath
	 *            ͼƬ�����ַ
	 * @throws IOException
	 */
	public static void getNetworkImage(String networkUrl, String outPath) throws IOException {
		// ���������
		FileOutputStream outputStream = null;
		InputStream inputStream = null;
		BufferedInputStream bis = null;
		Document doument;
		Elements elements;
		try {
			// ��ȡ��վ��Դ
			doument = (Document) Jsoup.connect(networkUrl).get();
			// ��ȡ��վ��ԴͼƬ
			elements = doument.select("img[src]");
			// ѭ����ȡ
			for (Element e : elements) {// ��ȡ��վ����ͼƬ
				String outImage = UUID.randomUUID().toString().replaceAll("-", "") + ".jpg";
				// ��������
				URL imgUrl = new URL(e.attr("src"));
				// ��ȡ������
				inputStream = imgUrl.openConnection().getInputStream();
				// ����������Ϣ���뻺����������д�ٶ�
				bis = new BufferedInputStream(inputStream);
				// ��ȡ�ֽ�¦
				byte[] buf = new byte[1024];
				// �����ļ�
				outputStream = new FileOutputStream(outPath + outImage);
				int size = 0;
				// �߶���д
				while ((size = bis.read(buf)) != -1) {
					outputStream.write(buf, 0, size);
				}
				// ˢ���ļ���
				outputStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// �ͷ���Դ ��ѭ�ȿ����ԭ��
			if (outputStream != null)
				outputStream.close();
			if (bis != null)
				bis.close();
			if (inputStream != null)
				inputStream.close();
		}
	}
}