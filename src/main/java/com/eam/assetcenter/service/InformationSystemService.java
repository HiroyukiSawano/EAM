package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.enums.CommonStatus;
import com.eam.assetcenter.common.enums.PersonRelationType;
import com.eam.assetcenter.common.enums.SystemType;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.AssetHardware;
import com.eam.assetcenter.domain.entity.AssetHardwareSystemRel;
import com.eam.assetcenter.domain.entity.InformationSystem;
import com.eam.assetcenter.domain.entity.Person;
import com.eam.assetcenter.domain.entity.ProjectInfo;
import com.eam.assetcenter.domain.entity.ProjectSystemRel;
import com.eam.assetcenter.domain.entity.ServiceProvider;
import com.eam.assetcenter.domain.entity.SystemPersonRel;
import com.eam.assetcenter.domain.entity.SystemVendorRel;
import com.eam.assetcenter.domain.entity.ServiceProviderCooperationScopeRel;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareSystemRelMapper;
import com.eam.assetcenter.infrastructure.mapper.InformationSystemMapper;
import com.eam.assetcenter.infrastructure.mapper.PersonMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectInfoMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectSystemRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderCooperationScopeRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderMapper;
import com.eam.assetcenter.infrastructure.mapper.SystemPersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.SystemVendorRelMapper;
import com.eam.assetcenter.web.request.InformationSystemRelationRequest;
import com.eam.assetcenter.web.request.InformationSystemUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 信息系统业务服务，负责软件资源台账和关联关系维护。
 */
@Service
@RequiredArgsConstructor
public class InformationSystemService {

    private final InformationSystemMapper informationSystemMapper;
    private final SystemVendorRelMapper systemVendorRelMapper;
    private final SystemPersonRelMapper systemPersonRelMapper;
    private final ProjectSystemRelMapper projectSystemRelMapper;
    private final AssetHardwareSystemRelMapper hardwareSystemRelMapper;
    private final ServiceProviderMapper serviceProviderMapper;
    private final ServiceProviderCooperationScopeRelMapper serviceProviderCooperationScopeRelMapper;
    private final PersonMapper personMapper;
    private final ProjectInfoMapper projectInfoMapper;
    private final AssetHardwareMapper assetHardwareMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增软件资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(InformationSystemUpsertRequest request) {
        validateRequest(request, null);
        InformationSystem informationSystem = toEntity(request, null);
        informationSystemMapper.insert(informationSystem);
        syncFormRelations(informationSystem.getId(), request, true);
        auditService.record("INFORMATION_SYSTEM", informationSystem.getId(), AuditActionType.CREATE,
                "Created information system " + informationSystem.getCode(), "SYSTEM");
        return toInformationSystemView(getById(informationSystem.getId()));
    }

    /**
     * 更新软件资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, InformationSystemUpsertRequest request) {
        InformationSystem existing = getById(id);
        validateRequest(request, id);
        InformationSystem informationSystem = toEntity(request, existing);
        updateInformationSystem(id, informationSystem);
        syncFormRelations(id, request, false);
        auditService.record("INFORMATION_SYSTEM", id, AuditActionType.UPDATE,
                "Updated information system " + informationSystem.getCode(), "SYSTEM");
        return toInformationSystemView(getById(id));
    }

    /**
     * 根据主键查询软件资源，不存在时抛出业务异常。
     */
    public InformationSystem getById(Long id) {
        InformationSystem informationSystem = informationSystemMapper.selectById(id);
        if (informationSystem == null) {
            throw new BusinessException("Information system not found: " + id);
        }
        return informationSystem;
    }

