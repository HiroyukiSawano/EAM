package com.eam.assetcenter.service;

import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.domain.entity.AuditLog;
import com.eam.assetcenter.infrastructure.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务，负责记录资源变更轨迹。
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogMapper auditLogMapper;

    /**
     * 记录一条审计日志。
     */
    public void record(String resourceType, Long resourceId, AuditActionType actionType, String content, String operator) {
        AuditLog auditLog = new AuditLog();
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setActionType(actionType.name());
        auditLog.setContent(content);
        auditLog.setOperator(defaultOperator(operator));
        auditLogMapper.insert(auditLog);
    }

    /**
     * 返回审计日志使用的默认操作人。
     */
    public String defaultOperator(String operator) {
        return operator == null || operator.trim().isEmpty() ? "SYSTEM" : operator.trim();
    }
}





