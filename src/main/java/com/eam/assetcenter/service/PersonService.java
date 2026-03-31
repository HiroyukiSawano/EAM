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
import com.eam.assetcenter.domain.entity.Department;
import com.eam.assetcenter.domain.entity.Person;
import com.eam.assetcenter.domain.entity.ProjectPersonRel;
import com.eam.assetcenter.domain.entity.ServiceProvider;
import com.eam.assetcenter.domain.entity.SystemPersonRel;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwarePersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.DepartmentMapper;
import com.eam.assetcenter.infrastructure.mapper.PersonMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectPersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderMapper;
import com.eam.assetcenter.infrastructure.mapper.SystemPersonRelMapper;
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
    private final AssetHardwareMapper assetHardwareMapper;
    private final AssetHardwarePersonRelMapper hardwarePersonRelMapper;
    private final SystemPersonRelMapper systemPersonRelMapper;
    private final ProjectPersonRelMapper projectPersonRelMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增人员。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(PersonUpsertRequest request) {
        validateRequest(request);
        Person person = toEntity(request);
        personMapper.insert(person);
        auditService.record("PERSON", person.getId(), AuditActionType.CREATE, "Created person " + person.getName(), "SYSTEM");
        return toPersonView(getById(person.getId()));
    }

    /**
     * 更新人员。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, PersonUpsertRequest request) {
        getById(id);
        validateRequest(request);
        Person person = toEntity(request);
        person.setId(id);
        personMapper.updateById(person);
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
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("person", toPersonView(getById(id)));
        detail.put("hardwareAssetIds", hardwarePersonRelMapper.selectList(
                        Wrappers.<AssetHardwarePersonRel>lambdaQuery().eq(AssetHardwarePersonRel::getPersonId, id))
                .stream().map(AssetHardwarePersonRel::getHardwareAssetId).collect(Collectors.toList()));
        detail.put("informationSystemIds", systemPersonRelMapper.selectList(
                        Wrappers.<SystemPersonRel>lambdaQuery().eq(SystemPersonRel::getPersonId, id))
                .stream().map(SystemPersonRel::getInformationSystemId).collect(Collectors.toList()));
        detail.put("projectIds", projectPersonRelMapper.selectList(
                        Wrappers.<ProjectPersonRel>lambdaQuery().eq(ProjectPersonRel::getPersonId, id))
                .stream().map(ProjectPersonRel::getProjectId).collect(Collectors.toList()));
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
        getById(id);
        List<Long> hardwareAssetIds = normalizeIds(request.getHardwareAssetIds());
        List<Long> informationSystemIds = normalizeIds(request.getInformationSystemIds());
        List<Long> projectIds = normalizeIds(request.getProjectIds());

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
        personMapper.deleteById(id);
        auditService.record("PERSON", id, AuditActionType.DELETE, "Deleted person " + id, "SYSTEM");
    }

    private void validateRequest(PersonUpsertRequest request) {
        supportService.ensureDepartmentExists(request.getDepartmentId());
        supportService.ensureServiceProviderExists(request.getServiceProviderId());
        supportService.ensureCommonStatusValid(request.getStatus(), "人员");
        supportService.ensurePersonTypeValid(request.getPersonType());
    }

    private Person toEntity(PersonUpsertRequest request) {
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
        person.setStatus(request.getStatus());
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

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream().filter(item -> item != null).distinct().collect(Collectors.toList());
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
