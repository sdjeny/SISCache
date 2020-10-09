package org.sdjen.download.cache_sis.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.sdjen.download.cache_sis.json.JsonUtil;
import org.sdjen.download.cache_sis.tool.ZipUtil;
import org.springframework.util.StringUtils;

import com.google.common.io.CharStreams;

public class JsoupAnalysisor {
	public final static String KEYFORMAT = "@!{{$1236547890JAJDYCBNEDYUEJFCLEOWHCS$%s$HUHFUSADKASHJDSAJASJ$}}~!@";

	private static String template;

	public static String toTextOnly(org.jsoup.select.Elements elements) {
		StringBuilder result = new StringBuilder(elements.text());
		elements.select("[src]").forEach(e -> result.append(' ').append(e.attr("src")));
		elements.select("div.postmessage.defaultpost div.box.postattachlist dl.t_attachlist a[href]")
				.forEach(e -> result.append(' ').append(e.attr("href")));
		return result.toString();
	}

	private static String readFile(File f) throws IOException {
		StringBuffer result = new StringBuffer();
		Files.readAllLines(Paths.get(f.toURI()), Charset.forName("GBK")).forEach(str -> result.append(str));
		return result.toString();
	}

	private static String getTemplate() throws IOException {
		if (null == template) {
			File file = new File("template.html");
			template = CharStreams.toString(new InputStreamReader(
					file.exists() ? new FileInputStream(file)
							: JsoupAnalysisor.class.getClassLoader().getResource("template.html").openStream(),
					Charset.forName("GBK")));
		}
		return template;
	}

	public static String restoreToHtml(Map<String, Object> details) throws IOException {
		String context = getTemplate();
		details.put("foruminfo", "<div id='nav'>" + details.get("foruminfo") + "</div>");
		StringBuilder form = new StringBuilder();
		{
			form.append("<div class='mainbox viewthread'>");
			{
				String title_tmp = (String) details.get("title");
				title_tmp = title_tmp.substring(0, title_tmp.lastIndexOf(" - "));
				String title = title_tmp.substring(0, title_tmp.lastIndexOf(" - "));
				form.append(MessageFormat.format(
						"<span class=\"headactions\">	<a href=\"viewthread.php?action=printable&amp;tid={0}\" target=\"_blank\" class=\"notabs\">{0}</a></span>",
						details.get("id")));
				form.append(MessageFormat.format(
						"<h1><a href=\"forumdisplay.php?fid={0}&amp;filter=type&amp;typeid={1}\">{1}</a>{2}</h1>",
						details.get("fid"), details.get("type"), title));
				List<Map<String, String>> contents = (List<Map<String, String>>) details.get("contents");
				if (null != contents && !contents.isEmpty()) {
					form.append("<table cellspacing=\"0\" cellpadding=\"0\">");
					contents.forEach(content -> {
						form.append("<tr>");
						form.append(MessageFormat.format(
								"<td class=\"postauthor\" style=\"width:0\"><cite><a target=\"_blank\" class=\"dropmenu\">{0}</a></cite><p><em><font color=\"skyblue\">{1}</font></em></p></td>",
								content.get("author"), content.get("level")));// 隐藏作者列
						form.append("<td class=\"postcontent\">");
						form.append(MessageFormat.format(
								"<div class=\"postinfo\"> <strong>{0}</strong>{1} <em>{2} {3}</em></div>",
								content.get("floor"), content.get("datetime"), content.get("author"),
								content.get("level")));// 在info增加作者列
						if ("1楼".equals(content.get("floor"))) {
							form.append(MessageFormat
									.format("<div class=\"postmessage defaultpost firstfloor\"><h2>{0}</h2>", title));
						} else {
							form.append("<div class=\"postmessage defaultpost\">");
						}
						form.append(MessageFormat.format("<div class=\"t_msgfont\">{0}</div>", content.get("message")));
						if (!StringUtils.isEmpty(content.get("attachlist"))) {
							form.append(MessageFormat.format(
									"<div class=\"box postattachlist\"><h4>附件</h4><dl class=\"t_attachlist\"><dt>{0}<em>{1}</em></dt></dl></div>",
									content.get("attachlist"), content.get("attachlist_size")));
						}
						form.append("</div></td></tr>");
					});
					form.append("</table>");
				}
				Map<String, String> actions = (Map<String, String>) details.get("actions");
				if (null != actions && !actions.isEmpty()) {
					form.append("<div class=\"postinfo postactions\" style=\"height:auto\">其他主题:<br>");
					actions.forEach((k, v) -> form.append(MessageFormat.format(
							"<a style=\"float:left;width:30%\" href=\"viewthread.php?tid={0}\" target=\"_blank\">{1}</a>",
							k, v)));
					form.append("</div>");
				}
			}
			form.append("</div>");
		}
		details.put("form", form.toString());
		for (Entry<String, Object> entry : details.entrySet()) {
			if (entry.getValue() instanceof String) {
				context = context.replace(String.format(JsoupAnalysisor.KEYFORMAT, entry.getKey()),
						(String) entry.getValue());
			}
		}
		return context;
	}

