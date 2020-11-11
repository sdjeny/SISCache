package org.sdjen.download.cache_sis;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.jsoup.Jsoup;
import org.sdjen.download.cache_sis.configuration.ConfUtil;
import org.sdjen.download.cache_sis.configuration.ConfigMain;
import org.sdjen.download.cache_sis.http.DefaultCss;
//import org.sdjen.download.cache_sis.http.HttpFactory;
import org.sdjen.download.cache_sis.http.HttpUtil;
import org.sdjen.download.cache_sis.log.LogUtil;
//import org.sdjen.download.cache_sis.log.MapDBFactory;
import org.sdjen.download.cache_sis.store.IStore;
import org.sdjen.download.cache_sis.tool.Comparor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class DownloadSingle {
	private final static Logger logger = LoggerFactory.getLogger(DownloadSingle.class);
	// private String html = "";
	public String chatset = "utf8";
	private String save_path = "WEBCACHE";
	// private String sub_images = "images";
	// private String sub_html = "html";
	// private String sub_torrent = "torrent";
	private MessageDigest md5;
	// private long length_download;
	private long length_flag_min_byte = 20000;
	private long length_flag_max_byte = 70000;
	// private Lock lock_w_replace = new ReentrantReadWriteLock().writeLock();
//rmlck	private Lock lock_w_db = new ReentrantReadWriteLock().writeLock();
//	private Lock lock_w_html = new ReentrantReadWriteLock().writeLock();
//	private Object lock_sis = new Object();
	private Object lock_file = new Object();
//	private EntryData<String, Object> locks = new EntryData<>(new ConcurrentHashMap<>());
	private long count = 0;
	// private org.sdjen.download.cache_sis.log.CassandraFactory
	// cassandraFactory;
//	private IStore store = null;
//	private HttpFactory httpUtil;
	@Resource(name = "${definde.service.name.store}")
	private IStore store;
	@Autowired
	private HttpUtil httpUtil;
	@Autowired
	private ConfigMain configMain;
	@Resource(name = "downloadSingleExecutor")
	private ThreadPoolTaskExecutor executor;

	public void init() {
		this.count = 0;
//		locks.getData().clear();
	}

	public DownloadSingle() throws Exception {
		System.out.println(">>>>>>>>>>>>>>>>>>DownloadSingle");
		ConfUtil conf = ConfUtil.getDefaultConf();
		md5 = MessageDigest.getInstance("MD5");
		chatset = conf.getProperties().getProperty("chatset");
		save_path = conf.getProperties().getProperty("save_path");
		boolean isStore = false;
		try {
			length_flag_min_byte = Long.valueOf(conf.getProperties().getProperty("length_flag_min_byte"));
		} catch (Exception e) {
			conf.getProperties().setProperty("length_flag_min_byte", String.valueOf(length_flag_min_byte));
			isStore = true;
		}
		try {
			length_flag_max_byte = Long.valueOf(conf.getProperties().getProperty("length_flag_max_byte"));
		} catch (Exception e) {
			conf.getProperties().setProperty("length_flag_max_byte", String.valueOf(length_flag_max_byte));
			isStore = true;
		}
		if (isStore)
			conf.store();
		// cassandraFactory = CassandraFactory.getDefaultFactory();
	}

	/**
	 * 替换文件系统不支持的字符
	 * 
	 * @param name
	 * @return
	 */
	private String getFileName(String name) {
		return name//
				.replace('\\', ' ')//
				.replace('/', ' ')//
				.replace(':', ' ')//
				.replace('*', ' ')//
				.replace('?', ' ')//
				.replace('<', ' ')//
				.replace('>', ' ')//
				.replace('|', ' ')//
				.replace('"', ' ')//
		;
	}

	public static void main(String[] args) throws Throwable {
		ConfUtil.getDefaultConf().getProperties().setProperty("retry_times", "1");
		ConfUtil.getDefaultConf().getProperties().setProperty("retry_time_second", "1");
		// ConfUtil.getDefaultConf().getProperties().setProperty("chatset",
		// "utf8");
		// ConfUtil.getDefaultConf().getProperties().setProperty("list_url",
		// "https://club.autohome.com.cn/bbs/thread/");
		LogUtil.init();
		// MapDBFactory.init();
//		HttpFactory httpUtil = new HttpFactory();
		try {
			// System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");
			// Security.setProperty("jdk.tls.disabledAlgorithms","SSLv3, DH
			// keySize < 768");
			DownloadSingle util = new DownloadSingle();// .setHttpUtil(httpUtil);
			// util.startDownload("http://www.sexinsex.net/bbs/thread-6720446-1-2000.html",
			// "370013862.html", "U");
			// util.downloadFile("http://img599.net/images/2013/06/02/CCe908c.th.jpg",
			// "1.jpg");
			// util.downloadFile("https://www.caribbeancom.com/moviepages/022712-953/images/l_l.jpg",
			// "2.jpg");
			// util.downloadFile("https://www.caribbeancom.com/moviepages/022712-953/images/l_l.jpg",
			// "rr", "2.jpg");
			// util.downloadFile("https://e.piclect.com/o180829_110f6.jpg",
			// "rr", "11.jpg", false);
			// util.downloadFile("https://www1.wi.to/2018/03/29/87188c533dce9cfaa1e34992c693a5d5.jpg",
			// "rr", "11.jpg");
			// https://www1.wi.to/2018/03/29/87188c533dce9cfaa1e34992c693a5d5.jpg
			// https://www1.wi.to/2018/03/29/04f7c405227da092576b127e640d07f8.jpg
		} finally {
//			httpUtil.finish();
			// MapDBFactory.finishAll();
			LogUtil.finishAll();
		}
	}

	private synchronized String getMD5(byte[] bytes) {
		return new BigInteger(1, md5.digest(bytes)).toString(Character.MAX_RADIX);
	}

	public Long startDownload(final String type, String fid, final String id, final String page, final String url,
			String title, String dateStr) throws Throwable {
		try {
			if (Integer.valueOf(page) > 30)
				return null;
		} catch (Exception e2) {
		}
		long startTime = System.currentTimeMillis();
		title = getFileName(title);
//		String key = store.getKey(id, page, url, title, dateStr);
		long l = System.currentTimeMillis();
		String tmp_html = type.contains("cover")// 覆盖模式
				? null // 不管是否存在，都重新读取
				: store.lookupLocalHtml(id, page);// 否则获取本地文件
		l = System.currentTimeMillis() - l;
		if ((type.isEmpty() || Integer.valueOf(page) > 1) && null != tmp_html)// 不是特殊模式且文件已存在!
			return null;// 跳过
		// File newFile = new File(save_path + "/" + sub_html + "/" + title);
		// if (newFile.exists()) {
		// return false;
		// }
		long d = System.currentTimeMillis();
		if (null == tmp_html && null != url && !url.isEmpty()) {
//			synchronized (lock_sis) {
			tmp_html = httpUtil.getHTML(url);// 覆盖模式下会进这里，本地没有再从网络取
//			}
//			try {
//				lock_w_html.lock();
//			} finally {
//				lock_w_html.unlock();
//			}
		}
		d = System.currentTimeMillis() - d;
		if (null == tmp_html)
			return null;
		String html = tmp_html;
		org.jsoup.nodes.Document doument = Jsoup.parse(html);
		if (null == title) {
			org.jsoup.nodes.Element h1 = doument.select("div.mainbox").select("h1").first();
			if (null != h1)
				title = h1.ownText();
		}
		if (null == dateStr && "1".equals(page)) {
			for (org.jsoup.nodes.Element postinfo : doument.select("div.mainbox.viewthread")//// class=mainbox的div
					.select("table")//
					.select("tbody")//
					.select("tr")//
					.select("td.postcontent")//
					.select("div.postinfo")//
			) {
				String floor = "";
				org.jsoup.nodes.Element temp = postinfo.select("strong").first();
				if (null != temp) {
					floor = temp.ownText();
					if ("1楼".equals(floor)) {
						dateStr = postinfo.ownText().replace("发表于 ", "");
						dateStr = dateStr.substring(0, dateStr.indexOf(" ")).trim();
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
						dateStr = dateFormat.format(dateFormat.parse(dateStr));
						break;
					}
				}
			}
		}
		String subKey;
		try {
			subKey = dateStr.substring(0, Math.min(7, dateStr.length())) + "/"
					+ dateStr.substring(8, dateStr.length()).replace("-", "");
		} catch (Exception e1) {
			subKey = "unknow";
		}
		final String sub_images = "images/" + subKey;
		// String sub_html = "html/" + subKey;
		final String sub_torrent = "torrent/" + subKey;
		long fc = System.currentTimeMillis();
		synchronized (lock_file) {
			File savePath = new File(save_path);
			if (!savePath.exists())
				savePath.mkdirs();
			for (String sub : new String[] { sub_images, sub_images + "/min", sub_images + "/mid", sub_images + "/max"//
					, sub_torrent, sub_torrent + "/min", sub_torrent + "/mid", sub_torrent + "/max"//
					// , sub_bttrack//
					// , sub_html //
			}) {
				File f = new File(savePath + "/" + sub);
				if (!f.exists()) {
					f.mkdirs();
					store.msg("{0}创建文件夹", f);
				}
			}
		}
		fc = System.currentTimeMillis() - fc;
//		try {
//			lock_w_html.lock();
//		} finally {
//			lock_w_html.unlock();
//		}
		Long length_download = 0l;
		StringBuilder msg = new StringBuilder(MessageFormat.format("{0} {1}	{2}", dateStr, title, url));
		List<Future<String[]>> resultList = new ArrayList<Future<String[]>>();
		for (org.jsoup.nodes.Element a : doument//
				.select("div.mainbox.viewthread")//
				.select("td.postcontent")//
				.select("div.postmessage.defaultpost")//
				.select("div.box.postattachlist")//
				.select("dl.t_attachlist")//
				.select("a[href]")//
		) {
			String href = a.attr("href");
			final String text = a.text();
			if (href.contains("attachment.php?aid=")) {
				resultList.add(executor.submit(() -> {
					String newName = href.contains("&") ? href.substring(0, href.indexOf("&")) : href;
					newName = getFileName(
							"(" + newName.substring(newName.lastIndexOf("=") + 1, newName.length()) + ")" + text);
					String downloadUrl = httpUtil.joinUrlPath(url, href);
					try {
						newName = downloadFile(downloadUrl, sub_torrent, newName, type.contains("torrent"));
						replace(a, "href", newName);
					} catch (Throwable ex) {
						ex.printStackTrace();
						store.err("异常	{0}	{1}", downloadUrl, ex);
					}
					return new String[] { href, newName };
				}));
			}
		}
		if (!configMain.getFids_unreplace_img().contains(fid)) {
			for (org.jsoup.nodes.Element e : doument.select("img[src]")) {
				final String src = e.attr("src");
				if (src.contains("../images/20") //
						|| src.contains("../images/unknow")//
						|| src.contains("../torrent/20") //
						|| src.contains("../torrent/unknow")//
				)
					continue;// 本地缓存文件就过了吧
				resultList.add(executor.submit(() -> {
					String downloadUrl = httpUtil.joinUrlPath(url, src);
					String newName;// = getMD5(downloadUrl.getBytes("utf-8"));
					if (src.contains("."))
						newName = getFileName(src.substring(src.lastIndexOf("."), src.length()));
					else {
						newName = ".jpg";
					}
					try {
						newName = downloadFile(downloadUrl, sub_images, newName, type.contains("image"));
						replace(e, "src", newName);
					} catch (Throwable ex) {
						ex.printStackTrace();
						store.err("异常	{0}	{1}", downloadUrl, ex);
					}
					// if (!newName.equals(downloadUrl))
					// replaceAll(src, newName);
					return new String[] { src, newName };
				}));
			}
		}
		for (Future<String[]> fs : resultList) {
			try {
				String[] names = fs.get(1, TimeUnit.MINUTES);// 1分钟超时
				if (null != names && !names[0].equals(names[1])) {
					// replaceAll(names[0], names[1]);
				}
			} catch (java.util.concurrent.TimeoutException e) {
				fs.cancel(false);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
			}
		}
		tmp_html = doument.html();
		if (!type.isEmpty()// 特殊模式
				&& !type.contains("cover")// 非覆盖模式
				&& Comparor.equals(tmp_html, html)// 没变化
		) {
			return null;
		}
		long s = System.currentTimeMillis();
		try {
			html = tmp_html;
			int cssLen = 0;
			for (org.jsoup.nodes.Element e : doument.select("head").select("style")) {
				cssLen += e.text().getBytes().length;
			}
			int length = html.length();
			length_download += length;
			if (true// 先忽略长度，存了再说
					|| (html.length() - cssLen) > (55000 - DefaultCss.getLength())) {
				try {
					// rmlck lock_w_db.lock();
					store.saveHtml(id, page, url, title, dateStr, html);
				} finally {
					// rmlck lock_w_db.unlock();
				}
				// newFile.createNewFile();
			} else {
				msg.append(MessageFormat.format("	长度过短	{0}	{1}({2}/{3})", length, title, length,
						(60000 - DefaultCss.getLength())));
				store.err("长度过短	{0}	{1}	{2}", length, title, url);
				return null;
			}
		} catch (Throwable e) {
			// e.printStackTrace();
			msg.append(MessageFormat.format("	异常	{0}", e));
			store.err("异常	{0}	{1}		{2}", title, url, e);
			return null;
		} finally {
			// store.msg("本次下载 {0} byte", length_download);
			try {
				// rmlck lock_w_html.lock();
				s = System.currentTimeMillis() - s;
				store.msg("{0}	(l:{3} + d:{4} + f:{6}+ s:{5}	)	耗时:{1}	{2}", (++count),
						(System.currentTimeMillis() - startTime), msg, l, d, s, fc);
			} finally {
				// rmlck lock_w_html.unlock();
			}
			// lock_w_html.unlock();
		}
		return length_download;
	}

	// public long getLength_download() {
	// return length_download;
	// }

	// private void replaceAll(String src, String targ) {
	// lock_w_replace.lock();
	// if (targ.startsWith("http")) {
	// html = html.replace("\"" + src + "\"", "\"" + targ + "\"");
	// } else {
	// html = html.replace("\"" + src + "\"", "\"../../../" + targ + "\"");
	// }
	// lock_w_replace.unlock();
	// }

	private void replace(org.jsoup.nodes.Element element, String attributeKey, String newAttribute) {
		synchronized (element) {
			String s = element.attr(attributeKey);
			if (!newAttribute.equals(s)) {
				if (!newAttribute.startsWith("http"))
					newAttribute = "../../../" + newAttribute;
				// System.out.println(attributeKey + " " + s + " -> " +
				// newAttribute);
				element.attr(attributeKey, newAttribute);
			}
		}
	}

	private boolean checkFile(boolean reload, String path) {
		if (!reload && path.startsWith("http"))
			return true;// 不需要重加载且路径为网址的，不校验
		return new File(save_path + "/" + path).exists();
	}

	private String downloadFile(final String url, final String path, final String name, boolean reload)
			throws Throwable {
		String result = store.lookupURL_Path(url);// MapDBFactory.getUrlDB().get(url);
		if (null != result && reload && result.equals(url))
			result = null;
		if (null == result || !checkFile(reload, result)) {
			HttpUtil.Executor<String> executor = new HttpUtil.Executor<String>() {
				public void execute(byte[] bytes) throws Throwable {
					setResult(null);
					long fmd = System.currentTimeMillis();
					String md5 = getMD5(bytes);
					fmd = System.currentTimeMillis() - fmd;
					long fl = System.currentTimeMillis();
					String result = store.lookupMD5_Path(md5);// MapDBFactory.getFileDB().get(md5);
					fl = System.currentTimeMillis() - fl;
					if (null == result || !checkFile(reload, result)) {
						long fd = System.currentTimeMillis();
						result = path;
						if (bytes.length < length_flag_min_byte)
							result += "/min";
						else if (bytes.length > length_flag_max_byte)
							result += "/max";
						else
							result += "/mid";
						result += "/";
						if (name.startsWith("."))
							result += md5;
						result += name;
						File file = new File(save_path + "/" + result);
						if (!file.exists()) {
							FileOutputStream fos = new FileOutputStream(file);
							try {
								fos.write(bytes);
							} finally {
								try {
									fos.close();
								} catch (Exception e) {
								}
							}
							// length_download += bytes.length;
						}
						// lock_w_mapdb.lock();
						// MapDBFactory.getFileDB().put(md5, result);
						// mapDBUtil.commit();
						// lock_w_mapdb.unlock();
						fd = System.currentTimeMillis() - fd;
						long fs = System.currentTimeMillis();
						store.saveMD5(md5, result);
						fs = System.currentTimeMillis() - fs;
						logger.debug("	md5:{}	lookup:{}	download:{}	save:{}	+{}	->{}", fmd, fl, fd, fs, url,
								result);
					}
					setResult(result);
				}
			};
			executor.setResult(null);
			try {
				result = null;
				if (path.startsWith("torrent")) {
//					synchronized (lock_sis) {
					httpUtil.retry(() -> {
						synchronized (httpUtil.getLock_sis()) {
							httpUtil.execute(url, executor);
							return null;
						}
					});
//					}
//					try {
//						lock_w_html.lock();
//					} finally {
//						lock_w_html.unlock();
//					}
				} else
					httpUtil.execute(url, executor);
				result = executor.getResult();
			} catch (Throwable e) {
				result = url;
				store.err("异常	{0}	{1}", url, e);
				// e.printStackTrace();
			} finally {
				if (null == result)
					result = url;
				store.saveURL(url, result);
			}
		}
		return result;
	}
}
