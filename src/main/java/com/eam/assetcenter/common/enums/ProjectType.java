package com.eam.assetcenter.common.enums;

/**
 * 项目类型枚举。
 */
public enum ProjectType {
    NEW_BUILD,
    SOFTWARE_UPGRADE,
    OPS_PROJECT,
    SERVICE_PURCHASE,
    HARDWARE_PURCHASE,
    INTEGRATION_PROJECT;

    /**
     * 判断项目类型是否合法。
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (ProjectType item : values()) {
            if (item.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}