	public static void main(String[] args) throws Throwable {
		File[] files = new File(new File("").getAbsolutePath()).listFiles((dir, name) -> name.startsWith("sisdemo"));
//		files = new File[] { new File("sisdemo-8757822.html"), new File("sisdemo_8720516.html") };
		for (File f : files) {
			System.out.println(f);
			StringBuffer stringBuffer = new StringBuffer();
			Files.readAllLines(Paths.get(f.toURI()), Charset.forName("GBK")).forEach(str -> stringBuffer.append(str));
			Map<String, Object> details = split(stringBuffer.toString());
			String temp = stringBuffer.toString();
			System.out.println(temp.getBytes().length + "	->	" + ZipUtil.compress(temp).length);
			temp = JsonUtil.toJson(details);
			System.out.println(temp.getBytes().length + "	->	" + ZipUtil.compress(temp).length);
			System.out.println(JsonUtil.toPrettyJson(details));
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
		for (org.jsoup.nodes.Element tbody : doument.select("div.postinfo.postactions").select("a")) {

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

	public static Map<String, Object> split(String html) {
		org.jsoup.nodes.Document doument = Jsoup.parse(html);
		doument.outputSettings(new Document.OutputSettings().prettyPrint(false));
		try {
			return split(doument);
		} finally {
//			System.out.println(toTextOnly(doument.select("div.postmessage.defaultpost")));
		}
	}

	public static Map<String, Object> split(org.jsoup.nodes.Document doument) {
		Map<String, Object> result = new LinkedHashMap<>();
		List<Map<String, String>> contents = new ArrayList<>();
		Map<String, String> actions = new HashMap<>();
		result.put("actions", actions);
		String fid = null;
		for (org.jsoup.nodes.Element element : doument.select("#foruminfo").select("a")) {
			String href = element.attr("href");
			if (null != href && href.startsWith("forum-")) {
				href = href.substring("forum-".length());
				fid = href.split("-")[0];
			}
		}
		result.put("fid", fid);
		org.jsoup.select.Elements mainbox = doument.select(".mainbox.viewthread");
		result.put("type", mainbox.select("h1").select("a").text());
		String id = mainbox.select(".headactions").select("a").attr("href");
		if (id.contains("viewthread.php?action=printable&tid="))// 不靠谱，再找 XXX
			result.put("id", id.substring("viewthread.php?action=printable&tid=".length()));
		for (org.jsoup.nodes.Element element : doument.select("div#wrapper form")) {
			element.select("[onclick]").forEach(e -> e.removeAttr("onclick"));
			element.select("[onload]").forEach(e -> e.removeAttr("onload"));
			for (org.jsoup.nodes.Element a : element.select(".postinfo.postactions").select("a")) {
				actions.put(a.attr("href").substring("viewthread.php?tid=".length()), a.ownText());
			}
			for (org.jsoup.nodes.Element tr : element.select(".mainbox.viewthread")//// class=mainbox的div
					.select("table")//
					.select("tbody")//
					.select("tr")//
			) {
				Map<String, String> content = new HashMap<String, String>();
				String floor = "";
				for (org.jsoup.nodes.Element postinfo : tr.select("div.postinfo")) {
					org.jsoup.nodes.Element temp = postinfo.select("strong").first();
					if (null != temp) {
						floor = temp.ownText();
						content.put("floor", floor);
						content.put("datetime", postinfo.ownText().replace("发表于 ", ""));
					}
				}
				if (floor.isEmpty())
					continue;
				org.jsoup.select.Elements postauthor = tr.select(".postauthor");
				content.put("author", postauthor.select("cite").select("a").first().ownText());
				content.put("level", postauthor.select("p").select("em").first().text());
				org.jsoup.select.Elements message = tr.select("div.postmessage.defaultpost");
				content.put("message", message.select(".t_msgfont").html());
				org.jsoup.select.Elements postattachlist = message.select(".box.postattachlist")
						.select(".t_attachlist");
				postattachlist.select("a.hover").remove();
				content.put("attachlist", postattachlist.select("a").outerHtml());
				content.put("attachlist_size", postattachlist.select("em").html());
				contents.add(content);
			}
		}
		result.put("contents", contents);
		result.put("foruminfo", doument.select("div#wrapper div#foruminfo #nav").html());
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
		return result;
	}

	public static String splitTemplate(org.jsoup.nodes.Document doument) {
		Map<String, String> result = new LinkedHashMap<>();
		for (org.jsoup.nodes.Element element : doument.select("div#wrapper form")) {
			element.select("[onclick]").forEach(e -> e.removeAttr("onclick"));
			element.select("[onload]").forEach(e -> e.removeAttr("onload"));
			element.select("table").forEach(t -> t.clearAttributes());
			element.select("td.postauthor").forEach(e -> e.children().forEach(c -> {
				if ("cite".equalsIgnoreCase(c.tagName())) {
					c.select("a").forEach(a -> a.clearAttributes());
				} else
					c.remove();
			}));
			element.select("td.postcontent div.postinfo").forEach(e -> e.children().forEach(c -> {
				if ("strong".equalsIgnoreCase(c.tagName()))
					c.clearAttributes();
				else
					c.remove();
			}));
			element.select(".postratings,.postactions").remove();
			element.select("div.postmessage.defaultpost div.box.postattachlist dl.t_attachlist a.hover").remove();
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

		String html = doument.html();
		for (Entry<String, String> entry : result.entrySet()) {
			System.out.println(entry.getKey() + ":	" + html.contains(entry.getValue()));
			html = html.replace(entry.getValue(), String.format(KEYFORMAT, entry.getKey()));
		}
		doument = Jsoup.parse(html);
		doument.select("img").forEach(e -> e.remove());
//		result.put("template", );

		return doument.html();
	}
}