    /**
     * 查询软件资源详情，并聚合关联对象。
     */
    public Map<String, Object> getDetail(Long id) {
        InformationSystem informationSystem = getById(id);
        List<Long> serviceProviderIds = systemVendorRelMapper.selectList(
                        Wrappers.<SystemVendorRel>lambdaQuery().eq(SystemVendorRel::getInformationSystemId, id))
                .stream().map(SystemVendorRel::getServiceProviderId).collect(Collectors.toList());
        List<Long> personIds = systemPersonRelMapper.selectList(
                        Wrappers.<SystemPersonRel>lambdaQuery().eq(SystemPersonRel::getInformationSystemId, id))
                .stream().map(SystemPersonRel::getPersonId).collect(Collectors.toList());
        List<Long> hardwareAssetIds = hardwareSystemRelMapper.selectList(
                        Wrappers.<AssetHardwareSystemRel>lambdaQuery().eq(AssetHardwareSystemRel::getInformationSystemId, id))
                .stream().map(AssetHardwareSystemRel::getHardwareAssetId).collect(Collectors.toList());
        List<Long> projectIds = projectSystemRelMapper.selectList(
                        Wrappers.<ProjectSystemRel>lambdaQuery().eq(ProjectSystemRel::getInformationSystemId, id))
                .stream().map(ProjectSystemRel::getProjectId).collect(Collectors.toList());

        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("informationSystem", toInformationSystemView(informationSystem));
        detail.put("serviceProviderIds", serviceProviderIds);
        detail.put("personIds", personIds);
        detail.put("hardwareAssetIds", hardwareAssetIds);
        detail.put("projectIds", projectIds);
        detail.put("serviceProviders", buildServiceProviderSummaries(serviceProviderIds));
        detail.put("persons", buildPersonSummaries(personIds));
        detail.put("hardwareAssets", buildHardwareAssetSummaries(hardwareAssetIds));
        detail.put("projects", buildProjectSummaries(projectIds));
        return detail;
    }

