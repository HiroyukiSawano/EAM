package com.eam.assetcenter.common.enums;

/**
 * 信息系统类型枚举。
 */
public enum SystemType {
    EXTERNAL_SERVICE,
    INTERNAL_OFFICE,
    DATABASE_SOFTWARE,
    BASIC_SUPPORT,
    SECURITY_SOFTWARE;

    /**
     * 判断信息系统类型是否合法。
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (SystemType item : values()) {
            if (item.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}

