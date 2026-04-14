package com.eam.assetcenter.common.enums;

/**
 * 中间件类型枚举。
 */
public enum MiddlewareType {
    TOMCAT,
    NGINX,
    REDIS,
    KAFKA,
    ROCKETMQ,
    ELASTICSEARCH,
    NACOS,
    OTHER;

    /**
     * 判断中间件类型是否合法。
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (MiddlewareType item : values()) {
            if (item.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
