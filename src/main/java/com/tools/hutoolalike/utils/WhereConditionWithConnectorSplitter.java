package com.tools.hutoolalike.utils;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;




@Slf4j
public class WhereConditionWithConnectorSplitter {

    @Test
    public void test() throws Exception {
        // ${}无法使用jsqlparser解析，需要手动替换成其他字符，这里使用USER_NOT_INPUT
//        String sql = "select * from table where id = 1 and name = USER_NOT_INPUT or age = 4 and salary > 1000 and create_date > date_format('yyyy-mm-dd', USER_NOT_INPUT)";
//        String sql = "select *from table";
//        String sql = "select *from table where id in (USER_NOT_INPUT) or name = 'my_name'";
        String sql = "select *from table where id in (USER_NOT_INPUT) or name like '%USER_NOT_INPUT'";
        handleEmptySqlConditions(sql);
    }

    private static final String USER_NOT_INPUT = "USER_NOT_INPUT";
    /**
     * 提取sql中where之前的部分
     * @param sql
     * @return

    /**
     * 处理用户输入为空的情况，即${id}没有值，则删除对应的查询条件
     * example:
     * 1. sql = "SELECT * FROM table WHERE id = ${id} AND name = 'test'"
     * return： SELECT * FROM table WHERE name = 'test'
     */
    public static String handleEmptySqlConditions(String sql) throws Exception {
        log.info("动态删除sql条件-开始处理sql：{}", sql);
        StringBuilder sb = new StringBuilder();
        String whereSql = extractUpToWhere(sql);
        log.info("动态删除sql条件-得到sql种where之前的部分：{}", whereSql);
        sb.append(whereSql);

        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plain = (PlainSelect) select.getSelectBody();
        Expression whereExpr = plain.getWhere();

        // 获取所有的sql查询条件
        List<ConditionNode> nodes = splitWithConnectors(whereExpr);
        List<ConditionNode> newNodes = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            ConditionNode node = nodes.get(i);
            // 对查询条件删除
            if (!node.toString().contains(USER_NOT_INPUT)) {
                newNodes.add(node);
            }
        }
        // 拼接sql
        for (int i = 0; i < newNodes.size(); i++) {
            ConditionNode node = nodes.get(i);
            // 当前节点为第一个节点时，不为sql加连接词
            if (i == 0 && Objects.nonNull(node.connector)) {
                sb.append(" ").append(node.condition.toString());
            } else {
                sb.append(" ").append(node.toString());
            }

        }
        sql = removeStandaloneWhere(sb.toString());
        log.info("动态删除sql条件-完成-得到删除空条件后的sql：{}", sql);

        return sql;
    }

    /**
     * 分割表达式为带连接词的条件项
     * @param expr
     * @return
     */
    private static List<ConditionNode> splitWithConnectors(Expression expr) {
        List<ConditionNode> result = new ArrayList<>();
        splitRecursive(expr, null, result);
        return result;
    }

    /**
     * 分割表达式为带连接词的条件项，递归实现
     * @param expr
     * @param connector
     * @param list
     */
    private static void splitRecursive(Expression expr, String connector, List<ConditionNode> list) {
        // 如果表达式为空，直接返回
        if (Objects.isNull(expr)) {
            return;
        }
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
        }    // 处理 IN 表达式（例如：col IN (1,2,3)）
        else if (expr instanceof InExpression) {
            list.add(new ConditionNode(connector, expr));
        }
        // 处理 LIKE 表达式（例如：col LIKE '%val%'）
        else if (expr instanceof LikeExpression) {
            list.add(new ConditionNode(connector, expr));
        }
        // 处理其他比较操作（=, >, < 等）
        else if (expr instanceof ComparisonOperator) {
            list.add(new ConditionNode(connector, expr));
        }
        // 处理括号包裹的表达式（例如：(a=1 OR b=2)）
        else if (expr instanceof Parenthesis) {
            Parenthesis parenthesis = (Parenthesis) expr;
            splitRecursive(parenthesis.getExpression(), connector, list);
        } else {
            // 叶子表达式，添加列表
            list.add(new ConditionNode(connector, expr));
        }
    }


    /**
     * 提取 SQL 中 WHERE 之前的部分（包括 WHERE）
     */
    private static String extractUpToWhere(String sql) throws JSQLParserException {
        // 1. 解析 SQL 并验证是否存在 WHERE 子句
        Statement statement = CCJSqlParserUtil.parse(sql);
        boolean hasWhere = hasWhereClause(statement);

        if (!hasWhere) {
            return sql;
        }

        // 2. 使用正则定位 WHERE 关键字
        Pattern pattern = Pattern.compile("\\bWHERE\\b", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find()) {
            int whereStart = matcher.start();
            return sql.substring(0, whereStart + "WHERE".length()).trim();
        } else {
            return sql; // 理论上不会执行此分支，因解析已确认存在 WHERE
        }
    }

    /**
     * 检查 SQL 是否包含 WHERE 子句
     */
    private static boolean hasWhereClause(Statement statement) {
        if (statement instanceof Select) {
            Select select = (Select) statement;
            SelectBody selectBody = select.getSelectBody();
            if (selectBody instanceof PlainSelect) {
                PlainSelect plainSelect = (PlainSelect) selectBody;
                return plainSelect.getWhere() != null;
            }
        }
        // 扩展支持 UPDATE/DELETE 语句
        return false;
    }

    /**
     * 只去除末尾单独存在的 where，后面没有任何条件
     */
    public static String removeStandaloneWhere(String sql) {
        if (sql == null) return null;

        // 匹配SQL末尾有一个where，且where后面没有任何非空字符
        // ^(.*?)(?i)\\bwhere\\b\\s*$
        // 分组1捕获where前内容，忽略where大小写，后面只能空白到结尾
        return sql.replaceAll("(?i)\\bwhere\\b\\s*$", "").trim();
    }
}


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