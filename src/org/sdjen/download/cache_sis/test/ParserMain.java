package org.sdjen.download.cache_sis.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sdjen.download.cache_sis.ESMap;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.visitor.SQLASTVisitor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 *
 * @author beanlam
 * @date 2017年1月10日 下午11:06:26
 * @version 1.0
 *
 */
public class ParserMain {

	public static void main(String[] args) throws JsonProcessingException {
		String sql = "select * from Ta a, Tb n where a.abd='1' and b.abE='1' and (fields(level(abT,3),adw) like '11' or (b.abf='12' and aaa<>1) and (aab<>2 or ace=3)) and a.ada like '%da%' and '1'=ade";
//		sql = "select * from Ta a, Tb n where aa=1 or dd = 3";

		// 使用Parser解析生成AST，这里SQLStatement就是AST
		SQLStatement statement = new MySqlStatementParser(sql).parseStatement();

		// 使用visitor来访问AST
		SISCacheSchemaStatVisitor visitor = new SISCacheSchemaStatVisitor();
		statement.accept(visitor);
		// 从visitor中拿出你所关注的信息
		System.out.println(statement.toString());
//		System.out.println(abc(visitor.item));
		visitor.item.op = "and";
		dafdfa(visitor.item);
		System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(visitor.item));
	}

	private static void dafdfa(org.sdjen.download.cache_sis.test.SISCacheSchemaStatVisitor.Item item) {
		item.parent = null;
		item.childs.forEach(i -> dafdfa(i));
	}

	private static ESMap get(org.sdjen.download.cache_sis.test.SISCacheSchemaStatVisitor.Item item) {
		item.parent = null;
		List<ESMap> or = new ArrayList<>();
		List<ESMap> and = new ArrayList<>();
		List<ESMap> nor = new ArrayList<>();
		if (null != item.name && null != item.value) {
			List<ESMap> list = item.op.equals("<>") || item.op.equals("!=") ? nor
					: (item.parent.op.equalsIgnoreCase("AND") ? and : or);
			ESMap i = ESMap.get()//
					.set("fields", Arrays.asList(item.name.split(",")));
			i.set("query", item.value);
			i.set("type", "phrase");
			list.add(ESMap.get().set("multi_match", i));
		}
		for (org.sdjen.download.cache_sis.test.SISCacheSchemaStatVisitor.Item child : item.childs) {

		}
		ESMap bool = ESMap.get();
		if (!and.isEmpty())
			bool.set("must", and);
		if (!or.isEmpty())
			bool.set("should", or);
		if (!nor.isEmpty())
			bool.set("must_not", nor);
		return ESMap.get().set("bool", bool);
	}

	private static StringBuilder abc(org.sdjen.download.cache_sis.test.SISCacheSchemaStatVisitor.Item item) {
		StringBuilder rst = new StringBuilder("(");
		if (null == item.name) {
			if (null != item.op)
				rst.append(" ").append(item.op).append(" ");
		}
		rst.append(")");
		return rst;
	}
}
