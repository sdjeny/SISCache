package org.sdjen.download.cache_sis.test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;

public class TestJsoup {
	public final static String KEYFORMAT = "@!{{$1236547890JAJDYCBNEDYUEJFCLEOWHCS$%s$HUHFUSADKASHJDSAJASJ$}}~!@";

	static String toTextOnly(org.jsoup.nodes.Node comment) {
		if (comment instanceof TextNode) {
			return ((TextNode) comment).text();
		} else {
			StringBuilder result = new StringBuilder();
			comment.childNodes().forEach(child -> result.append(toTextOnly(child)).append(' '));
			return result.toString().trim();
		}
	}

	public static void main(String[] args) throws Throwable {
		StringBuffer stringBuffer = new StringBuffer();
		Files.readAllLines(Paths.get("sisdemo_8720516.html"), Charset.forName("GBK"))
				.forEach(str -> stringBuffer.append(str));
		Map<String, String> details = analysis(stringBuffer.toString());
//		com.google.common.io.Files.write(details.get("template").getBytes("GBK"), new File("template.html"));
	}

	public static Map<String, String> analysis(String html) {
		org.jsoup.nodes.Document doument = Jsoup.parse(html);
		doument.outputSettings(new Document.OutputSettings().prettyPrint(false));
		html = doument.html();
		if (false) {
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
		Map<String, String> details = new LinkedHashMap<>();
		for (org.jsoup.nodes.Element element : doument.select("div#wrapper form")) {
			details.put("form", element.html());
		}
		for (org.jsoup.nodes.Element element : doument.select("div#wrapper div#foruminfo")) {
			details.put("foruminfo", element.html());
		}
		for (org.jsoup.nodes.Element element : doument.select("div#wrapper div.maintable")) {
			details.put("maintable", element.html());
		}
		for (org.jsoup.nodes.Element element : doument.select("meta")) {
			if ("description".equals(element.attr("name")))
				details.put("description", element.attr("content"));
		}
		for (org.jsoup.nodes.Element element : doument.select("title")) {
			details.put("title", element.text());
		}
		for (Entry<String, String> entry : details.entrySet()) {
			html = html.replace(entry.getValue(), String.format(KEYFORMAT, entry.getKey()));
		}
		doument = Jsoup.parse(html);
		doument.select("img").forEach(e -> e.remove());
		details.put("template", doument.html());
		return details;
	}
}
