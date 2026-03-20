package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.AssetHardwarePersonRel;
import com.eam.assetcenter.domain.entity.Person;
import com.eam.assetcenter.domain.entity.ProjectPersonRel;
import com.eam.assetcenter.domain.entity.SystemPersonRel;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwarePersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.PersonMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectPersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.SystemPersonRelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 人员业务服务，负责人员主数据及关联信息聚合。
 */
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonMapper personMapper;
    private final AssetHardwarePersonRelMapper hardwarePersonRelMapper;
    private final SystemPersonRelMapper systemPersonRelMapper;
    private final ProjectPersonRelMapper projectPersonRelMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public Person create(Person person) {
        supportService.ensureDepartmentExists(person.getDepartmentId());
        personMapper.insert(person);
        auditService.record("PERSON", person.getId(), AuditActionType.CREATE, "Created person " + person.getName(), "SYSTEM");
        return person;
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public Person update(Long id, Person person) {
        getById(id);
        supportService.ensureDepartmentExists(person.getDepartmentId());
        person.setId(id);
        personMapper.updateById(person);
        auditService.record("PERSON", id, AuditActionType.UPDATE, "Updated person " + person.getName(), "SYSTEM");
        return getById(id);
    }

    /**
     * 根据主键查询资源记录，不存在时抛出业务异常。
     */
    public Person getById(Long id) {
        Person person = personMapper.selectById(id);
        if (person == null) {
            throw new BusinessException("Person not found: " + id);
        }
        return person;
    }

    /**
     * 查询资源详情，并聚合相关联的数据。
     */
    public Map<String, Object> getDetail(Long id) {
        Map<String, Object> detail = new HashMap<String, Object>();
        detail.put("person", getById(id));
        detail.put("hardwareAssetIds", hardwarePersonRelMapper.selectList(
                new LambdaQueryWrapper<AssetHardwarePersonRel>().eq(AssetHardwarePersonRel::getPersonId, id))
                .stream().map(AssetHardwarePersonRel::getHardwareAssetId).collect(Collectors.toList()));
        detail.put("informationSystemIds", systemPersonRelMapper.selectList(
                new LambdaQueryWrapper<SystemPersonRel>().eq(SystemPersonRel::getPersonId, id))
                .stream().map(SystemPersonRel::getInformationSystemId).collect(Collectors.toList()));
        detail.put("projectIds", projectPersonRelMapper.selectList(
                new LambdaQueryWrapper<ProjectPersonRel>().eq(ProjectPersonRel::getPersonId, id))
                .stream().map(ProjectPersonRel::getProjectId).collect(Collectors.toList()));
        return detail;
    }

    /**
     * 按条件分页查询资源列表。
     */
    public PageResponse<Person> page(int pageNo, int pageSize, String keyword, Long departmentId, String status) {
        LambdaQueryWrapper<Person> wrapper = new LambdaQueryWrapper<Person>()
                .and(keyword != null && !keyword.trim().isEmpty(),
                        q -> q.like(Person::getName, keyword).or().like(Person::getEmployeeNo, keyword).or().like(Person::getMobile, keyword))
                .eq(departmentId != null, Person::getDepartmentId, departmentId)
                .eq(status != null && !status.trim().isEmpty(), Person::getStatus, status)
                .orderByAsc(Person::getName);
        return PageResponse.from(personMapper.selectPage(new Page<Person>(pageNo, pageSize), wrapper));
    }

    /**
     * 查询可用于下拉选择的资源列表。
     */
    public List<Person> options() {
        return personMapper.selectList(new LambdaQueryWrapper<Person>().eq(Person::getStatus, "ACTIVE").orderByAsc(Person::getName));
    }

    /**
     * 删除指定主键对应的资源记录，删除前检查是否被关联表引用。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        // 检查是否被硬件资产引用
        Long hwCount = hardwarePersonRelMapper.selectCount(new LambdaQueryWrapper<AssetHardwarePersonRel>().eq(AssetHardwarePersonRel::getPersonId, id));
        if (hwCount > 0) {
            throw new BusinessException("该人员仍被 " + hwCount + " 件硬件资产关联，无法删除");
        }
        // 检查是否被信息系统引用
        Long sysCount = systemPersonRelMapper.selectCount(new LambdaQueryWrapper<SystemPersonRel>().eq(SystemPersonRel::getPersonId, id));
        if (sysCount > 0) {
            throw new BusinessException("该人员仍被 " + sysCount + " 个信息系统关联，无法删除");
        }
        // 检查是否被项目引用
        Long projCount = projectPersonRelMapper.selectCount(new LambdaQueryWrapper<ProjectPersonRel>().eq(ProjectPersonRel::getPersonId, id));
        if (projCount > 0) {
            throw new BusinessException("该人员仍被 " + projCount + " 个项目关联，无法删除");
        }
        personMapper.deleteById(id);
        auditService.record("PERSON", id, AuditActionType.DELETE, "Deleted person " + id, "SYSTEM");
    }
}





