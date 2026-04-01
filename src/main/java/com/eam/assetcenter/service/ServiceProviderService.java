package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.enums.CooperationScope;
import com.eam.assetcenter.common.enums.VendorLevel;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.AssetHardwareVendorRel;
import com.eam.assetcenter.domain.entity.Person;
import com.eam.assetcenter.domain.entity.ProjectVendorRel;
import com.eam.assetcenter.domain.entity.ServiceProvider;
import com.eam.assetcenter.domain.entity.ServiceProviderCooperationScopeRel;
import com.eam.assetcenter.domain.entity.ServiceProviderPersonRel;
import com.eam.assetcenter.domain.entity.SystemVendorRel;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareVendorRelMapper;
import com.eam.assetcenter.infrastructure.mapper.PersonMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectVendorRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderCooperationScopeRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderPersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.SystemVendorRelMapper;
import com.eam.assetcenter.web.request.ServiceProviderRelationRequest;
import com.eam.assetcenter.web.request.ServiceProviderUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服务商业务服务，负责服务商主数据及关联信息聚合。
 */
@Service
@RequiredArgsConstructor
public class ServiceProviderService {

    private final ServiceProviderMapper serviceProviderMapper;
    private final ServiceProviderCooperationScopeRelMapper cooperationScopeRelMapper;
    private final ServiceProviderPersonRelMapper serviceProviderPersonRelMapper;
    private final AssetHardwareVendorRelMapper hardwareVendorRelMapper;
    private final SystemVendorRelMapper systemVendorRelMapper;
    private final ProjectVendorRelMapper projectVendorRelMapper;
    private final PersonMapper personMapper;
    private final AuditService auditService;
    private final SupportService supportService;

    /**
     * 新增服务商。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(ServiceProviderUpsertRequest request) {
        validateRequest(request, null);
        ServiceProvider serviceProvider = toEntity(request);
        serviceProviderMapper.insert(serviceProvider);
        syncCooperationScopes(serviceProvider.getId(), resolveCooperationScopes(request));
        syncFormRelations(serviceProvider.getId(), request, true);
        auditService.record("SERVICE_PROVIDER", serviceProvider.getId(), AuditActionType.CREATE,
                "Created provider " + serviceProvider.getCode(), "SYSTEM");
        return toProviderView(getById(serviceProvider.getId()));
    }

    /**
     * 更新服务商。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, ServiceProviderUpsertRequest request) {
        getById(id);
        validateRequest(request, id);
        ServiceProvider serviceProvider = toEntity(request);
        serviceProvider.setId(id);
        serviceProviderMapper.updateById(serviceProvider);
        syncCooperationScopes(id, resolveCooperationScopes(request));
        syncFormRelations(id, request, false);
        auditService.record("SERVICE_PROVIDER", id, AuditActionType.UPDATE,
                "Updated provider " + serviceProvider.getCode(), "SYSTEM");
        return toProviderView(getById(id));
    }

    /**
     * 根据主键查询服务商。
     */
    public ServiceProvider getById(Long id) {
        ServiceProvider serviceProvider = serviceProviderMapper.selectById(id);
        if (serviceProvider == null) {
            throw new BusinessException("Service provider not found: " + id);
        }
        return serviceProvider;
    }

    /**
     * 查询服务商详情。
     */
    public Map<String, Object> getDetail(Long id) {
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("serviceProvider", toProviderView(getById(id)));
        detail.put("hardwareAssetIds", hardwareVendorRelMapper.selectList(
                        Wrappers.<AssetHardwareVendorRel>lambdaQuery().eq(AssetHardwareVendorRel::getServiceProviderId, id))
                .stream().map(AssetHardwareVendorRel::getHardwareAssetId).collect(Collectors.toList()));
        detail.put("informationSystemIds", systemVendorRelMapper.selectList(
                        Wrappers.<SystemVendorRel>lambdaQuery().eq(SystemVendorRel::getServiceProviderId, id))
                .stream().map(SystemVendorRel::getInformationSystemId).collect(Collectors.toList()));
        detail.put("personIds", personMapper.selectList(
                        Wrappers.<Person>lambdaQuery().eq(Person::getServiceProviderId, id))
                .stream().map(Person::getId).collect(Collectors.toList()));
        detail.put("projectIds", projectVendorRelMapper.selectList(
                        Wrappers.<ProjectVendorRel>lambdaQuery().eq(ProjectVendorRel::getServiceProviderId, id))
                .stream().map(ProjectVendorRel::getProjectId).collect(Collectors.toList()));
        return detail;
    }

