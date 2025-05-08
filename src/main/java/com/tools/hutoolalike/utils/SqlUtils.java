package com.tools.hutoolalike.utils;

/**
 * 处理sql工具类
 */
public class SqlUtils {

    /**
     * 处理sql中where条件为空的情况
     * @param sql
     * @return
     * @throws Exception
     *
     * example:
     * 1. sql = "SELECT * FROM table WHERE id = ${id} AND name = 'test'"
     * return： SELECT * FROM table WHERE name = 'test'
     */
    public static String HandleEmptySqlConditionsResult(String sql) throws Exception {
        return WhereConditionWithConnectorSplitter.handleEmptySqlConditions(sql);
    }
}
