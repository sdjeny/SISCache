package org.sdjen.download.cache_sis.tool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * 
 * 对字符串进行加解密和加解压
 * 
 * @author ***
 *
 */
@SuppressWarnings("restriction")
public class ZipUtil {

	private static Logger log = LoggerFactory.getLogger(ZipUtil.class);
	private static final String CHARSET = "UTF-8";

	/**
	 * 将字符串压缩后Base64
	 * 
	 * @param primStr
	 *            待加压加密函数
	 * @return
	 */
	public static String zipString(String primStr) {
		if (primStr == null || primStr.length() == 0) {
			return primStr;
		}
		ByteArrayOutputStream out = null;
		ZipOutputStream zout = null;
		try {
			out = new ByteArrayOutputStream();
			zout = new ZipOutputStream(out);
			zout.putNextEntry(new ZipEntry("0"));
			zout.write(primStr.getBytes(CHARSET));
			zout.closeEntry();
			return bytesToString(out.toByteArray());
		} catch (IOException e) {
			log.error("对字符串进行加压加密操作失败：", e);
			return null;
		} finally {
			if (zout != null) {
				try {
					zout.close();
				} catch (IOException e) {
					log.error("对字符串进行加压加密操作，关闭zip操作流失败：", e);
				}
			}
		}
	}

	/**
	 * 将压缩并Base64后的字符串进行解密解压
	 * 
	 * @param compressedStr
	 *            待解密解压字符串
	 * @return
	 */
	public static final String unzipString(String compressedStr) {
		if (compressedStr == null) {
			return null;
		}
		ByteArrayOutputStream out = null;
		ByteArrayInputStream in = null;
		ZipInputStream zin = null;
		String decompressed = null;
		try {
			byte[] compressed = stringToBytes(compressedStr);
			out = new ByteArrayOutputStream();
			in = new ByteArrayInputStream(compressed);
			zin = new ZipInputStream(in);
			zin.getNextEntry();
			byte[] buffer = new byte[1024];
			int offset = -1;
			while ((offset = zin.read(buffer)) != -1) {
				out.write(buffer, 0, offset);
			}
			decompressed = out.toString(CHARSET);
		} catch (IOException e) {
			log.error("对字符串进行解密解压操作失败：", e);
			decompressed = null;
		} finally {
			if (zin != null) {
				try {
					zin.close();
				} catch (IOException e) {
					log.error("对字符串进行解密解压操作，关闭压缩流失败：", e);
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					log.error("对字符串进行解密解压操作，关闭输入流失败：", e);
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					log.error("对字符串进行解密解压操作，关闭输出流失败：", e);
				}
			}
		}
		return decompressed;
	}

	public static String compress(String primStr) throws IOException {
		return compress(primStr, 9);
	}

	public static String compress(String primStr, int level) throws IOException {
		byte input[] = primStr.getBytes(CHARSET);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Deflater compressor = new Deflater(level);
		try {
			compressor.setInput(input);
			compressor.finish();
			final byte[] buf = new byte[2048];
			while (!compressor.finished()) {
				int count = compressor.deflate(buf);
				bos.write(buf, 0, count);
			}
		} finally {
			compressor.end();
		}
		return bytesToString(bos.toByteArray());
	}

	public static String bytesToString(byte[] bytes) throws IOException {
		 return new BASE64Encoder().encode(bytes);
//		return new String(bytes, CHARSET);
	}

	public static byte[] stringToBytes(String str) throws IOException {
		 return new BASE64Decoder().decodeBuffer(str);
//		return str.getBytes(CHARSET);
	}

	public static String uncompress(String compressedStr) throws DataFormatException, IOException {
		byte[] input = stringToBytes(compressedStr);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Inflater decompressor = new Inflater();
		try {
			decompressor.setInput(input);
			final byte[] buf = new byte[2048];
			while (!decompressor.finished()) {
				int count = decompressor.inflate(buf);
				bos.write(buf, 0, count);
			}
		} finally {
			decompressor.end();
		}
		return bos.toString(CHARSET);// bos.toByteArray();
	}

}
