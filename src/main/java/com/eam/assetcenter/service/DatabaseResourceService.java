package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.enums.CommonStatus;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.DatabaseResource;
import com.eam.assetcenter.domain.entity.SoftwareDatabaseRel;
import com.eam.assetcenter.infrastructure.mapper.DatabaseResourceMapper;
import com.eam.assetcenter.infrastructure.mapper.SoftwareDatabaseRelMapper;
import com.eam.assetcenter.web.request.DatabaseResourceUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据库资源业务服务。
 */
@Service
@RequiredArgsConstructor
public class DatabaseResourceService {

    private final DatabaseResourceMapper databaseResourceMapper;
    private final SoftwareDatabaseRelMapper softwareDatabaseRelMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增数据库资源。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(DatabaseResourceUpsertRequest request) {
        validateRequest(request, null);
        DatabaseResource resource = toEntity(request, null);
        databaseResourceMapper.insert(resource);
        auditService.record("DATABASE_RESOURCE", resource.getId(), AuditActionType.CREATE,
                "Created database resource " + resource.getDatabaseCode(), "SYSTEM");
        return toView(getById(resource.getId()));
    }

    /**
     * 更新数据库资源。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, DatabaseResourceUpsertRequest request) {
        DatabaseResource existing = getById(id);
        validateRequest(request, id);
        DatabaseResource resource = toEntity(request, existing);
        databaseResourceMapper.update(
                null,
                Wrappers.<DatabaseResource>lambdaUpdate()
                        .eq(DatabaseResource::getId, id)
                        .set(DatabaseResource::getDatabaseCode, resource.getDatabaseCode())
                        .set(DatabaseResource::getDatabaseName, resource.getDatabaseName())
                        .set(DatabaseResource::getDatabaseType, resource.getDatabaseType())
                        .set(DatabaseResource::getVersion, resource.getVersion())
                        .set(DatabaseResource::getStatus, resource.getStatus())
                        .set(DatabaseResource::getRemark, resource.getRemark())
                        .set(DatabaseResource::getUpdatedAt, LocalDateTime.now()));
        auditService.record("DATABASE_RESOURCE", id, AuditActionType.UPDATE,
                "Updated database resource " + resource.getDatabaseCode(), "SYSTEM");
        return toView(getById(id));
    }

    /**
     * 查询数据库资源详情。
     */
    public Map<String, Object> getDetail(Long id) {
        return toView(getById(id));
    }

    /**
     * 分页查询数据库资源。
     */
    public PageResponse<Map<String, Object>> page(int pageNo, int pageSize, String keyword, String databaseType, String status) {
        supportService.ensureDatabaseTypeValid(databaseType);
        if (StringUtils.hasText(status)) {
            supportService.ensureCommonStatusValid(status, "数据库资源");
        }

        LambdaQueryWrapper<DatabaseResource> wrapper = new LambdaQueryWrapper<DatabaseResource>()
                .and(StringUtils.hasText(keyword),
                        query -> query.like(DatabaseResource::getDatabaseCode, keyword)
                                .or().like(DatabaseResource::getDatabaseName, keyword))
                .eq(StringUtils.hasText(databaseType), DatabaseResource::getDatabaseType, databaseType)
                .eq(StringUtils.hasText(status), DatabaseResource::getStatus, status)
                .orderByAsc(DatabaseResource::getDatabaseCode);

        Page<DatabaseResource> page = databaseResourceMapper.selectPage(new Page<DatabaseResource>(pageNo, pageSize), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream().map(this::toView).collect(Collectors.toList());
        return new PageResponse<Map<String, Object>>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /**
     * 查询数据库资源下拉选项。
     */
    public List<DatabaseResource> options() {
        return databaseResourceMapper.selectList(new LambdaQueryWrapper<DatabaseResource>()
                .orderByAsc(DatabaseResource::getDatabaseCode));
    }

    /**
     * 查询数据库资源统计。
     */
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", databaseResourceMapper.selectCount(Wrappers.<DatabaseResource>lambdaQuery()));
        result.put("active", databaseResourceMapper.selectCount(Wrappers.<DatabaseResource>lambdaQuery()
                .eq(DatabaseResource::getStatus, CommonStatus.ACTIVE.name())));
        result.put("inactive", databaseResourceMapper.selectCount(Wrappers.<DatabaseResource>lambdaQuery()
                .eq(DatabaseResource::getStatus, CommonStatus.INACTIVE.name())));
        return result;
    }

    /**
     * 删除数据库资源，并清理软件依赖关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DatabaseResource existing = getById(id);
        softwareDatabaseRelMapper.delete(Wrappers.<SoftwareDatabaseRel>lambdaQuery()
                .eq(SoftwareDatabaseRel::getDatabaseId, id));
        databaseResourceMapper.deleteById(id);
        auditService.record("DATABASE_RESOURCE", id, AuditActionType.DELETE,
                "Deleted database resource " + existing.getDatabaseCode(), "SYSTEM");
    }

    /**
     * 根据主键查询数据库资源。
     */
    public DatabaseResource getById(Long id) {
        DatabaseResource resource = databaseResourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException("Database resource not found: " + id);
        }
        return resource;
    }

    private void validateRequest(DatabaseResourceUpsertRequest request, Long excludeId) {
        supportService.ensureUniqueDatabaseCode(request.getDatabaseCode(), excludeId);
        supportService.ensureDatabaseTypeValid(request.getDatabaseType());
        if (StringUtils.hasText(request.getStatus())) {
            supportService.ensureCommonStatusValid(request.getStatus(), "数据库资源");
        }
    }

    private DatabaseResource toEntity(DatabaseResourceUpsertRequest request, DatabaseResource existing) {
        DatabaseResource resource = new DatabaseResource();
        resource.setDatabaseCode(request.getDatabaseCode());
        resource.setDatabaseName(request.getDatabaseName());
        resource.setDatabaseType(request.getDatabaseType());
        resource.setVersion(request.getVersion());
        resource.setStatus(StringUtils.hasText(request.getStatus())
                ? request.getStatus()
                : existing == null ? CommonStatus.ACTIVE.name() : existing.getStatus());
        resource.setRemark(request.getRemark());
        return resource;
    }

    private Map<String, Object> toView(DatabaseResource resource) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("id", resource.getId());
        view.put("databaseCode", resource.getDatabaseCode());
        view.put("databaseName", resource.getDatabaseName());
        view.put("databaseType", resource.getDatabaseType());
        view.put("version", resource.getVersion());
        view.put("status", resource.getStatus());
        view.put("remark", resource.getRemark());
        view.put("createdAt", resource.getCreatedAt());
        view.put("updatedAt", resource.getUpdatedAt());
        return view;
    }
}
