package com.tools.hutoolalike.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlUtil {
    public static void main(String[] args) {
        String sql = "select * from table where id = ${id} and name = zhangsan or time in (${time}) and address in (select address from address where address = ${address})";
        sql = prepareSql(sql);
        System.out.println(sql);
    }

    /**
     * 预处理sql，去除${name}
     * @param sql
     * @return
     */
    private static String prepareSql(String sql) {
        sql = removeConditionIncludingSubqueries(sql);
        return sql;
    }

    // 关键：找到包含占位符的条件块（考虑子查询）
    private static String removeConditionIncludingSubqueries(String sql) {
        // 正则表达式匹配WHERE子句及其后的内容
        Pattern wherePattern = Pattern.compile(
                "(?i)(\\bWHERE\\b)(.*?)(?=\\b(GROUP BY|HAVING|ORDER BY|LIMIT)\\b|$)",
                Pattern.DOTALL
        );
        Matcher whereMatcher = wherePattern.matcher(sql);

        if (whereMatcher.find()) {
            String originalWhere = whereMatcher.group(0); // 整个匹配的WHERE部分
            String conditionsStr = whereMatcher.group(2).trim();

            // 分割条件和逻辑运算符
            String[] tokens = conditionsStr.split("(?i)\\s+(AND|OR)\\s+");
            List<String> conditions = new ArrayList<>();
            List<String> operators = new ArrayList<>();

            if (tokens.length > 0) {
                conditions.add(tokens[0]);
                for (int i = 1; i < tokens.length; i++) {
                    if (i % 2 == 1) {
                        operators.add(tokens[i].toUpperCase());
                    } else {
                        conditions.add(tokens[i]);
                    }
                }
            }

            // 过滤包含占位符的条件
            List<String> filteredConditions = new ArrayList<>();
            List<String> filteredOperators = new ArrayList<>();
            Pattern placeholderPattern = Pattern.compile("\\$\\{\\w+\\}");

            for (int i = 0; i < conditions.size(); i++) {
                String condition = conditions.get(i);
                if (!placeholderPattern.matcher(condition).find()) {
                    filteredConditions.add(condition);
                    // 添加关联的运算符（如果存在）
                    if (i > 0 && filteredConditions.size() > 1) {
                        filteredOperators.add(operators.get(i - 1));
                    }
                }
            }

            // 重新构建WHERE子句
            StringBuilder newWhere = new StringBuilder();
            if (!filteredConditions.isEmpty()) {
                newWhere.append(" WHERE ");
                for (int i = 0; i < filteredConditions.size(); i++) {
                    if (i > 0) {
                        newWhere.append(" ").append(filteredOperators.get(i - 1)).append(" ");
                    }
                    newWhere.append(filteredConditions.get(i));
                }
            }

            // 替换原SQL中的WHERE部分
            return sql.replace(originalWhere, newWhere.toString())
                    .replaceAll("\\s+", " ")
                    .trim();
        }
        return sql.replaceAll("\\s+", " ").trim();
    }
}
