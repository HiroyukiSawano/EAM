package com.eam.assetcenter.common.enums;

/**
 * 硬件资产状态枚举。
 */
public enum HardwareStatus implements StatusDictionaryEnum {
    REGISTERED("已登记", "Registered", "info"),
    IN_STOCK("已入库", "In Stock", "info"),
    ASSIGNED("已分配", "Assigned", "success"),
    CHANGED("已变更", "Changed", "info"),
    IDLE("闲置", "Idle", "info"),
    MAINTAINING("维护中", "Maintaining", "warning"),
    OFFLINE("已下线", "Offline", "danger"),
    SCRAPPED("已报废", "Scrapped", "danger");

    private final String label;
    private final String labelEn;
    private final String tagType;

    HardwareStatus(String label, String labelEn, String tagType) {
        this.label = label;
        this.labelEn = labelEn;
        this.tagType = tagType;
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getLabelEn() {
        return labelEn;
    }

    @Override
    public String getTagType() {
        return tagType;
    }

    /**
     * 判断状态值是否有效。
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (HardwareStatus item : values()) {
            if (item.name().equals(value)) {
                return true;
            }
        }
        return false;
    }
}





