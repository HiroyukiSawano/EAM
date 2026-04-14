package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.enums.CommonStatus;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.MiddlewareResource;
import com.eam.assetcenter.domain.entity.SoftwareMiddlewareRel;
import com.eam.assetcenter.infrastructure.mapper.MiddlewareResourceMapper;
import com.eam.assetcenter.infrastructure.mapper.SoftwareMiddlewareRelMapper;
import com.eam.assetcenter.web.request.MiddlewareResourceUpsertRequest;
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
 * 中间件资源业务服务。
 */
@Service
@RequiredArgsConstructor
public class MiddlewareResourceService {

    private final MiddlewareResourceMapper middlewareResourceMapper;
    private final SoftwareMiddlewareRelMapper softwareMiddlewareRelMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增中间件资源。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(MiddlewareResourceUpsertRequest request) {
        validateRequest(request, null);
        MiddlewareResource resource = toEntity(request, null);
        middlewareResourceMapper.insert(resource);
        auditService.record("MIDDLEWARE_RESOURCE", resource.getId(), AuditActionType.CREATE,
                "Created middleware resource " + resource.getMiddlewareCode(), "SYSTEM");
        return toView(getById(resource.getId()));
    }

    /**
     * 更新中间件资源。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, MiddlewareResourceUpsertRequest request) {
        MiddlewareResource existing = getById(id);
        validateRequest(request, id);
        MiddlewareResource resource = toEntity(request, existing);
        middlewareResourceMapper.update(
                null,
                Wrappers.<MiddlewareResource>lambdaUpdate()
                        .eq(MiddlewareResource::getId, id)
                        .set(MiddlewareResource::getMiddlewareCode, resource.getMiddlewareCode())
                        .set(MiddlewareResource::getMiddlewareName, resource.getMiddlewareName())
                        .set(MiddlewareResource::getMiddlewareType, resource.getMiddlewareType())
                        .set(MiddlewareResource::getVersion, resource.getVersion())
                        .set(MiddlewareResource::getStatus, resource.getStatus())
                        .set(MiddlewareResource::getRemark, resource.getRemark())
                        .set(MiddlewareResource::getUpdatedAt, LocalDateTime.now()));
        auditService.record("MIDDLEWARE_RESOURCE", id, AuditActionType.UPDATE,
                "Updated middleware resource " + resource.getMiddlewareCode(), "SYSTEM");
        return toView(getById(id));
    }

    /**
     * 查询中间件资源详情。
     */
    public Map<String, Object> getDetail(Long id) {
        return toView(getById(id));
    }

    /**
     * 分页查询中间件资源。
     */
    public PageResponse<Map<String, Object>> page(int pageNo, int pageSize, String keyword, String middlewareType, String status) {
        supportService.ensureMiddlewareTypeValid(middlewareType);
        if (StringUtils.hasText(status)) {
            supportService.ensureCommonStatusValid(status, "中间件资源");
        }

        LambdaQueryWrapper<MiddlewareResource> wrapper = new LambdaQueryWrapper<MiddlewareResource>()
                .and(StringUtils.hasText(keyword),
                        query -> query.like(MiddlewareResource::getMiddlewareCode, keyword)
                                .or().like(MiddlewareResource::getMiddlewareName, keyword))
                .eq(StringUtils.hasText(middlewareType), MiddlewareResource::getMiddlewareType, middlewareType)
                .eq(StringUtils.hasText(status), MiddlewareResource::getStatus, status)
                .orderByAsc(MiddlewareResource::getMiddlewareCode);

        Page<MiddlewareResource> page = middlewareResourceMapper.selectPage(new Page<MiddlewareResource>(pageNo, pageSize), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream().map(this::toView).collect(Collectors.toList());
        return new PageResponse<Map<String, Object>>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /**
     * 查询中间件资源下拉选项。
     */
    public List<MiddlewareResource> options() {
        return middlewareResourceMapper.selectList(new LambdaQueryWrapper<MiddlewareResource>()
                .orderByAsc(MiddlewareResource::getMiddlewareCode));
    }

    /**
     * 查询中间件资源统计。
     */
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", middlewareResourceMapper.selectCount(Wrappers.<MiddlewareResource>lambdaQuery()));
        result.put("active", middlewareResourceMapper.selectCount(Wrappers.<MiddlewareResource>lambdaQuery()
                .eq(MiddlewareResource::getStatus, CommonStatus.ACTIVE.name())));
        result.put("inactive", middlewareResourceMapper.selectCount(Wrappers.<MiddlewareResource>lambdaQuery()
                .eq(MiddlewareResource::getStatus, CommonStatus.INACTIVE.name())));
        return result;
    }

    /**
     * 删除中间件资源，并清理软件依赖关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        MiddlewareResource existing = getById(id);
        softwareMiddlewareRelMapper.delete(Wrappers.<SoftwareMiddlewareRel>lambdaQuery()
                .eq(SoftwareMiddlewareRel::getMiddlewareId, id));
        middlewareResourceMapper.deleteById(id);
        auditService.record("MIDDLEWARE_RESOURCE", id, AuditActionType.DELETE,
                "Deleted middleware resource " + existing.getMiddlewareCode(), "SYSTEM");
    }

    /**
     * 根据主键查询中间件资源。
     */
    public MiddlewareResource getById(Long id) {
        MiddlewareResource resource = middlewareResourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException("Middleware resource not found: " + id);
        }
        return resource;
    }

    private void validateRequest(MiddlewareResourceUpsertRequest request, Long excludeId) {
        supportService.ensureUniqueMiddlewareCode(request.getMiddlewareCode(), excludeId);
        supportService.ensureMiddlewareTypeValid(request.getMiddlewareType());
        if (StringUtils.hasText(request.getStatus())) {
            supportService.ensureCommonStatusValid(request.getStatus(), "中间件资源");
        }
    }

    private MiddlewareResource toEntity(MiddlewareResourceUpsertRequest request, MiddlewareResource existing) {
        MiddlewareResource resource = new MiddlewareResource();
        resource.setMiddlewareCode(request.getMiddlewareCode());
        resource.setMiddlewareName(request.getMiddlewareName());
        resource.setMiddlewareType(request.getMiddlewareType());
        resource.setVersion(request.getVersion());
        resource.setStatus(StringUtils.hasText(request.getStatus())
                ? request.getStatus()
                : existing == null ? CommonStatus.ACTIVE.name() : existing.getStatus());
        resource.setRemark(request.getRemark());
        return resource;
    }

    private Map<String, Object> toView(MiddlewareResource resource) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("id", resource.getId());
        view.put("middlewareCode", resource.getMiddlewareCode());
        view.put("middlewareName", resource.getMiddlewareName());
        view.put("middlewareType", resource.getMiddlewareType());
        view.put("version", resource.getVersion());
        view.put("status", resource.getStatus());
        view.put("remark", resource.getRemark());
        view.put("createdAt", resource.getCreatedAt());
        view.put("updatedAt", resource.getUpdatedAt());
        return view;
    }
}
