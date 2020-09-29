package org.sdjen.download.cache_sis.util;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.tool.ZipUtil;

public class JsoupAnalysisor {
	public final static String KEYFORMAT = "@!{{$1236547890JAJDYCBNEDYUEJFCLEOWHCS$%s$HUHFUSADKASHJDSAJASJ$}}~!@";

	public static String toTextOnly(org.jsoup.select.Elements elements) {
		StringBuilder result = new StringBuilder(elements.text());
		elements.select("[src]").forEach(e -> result.append(' ').append(e.attr("src")));
		elements.select("[href]").forEach(e -> result.append(' ').append(e.attr("href")));
		return result.toString();
	}

	public static void main(String[] args) throws Throwable {
		for (File f : new File(new File("").getAbsolutePath()).listFiles((dir, name) -> name.startsWith("sisdemo"))) {
			System.out.println(f);
			StringBuffer stringBuffer = new StringBuffer();
			Files.readAllLines(Paths.get(f.toURI()), Charset.forName("GBK")).forEach(str -> stringBuffer.append(str));
			Map<String, String> details = split(stringBuffer.toString(), false);
			String temp = stringBuffer.toString();
			System.out.println(temp.getBytes().length + "	->	" + ZipUtil.compress(temp).length);
			temp = JsonUtil.toJson(details);
			System.out.println(temp.getBytes().length + "	->	" + ZipUtil.compress(temp).length);
		}
//		com.google.common.io.Files.write(details.get("template").getBytes("GBK"), new File("template.html"));
	}

	private void analysis(org.jsoup.nodes.Document doument) {
		org.jsoup.nodes.Element title = doument.select("div.mainbox").select("h1").first();
		System.out.println(title.textNodes());
		for (org.jsoup.nodes.Element element : doument.select("#foruminfo").select("a")) {
			String href = element.attr("href");
			if (href.startsWith("forum-")) {
				href = href.substring("forum-".length());
				System.out.println(href.split("-")[0]);
			}
		}
		System.out.println(doument.select("#foruminfo #nav").first().textNodes());
		for (org.jsoup.nodes.Element element : doument.select("#foruminfo #nav")) {
			System.out.println(element.text());
		}

		for (org.jsoup.nodes.Element postcontent : doument.select("div.mainbox.viewthread")//// class=mainbox的div
				.select("table")//
				.select("tbody")//
				.select("tr")//
				.select("td.postcontent")//
		) {
			String floor = "";
			for (org.jsoup.nodes.Element postinfo : postcontent.select("div.postinfo")) {
				org.jsoup.nodes.Element temp = postinfo.select("strong").first();
				if (null != temp) {
					floor = temp.ownText();
				}
			}
			if (floor.isEmpty())
				continue;
			if ("1楼".equals(floor)) {
				for (org.jsoup.nodes.Element comment : postcontent.select("div.postmessage.defaultpost")) {
//					System.out.println(toTextOnly(comment));
				}
			} else {
			}
		}

	}

	public static Map<String, String> split(String html, boolean splitTemplate) {
		org.jsoup.nodes.Document doument = Jsoup.parse(html);
		doument.outputSettings(new Document.OutputSettings().prettyPrint(false));
		try {
			return split(doument, splitTemplate);
		} finally {
			System.out.println(toTextOnly(doument.select("div.postmessage.defaultpost")).length());
		}
	}

	public static Map<String, String> split(org.jsoup.nodes.Document doument, boolean splitTemplate) {
		Map<String, String> result = new LinkedHashMap<>();
		for (org.jsoup.nodes.Element element : doument.select("div#wrapper form")) {
			element.select("[onclick]").forEach(e -> e.removeAttr("onclick"));
			element.select("[onload]").forEach(e -> e.removeAttr("onload"));
			element.select("td.postauthor").forEach(e -> e.children().stream()
					.filter(c -> !"cite".equalsIgnoreCase(c.tagName())).forEach(c -> c.remove()));
			element.select("td.postcontent div.postinfo").forEach(e -> e.children().stream()
					.filter(c -> !"strong".equalsIgnoreCase(c.tagName())).forEach(c -> c.remove()));
			element.select(".postratings,.postactions").forEach(c -> c.remove());
//			element.select("#:contains(ad_thread)").forEach(System.out::println);
			result.put("form", element.html());
		}
		for (org.jsoup.nodes.Element element : doument.select("div#wrapper div#foruminfo")) {
			result.put("foruminfo", element.html());
		}
		for (org.jsoup.nodes.Element element : doument.select("div#wrapper div.maintable")) {
			result.put("maintable", element.html());
		}
		for (org.jsoup.nodes.Element element : doument.select("meta")) {
			if ("description".equals(element.attr("name")))
				result.put("description", element.attr("content"));
		}
		for (org.jsoup.nodes.Element element : doument.select("title")) {
			result.put("title", element.text());
		}
		if (splitTemplate) {
			String html = doument.html();
			for (Entry<String, String> entry : result.entrySet()) {
				System.out.println(entry.getKey() + ":	" + html.contains(entry.getValue()));
				html = html.replace(entry.getValue(), String.format(KEYFORMAT, entry.getKey()));
			}
			doument = Jsoup.parse(html);
			doument.select("img").forEach(e -> e.remove());
			result.put("template", doument.html());
		}
		return result;
	}
}
