package org.sdjen.download.cache_sis.test;

import java.io.FileInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.TextNode;

public class TestJsoup {
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

		FileInputStream inputStream = new FileInputStream("src/org/sdjen/download/cache_sis/test/sisdemo.html");
		String html = new String(inputStream.readAllBytes(), "GBK");
		inputStream.close();

		org.jsoup.nodes.Document doument = Jsoup.parse(html);
		for (org.jsoup.nodes.Element element : doument.select("#foruminfo").select("a")) {
			String href = element.attr("href");
			if (href.startsWith("forum-")) {
				href = href.substring("forum-".length());
				System.out.println(href.split("-")[0]);
			}
		}
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
					System.out.println(toTextOnly(comment));
				}
			} else {
			}
		}
	}
}
