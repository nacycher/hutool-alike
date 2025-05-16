package com.tools.hutoolalike.utils;

/**
 * 处理sql工具类
 */
public class SqlUtils {

    /**
     * 处理sql中where条件占位符为空的情况，如果占位符为空，则删除对应的查询条件
     * 能处理复杂sql：子查询，like in 函数等情况
     * @param sql
     * @return
     * @throws Exception
     *
     * example:
     * 1. sql = "SELECT * FROM table WHERE id = ${id} AND name = 'test'";
     * return： SELECT * FROM table WHERE name = 'test';
     *
     * 2.sql = "select *from table
     * where id in (${ids})
     * and age like '%${age}%' or address in (select address from table2 where id = 666)";
     * return: select *from table where address IN (SELECT address FROM table2 WHERE id = 666);
     */
    public static String HandleEmptySqlConditionsResult(String sql) throws Exception {
        return WhereConditionWithConnectorSplitter.handleEmptySqlConditions(sql);
    }
}
