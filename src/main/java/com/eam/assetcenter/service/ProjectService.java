package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.enums.PersonRelationType;
import com.eam.assetcenter.common.enums.ProjectType;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.AssetHardware;
import com.eam.assetcenter.domain.entity.AssetHardwareVendorRel;
import com.eam.assetcenter.domain.entity.InformationSystem;
import com.eam.assetcenter.domain.entity.Person;
import com.eam.assetcenter.domain.entity.ProjectDocument;
import com.eam.assetcenter.domain.entity.ProjectHardwareRel;
import com.eam.assetcenter.domain.entity.ProjectInfo;
import com.eam.assetcenter.domain.entity.ProjectPersonRel;
import com.eam.assetcenter.domain.entity.ProjectSystemRel;
import com.eam.assetcenter.domain.entity.ProjectVendorRel;
import com.eam.assetcenter.domain.entity.ServiceProvider;
import com.eam.assetcenter.domain.entity.ServiceProviderCooperationScopeRel;
import com.eam.assetcenter.domain.entity.SystemVendorRel;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareVendorRelMapper;
import com.eam.assetcenter.infrastructure.mapper.InformationSystemMapper;
import com.eam.assetcenter.infrastructure.mapper.PersonMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectDocumentMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectHardwareRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectInfoMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectPersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectSystemRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectVendorRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderCooperationScopeRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderMapper;
import com.eam.assetcenter.infrastructure.mapper.SystemVendorRelMapper;
import com.eam.assetcenter.web.request.ProjectDocumentRequest;
import com.eam.assetcenter.web.request.ProjectRelationRequest;
import com.eam.assetcenter.web.request.ProjectUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目业务服务，负责项目台账、文档与关联关系维护。
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectInfoMapper projectInfoMapper;
    private final ProjectDocumentMapper projectDocumentMapper;
    private final ProjectSystemRelMapper projectSystemRelMapper;
    private final ProjectVendorRelMapper projectVendorRelMapper;
    private final ProjectPersonRelMapper projectPersonRelMapper;
    private final ProjectHardwareRelMapper projectHardwareRelMapper;
    private final InformationSystemMapper informationSystemMapper;
    private final SystemVendorRelMapper systemVendorRelMapper;
    private final ServiceProviderMapper serviceProviderMapper;
    private final ServiceProviderCooperationScopeRelMapper serviceProviderCooperationScopeRelMapper;
    private final PersonMapper personMapper;
    private final AssetHardwareMapper assetHardwareMapper;
    private final AssetHardwareVendorRelMapper assetHardwareVendorRelMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增项目。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(ProjectUpsertRequest request) {
        validateRequest(request, null);
        ProjectInfo projectInfo = toEntity(request);
        projectInfoMapper.insert(projectInfo);
        syncDocuments(projectInfo.getId(), request.getDocuments());
        syncFormRelations(projectInfo.getId(), request, true);
        auditService.record("PROJECT", projectInfo.getId(), AuditActionType.CREATE,
                "Created project " + projectInfo.getCode(), "SYSTEM");
        return toProjectView(getById(projectInfo.getId()));
    }

    /**
     * 更新项目。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, ProjectUpsertRequest request) {
        getById(id);
        validateRequest(request, id);
        ProjectInfo projectInfo = toEntity(request);
        projectInfo.setId(id);
        projectInfoMapper.updateById(projectInfo);
        syncDocuments(id, request.getDocuments());
        syncFormRelations(id, request, false);
        auditService.record("PROJECT", id, AuditActionType.UPDATE,
                "Updated project " + projectInfo.getCode(), "SYSTEM");
        return toProjectView(getById(id));
    }

    /**
     * 查询项目。
     */
    public ProjectInfo getById(Long id) {
        ProjectInfo projectInfo = projectInfoMapper.selectById(id);
        if (projectInfo == null) {
            throw new BusinessException("Project not found: " + id);
        }
        return projectInfo;
    }

    /**
     * 查询项目详情。
     */
    public Map<String, Object> getDetail(Long id) {
        ProjectInfo projectInfo = getById(id);
        List<Long> informationSystemIds = projectSystemRelMapper.selectList(
                        Wrappers.<ProjectSystemRel>lambdaQuery().eq(ProjectSystemRel::getProjectId, id))
                .stream().map(ProjectSystemRel::getInformationSystemId).collect(Collectors.toList());
        List<Long> serviceProviderIds = projectVendorRelMapper.selectList(
                        Wrappers.<ProjectVendorRel>lambdaQuery().eq(ProjectVendorRel::getProjectId, id))
                .stream().map(ProjectVendorRel::getServiceProviderId).collect(Collectors.toList());
        List<Long> personIds = projectPersonRelMapper.selectList(
                        Wrappers.<ProjectPersonRel>lambdaQuery().eq(ProjectPersonRel::getProjectId, id))
                .stream().map(ProjectPersonRel::getPersonId).collect(Collectors.toList());
        List<Long> hardwareAssetIds = projectHardwareRelMapper.selectList(
                        Wrappers.<ProjectHardwareRel>lambdaQuery().eq(ProjectHardwareRel::getProjectId, id))
                .stream().map(ProjectHardwareRel::getHardwareAssetId).collect(Collectors.toList());

        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("project", toProjectView(projectInfo));
        detail.put("documents", buildDocumentViews(id));
        detail.put("informationSystemIds", informationSystemIds);
        detail.put("serviceProviderIds", serviceProviderIds);
        detail.put("personIds", personIds);
        detail.put("hardwareAssetIds", hardwareAssetIds);
        detail.put("persons", buildPersonSummaries(personIds));
        detail.put("informationSystems", buildInformationSystemSummaries(informationSystemIds));
        detail.put("hardwareAssets", buildHardwareAssetSummaries(hardwareAssetIds));
        detail.put("serviceProviders", buildServiceProviderSummaries(serviceProviderIds));
        return detail;
    }

    /**
     * 分页查询项目。
     */
    public PageResponse<Map<String, Object>> page(int pageNo, int pageSize, String keyword, String projectType,
                                                  String projectStatus, String paymentStatus) {
        supportService.ensureProjectTypeValid(projectType);
        if (StringUtils.hasText(projectStatus)) {
            supportService.ensureProjectStatusValid(projectStatus);
        }
        supportService.ensurePaymentStatusValid(paymentStatus);

        LambdaQueryWrapper<ProjectInfo> wrapper = new LambdaQueryWrapper<ProjectInfo>()
                .and(StringUtils.hasText(keyword),
                        query -> query.like(ProjectInfo::getCode, keyword).or().like(ProjectInfo::getName, keyword))
                .eq(StringUtils.hasText(projectType), ProjectInfo::getProjectType, projectType)
                .eq(StringUtils.hasText(projectStatus), ProjectInfo::getProjectStatus, projectStatus)
                .eq(StringUtils.hasText(paymentStatus), ProjectInfo::getPaymentStatus, paymentStatus)
                .orderByAsc(ProjectInfo::getCode);

        Page<ProjectInfo> page = projectInfoMapper.selectPage(new Page<ProjectInfo>(pageNo, pageSize), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream().map(this::toProjectView).collect(Collectors.toList());
        return new PageResponse<Map<String, Object>>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /**
     * 查询项目选项。
     */
    public List<ProjectInfo> options() {
        return projectInfoMapper.selectList(new LambdaQueryWrapper<ProjectInfo>().orderByAsc(ProjectInfo::getCode));
    }

    /**
     * 项目统计。
     */
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", projectInfoMapper.selectCount(Wrappers.<ProjectInfo>lambdaQuery()));
        result.put("newBuild", countByProjectType(ProjectType.NEW_BUILD.name()));
        result.put("softwareUpgrade", countByProjectType(ProjectType.SOFTWARE_UPGRADE.name()));
        result.put("opsProject", countByProjectType(ProjectType.OPS_PROJECT.name()));
        result.put("servicePurchase", countByProjectType(ProjectType.SERVICE_PURCHASE.name()));
        result.put("hardwarePurchase", countByProjectType(ProjectType.HARDWARE_PURCHASE.name()));
        result.put("integrationProject", countByProjectType(ProjectType.INTEGRATION_PROJECT.name()));
        return result;
    }

    /**
     * 同步项目关联关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncRelations(Long id, ProjectRelationRequest request) {
        getById(id);
        List<Long> informationSystemIds = validateInformationSystemIds(request.getInformationSystemIds());
        List<Long> serviceProviderIds = validateServiceProviderIds(request.getServiceProviderIds());
        List<Long> personIds = validatePersonIds(request.getPersonIds());
        List<Long> hardwareAssetIds = validateHardwareIds(request.getHardwareAssetIds());

        syncInformationSystemRelations(id, informationSystemIds);
        syncServiceProviderRelations(id, serviceProviderIds);
        syncPersonRelations(id, personIds);
        syncHardwareRelations(id, hardwareAssetIds);

        auditService.record("PROJECT", id, AuditActionType.RELATION_SYNC, "Synchronized project relations", "SYSTEM");
    }

    /**
     * 删除项目。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ProjectInfo existing = getById(id);
        projectDocumentMapper.delete(Wrappers.<ProjectDocument>lambdaQuery().eq(ProjectDocument::getProjectId, id));
        projectSystemRelMapper.delete(Wrappers.<ProjectSystemRel>lambdaQuery().eq(ProjectSystemRel::getProjectId, id));
        projectVendorRelMapper.delete(Wrappers.<ProjectVendorRel>lambdaQuery().eq(ProjectVendorRel::getProjectId, id));
        projectPersonRelMapper.delete(Wrappers.<ProjectPersonRel>lambdaQuery().eq(ProjectPersonRel::getProjectId, id));
        projectHardwareRelMapper.delete(Wrappers.<ProjectHardwareRel>lambdaQuery().eq(ProjectHardwareRel::getProjectId, id));
        projectInfoMapper.deleteById(id);
        auditService.record("PROJECT", id, AuditActionType.DELETE, "Deleted project " + existing.getCode(), "SYSTEM");
    }

    private void validateRequest(ProjectUpsertRequest request, Long excludeId) {
        supportService.ensureUniqueProjectCode(request.getCode(), excludeId);
        supportService.ensureProjectTypeValid(request.getProjectType());
        supportService.ensureProjectStatusValid(request.getProjectStatus());
        supportService.ensurePaymentStatusValid(request.getPaymentStatus());
        validateDocumentRequests(request.getDocuments());
        validatePersonIds(request.getPersonIds());
        validateInformationSystemIds(request.getInformationSystemIds());
        validateHardwareIds(request.getHardwareAssetIds());
    }

    private ProjectInfo toEntity(ProjectUpsertRequest request) {
        ProjectInfo projectInfo = new ProjectInfo();
        projectInfo.setCode(request.getCode());
        projectInfo.setName(request.getName());
        projectInfo.setProjectType(request.getProjectType());
        projectInfo.setProjectStatus(request.getProjectStatus());
        projectInfo.setApprovalBatchNo(request.getApprovalBatchNo());
        projectInfo.setProjectBudget(request.getProjectBudget());
        projectInfo.setContractAmount(request.getContractAmount());
        projectInfo.setOwnerName(request.getOwnerName());
        projectInfo.setOwnerPhone(request.getOwnerPhone());
        projectInfo.setApprovalDate(request.getApprovalDate());
        projectInfo.setStartDate(request.getStartDate());
        projectInfo.setInitialDeliveryDate(request.getInitialDeliveryDate());
        projectInfo.setEndDate(request.getEndDate());
        projectInfo.setWarrantyEndDate(request.getWarrantyEndDate());
        projectInfo.setStage(request.getStage());
        projectInfo.setPaymentCycleName(request.getPaymentCycleName());
        projectInfo.setPaymentRatio(request.getPaymentRatio());
        projectInfo.setPaymentAmount(request.getPaymentAmount());
        projectInfo.setPlannedPaymentDate(request.getPlannedPaymentDate());
        projectInfo.setActualPaymentDate(request.getActualPaymentDate());
        projectInfo.setPaymentStatus(request.getPaymentStatus());
        projectInfo.setRemark(request.getRemark());
        return projectInfo;
    }

    private Map<String, Object> toProjectView(ProjectInfo projectInfo) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("id", projectInfo.getId());
        view.put("code", projectInfo.getCode());
        view.put("name", projectInfo.getName());
        view.put("projectType", projectInfo.getProjectType());
        view.put("projectStatus", projectInfo.getProjectStatus());
        view.put("approvalBatchNo", projectInfo.getApprovalBatchNo());
        view.put("projectBudget", projectInfo.getProjectBudget());
        view.put("contractAmount", projectInfo.getContractAmount());
        view.put("ownerName", projectInfo.getOwnerName());
        view.put("ownerPhone", projectInfo.getOwnerPhone());
        view.put("approvalDate", projectInfo.getApprovalDate());
        view.put("startDate", projectInfo.getStartDate());
        view.put("initialDeliveryDate", projectInfo.getInitialDeliveryDate());
        view.put("endDate", projectInfo.getEndDate());
        view.put("warrantyEndDate", projectInfo.getWarrantyEndDate());
        view.put("stage", projectInfo.getStage());
        view.put("paymentCycleName", projectInfo.getPaymentCycleName());
        view.put("paymentRatio", projectInfo.getPaymentRatio());
        view.put("paymentAmount", projectInfo.getPaymentAmount());
        view.put("plannedPaymentDate", projectInfo.getPlannedPaymentDate());
        view.put("actualPaymentDate", projectInfo.getActualPaymentDate());
        view.put("paymentStatus", projectInfo.getPaymentStatus());
        view.put("remark", projectInfo.getRemark());
        view.put("createdAt", projectInfo.getCreatedAt());
        view.put("updatedAt", projectInfo.getUpdatedAt());
        return view;
    }

    private void syncFormRelations(Long projectId, ProjectUpsertRequest request, boolean createMode) {
        if (createMode || request.getInformationSystemIds() != null) {
            syncInformationSystemRelations(projectId, validateInformationSystemIds(request.getInformationSystemIds()));
        }
        if (createMode || request.getPersonIds() != null) {
            syncPersonRelations(projectId, validatePersonIds(request.getPersonIds()));
        }
        if (createMode || request.getHardwareAssetIds() != null) {
            syncHardwareRelations(projectId, validateHardwareIds(request.getHardwareAssetIds()));
        }
    }

    private void syncDocuments(Long projectId, List<ProjectDocumentRequest> documents) {
        List<ProjectDocumentRequest> normalizedDocuments = normalizeDocuments(documents);
        projectDocumentMapper.delete(Wrappers.<ProjectDocument>lambdaQuery().eq(ProjectDocument::getProjectId, projectId));
        for (ProjectDocumentRequest item : normalizedDocuments) {
            ProjectDocument document = new ProjectDocument();
            document.setProjectId(projectId);
            document.setFileName(item.getFileName());
            document.setOriginalName(item.getOriginalName());
            document.setFileSize(item.getFileSize());
            document.setContentType(item.getContentType());
            document.setFileUrl(item.getFileUrl());
            document.setUploadedAt(LocalDateTime.now());
            projectDocumentMapper.insert(document);
        }
    }

    private void syncInformationSystemRelations(Long projectId, List<Long> informationSystemIds) {
        projectSystemRelMapper.delete(Wrappers.<ProjectSystemRel>lambdaQuery().eq(ProjectSystemRel::getProjectId, projectId));
        for (Long informationSystemId : informationSystemIds) {
            ProjectSystemRel relation = new ProjectSystemRel();
            relation.setProjectId(projectId);
            relation.setInformationSystemId(informationSystemId);
            projectSystemRelMapper.insert(relation);
        }
    }

    private void syncServiceProviderRelations(Long projectId, List<Long> serviceProviderIds) {
        projectVendorRelMapper.delete(Wrappers.<ProjectVendorRel>lambdaQuery().eq(ProjectVendorRel::getProjectId, projectId));
        for (Long serviceProviderId : serviceProviderIds) {
            ProjectVendorRel relation = new ProjectVendorRel();
            relation.setProjectId(projectId);
            relation.setServiceProviderId(serviceProviderId);
            projectVendorRelMapper.insert(relation);
        }
    }

    private void syncPersonRelations(Long projectId, List<Long> personIds) {
        projectPersonRelMapper.delete(Wrappers.<ProjectPersonRel>lambdaQuery().eq(ProjectPersonRel::getProjectId, projectId));
        for (Long personId : personIds) {
            ProjectPersonRel relation = new ProjectPersonRel();
            relation.setProjectId(projectId);
            relation.setPersonId(personId);
            relation.setRelationType(PersonRelationType.RESPONSIBLE.name());
            projectPersonRelMapper.insert(relation);
        }
    }

    private void syncHardwareRelations(Long projectId, List<Long> hardwareAssetIds) {
        projectHardwareRelMapper.delete(Wrappers.<ProjectHardwareRel>lambdaQuery().eq(ProjectHardwareRel::getProjectId, projectId));
        for (Long hardwareAssetId : hardwareAssetIds) {
            ProjectHardwareRel relation = new ProjectHardwareRel();
            relation.setProjectId(projectId);
            relation.setHardwareAssetId(hardwareAssetId);
            projectHardwareRelMapper.insert(relation);
        }
    }

    private List<Map<String, Object>> buildDocumentViews(Long projectId) {
        return projectDocumentMapper.selectList(
                        Wrappers.<ProjectDocument>lambdaQuery()
                                .eq(ProjectDocument::getProjectId, projectId)
                                .orderByDesc(ProjectDocument::getUploadedAt)
                                .orderByDesc(ProjectDocument::getId))
                .stream()
                .map(item -> {
                    Map<String, Object> view = new LinkedHashMap<String, Object>();
                    view.put("id", item.getId());
                    view.put("fileName", item.getFileName());
                    view.put("originalName", item.getOriginalName());
                    view.put("fileSize", item.getFileSize());
                    view.put("contentType", item.getContentType());
                    view.put("fileUrl", item.getFileUrl());
                    view.put("uploadedAt", item.getUploadedAt());
                    return view;
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
                    return summary;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildInformationSystemSummaries(List<Long> informationSystemIds) {
        List<Long> normalizedIds = normalizeIds(informationSystemIds);
        if (normalizedIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, InformationSystem> systemMap = informationSystemMapper.selectList(
                        Wrappers.<InformationSystem>lambdaQuery().in(InformationSystem::getId, normalizedIds))
                .stream()
                .collect(Collectors.toMap(InformationSystem::getId, item -> item));

        Map<Long, Long> vendorIdMap = new LinkedHashMap<Long, Long>();
        List<SystemVendorRel> vendorRelations = systemVendorRelMapper.selectList(
                Wrappers.<SystemVendorRel>lambdaQuery()
                        .in(SystemVendorRel::getInformationSystemId, normalizedIds)
                        .orderByAsc(SystemVendorRel::getInformationSystemId)
                        .orderByAsc(SystemVendorRel::getId));
        for (SystemVendorRel relation : vendorRelations) {
            vendorIdMap.putIfAbsent(relation.getInformationSystemId(), relation.getServiceProviderId());
        }
        Map<Long, String> vendorNameMap = loadServiceProviderNameMap(vendorRelations.stream()
                .map(SystemVendorRel::getServiceProviderId)
                .collect(Collectors.toList()));

        Map<Long, String> ownerNameMap = loadPersonNameMap(systemMap.values().stream()
                .map(InformationSystem::getOwnerPersonId)
                .collect(Collectors.toList()));

        return normalizedIds.stream()
                .map(systemMap::get)
                .filter(item -> item != null)
                .map(item -> {
                    Map<String, Object> summary = new LinkedHashMap<String, Object>();
                    summary.put("id", item.getId());
                    summary.put("name", item.getName());
                    summary.put("code", item.getCode());
                    summary.put("systemType", item.getSystemType());
                    summary.put("serviceProviderName", vendorNameMap.get(vendorIdMap.get(item.getId())));
                    summary.put("ownerName", ownerNameMap.get(item.getOwnerPersonId()));
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

        Map<Long, Long> vendorIdMap = new LinkedHashMap<Long, Long>();
        List<AssetHardwareVendorRel> vendorRelations = assetHardwareVendorRelMapper.selectList(
                Wrappers.<AssetHardwareVendorRel>lambdaQuery()
                        .in(AssetHardwareVendorRel::getHardwareAssetId, normalizedIds)
                        .orderByAsc(AssetHardwareVendorRel::getHardwareAssetId)
                        .orderByAsc(AssetHardwareVendorRel::getId));
        for (AssetHardwareVendorRel relation : vendorRelations) {
            vendorIdMap.putIfAbsent(relation.getHardwareAssetId(), relation.getServiceProviderId());
        }
        Map<Long, String> vendorNameMap = loadServiceProviderNameMap(vendorRelations.stream()
                .map(AssetHardwareVendorRel::getServiceProviderId)
                .collect(Collectors.toList()));

        return normalizedIds.stream()
                .map(hardwareMap::get)
                .filter(item -> item != null)
                .map(item -> {
                    Map<String, Object> summary = new LinkedHashMap<String, Object>();
                    summary.put("id", item.getId());
                    summary.put("name", item.getAssetName());
                    summary.put("code", item.getAssetCode());
                    summary.put("managementIp", item.getManagementIp());
                    summary.put("cpuModel", item.getCpuModel());
                    summary.put("memoryGb", item.getMemoryGb());
                    summary.put("serviceProviderName", vendorNameMap.get(vendorIdMap.get(item.getId())));
                    summary.put("ownerName", null);
                    return summary;
                })
                .collect(Collectors.toList());
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
                    summary.put("cooperationScopes", scopeMap.getOrDefault(item.getId(), Collections.<String>emptyList()));
                    summary.put("score", item.getScore());
                    summary.put("businessContact", item.getBusinessContact());
                    summary.put("businessPhone", item.getBusinessPhone());
                    return summary;
                })
                .collect(Collectors.toList());
    }

    private Long countByProjectType(String projectType) {
        return projectInfoMapper.selectCount(Wrappers.<ProjectInfo>lambdaQuery().eq(ProjectInfo::getProjectType, projectType));
    }

    private List<ProjectDocumentRequest> normalizeDocuments(List<ProjectDocumentRequest> documents) {
        if (documents == null) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, ProjectDocumentRequest> deduplicated = new LinkedHashMap<String, ProjectDocumentRequest>();
        for (ProjectDocumentRequest item : documents) {
            if (item == null || !StringUtils.hasText(item.getFileName())) {
                continue;
            }
            deduplicated.put(item.getFileName(), item);
        }
        return new ArrayList<ProjectDocumentRequest>(deduplicated.values());
    }

    private void validateDocumentRequests(List<ProjectDocumentRequest> documents) {
        for (ProjectDocumentRequest item : normalizeDocuments(documents)) {
            if (!StringUtils.hasText(item.getFileName())
                    || !StringUtils.hasText(item.getOriginalName())
                    || !StringUtils.hasText(item.getContentType())
                    || !StringUtils.hasText(item.getFileUrl())
                    || item.getFileSize() == null || item.getFileSize().longValue() < 0) {
                throw new BusinessException("项目文档信息不完整");
            }
        }
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream().filter(item -> item != null).distinct().collect(Collectors.toList());
    }

    private List<Long> validateInformationSystemIds(List<Long> informationSystemIds) {
        List<Long> normalizedIds = normalizeIds(informationSystemIds);
        for (Long informationSystemId : normalizedIds) {
            supportService.ensureInformationSystemExists(informationSystemId);
        }
        return normalizedIds;
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

    private Map<Long, String> loadServiceProviderNameMap(List<Long> serviceProviderIds) {
        List<Long> filteredIds = normalizeIds(serviceProviderIds);
        if (filteredIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return serviceProviderMapper.selectList(Wrappers.<ServiceProvider>lambdaQuery().in(ServiceProvider::getId, filteredIds))
                .stream()
                .collect(Collectors.toMap(ServiceProvider::getId, ServiceProvider::getName));
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

        Map<Long, LinkedHashSet<String>> grouped = new LinkedHashMap<Long, LinkedHashSet<String>>();
        serviceProviderCooperationScopeRelMapper.selectList(
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
}
