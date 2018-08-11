package test.jsoup;

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
 * JSoup 网络爬虫工具类
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
	 * <span style="color:red;font-size:18px;">获取网站图片</span>
	 * 
	 * @param networkUrl
	 *            网站路径
	 * @param outPath
	 *            图片保存地址
	 * @throws IOException
	 */
	public static void getNetworkImage(String networkUrl, String outPath) throws IOException {
		// 输入输出流
		FileOutputStream outputStream = null;
		InputStream inputStream = null;
		BufferedInputStream bis = null;
		Document doument;
		Elements elements;
		try {
			// 获取网站资源
			doument = (Document) Jsoup.connect(networkUrl).get();
			// 获取网站资源图片
			elements = doument.select("img[src]");
			// 循环读取
			for (Element e : elements) {// 读取网站所有图片
				String outImage = UUID.randomUUID().toString().replaceAll("-", "") + ".jpg";
				// 创建连接
				URL imgUrl = new URL(e.attr("src"));
				// 获取输入流
				inputStream = imgUrl.openConnection().getInputStream();
				// 将输入流信息放入缓冲流提升读写速度
				bis = new BufferedInputStream(inputStream);
				// 读取字节娄
				byte[] buf = new byte[1024];
				// 生成文件
				outputStream = new FileOutputStream(outPath + outImage);
				int size = 0;
				// 边读边写
				while ((size = bis.read(buf)) != -1) {
					outputStream.write(buf, 0, size);
				}
				// 刷新文件流
				outputStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 释放资源 遵循先开后关原则
			if (outputStream != null)
				outputStream.close();
			if (bis != null)
				bis.close();
			if (inputStream != null)
				inputStream.close();
		}
	}
}