    /**
     * 按条件分页查询服务商。
     */
    public PageResponse<Map<String, Object>> page(int pageNo, int pageSize, String keyword, String cooperationScope, String status) {
        if (status != null && !status.trim().isEmpty()) {
            supportService.ensureCommonStatusValid(status, "服务商");
        }
        supportService.ensureCooperationScopesValid(cooperationScope == null ? null : Collections.singletonList(cooperationScope));

        List<Long> serviceProviderIds = null;
        if (cooperationScope != null && !cooperationScope.trim().isEmpty()) {
            serviceProviderIds = cooperationScopeRelMapper.selectList(
                            Wrappers.<ServiceProviderCooperationScopeRel>lambdaQuery()
                                    .eq(ServiceProviderCooperationScopeRel::getScopeCode, cooperationScope))
                    .stream()
                    .map(ServiceProviderCooperationScopeRel::getServiceProviderId)
                    .distinct()
                    .collect(Collectors.toList());
            if (serviceProviderIds.isEmpty()) {
                return new PageResponse<Map<String, Object>>(0, pageNo, pageSize, Collections.<Map<String, Object>>emptyList());
            }
        }

        LambdaQueryWrapper<ServiceProvider> wrapper = new LambdaQueryWrapper<ServiceProvider>()
                .and(keyword != null && !keyword.trim().isEmpty(),
                        q -> q.like(ServiceProvider::getCode, keyword)
                                .or().like(ServiceProvider::getName, keyword)
                                .or().like(ServiceProvider::getShortName, keyword)
                                .or().like(ServiceProvider::getUnifiedSocialCreditCode, keyword))
                .in(serviceProviderIds != null, ServiceProvider::getId, serviceProviderIds)
                .eq(status != null && !status.trim().isEmpty(), ServiceProvider::getStatus, status)
                .orderByAsc(ServiceProvider::getCode);

        Page<ServiceProvider> page = serviceProviderMapper.selectPage(new Page<ServiceProvider>(pageNo, pageSize), wrapper);
        List<Map<String, Object>> records = toProviderViews(page.getRecords());
        return new PageResponse<Map<String, Object>>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /**
     * 查询服务商下拉选项。
     */
    public List<Map<String, Object>> options() {
        return toProviderViews(serviceProviderMapper.selectList(
                Wrappers.<ServiceProvider>lambdaQuery().orderByAsc(ServiceProvider::getCode)));
    }

    /**
     * 查询服务商统计。
     */
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", serviceProviderMapper.selectCount(Wrappers.<ServiceProvider>lambdaQuery()));
        result.put("development", countByScope(CooperationScope.SOFTWARE_DEVELOPMENT.name()));
        result.put("ops", countByScope(CooperationScope.OPERATIONS_SERVICE.name()));
        result.put("hardware", countByScope(CooperationScope.HARDWARE_PROCUREMENT.name()));
        result.put("integration", countByScope(CooperationScope.INTEGRATION.name()));
        return result;
    }

