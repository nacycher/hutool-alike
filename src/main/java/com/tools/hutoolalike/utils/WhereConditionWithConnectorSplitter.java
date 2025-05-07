package com.tools.hutoolalike.utils;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

import java.util.ArrayList;
import java.util.List;

// 表示带连接词的条件项
class ConditionNode {
    String connector; // "AND", "OR", null（首项没有连接词）
    Expression condition;

    ConditionNode(String connector, Expression condition) {
        this.connector = connector;
        this.condition = condition;
    }

    @Override
    public String toString() {
        return (connector == null ? "" : connector + " ") + condition.toString();
    }
}

public class WhereConditionWithConnectorSplitter {

    public static List<ConditionNode> splitWithConnectors(Expression expr) {
        List<ConditionNode> result = new ArrayList<>();
        splitRecursive(expr, null, result);
        return result;
    }

    private static void splitRecursive(Expression expr, String connector, List<ConditionNode> list) {
        if (expr instanceof AndExpression) {
            AndExpression andExpr = (AndExpression) expr;
            // 左边连接词继续传connector
            splitRecursive(andExpr.getLeftExpression(), connector, list);
            // 右边连接词变为 AND
            splitRecursive(andExpr.getRightExpression(), "AND", list);
        } else if (expr instanceof OrExpression) {
            OrExpression orExpr = (OrExpression) expr;
            splitRecursive(orExpr.getLeftExpression(), connector, list);
            splitRecursive(orExpr.getRightExpression(), "OR", list);
        } else {
            // 叶子表达式，添加列表
            list.add(new ConditionNode(connector, expr));
        }
    }

    public static void main(String[] args) throws Exception {
        String sql = "select * from table where id = 1 and name = USER_NOT_INPUT or age = 4 and salary > 1000 and user_id in (select id from user where u_name = 2)";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plain = (PlainSelect) select.getSelectBody();
        Expression whereExpr = plain.getWhere();

        // 获取所有的sql查询条件
        List<ConditionNode> nodes = splitWithConnectors(whereExpr);
        for (int i = 0; i < nodes.size(); i++) {
            ConditionNode node = nodes.get(i);
            // 对查询条件删除
            if (node.toString().contains("USER_NOT_INPUT")) {
                nodes.remove(node);
            }
        }
        for (ConditionNode node : nodes) {
            System.out.println(node);
        }

    }
}