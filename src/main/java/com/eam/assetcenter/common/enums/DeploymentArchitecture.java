package com.eam.assetcenter.common.enums;

/**
 * 软件部署架构枚举。
 */
public enum DeploymentArchitecture {
    SINGLE,
    CLUSTER,
    CONTAINERIZED;

    /**
     * 判断部署架构是否合法。
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (DeploymentArchitecture item : values()) {
            if (item.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