    /**
     * 同步服务商关联关系数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncRelations(Long id, ServiceProviderRelationRequest request) {
        getById(id);
        List<Long> hardwareAssetIds = validateHardwareAssetIds(request.getHardwareAssetIds());
        List<Long> informationSystemIds = validateInformationSystemIds(request.getInformationSystemIds());
        List<Long> personIds = validatePersonIds(request.getPersonIds());
        List<Long> projectIds = normalizeIds(request.getProjectIds());

        for (Long projectId : projectIds) {
            supportService.ensureProjectExists(projectId);
        }

        syncHardwareRelations(id, hardwareAssetIds);
        syncInformationSystemRelations(id, informationSystemIds);
        projectVendorRelMapper.delete(Wrappers.<ProjectVendorRel>lambdaQuery()
                .eq(ProjectVendorRel::getServiceProviderId, id));
        for (Long projectId : projectIds) {
            ProjectVendorRel relation = new ProjectVendorRel();
            relation.setProjectId(projectId);
            relation.setServiceProviderId(id);
            projectVendorRelMapper.insert(relation);
        }

        syncPersonRelations(id, personIds);

        auditService.record("SERVICE_PROVIDER", id, AuditActionType.RELATION_SYNC,
                "Synchronized service provider relations", "SYSTEM");
    }

    /**
     * 删除服务商。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        Long hwCount = hardwareVendorRelMapper.selectCount(
                Wrappers.<AssetHardwareVendorRel>lambdaQuery().eq(AssetHardwareVendorRel::getServiceProviderId, id));
        if (hwCount > 0) {
            throw new BusinessException("该服务商仍被 " + hwCount + " 件硬件资产关联，无法删除");
        }
        Long sysCount = systemVendorRelMapper.selectCount(
                Wrappers.<SystemVendorRel>lambdaQuery().eq(SystemVendorRel::getServiceProviderId, id));
        if (sysCount > 0) {
            throw new BusinessException("该服务商仍被 " + sysCount + " 个信息系统关联，无法删除");
        }
        Long personCount = personMapper.selectCount(
                Wrappers.<Person>lambdaQuery().eq(Person::getServiceProviderId, id));
        if (personCount > 0) {
            throw new BusinessException("该服务商仍被 " + personCount + " 名人员关联，无法删除");
        }
        Long relatedPersonCount = serviceProviderPersonRelMapper.selectCount(
                Wrappers.<ServiceProviderPersonRel>lambdaQuery().eq(ServiceProviderPersonRel::getServiceProviderId, id));
        if (relatedPersonCount > 0) {
            throw new BusinessException("该服务商仍被 " + relatedPersonCount + " 条人员关联服务商记录引用，无法删除");
        }
        Long projectCount = projectVendorRelMapper.selectCount(
                Wrappers.<ProjectVendorRel>lambdaQuery().eq(ProjectVendorRel::getServiceProviderId, id));
        if (projectCount > 0) {
            throw new BusinessException("该服务商仍被 " + projectCount + " 个项目关联，无法删除");
        }
        cooperationScopeRelMapper.delete(
                Wrappers.<ServiceProviderCooperationScopeRel>lambdaQuery().eq(ServiceProviderCooperationScopeRel::getServiceProviderId, id));
        serviceProviderMapper.deleteById(id);
        auditService.record("SERVICE_PROVIDER", id, AuditActionType.DELETE, "Deleted provider " + id, "SYSTEM");
    }

    private void validateRequest(ServiceProviderUpsertRequest request, Long id) {
        supportService.ensureUniqueServiceProviderCode(request.getCode(), id);
        supportService.ensureCommonStatusValid(request.getStatus(), "服务商");
        supportService.ensureEnterpriseNatureValid(request.getEnterpriseNature());
        supportService.ensureVendorLevelValid(request.getVendorLevel());
        supportService.ensureCooperationScopesValid(resolveCooperationScopes(request));
        supportService.ensureScoreValid(resolveScore(request));
        validateHardwareAssetIds(request.getHardwareAssetIds());
        validateInformationSystemIds(request.getInformationSystemIds());
        validatePersonIds(request.getPersonIds());
    }

    private ServiceProvider toEntity(ServiceProviderUpsertRequest request) {
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setCode(request.getCode());
        serviceProvider.setName(request.getName());
        serviceProvider.setShortName(request.getShortName());
        serviceProvider.setLogoUrl(request.getLogoUrl());
        serviceProvider.setUnifiedSocialCreditCode(request.getUnifiedSocialCreditCode());
        serviceProvider.setType(resolveLegacyType(resolveCooperationScopes(request)));
        serviceProvider.setEnterpriseNature(request.getEnterpriseNature());
        serviceProvider.setVendorLevel(resolveVendorLevel(request));
        serviceProvider.setScore(resolveScore(request));
        serviceProvider.setRatingLevel(resolveLegacyRatingLevel(request));
        serviceProvider.setBusinessContact(request.getBusinessContact());
        serviceProvider.setBusinessPhone(request.getBusinessPhone());
        serviceProvider.setStatus(request.getStatus());
        serviceProvider.setRemark(request.getRemark());
        return serviceProvider;
    }

    private List<Map<String, Object>> toProviderViews(List<ServiceProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = providers.stream().map(ServiceProvider::getId).collect(Collectors.toList());
        Map<Long, List<String>> scopeMap = loadCooperationScopeMap(ids);
        return providers.stream()
                .map(item -> toProviderView(item, scopeMap.get(item.getId())))
                .collect(Collectors.toList());
    }

    private Map<String, Object> toProviderView(ServiceProvider serviceProvider) {
        return toProviderView(serviceProvider, loadCooperationScopeMap(Collections.singletonList(serviceProvider.getId()))
                .get(serviceProvider.getId()));
    }

    private Map<String, Object> toProviderView(ServiceProvider serviceProvider, List<String> cooperationScopes) {
        List<String> scopeValues = cooperationScopes == null ? Collections.<String>emptyList() : cooperationScopes;
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("id", serviceProvider.getId());
        view.put("code", serviceProvider.getCode());
        view.put("name", serviceProvider.getName());
        view.put("shortName", serviceProvider.getShortName());
        view.put("logoUrl", serviceProvider.getLogoUrl());
        view.put("unifiedSocialCreditCode", serviceProvider.getUnifiedSocialCreditCode());
        view.put("type", serviceProvider.getType());
        view.put("enterpriseNature", serviceProvider.getEnterpriseNature());
        view.put("cooperationScopes", scopeValues);
        view.put("vendorLevel", serviceProvider.getVendorLevel());
        view.put("score", serviceProvider.getScore());
        view.put("ratingLevel", serviceProvider.getRatingLevel());
        view.put("businessContact", serviceProvider.getBusinessContact());
        view.put("businessPhone", serviceProvider.getBusinessPhone());
        view.put("status", serviceProvider.getStatus());
        view.put("remark", serviceProvider.getRemark());
        view.put("createdAt", serviceProvider.getCreatedAt());
        view.put("updatedAt", serviceProvider.getUpdatedAt());
        return view;
    }

    private Map<Long, List<String>> loadCooperationScopeMap(List<Long> serviceProviderIds) {
        List<Long> filteredIds = serviceProviderIds.stream().filter(item -> item != null).distinct().collect(Collectors.toList());
        if (filteredIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, LinkedHashSet<String>> grouped = new LinkedHashMap<Long, LinkedHashSet<String>>();
        cooperationScopeRelMapper.selectList(
                        Wrappers.<ServiceProviderCooperationScopeRel>lambdaQuery()
                                .in(ServiceProviderCooperationScopeRel::getServiceProviderId, filteredIds)
                                .orderByAsc(ServiceProviderCooperationScopeRel::getServiceProviderId)
                                .orderByAsc(ServiceProviderCooperationScopeRel::getScopeCode))
                .forEach(item -> grouped.computeIfAbsent(item.getServiceProviderId(), key -> new LinkedHashSet<String>())
                        .add(item.getScopeCode()));

        Map<Long, List<String>> result = new LinkedHashMap<Long, List<String>>();
        for (Map.Entry<Long, LinkedHashSet<String>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
        }
        return result;
    }

    private void syncCooperationScopes(Long serviceProviderId, List<String> scopeCodes) {
        List<String> normalizedScopes = scopeCodes == null ? Collections.<String>emptyList() :
                scopeCodes.stream().filter(item -> item != null && !item.trim().isEmpty()).distinct().collect(Collectors.toList());
        cooperationScopeRelMapper.delete(Wrappers.<ServiceProviderCooperationScopeRel>lambdaQuery()
                .eq(ServiceProviderCooperationScopeRel::getServiceProviderId, serviceProviderId));
        for (String scopeCode : normalizedScopes) {
            ServiceProviderCooperationScopeRel relation = new ServiceProviderCooperationScopeRel();
            relation.setServiceProviderId(serviceProviderId);
            relation.setScopeCode(scopeCode);
            cooperationScopeRelMapper.insert(relation);
        }
    }

    private Long countByScope(String scopeCode) {
        return cooperationScopeRelMapper.selectList(
                        Wrappers.<ServiceProviderCooperationScopeRel>lambdaQuery()
                                .eq(ServiceProviderCooperationScopeRel::getScopeCode, scopeCode))
                .stream()
                .map(ServiceProviderCooperationScopeRel::getServiceProviderId)
                .distinct()
                .count();
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream().filter(item -> item != null).distinct().collect(Collectors.toList());
    }

    private List<Long> validateHardwareAssetIds(List<Long> hardwareAssetIds) {
        List<Long> normalizedIds = normalizeIds(hardwareAssetIds);
        for (Long hardwareAssetId : normalizedIds) {
            supportService.ensureHardwareExists(hardwareAssetId);
        }
        return normalizedIds;
    }

    private List<Long> validateInformationSystemIds(List<Long> informationSystemIds) {
        List<Long> normalizedIds = normalizeIds(informationSystemIds);
        for (Long informationSystemId : normalizedIds) {
            supportService.ensureInformationSystemExists(informationSystemId);
        }
        return normalizedIds;
    }

    private List<Long> validatePersonIds(List<Long> personIds) {
        List<Long> normalizedIds = normalizeIds(personIds);
        for (Long personId : normalizedIds) {
            supportService.ensurePersonExists(personId);
        }
        return normalizedIds;
    }

    private void syncFormRelations(Long serviceProviderId, ServiceProviderUpsertRequest request, boolean createMode) {
        if (createMode || request.getHardwareAssetIds() != null) {
            syncHardwareRelations(serviceProviderId, validateHardwareAssetIds(request.getHardwareAssetIds()));
        }
        if (createMode || request.getInformationSystemIds() != null) {
            syncInformationSystemRelations(serviceProviderId, validateInformationSystemIds(request.getInformationSystemIds()));
        }
        if (createMode || request.getPersonIds() != null) {
            syncPersonRelations(serviceProviderId, validatePersonIds(request.getPersonIds()));
        }
    }

    private void syncHardwareRelations(Long serviceProviderId, List<Long> hardwareAssetIds) {
        hardwareVendorRelMapper.delete(Wrappers.<AssetHardwareVendorRel>lambdaQuery()
                .eq(AssetHardwareVendorRel::getServiceProviderId, serviceProviderId));
        for (Long hardwareAssetId : hardwareAssetIds) {
            AssetHardwareVendorRel relation = new AssetHardwareVendorRel();
            relation.setHardwareAssetId(hardwareAssetId);
            relation.setServiceProviderId(serviceProviderId);
            hardwareVendorRelMapper.insert(relation);
        }
    }

    private void syncInformationSystemRelations(Long serviceProviderId, List<Long> informationSystemIds) {
        systemVendorRelMapper.delete(Wrappers.<SystemVendorRel>lambdaQuery()
                .eq(SystemVendorRel::getServiceProviderId, serviceProviderId));
        for (Long informationSystemId : informationSystemIds) {
            SystemVendorRel relation = new SystemVendorRel();
            relation.setInformationSystemId(informationSystemId);
            relation.setServiceProviderId(serviceProviderId);
            systemVendorRelMapper.insert(relation);
        }
    }

    private void syncPersonRelations(Long serviceProviderId, List<Long> personIds) {
        List<Person> currentPersons = personMapper.selectList(
                Wrappers.<Person>lambdaQuery().eq(Person::getServiceProviderId, serviceProviderId));
        for (Person person : currentPersons) {
            if (personIds.contains(person.getId())) {
                continue;
            }
            Person update = new Person();
            update.setId(person.getId());
            update.setServiceProviderId(null);
            personMapper.updateById(update);
        }
        for (Long personId : personIds) {
            Person update = new Person();
            update.setId(personId);
            update.setServiceProviderId(serviceProviderId);
            personMapper.updateById(update);
        }
    }

    private String resolveLegacyType(List<String> cooperationScopes) {
        List<String> normalizedScopes = cooperationScopes == null ? Collections.<String>emptyList() :
                cooperationScopes.stream().filter(item -> item != null && !item.trim().isEmpty()).collect(Collectors.toList());
        if (normalizedScopes.contains(CooperationScope.SOFTWARE_DEVELOPMENT.name())) {
            return "SERVICE_PROVIDER";
        }
        if (normalizedScopes.contains(CooperationScope.OPERATIONS_SERVICE.name())) {
            return "MAINTENANCE";
        }
        if (normalizedScopes.contains(CooperationScope.HARDWARE_PROCUREMENT.name())) {
            return "SUPPLIER";
        }
        if (normalizedScopes.contains(CooperationScope.INTEGRATION.name())) {
            return "INTEGRATOR";
        }
        return null;
    }

    private List<String> resolveCooperationScopes(ServiceProviderUpsertRequest request) {
        if (request.getCooperationScopes() != null && !request.getCooperationScopes().isEmpty()) {
            return request.getCooperationScopes();
        }
        if (request.getType() == null || request.getType().trim().isEmpty()) {
            return Collections.emptyList();
        }
        if ("SUPPLIER".equals(request.getType())) {
            return Collections.singletonList(CooperationScope.HARDWARE_PROCUREMENT.name());
        }
        if ("SERVICE_PROVIDER".equals(request.getType())) {
            return Collections.singletonList(CooperationScope.SOFTWARE_DEVELOPMENT.name());
        }
        if ("INTEGRATOR".equals(request.getType())) {
            return Collections.singletonList(CooperationScope.INTEGRATION.name());
        }
        if ("MAINTENANCE".equals(request.getType())) {
            return Collections.singletonList(CooperationScope.OPERATIONS_SERVICE.name());
        }
        return Collections.emptyList();
    }

    private Integer resolveScore(ServiceProviderUpsertRequest request) {
        if (request.getScore() != null) {
            return request.getScore();
        }
        if (request.getRatingLevel() == null || request.getRatingLevel().trim().isEmpty()) {
            return null;
        }
        if ("S".equals(request.getRatingLevel())) {
            return 5;
        }
        if ("A".equals(request.getRatingLevel())) {
            return 4;
        }
        if ("B".equals(request.getRatingLevel())) {
            return 3;
        }
        if ("C".equals(request.getRatingLevel())) {
            return 2;
        }
        return null;
    }

    private String resolveVendorLevel(ServiceProviderUpsertRequest request) {
        if (request.getVendorLevel() != null && !request.getVendorLevel().trim().isEmpty()) {
            return request.getVendorLevel();
        }
        if (request.getRatingLevel() == null || request.getRatingLevel().trim().isEmpty()) {
            return null;
        }
        if ("S".equals(request.getRatingLevel())) {
            return VendorLevel.STRATEGIC_PARTNER.name();
        }
        if ("A".equals(request.getRatingLevel())) {
            return VendorLevel.CORE_SUPPLIER.name();
        }
        if ("B".equals(request.getRatingLevel()) || "C".equals(request.getRatingLevel())) {
            return VendorLevel.GENERAL_SUPPLIER.name();
        }
        return null;
    }

    private String resolveLegacyRatingLevel(ServiceProviderUpsertRequest request) {
        if (request.getVendorLevel() == null || request.getVendorLevel().trim().isEmpty()) {
            if (request.getRatingLevel() != null && !request.getRatingLevel().trim().isEmpty()) {
                return request.getRatingLevel();
            }
            Integer score = resolveScore(request);
            if (score == null) {
                return null;
            }
            if (score >= 5) {
                return "S";
            }
            if (score == 4) {
                return "A";
            }
            if (score == 3) {
                return "B";
            }
            if (score <= 2) {
                return "C";
            }
            return null;
        }

        if (VendorLevel.STRATEGIC_PARTNER.name().equals(request.getVendorLevel())) {
            return "S";
        }
        if (VendorLevel.CORE_SUPPLIER.name().equals(request.getVendorLevel())) {
            return "A";
        }
        if (VendorLevel.GENERAL_SUPPLIER.name().equals(request.getVendorLevel())) {
            Integer score = resolveScore(request);
            return score != null && score >= 3 ? "B" : "C";
        }
        return null;
    }
}
