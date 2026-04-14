package com.eam.assetcenter.common.enums;

/**
 * 数据库类型枚举。
 */
public enum DatabaseType {
    MYSQL,
    ORACLE,
    POSTGRESQL,
    SQLSERVER,
    GAUSSDB,
    DAMENG,
    OTHER;

    /**
     * 判断数据库类型是否合法。
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (DatabaseType item : values()) {
            if (item.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