    /**
     * 按条件分页查询软件资源。
     */
    public PageResponse<Map<String, Object>> page(int pageNo, int pageSize, String keyword, String systemType,
                                                  String deploymentArchitecture, String status) {
        supportService.ensureSystemTypeValid(systemType);
        supportService.ensureDeploymentArchitectureValid(deploymentArchitecture);
        if (StringUtils.hasText(status)) {
            supportService.ensureCommonStatusValid(status, "软件资源");
        }

        LambdaQueryWrapper<InformationSystem> wrapper = new LambdaQueryWrapper<InformationSystem>()
                .and(StringUtils.hasText(keyword),
                        query -> query.like(InformationSystem::getCode, keyword).or().like(InformationSystem::getName, keyword))
                .eq(StringUtils.hasText(systemType), InformationSystem::getSystemType, systemType)
                .eq(StringUtils.hasText(deploymentArchitecture), InformationSystem::getDeploymentArchitecture, deploymentArchitecture)
                .eq(StringUtils.hasText(status), InformationSystem::getStatus, status)
                .orderByAsc(InformationSystem::getCode);

        Page<InformationSystem> page = informationSystemMapper.selectPage(new Page<InformationSystem>(pageNo, pageSize), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream().map(this::toInformationSystemView).collect(Collectors.toList());
        return new PageResponse<Map<String, Object>>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /**
     * 查询软件资源下拉选项。
     */
    public List<InformationSystem> options() {
        return informationSystemMapper.selectList(new LambdaQueryWrapper<InformationSystem>().orderByAsc(InformationSystem::getCode));
    }

    /**
     * 查询软件资源统计。
     */
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", informationSystemMapper.selectCount(Wrappers.<InformationSystem>lambdaQuery()));
        result.put("externalService", countBySystemType(SystemType.EXTERNAL_SERVICE.name()));
        result.put("internalOffice", countBySystemType(SystemType.INTERNAL_OFFICE.name()));
        result.put("databaseSoftware", countBySystemType(SystemType.DATABASE_SOFTWARE.name()));
        result.put("basicSupport", countBySystemType(SystemType.BASIC_SUPPORT.name()));
        result.put("securitySoftware", countBySystemType(SystemType.SECURITY_SOFTWARE.name()));
        return result;
    }

    /**
     * 同步软件资源关联关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncRelations(Long id, InformationSystemRelationRequest request) {
        getById(id);
        syncServiceProviderRelations(id, validateServiceProviderIds(request.getServiceProviderIds()));
        syncPersonRelations(id, validatePersonIds(request.getPersonIds()));
        syncHardwareRelations(id, validateHardwareIds(request.getHardwareAssetIds()));
        auditService.record("INFORMATION_SYSTEM", id, AuditActionType.RELATION_SYNC, "Synchronized information system relations", "SYSTEM");
    }

    /**
     * 删除软件资源记录，同时级联清理所有关联表。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        InformationSystem existing = getById(id);
        systemVendorRelMapper.delete(Wrappers.<SystemVendorRel>lambdaQuery().eq(SystemVendorRel::getInformationSystemId, id));
        systemPersonRelMapper.delete(Wrappers.<SystemPersonRel>lambdaQuery().eq(SystemPersonRel::getInformationSystemId, id));
        projectSystemRelMapper.delete(Wrappers.<ProjectSystemRel>lambdaQuery().eq(ProjectSystemRel::getInformationSystemId, id));
        hardwareSystemRelMapper.delete(Wrappers.<AssetHardwareSystemRel>lambdaQuery().eq(AssetHardwareSystemRel::getInformationSystemId, id));
        informationSystemMapper.deleteById(id);
        auditService.record("INFORMATION_SYSTEM", id, AuditActionType.DELETE, "Deleted information system " + existing.getCode(), "SYSTEM");
    }

    private void validateRequest(InformationSystemUpsertRequest request, Long excludeId) {
        supportService.ensureUniqueSystemCode(request.getCode(), excludeId);
        supportService.ensureSystemTypeValid(request.getSystemType());
        supportService.ensureDeploymentArchitectureValid(request.getDeploymentArchitecture());
        if (StringUtils.hasText(request.getStatus())) {
            supportService.ensureCommonStatusValid(request.getStatus(), "软件资源");
        }
        if (!StringUtils.hasText(request.getOwnerName())) {
            supportService.ensurePersonExists(request.getOwnerPersonId());
        }
        validateServiceProviderIds(request.getServiceProviderIds());
        validatePersonIds(request.getPersonIds());
        validateHardwareIds(request.getHardwareAssetIds());
    }

    private InformationSystem toEntity(InformationSystemUpsertRequest request, InformationSystem existing) {
        InformationSystem informationSystem = new InformationSystem();
        informationSystem.setCode(request.getCode());
        informationSystem.setName(request.getName());
        informationSystem.setSystemType(request.getSystemType());
        informationSystem.setVersionNo(request.getVersionNo());
        informationSystem.setDeploymentArchitecture(request.getDeploymentArchitecture());
        informationSystem.setOwnerPersonId(StringUtils.hasText(request.getOwnerName()) ? null : request.getOwnerPersonId());
        informationSystem.setOwnerName(request.getOwnerName());
        informationSystem.setContactPhone(request.getContactPhone());
        informationSystem.setStatus(StringUtils.hasText(request.getStatus())
                ? request.getStatus()
                : existing == null ? CommonStatus.ACTIVE.name() : existing.getStatus());
        informationSystem.setRemark(request.getRemark());
        return informationSystem;
    }

    private void updateInformationSystem(Long id, InformationSystem informationSystem) {
        informationSystemMapper.update(
                null,
                Wrappers.<InformationSystem>lambdaUpdate()
                        .eq(InformationSystem::getId, id)
                        .set(InformationSystem::getCode, informationSystem.getCode())
                        .set(InformationSystem::getName, informationSystem.getName())
                        .set(InformationSystem::getSystemType, informationSystem.getSystemType())
                        .set(InformationSystem::getVersionNo, informationSystem.getVersionNo())
                        .set(InformationSystem::getDeploymentArchitecture, informationSystem.getDeploymentArchitecture())
                        .set(InformationSystem::getOwnerPersonId, informationSystem.getOwnerPersonId())
                        .set(InformationSystem::getOwnerName, informationSystem.getOwnerName())
                        .set(InformationSystem::getContactPhone, informationSystem.getContactPhone())
                        .set(InformationSystem::getStatus, informationSystem.getStatus())
                        .set(InformationSystem::getRemark, informationSystem.getRemark())
                        .set(InformationSystem::getUpdatedAt, LocalDateTime.now()));
    }

    private Map<String, Object> toInformationSystemView(InformationSystem informationSystem) {
        Map<Long, String> ownerNameMap = loadPersonNameMap(Collections.singletonList(informationSystem.getOwnerPersonId()));
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("id", informationSystem.getId());
        view.put("code", informationSystem.getCode());
        view.put("name", informationSystem.getName());
        view.put("systemType", informationSystem.getSystemType());
        view.put("versionNo", informationSystem.getVersionNo());
        view.put("deploymentArchitecture", informationSystem.getDeploymentArchitecture());
        view.put("ownerPersonId", informationSystem.getOwnerPersonId());
        view.put("ownerName", firstNonBlank(informationSystem.getOwnerName(), ownerNameMap.get(informationSystem.getOwnerPersonId())));
        view.put("contactPhone", informationSystem.getContactPhone());
        view.put("status", informationSystem.getStatus());
        view.put("remark", informationSystem.getRemark());
        view.put("createdAt", informationSystem.getCreatedAt());
        view.put("updatedAt", informationSystem.getUpdatedAt());
        return view;
    }

    private void syncFormRelations(Long informationSystemId, InformationSystemUpsertRequest request, boolean createMode) {
        if (createMode || request.getServiceProviderIds() != null) {
            syncServiceProviderRelations(informationSystemId, validateServiceProviderIds(request.getServiceProviderIds()));
        }
        if (createMode || request.getPersonIds() != null) {
            syncPersonRelations(informationSystemId, validatePersonIds(request.getPersonIds()));
        }
        if (createMode || request.getHardwareAssetIds() != null) {
            syncHardwareRelations(informationSystemId, validateHardwareIds(request.getHardwareAssetIds()));
        }
    }

    private void syncServiceProviderRelations(Long informationSystemId, List<Long> serviceProviderIds) {
        systemVendorRelMapper.delete(Wrappers.<SystemVendorRel>lambdaQuery().eq(SystemVendorRel::getInformationSystemId, informationSystemId));
        for (Long serviceProviderId : serviceProviderIds) {
            SystemVendorRel relation = new SystemVendorRel();
            relation.setInformationSystemId(informationSystemId);
            relation.setServiceProviderId(serviceProviderId);
            systemVendorRelMapper.insert(relation);
        }
    }

    private void syncPersonRelations(Long informationSystemId, List<Long> personIds) {
        systemPersonRelMapper.delete(Wrappers.<SystemPersonRel>lambdaQuery().eq(SystemPersonRel::getInformationSystemId, informationSystemId));
        for (Long personId : personIds) {
            SystemPersonRel relation = new SystemPersonRel();
            relation.setInformationSystemId(informationSystemId);
            relation.setPersonId(personId);
            relation.setRelationType(PersonRelationType.USER.name());
            systemPersonRelMapper.insert(relation);
        }
    }

    private void syncHardwareRelations(Long informationSystemId, List<Long> hardwareAssetIds) {
        hardwareSystemRelMapper.delete(Wrappers.<AssetHardwareSystemRel>lambdaQuery().eq(AssetHardwareSystemRel::getInformationSystemId, informationSystemId));
        for (Long hardwareAssetId : hardwareAssetIds) {
            AssetHardwareSystemRel relation = new AssetHardwareSystemRel();
            relation.setInformationSystemId(informationSystemId);
            relation.setHardwareAssetId(hardwareAssetId);
            hardwareSystemRelMapper.insert(relation);
        }
    }

    private List<Map<String, Object>> buildServiceProviderSummaries(List<Long> serviceProviderIds) {
        List<Long> normalizedIds = normalizeIds(serviceProviderIds);
        if (normalizedIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<String>> scopeMap = loadCooperationScopeMap(normalizedIds);
        Map<Long, ServiceProvider> providerMap = serviceProviderMapper.selectList(
                        Wrappers.<ServiceProvider>lambdaQuery().in(ServiceProvider::getId, normalizedIds))
                .stream()
                .collect(Collectors.toMap(ServiceProvider::getId, item -> item));

        return normalizedIds.stream()
                .map(providerMap::get)
                .filter(item -> item != null)
                .map(item -> {
                    Map<String, Object> summary = new LinkedHashMap<String, Object>();
                    summary.put("id", item.getId());
                    summary.put("name", item.getName());
                    summary.put("code", item.getCode());
                    summary.put("unifiedSocialCreditCode", item.getUnifiedSocialCreditCode());
                    summary.put("logoUrl", item.getLogoUrl());
                    summary.put("vendorLevel", item.getVendorLevel());
                    summary.put("cooperationScopes", scopeMap.getOrDefault(item.getId(), Collections.<String>emptyList()));
                    summary.put("score", item.getScore());
                    summary.put("businessContact", item.getBusinessContact());
                    summary.put("businessPhone", item.getBusinessPhone());
                    return summary;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildPersonSummaries(List<Long> personIds) {
        List<Long> normalizedIds = normalizeIds(personIds);
        if (normalizedIds.isEmpty()) {
            return Collections.emptyList();
        }

        return personMapper.selectList(Wrappers.<Person>lambdaQuery().in(Person::getId, normalizedIds))
                .stream()
                .sorted((left, right) -> normalizedIds.indexOf(left.getId()) - normalizedIds.indexOf(right.getId()))
                .map(item -> {
                    Map<String, Object> summary = new LinkedHashMap<String, Object>();
                    summary.put("id", item.getId());
                    summary.put("name", item.getName());
                    summary.put("employeeNo", item.getEmployeeNo());
                    summary.put("mobile", item.getMobile());
                    summary.put("gender", item.getGender());
                    return summary;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildHardwareAssetSummaries(List<Long> hardwareAssetIds) {
        List<Long> normalizedIds = normalizeIds(hardwareAssetIds);
        if (normalizedIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, AssetHardware> hardwareMap = assetHardwareMapper.selectList(
                        Wrappers.<AssetHardware>lambdaQuery().in(AssetHardware::getId, normalizedIds))
                .stream()
                .collect(Collectors.toMap(AssetHardware::getId, item -> item));

        return normalizedIds.stream()
                .map(hardwareMap::get)
                .filter(item -> item != null)
                .map(item -> {
                    Map<String, Object> summary = new LinkedHashMap<String, Object>();
                    summary.put("id", item.getId());
                    summary.put("name", item.getAssetName());
                    summary.put("code", item.getAssetCode());
                    summary.put("hardwareCategory", item.getHardwareCategory());
                    summary.put("managementIp", item.getManagementIp());
                    summary.put("cpuModel", item.getCpuModel());
                    summary.put("memoryGb", item.getMemoryGb());
                    return summary;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildProjectSummaries(List<Long> projectIds) {
        List<Long> normalizedIds = normalizeIds(projectIds);
        if (normalizedIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ProjectInfo> projectMap = projectInfoMapper.selectList(
                        Wrappers.<ProjectInfo>lambdaQuery().in(ProjectInfo::getId, normalizedIds))
                .stream()
                .collect(Collectors.toMap(ProjectInfo::getId, item -> item));

        return normalizedIds.stream()
                .map(projectMap::get)
                .filter(item -> item != null)
                .map(item -> {
                    Map<String, Object> summary = new LinkedHashMap<String, Object>();
                    summary.put("id", item.getId());
                    summary.put("name", item.getName());
                    summary.put("code", item.getCode());
                    summary.put("projectType", item.getProjectType());
                    summary.put("projectStatus", item.getProjectStatus());
                    summary.put("ownerName", item.getOwnerName());
                    return summary;
                })
                .collect(Collectors.toList());
    }

    private Long countBySystemType(String systemType) {
        return informationSystemMapper.selectCount(Wrappers.<InformationSystem>lambdaQuery().eq(InformationSystem::getSystemType, systemType));
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream().filter(item -> item != null).distinct().collect(Collectors.toList());
    }

    private List<Long> validateServiceProviderIds(List<Long> serviceProviderIds) {
        List<Long> normalizedIds = normalizeIds(serviceProviderIds);
        for (Long serviceProviderId : normalizedIds) {
            supportService.ensureServiceProviderExists(serviceProviderId);
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

    private List<Long> validateHardwareIds(List<Long> hardwareAssetIds) {
        List<Long> normalizedIds = normalizeIds(hardwareAssetIds);
        for (Long hardwareAssetId : normalizedIds) {
            supportService.ensureHardwareExists(hardwareAssetId);
        }
        return normalizedIds;
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private Map<Long, String> loadPersonNameMap(List<Long> personIds) {
        List<Long> filteredIds = normalizeIds(personIds);
        if (filteredIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return personMapper.selectList(Wrappers.<Person>lambdaQuery().in(Person::getId, filteredIds))
                .stream()
                .collect(Collectors.toMap(Person::getId, Person::getName));
    }

    private Map<Long, List<String>> loadCooperationScopeMap(List<Long> serviceProviderIds) {
        List<Long> filteredIds = normalizeIds(serviceProviderIds);
        if (filteredIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, java.util.LinkedHashSet<String>> grouped = new LinkedHashMap<Long, java.util.LinkedHashSet<String>>();
        serviceProviderCooperationScopeRelMapper.selectList(
                        Wrappers.<ServiceProviderCooperationScopeRel>lambdaQuery()
                                .in(ServiceProviderCooperationScopeRel::getServiceProviderId, filteredIds)
                                .orderByAsc(ServiceProviderCooperationScopeRel::getServiceProviderId)
                                .orderByAsc(ServiceProviderCooperationScopeRel::getScopeCode))
                .forEach(item -> grouped.computeIfAbsent(item.getServiceProviderId(), key -> new java.util.LinkedHashSet<String>())
                        .add(item.getScopeCode()));

        Map<Long, List<String>> result = new LinkedHashMap<Long, List<String>>();
        for (Map.Entry<Long, java.util.LinkedHashSet<String>> entry : grouped.entrySet()) {
            result.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
        }
        return result;
    }
}
