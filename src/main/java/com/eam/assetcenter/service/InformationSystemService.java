package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.enums.PersonRelationType;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.AssetHardwareSystemRel;
import com.eam.assetcenter.domain.entity.InformationSystem;
import com.eam.assetcenter.domain.entity.ProjectSystemRel;
import com.eam.assetcenter.domain.entity.SystemPersonRel;
import com.eam.assetcenter.domain.entity.SystemVendorRel;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareSystemRelMapper;
import com.eam.assetcenter.infrastructure.mapper.InformationSystemMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectSystemRelMapper;
import com.eam.assetcenter.infrastructure.mapper.SystemPersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.SystemVendorRelMapper;
import com.eam.assetcenter.web.request.InformationSystemRelationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 信息系统业务服务，负责系统台账和关联关系维护。
 */
@Service
@RequiredArgsConstructor
public class InformationSystemService {

    private final InformationSystemMapper informationSystemMapper;
    private final SystemVendorRelMapper systemVendorRelMapper;
    private final SystemPersonRelMapper systemPersonRelMapper;
    private final ProjectSystemRelMapper projectSystemRelMapper;
    private final AssetHardwareSystemRelMapper hardwareSystemRelMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public InformationSystem create(InformationSystem informationSystem) {
        supportService.ensureUniqueSystemCode(informationSystem.getCode(), null);
        supportService.ensureCommonStatusValid(informationSystem.getStatus(), "信息系统");
        informationSystemMapper.insert(informationSystem);
        auditService.record("INFORMATION_SYSTEM", informationSystem.getId(), AuditActionType.CREATE,
                "Created information system " + informationSystem.getCode(), "SYSTEM");
        return informationSystem;
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public InformationSystem update(Long id, InformationSystem informationSystem) {
        getById(id);
        supportService.ensureUniqueSystemCode(informationSystem.getCode(), id);
        supportService.ensureCommonStatusValid(informationSystem.getStatus(), "信息系统");
        informationSystem.setId(id);
        informationSystemMapper.updateById(informationSystem);
        auditService.record("INFORMATION_SYSTEM", id, AuditActionType.UPDATE,
                "Updated information system " + informationSystem.getCode(), "SYSTEM");
        return getById(id);
    }

    /**
     * 根据主键查询资源记录，不存在时抛出业务异常。
     */
    public InformationSystem getById(Long id) {
        InformationSystem informationSystem = informationSystemMapper.selectById(id);
        if (informationSystem == null) {
            throw new BusinessException("Information system not found: " + id);
        }
        return informationSystem;
    }

    /**
     * 查询资源详情，并聚合相关联的数据。
     */
    public Map<String, Object> getDetail(Long id) {
        Map<String, Object> detail = new HashMap<String, Object>();
        detail.put("informationSystem", getById(id));
        detail.put("serviceProviderIds", systemVendorRelMapper.selectList(
                        new LambdaQueryWrapper<SystemVendorRel>().eq(SystemVendorRel::getInformationSystemId, id))
                .stream().map(SystemVendorRel::getServiceProviderId).collect(Collectors.toList()));
        detail.put("personIds", systemPersonRelMapper.selectList(
                        new LambdaQueryWrapper<SystemPersonRel>().eq(SystemPersonRel::getInformationSystemId, id))
                .stream().map(SystemPersonRel::getPersonId).collect(Collectors.toList()));
        detail.put("projectIds", projectSystemRelMapper.selectList(
                        new LambdaQueryWrapper<ProjectSystemRel>().eq(ProjectSystemRel::getInformationSystemId, id))
                .stream().map(ProjectSystemRel::getProjectId).collect(Collectors.toList()));
        return detail;
    }

    /**
     * 按条件分页查询资源列表。
     */
    public PageResponse<InformationSystem> page(int pageNo, int pageSize, String keyword, String systemType, String status) {
        if (status != null && !status.trim().isEmpty()) {
            supportService.ensureCommonStatusValid(status, "信息系统");
        }
        LambdaQueryWrapper<InformationSystem> wrapper = new LambdaQueryWrapper<InformationSystem>()
                .and(keyword != null && !keyword.trim().isEmpty(),
                        q -> q.like(InformationSystem::getCode, keyword).or().like(InformationSystem::getName, keyword))
                .eq(systemType != null && !systemType.trim().isEmpty(), InformationSystem::getSystemType, systemType)
                .eq(status != null && !status.trim().isEmpty(), InformationSystem::getStatus, status)
                .orderByAsc(InformationSystem::getCode);
        return PageResponse.from(informationSystemMapper.selectPage(new Page<InformationSystem>(pageNo, pageSize), wrapper));
    }

    /**
     * 查询可用于下拉选择的资源列表。
     */
    public List<InformationSystem> options() {
        return informationSystemMapper.selectList(new LambdaQueryWrapper<InformationSystem>()
                .orderByAsc(InformationSystem::getCode));
    }

    /**
     * 同步资源的关联关系数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncRelations(Long id, InformationSystemRelationRequest request) {
        getById(id);
        List<Long> serviceProviderIds = request.getServiceProviderIds() == null ? Collections.<Long>emptyList() : request.getServiceProviderIds();
        List<Long> personIds = request.getPersonIds() == null ? Collections.<Long>emptyList() : request.getPersonIds();
        List<Long> projectIds = request.getProjectIds() == null ? Collections.<Long>emptyList() : request.getProjectIds();

        for (Long serviceProviderId : serviceProviderIds) {
            supportService.ensureServiceProviderExists(serviceProviderId);
        }
        for (Long personId : personIds) {
            supportService.ensurePersonExists(personId);
        }
        for (Long projectId : projectIds) {
            supportService.ensureProjectExists(projectId);
        }

        systemVendorRelMapper.delete(new LambdaQueryWrapper<SystemVendorRel>().eq(SystemVendorRel::getInformationSystemId, id));
        for (Long serviceProviderId : serviceProviderIds) {
            SystemVendorRel relation = new SystemVendorRel();
            relation.setInformationSystemId(id);
            relation.setServiceProviderId(serviceProviderId);
            systemVendorRelMapper.insert(relation);
        }

        systemPersonRelMapper.delete(new LambdaQueryWrapper<SystemPersonRel>().eq(SystemPersonRel::getInformationSystemId, id));
        for (Long personId : personIds) {
            SystemPersonRel relation = new SystemPersonRel();
            relation.setInformationSystemId(id);
            relation.setPersonId(personId);
            relation.setRelationType(PersonRelationType.RESPONSIBLE.name());
            systemPersonRelMapper.insert(relation);
        }

        projectSystemRelMapper.delete(new LambdaQueryWrapper<ProjectSystemRel>().eq(ProjectSystemRel::getInformationSystemId, id));
        for (Long projectId : projectIds) {
            ProjectSystemRel relation = new ProjectSystemRel();
            relation.setProjectId(projectId);
            relation.setInformationSystemId(id);
            projectSystemRelMapper.insert(relation);
        }

        auditService.record("INFORMATION_SYSTEM", id, AuditActionType.RELATION_SYNC, "Synchronized information system relations", "SYSTEM");
    }

    /**
     * 删除指定主键对应的资源记录，同时级联清理所有关联表。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        InformationSystem existing = getById(id);
        // 清理关联关系表
        systemVendorRelMapper.delete(new LambdaQueryWrapper<SystemVendorRel>().eq(SystemVendorRel::getInformationSystemId, id));
        systemPersonRelMapper.delete(new LambdaQueryWrapper<SystemPersonRel>().eq(SystemPersonRel::getInformationSystemId, id));
        projectSystemRelMapper.delete(new LambdaQueryWrapper<ProjectSystemRel>().eq(ProjectSystemRel::getInformationSystemId, id));
        hardwareSystemRelMapper.delete(new LambdaQueryWrapper<AssetHardwareSystemRel>().eq(AssetHardwareSystemRel::getInformationSystemId, id));
        // 删除主记录
        informationSystemMapper.deleteById(id);
        auditService.record("INFORMATION_SYSTEM", id, AuditActionType.DELETE, "Deleted information system " + existing.getCode(), "SYSTEM");
    }
}





