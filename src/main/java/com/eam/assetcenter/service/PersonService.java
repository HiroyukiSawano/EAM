package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.enums.PersonRelationType;
import com.eam.assetcenter.common.enums.PersonType;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.AssetHardware;
import com.eam.assetcenter.domain.entity.AssetHardwarePersonRel;
import com.eam.assetcenter.domain.entity.AssetHardwareVendorRel;
import com.eam.assetcenter.domain.entity.Department;
import com.eam.assetcenter.domain.entity.InformationSystem;
import com.eam.assetcenter.domain.entity.Person;
import com.eam.assetcenter.domain.entity.ProjectInfo;
import com.eam.assetcenter.domain.entity.ProjectPersonRel;
import com.eam.assetcenter.domain.entity.ServiceProviderPersonRel;
import com.eam.assetcenter.domain.entity.ServiceProvider;
import com.eam.assetcenter.domain.entity.SystemPersonRel;
import com.eam.assetcenter.domain.entity.SystemVendorRel;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwarePersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareVendorRelMapper;
import com.eam.assetcenter.infrastructure.mapper.DepartmentMapper;
import com.eam.assetcenter.infrastructure.mapper.InformationSystemMapper;
import com.eam.assetcenter.infrastructure.mapper.PersonMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectInfoMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectPersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderPersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.SystemPersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.SystemVendorRelMapper;
import com.eam.assetcenter.web.request.PersonRelationRequest;
import com.eam.assetcenter.web.request.PersonUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 人员业务服务，负责人员主数据及关联信息聚合。
 */
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonMapper personMapper;
    private final DepartmentMapper departmentMapper;
    private final ServiceProviderMapper serviceProviderMapper;
    private final ServiceProviderPersonRelMapper serviceProviderPersonRelMapper;
    private final InformationSystemMapper informationSystemMapper;
    private final ProjectInfoMapper projectInfoMapper;
    private final AssetHardwareMapper assetHardwareMapper;
    private final AssetHardwarePersonRelMapper hardwarePersonRelMapper;
    private final AssetHardwareVendorRelMapper hardwareVendorRelMapper;
    private final SystemPersonRelMapper systemPersonRelMapper;
    private final SystemVendorRelMapper systemVendorRelMapper;
    private final ProjectPersonRelMapper projectPersonRelMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增人员。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(PersonUpsertRequest request) {
        validateRequest(request, null);
        Person person = toEntity(request, null);
        personMapper.insert(person);
        syncFormRelations(person.getId(), person.getServiceProviderId(), request, true);
        auditService.record("PERSON", person.getId(), AuditActionType.CREATE, "Created person " + person.getName(), "SYSTEM");
        return toPersonView(getById(person.getId()));
    }

    /**
     * 更新人员。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, PersonUpsertRequest request) {
        Person existing = getById(id);
        validateRequest(request, existing);
        Person person = toEntity(request, existing);
        person.setId(id);
        personMapper.updateById(person);
        syncFormRelations(id, resolvePrimaryServiceProviderId(request, existing), request, false);
        auditService.record("PERSON", id, AuditActionType.UPDATE, "Updated person " + person.getName(), "SYSTEM");
        return toPersonView(getById(id));
    }

    /**
     * 根据主键查询人员。
     */
    public Person getById(Long id) {
        Person person = personMapper.selectById(id);
        if (person == null) {
            throw new BusinessException("Person not found: " + id);
        }
        return person;
    }

    /**
     * 查询人员详情。
     */
    public Map<String, Object> getDetail(Long id) {
        Person person = getById(id);
        List<Long> hardwareAssetIds = hardwarePersonRelMapper.selectList(
                        Wrappers.<AssetHardwarePersonRel>lambdaQuery().eq(AssetHardwarePersonRel::getPersonId, id))
                .stream().map(AssetHardwarePersonRel::getHardwareAssetId).collect(Collectors.toList());
        List<Long> informationSystemIds = systemPersonRelMapper.selectList(
                        Wrappers.<SystemPersonRel>lambdaQuery().eq(SystemPersonRel::getPersonId, id))
                .stream().map(SystemPersonRel::getInformationSystemId).collect(Collectors.toList());
        List<Long> projectIds = projectPersonRelMapper.selectList(
                        Wrappers.<ProjectPersonRel>lambdaQuery().eq(ProjectPersonRel::getPersonId, id))
                .stream().map(ProjectPersonRel::getProjectId).collect(Collectors.toList());
        List<Long> relatedServiceProviderIds = loadRelatedServiceProviderIds(id, person.getServiceProviderId());

        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("person", toPersonView(person));
        detail.put("hardwareAssetIds", hardwareAssetIds);
        detail.put("informationSystemIds", informationSystemIds);
        detail.put("projectIds", projectIds);
        detail.put("relatedServiceProviderIds", relatedServiceProviderIds);
        detail.put("informationSystems", buildInformationSystemSummaries(informationSystemIds));
        detail.put("hardwareAssets", buildHardwareAssetSummaries(hardwareAssetIds));
        detail.put("projects", buildProjectSummaries(projectIds));
        return detail;
    }

    /**
     * 分页查询人员。
     */
    public PageResponse<Map<String, Object>> page(int pageNo, int pageSize, String keyword, Long serviceProviderId, String personType, String status) {
        if (status != null && !status.trim().isEmpty()) {
            supportService.ensureCommonStatusValid(status, "人员");
        }
        supportService.ensureServiceProviderExists(serviceProviderId);
        supportService.ensurePersonTypeValid(personType);

        LambdaQueryWrapper<Person> wrapper = new LambdaQueryWrapper<Person>()
                .and(keyword != null && !keyword.trim().isEmpty(),
                        q -> q.like(Person::getName, keyword)
                                .or().like(Person::getEmployeeNo, keyword)
                                .or().like(Person::getMobile, keyword))
                .eq(serviceProviderId != null, Person::getServiceProviderId, serviceProviderId)
                .eq(personType != null && !personType.trim().isEmpty(), Person::getPersonType, personType)
                .eq(status != null && !status.trim().isEmpty(), Person::getStatus, status)
                .orderByAsc(Person::getName);

        Page<Person> page = personMapper.selectPage(new Page<Person>(pageNo, pageSize), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream().map(this::toPersonView).collect(Collectors.toList());
        return new PageResponse<Map<String, Object>>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /**
     * 查询人员下拉选项。
     */
    public List<Person> options() {
        return personMapper.selectList(new LambdaQueryWrapper<Person>().orderByAsc(Person::getName));
    }

    /**
     * 查询人员统计。
     */
    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", personMapper.selectCount(Wrappers.<Person>lambdaQuery()));
        result.put("development", personMapper.selectCount(Wrappers.<Person>lambdaQuery().eq(Person::getPersonType, PersonType.DEV.name())));
        result.put("ops", personMapper.selectCount(Wrappers.<Person>lambdaQuery().eq(Person::getPersonType, PersonType.OPS.name())));
        Set<Long> hardwareOwnerIds = hardwarePersonRelMapper.selectList(
                        Wrappers.<AssetHardwarePersonRel>lambdaQuery()
                                .eq(AssetHardwarePersonRel::getRelationType, PersonRelationType.RESPONSIBLE.name()))
                .stream()
                .map(AssetHardwarePersonRel::getPersonId)
                .collect(Collectors.toSet());
        result.put("hardwareOwners", hardwareOwnerIds.size());
        return result;
    }

    /**
     * 同步人员关联关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncRelations(Long id, PersonRelationRequest request) {
        Person person = getById(id);
        List<Long> hardwareAssetIds = normalizeIds(request.getHardwareAssetIds());
        List<Long> informationSystemIds = normalizeIds(request.getInformationSystemIds());
        List<Long> projectIds = normalizeIds(request.getProjectIds());
        List<Long> relatedServiceProviderIds = validateRelatedServiceProviderIds(
                request.getRelatedServiceProviderIds(), person.getServiceProviderId());

        for (Long hardwareAssetId : hardwareAssetIds) {
            supportService.ensureHardwareExists(hardwareAssetId);
        }
        for (Long informationSystemId : informationSystemIds) {
            supportService.ensureInformationSystemExists(informationSystemId);
        }
        for (Long projectId : projectIds) {
            supportService.ensureProjectExists(projectId);
        }

        assertResponsibleHardwareConflicts(id, hardwareAssetIds);

        hardwarePersonRelMapper.delete(Wrappers.<AssetHardwarePersonRel>lambdaQuery()
                .eq(AssetHardwarePersonRel::getPersonId, id)
                .eq(AssetHardwarePersonRel::getRelationType, PersonRelationType.RESPONSIBLE.name()));
        for (Long hardwareAssetId : hardwareAssetIds) {
            AssetHardwarePersonRel relation = new AssetHardwarePersonRel();
            relation.setHardwareAssetId(hardwareAssetId);
            relation.setPersonId(id);
            relation.setRelationType(PersonRelationType.RESPONSIBLE.name());
            hardwarePersonRelMapper.insert(relation);
        }

        systemPersonRelMapper.delete(Wrappers.<SystemPersonRel>lambdaQuery().eq(SystemPersonRel::getPersonId, id));
        for (Long informationSystemId : informationSystemIds) {
            SystemPersonRel relation = new SystemPersonRel();
            relation.setInformationSystemId(informationSystemId);
            relation.setPersonId(id);
            relation.setRelationType(PersonRelationType.RESPONSIBLE.name());
            systemPersonRelMapper.insert(relation);
        }

        projectPersonRelMapper.delete(Wrappers.<ProjectPersonRel>lambdaQuery().eq(ProjectPersonRel::getPersonId, id));
        for (Long projectId : projectIds) {
            ProjectPersonRel relation = new ProjectPersonRel();
            relation.setProjectId(projectId);
            relation.setPersonId(id);
            relation.setRelationType(PersonRelationType.RESPONSIBLE.name());
            projectPersonRelMapper.insert(relation);
        }

        syncRelatedServiceProviderRelations(id, relatedServiceProviderIds);

        auditService.record("PERSON", id, AuditActionType.RELATION_SYNC, "Synchronized person relations", "SYSTEM");
    }

    /**
     * 删除人员。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        Long hwCount = hardwarePersonRelMapper.selectCount(Wrappers.<AssetHardwarePersonRel>lambdaQuery().eq(AssetHardwarePersonRel::getPersonId, id));
        if (hwCount > 0) {
            throw new BusinessException("该人员仍被 " + hwCount + " 件硬件资产关联，无法删除");
        }
        Long sysCount = systemPersonRelMapper.selectCount(Wrappers.<SystemPersonRel>lambdaQuery().eq(SystemPersonRel::getPersonId, id));
        if (sysCount > 0) {
            throw new BusinessException("该人员仍被 " + sysCount + " 个信息系统关联，无法删除");
        }
        Long projCount = projectPersonRelMapper.selectCount(Wrappers.<ProjectPersonRel>lambdaQuery().eq(ProjectPersonRel::getPersonId, id));
        if (projCount > 0) {
            throw new BusinessException("该人员仍被 " + projCount + " 个项目关联，无法删除");
        }
        Long relProviderCount = serviceProviderPersonRelMapper.selectCount(
                Wrappers.<ServiceProviderPersonRel>lambdaQuery().eq(ServiceProviderPersonRel::getPersonId, id));
        if (relProviderCount > 0) {
            throw new BusinessException("该人员仍被 " + relProviderCount + " 个关联服务商引用，无法删除");
        }
        personMapper.deleteById(id);
        auditService.record("PERSON", id, AuditActionType.DELETE, "Deleted person " + id, "SYSTEM");
    }

    private void validateRequest(PersonUpsertRequest request, Person existing) {
        supportService.ensureDepartmentExists(request.getDepartmentId());
        supportService.ensureServiceProviderExists(request.getServiceProviderId());
        supportService.ensureCommonStatusValid(resolveStatus(request, existing), "人员");
        supportService.ensurePersonTypeValid(request.getPersonType());
        validateHardwareAssetIds(request.getHardwareAssetIds());
        validateInformationSystemIds(request.getInformationSystemIds());
        validateRelatedServiceProviderIds(request.getRelatedServiceProviderIds(), resolvePrimaryServiceProviderId(request, existing));
    }

    private Person toEntity(PersonUpsertRequest request, Person existing) {
        Person person = new Person();
        person.setName(request.getName());
        person.setGender(request.getGender());
        person.setIdCardNo(request.getIdCardNo());
        person.setMobile(request.getMobile());
        person.setEmployeeNo(request.getEmployeeNo());
        person.setPhotoUrl(request.getPhotoUrl());
        person.setAccount(request.getAccount());
        person.setDepartmentId(request.getDepartmentId());
        person.setServiceProviderId(request.getServiceProviderId());
        person.setPersonType(request.getPersonType());
        person.setHasOpsAccount(resolveHasOpsAccount(request, existing));
        person.setStatus(resolveStatus(request, existing));
        return person;
    }

    private Map<String, Object> toPersonView(Person person) {
        Map<Long, String> departmentNameMap = loadDepartmentNameMap(Collections.singletonList(person.getDepartmentId()));
        Map<Long, String> serviceProviderNameMap = loadServiceProviderNameMap(Collections.singletonList(person.getServiceProviderId()));
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("id", person.getId());
        view.put("name", person.getName());
        view.put("gender", person.getGender());
        view.put("idCardNo", person.getIdCardNo());
        view.put("mobile", person.getMobile());
        view.put("employeeNo", person.getEmployeeNo());
        view.put("photoUrl", person.getPhotoUrl());
        view.put("account", person.getAccount());
        view.put("departmentId", person.getDepartmentId());
        view.put("departmentName", departmentNameMap.get(person.getDepartmentId()));
        view.put("serviceProviderId", person.getServiceProviderId());
        view.put("serviceProviderName", serviceProviderNameMap.get(person.getServiceProviderId()));
        view.put("personType", person.getPersonType());
        view.put("hasOpsAccount", Boolean.TRUE.equals(person.getHasOpsAccount()));
        view.put("status", person.getStatus());
        view.put("createdAt", person.getCreatedAt());
        view.put("updatedAt", person.getUpdatedAt());
        return view;
    }

    private Map<Long, String> loadDepartmentNameMap(List<Long> departmentIds) {
        List<Long> filteredIds = departmentIds.stream().filter(item -> item != null).distinct().collect(Collectors.toList());
        if (filteredIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return departmentMapper.selectList(Wrappers.<Department>lambdaQuery().in(Department::getId, filteredIds))
                .stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
    }

    private Map<Long, String> loadServiceProviderNameMap(List<Long> serviceProviderIds) {
        List<Long> filteredIds = serviceProviderIds.stream().filter(item -> item != null).distinct().collect(Collectors.toList());
        if (filteredIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return serviceProviderMapper.selectList(Wrappers.<ServiceProvider>lambdaQuery().in(ServiceProvider::getId, filteredIds))
                .stream()
                .collect(Collectors.toMap(ServiceProvider::getId, ServiceProvider::getName));
    }

    private Map<Long, String> loadPersonNameMap(List<Long> personIds) {
        List<Long> filteredIds = personIds.stream().filter(item -> item != null).distinct().collect(Collectors.toList());
        if (filteredIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return personMapper.selectList(Wrappers.<Person>lambdaQuery().in(Person::getId, filteredIds))
                .stream()
                .collect(Collectors.toMap(Person::getId, Person::getName));
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

    private List<Long> validateRelatedServiceProviderIds(List<Long> relatedServiceProviderIds, Long primaryServiceProviderId) {
        List<Long> normalizedIds = normalizeIds(relatedServiceProviderIds).stream()
                .filter(item -> primaryServiceProviderId == null || !primaryServiceProviderId.equals(item))
                .collect(Collectors.toList());
        for (Long serviceProviderId : normalizedIds) {
            supportService.ensureServiceProviderExists(serviceProviderId);
        }
        return normalizedIds;
    }

    private void syncFormRelations(Long personId, Long primaryServiceProviderId, PersonUpsertRequest request, boolean createMode) {
        if (createMode || request.getHardwareAssetIds() != null) {
            syncHardwareRelations(personId, validateHardwareAssetIds(request.getHardwareAssetIds()));
        }
        if (createMode || request.getInformationSystemIds() != null) {
            syncInformationSystemRelations(personId, validateInformationSystemIds(request.getInformationSystemIds()));
        }
        if (createMode || request.getRelatedServiceProviderIds() != null) {
            syncRelatedServiceProviderRelations(personId,
                    validateRelatedServiceProviderIds(request.getRelatedServiceProviderIds(), primaryServiceProviderId));
        }
    }

    private void syncHardwareRelations(Long personId, List<Long> hardwareAssetIds) {
        assertResponsibleHardwareConflicts(personId, hardwareAssetIds);
        hardwarePersonRelMapper.delete(Wrappers.<AssetHardwarePersonRel>lambdaQuery()
                .eq(AssetHardwarePersonRel::getPersonId, personId)
                .eq(AssetHardwarePersonRel::getRelationType, PersonRelationType.RESPONSIBLE.name()));
        for (Long hardwareAssetId : hardwareAssetIds) {
            AssetHardwarePersonRel relation = new AssetHardwarePersonRel();
            relation.setHardwareAssetId(hardwareAssetId);
            relation.setPersonId(personId);
            relation.setRelationType(PersonRelationType.RESPONSIBLE.name());
            hardwarePersonRelMapper.insert(relation);
        }
    }

    private void syncInformationSystemRelations(Long personId, List<Long> informationSystemIds) {
        systemPersonRelMapper.delete(Wrappers.<SystemPersonRel>lambdaQuery().eq(SystemPersonRel::getPersonId, personId));
        for (Long informationSystemId : informationSystemIds) {
            SystemPersonRel relation = new SystemPersonRel();
            relation.setInformationSystemId(informationSystemId);
            relation.setPersonId(personId);
            relation.setRelationType(PersonRelationType.RESPONSIBLE.name());
            systemPersonRelMapper.insert(relation);
        }
    }

    private void syncRelatedServiceProviderRelations(Long personId, List<Long> relatedServiceProviderIds) {
        serviceProviderPersonRelMapper.delete(Wrappers.<ServiceProviderPersonRel>lambdaQuery()
                .eq(ServiceProviderPersonRel::getPersonId, personId));
        for (Long serviceProviderId : relatedServiceProviderIds) {
            ServiceProviderPersonRel relation = new ServiceProviderPersonRel();
            relation.setServiceProviderId(serviceProviderId);
            relation.setPersonId(personId);
            serviceProviderPersonRelMapper.insert(relation);
        }
    }

    private List<Long> loadRelatedServiceProviderIds(Long personId, Long primaryServiceProviderId) {
        return serviceProviderPersonRelMapper.selectList(
                        Wrappers.<ServiceProviderPersonRel>lambdaQuery()
                                .eq(ServiceProviderPersonRel::getPersonId, personId)
                                .orderByAsc(ServiceProviderPersonRel::getCreatedAt)
                                .orderByAsc(ServiceProviderPersonRel::getId))
                .stream()
                .map(ServiceProviderPersonRel::getServiceProviderId)
                .filter(item -> item != null && (primaryServiceProviderId == null || !primaryServiceProviderId.equals(item)))
                .distinct()
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
                    summary.put("status", item.getStatus());
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
        List<AssetHardwareVendorRel> vendorRelations = hardwareVendorRelMapper.selectList(
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

        Map<Long, Long> ownerIdMap = new LinkedHashMap<Long, Long>();
        List<AssetHardwarePersonRel> ownerRelations = hardwarePersonRelMapper.selectList(
                Wrappers.<AssetHardwarePersonRel>lambdaQuery()
                        .in(AssetHardwarePersonRel::getHardwareAssetId, normalizedIds)
                        .eq(AssetHardwarePersonRel::getRelationType, PersonRelationType.RESPONSIBLE.name())
                        .orderByAsc(AssetHardwarePersonRel::getHardwareAssetId)
                        .orderByAsc(AssetHardwarePersonRel::getId));
        for (AssetHardwarePersonRel relation : ownerRelations) {
            ownerIdMap.putIfAbsent(relation.getHardwareAssetId(), relation.getPersonId());
        }
        Map<Long, String> ownerNameMap = loadPersonNameMap(ownerRelations.stream()
                .map(AssetHardwarePersonRel::getPersonId)
                .collect(Collectors.toList()));

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
                    summary.put("serviceProviderName", vendorNameMap.get(vendorIdMap.get(item.getId())));
                    summary.put("ownerName", ownerNameMap.get(ownerIdMap.get(item.getId())));
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

        Map<Long, Long> ownerIdMap = new LinkedHashMap<Long, Long>();
        List<ProjectPersonRel> personRelations = projectPersonRelMapper.selectList(
                Wrappers.<ProjectPersonRel>lambdaQuery()
                        .in(ProjectPersonRel::getProjectId, normalizedIds)
                        .eq(ProjectPersonRel::getRelationType, PersonRelationType.RESPONSIBLE.name())
                        .orderByAsc(ProjectPersonRel::getProjectId)
                        .orderByAsc(ProjectPersonRel::getId));
        for (ProjectPersonRel relation : personRelations) {
            ownerIdMap.putIfAbsent(relation.getProjectId(), relation.getPersonId());
        }
        Map<Long, String> ownerNameMap = loadPersonNameMap(personRelations.stream()
                .map(ProjectPersonRel::getPersonId)
                .collect(Collectors.toList()));

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
                    summary.put("ownerName", ownerNameMap.get(ownerIdMap.get(item.getId())));
                    return summary;
                })
                .collect(Collectors.toList());
    }

    private Long resolvePrimaryServiceProviderId(PersonUpsertRequest request, Person existing) {
        return request.getServiceProviderId() != null ? request.getServiceProviderId()
                : existing == null ? null : existing.getServiceProviderId();
    }

    private Boolean resolveHasOpsAccount(PersonUpsertRequest request, Person existing) {
        if (request.getHasOpsAccount() != null) {
            return request.getHasOpsAccount();
        }
        if (existing != null && existing.getHasOpsAccount() != null) {
            return existing.getHasOpsAccount();
        }
        return Boolean.FALSE;
    }

    private String resolveStatus(PersonUpsertRequest request, Person existing) {
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            return request.getStatus();
        }
        if (existing != null && existing.getStatus() != null && !existing.getStatus().trim().isEmpty()) {
            return existing.getStatus();
        }
        return "ACTIVE";
    }

    private void assertResponsibleHardwareConflicts(Long personId, List<Long> hardwareAssetIds) {
        if (hardwareAssetIds == null || hardwareAssetIds.isEmpty()) {
            return;
        }

        List<AssetHardwarePersonRel> conflictingRelations = hardwarePersonRelMapper.selectList(
                Wrappers.<AssetHardwarePersonRel>lambdaQuery()
                        .in(AssetHardwarePersonRel::getHardwareAssetId, hardwareAssetIds)
                        .eq(AssetHardwarePersonRel::getRelationType, PersonRelationType.RESPONSIBLE.name())
                        .ne(AssetHardwarePersonRel::getPersonId, personId));
        if (conflictingRelations.isEmpty()) {
            return;
        }

        List<Long> conflictHardwareIds = conflictingRelations.stream()
                .map(AssetHardwarePersonRel::getHardwareAssetId)
                .distinct()
                .collect(Collectors.toList());
        List<AssetHardware> conflictAssets = assetHardwareMapper.selectList(
                Wrappers.<AssetHardware>lambdaQuery().in(AssetHardware::getId, conflictHardwareIds));
        String conflictLabels = conflictAssets.stream()
                .map(item -> (item.getAssetCode() == null ? "" : item.getAssetCode()) +
                        (item.getAssetName() == null ? "" : "/" + item.getAssetName()))
                .collect(Collectors.joining("、"));
        if (conflictLabels == null || conflictLabels.trim().isEmpty()) {
            conflictLabels = conflictHardwareIds.stream().map(String::valueOf).collect(Collectors.joining("、"));
        }
        throw new BusinessException("所选硬件中存在已分配其他负责人的资产：" + conflictLabels);
    }
}
