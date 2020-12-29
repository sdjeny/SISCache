package org.sdjen.download.cache_sis.controller;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@EnableAutoConfiguration
@RequestMapping("/calc")
public class Controller_calc {
	private final static Logger logger = LoggerFactory.getLogger(Controller_calc.class);

	@RequestMapping("/")
	@ResponseBody
	private String root() {
		StringBuilder rst = new StringBuilder();
		rst.append("</br><table border='0'>");
		{
			rst.append("<tbody><tr>");
			rst.append(
					"<td><a href='/calc/wg/127' title='新窗口打开' target='_blank'>reload</a></td>");
			rst.append(String.format("<td>%s</td>", "波导计算"));
			rst.append(String.format("<td>%s</td>", ""));
			rst.append("</tr></tbody>");
		}
		rst.append("</table>");
		return rst.toString(); // XXX
	}

	@RequestMapping("/wg/{diam}")
	@ResponseBody
	String list(@PathVariable("diam") int diam) {
		StringBuilder result = new StringBuilder();
//		TE11:This mode will work if f = 2.412GHz > fcutoff = 2*c/(3.41*D)
//		TM01:This mode will not work if f = 2.462GHz < fcutoff = 2*c/(2.61*D) 
//		2.87” < D < 3.67”
//		2.87” < D < 3.67”
		double f = 2445;// 频率Hz 2437
		double c = 299792.458;// 光速km/s
		double λ = c / f;// 光速km/s
		double fcte11 = 2 * c / (3.41 * diam);
		double fctm01 = 2 * c / (2.61 * diam);
		double wg;
		wg = c / Math.sqrt(Math.pow(f, 2) - Math.pow(fcte11, 2));// 波导长度
//		wg = λ / Math.sqrt(1 - Math.pow(λ / (1.706 * diam), 2));// 波导长度
//		System.out.println(MessageFormat.format("TE11	{0}", fcte11));
//		System.out.println(MessageFormat.format("TM01	{0}", fctm01));
		result.append(MessageFormat.format("罐直径 	{0} mm", diam)).append("</br>");
		result.append(MessageFormat.format("波导长度 	{0}mm", wg)).append("</br>");
		result.append(MessageFormat.format("罐长 	{0}mm", wg * 0.75)).append("</br>");
		result.append(MessageFormat.format("距底 	{0}mm", wg * 0.25)).append("</br>");
		result.append(MessageFormat.format("波长 	{0}mm", c / f)).append("</br>");
		result.append(MessageFormat.format("端子长 	{0}mm", c / f / 4.00)).append("</br>");
		return result.toString();
	}
}
