package org.sdjen.download.cache_sis.test;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLObject;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLIntegerExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement.ValuesClause;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;

public class SISCacheSchemaStatVisitor extends SQLASTVisitorAdapter {
	public SISCacheSchemaStatVisitor() {
		super();
	}

	public class Item {
		public Item parent;
		public String name;
		public String op;
		public String value;
		public String method;
		int hist = 0;
		public List<Item> childs = new ArrayList<>();
	}

	Item item = new Item();

	@Override
	public boolean visit(SQLIdentifierExpr x) {
		item.name = null == item.name ? x.getName() : (item.name + "," + x.getName());
		return super.visit(x);
	}

	@Override
	public boolean visit(SQLPropertyExpr x) {
		item.name = x.getName();
		return super.visit(x);
	}

	@Override
	public boolean visit(SQLMethodInvokeExpr x) {
		item.method = x.toString();// .getMethodName();
		return super.visit(x);
	}

	@Override
	public boolean visit(ValuesClause x) {
		System.out.println("V:	" + x);
		return super.visit(x);
	}

	@Override
	public boolean visit(SQLCharExpr x) {
		System.out.println("C:	" + x);
		return super.visit(x);
	}

	@Override
	public boolean visit(SQLIntegerExpr x) {
		System.out.println("I:	" + x);
		return super.visit(x);
	}

	@Override
	public boolean visit(SQLBinaryOpExpr x) {
		Item item = new Item();
		item.hist = this.item.hist + 1;
		item.parent = this.item;
		item.op = x.getOperator().getName();
		this.item.childs.add(item);
		this.item = item;
		System.out.println(x.getLeft().getClass() + "	_	" + x.getRight().getClass());
		System.out.println(x.toString());
//		if (x.isNameAndLiteral()) {
//			System.out.println(x.getLeft() + "	" + x.getOperator().getName() + "	" + x.getRight());
//			item.name = x.getLeft().toString();
//			item.value = x.getRight().toString();
//			System.out.println("B:	" + x);
//		} else {
//			System.out.println(x.getOperator().getName());
//		}
		boolean result = super.visit(x);
		this.item = item.parent;
//		System.out.println("E:	" + x + "~	" + this.item.hist);
		return result;
	}
}
