package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.enums.PersonRelationType;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.ProjectHardwareRel;
import com.eam.assetcenter.domain.entity.ProjectInfo;
import com.eam.assetcenter.domain.entity.ProjectPersonRel;
import com.eam.assetcenter.domain.entity.ProjectSystemRel;
import com.eam.assetcenter.domain.entity.ProjectVendorRel;
import com.eam.assetcenter.infrastructure.mapper.ProjectHardwareRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectInfoMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectPersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectSystemRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectVendorRelMapper;
import com.eam.assetcenter.web.request.ProjectRelationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目业务服务，负责项目台账和关联关系维护。
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectInfoMapper projectInfoMapper;
    private final ProjectSystemRelMapper projectSystemRelMapper;
    private final ProjectVendorRelMapper projectVendorRelMapper;
    private final ProjectPersonRelMapper projectPersonRelMapper;
    private final ProjectHardwareRelMapper projectHardwareRelMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增资源记录。
     */
    public ProjectInfo create(ProjectInfo projectInfo) {
        supportService.ensureUniqueProjectCode(projectInfo.getCode(), null);
        projectInfoMapper.insert(projectInfo);
        auditService.record("PROJECT", projectInfo.getId(), AuditActionType.CREATE, "Created project " + projectInfo.getCode(), "SYSTEM");
        return projectInfo;
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    public ProjectInfo update(Long id, ProjectInfo projectInfo) {
        getById(id);
        supportService.ensureUniqueProjectCode(projectInfo.getCode(), id);
        projectInfo.setId(id);
        projectInfoMapper.updateById(projectInfo);
        auditService.record("PROJECT", id, AuditActionType.UPDATE, "Updated project " + projectInfo.getCode(), "SYSTEM");
        return getById(id);
    }

    /**
     * 根据主键查询资源记录，不存在时抛出业务异常。
     */
    public ProjectInfo getById(Long id) {
        ProjectInfo projectInfo = projectInfoMapper.selectById(id);
        if (projectInfo == null) {
            throw new BusinessException("Project not found: " + id);
        }
        return projectInfo;
    }

    /**
     * 查询资源详情，并聚合相关联的数据。
     */
    public Map<String, Object> getDetail(Long id) {
        Map<String, Object> detail = new HashMap<String, Object>();
        detail.put("project", getById(id));
        detail.put("informationSystemIds", projectSystemRelMapper.selectList(
                        new LambdaQueryWrapper<ProjectSystemRel>().eq(ProjectSystemRel::getProjectId, id))
                .stream().map(ProjectSystemRel::getInformationSystemId).collect(Collectors.toList()));
        detail.put("serviceProviderIds", projectVendorRelMapper.selectList(
                        new LambdaQueryWrapper<ProjectVendorRel>().eq(ProjectVendorRel::getProjectId, id))
                .stream().map(ProjectVendorRel::getServiceProviderId).collect(Collectors.toList()));
        detail.put("personIds", projectPersonRelMapper.selectList(
                        new LambdaQueryWrapper<ProjectPersonRel>().eq(ProjectPersonRel::getProjectId, id))
                .stream().map(ProjectPersonRel::getPersonId).collect(Collectors.toList()));
        detail.put("hardwareAssetIds", projectHardwareRelMapper.selectList(
                        new LambdaQueryWrapper<ProjectHardwareRel>().eq(ProjectHardwareRel::getProjectId, id))
                .stream().map(ProjectHardwareRel::getHardwareAssetId).collect(Collectors.toList()));
        return detail;
    }

    /**
     * 按条件分页查询资源列表。
     */
    public PageResponse<ProjectInfo> page(int pageNo, int pageSize, String keyword, String projectType, String projectStatus) {
        LambdaQueryWrapper<ProjectInfo> wrapper = new LambdaQueryWrapper<ProjectInfo>()
                .and(keyword != null && !keyword.trim().isEmpty(),
                        q -> q.like(ProjectInfo::getCode, keyword).or().like(ProjectInfo::getName, keyword))
                .eq(projectType != null && !projectType.trim().isEmpty(), ProjectInfo::getProjectType, projectType)
                .eq(projectStatus != null && !projectStatus.trim().isEmpty(), ProjectInfo::getProjectStatus, projectStatus)
                .orderByAsc(ProjectInfo::getCode);
        return PageResponse.from(projectInfoMapper.selectPage(new Page<ProjectInfo>(pageNo, pageSize), wrapper));
    }

    /**
     * 查询可用于下拉选择的资源列表。
     */
    public List<ProjectInfo> options() {
        return projectInfoMapper.selectList(new LambdaQueryWrapper<ProjectInfo>().orderByAsc(ProjectInfo::getCode));
    }

    /**
     * 同步资源的关联关系数据。
     */
    public void syncRelations(Long id, ProjectRelationRequest request) {
        getById(id);
        List<Long> informationSystemIds = request.getInformationSystemIds() == null ? Collections.<Long>emptyList() : request.getInformationSystemIds();
        List<Long> serviceProviderIds = request.getServiceProviderIds() == null ? Collections.<Long>emptyList() : request.getServiceProviderIds();
        List<Long> personIds = request.getPersonIds() == null ? Collections.<Long>emptyList() : request.getPersonIds();
        List<Long> hardwareAssetIds = request.getHardwareAssetIds() == null ? Collections.<Long>emptyList() : request.getHardwareAssetIds();

        for (Long informationSystemId : informationSystemIds) {
            supportService.ensureInformationSystemExists(informationSystemId);
        }
        for (Long serviceProviderId : serviceProviderIds) {
            supportService.ensureServiceProviderExists(serviceProviderId);
        }
        for (Long personId : personIds) {
            supportService.ensurePersonExists(personId);
        }
        for (Long hardwareAssetId : hardwareAssetIds) {
            supportService.ensureHardwareExists(hardwareAssetId);
        }

        projectSystemRelMapper.delete(new LambdaQueryWrapper<ProjectSystemRel>().eq(ProjectSystemRel::getProjectId, id));
        for (Long informationSystemId : informationSystemIds) {
            ProjectSystemRel relation = new ProjectSystemRel();
            relation.setProjectId(id);
            relation.setInformationSystemId(informationSystemId);
            projectSystemRelMapper.insert(relation);
        }

        projectVendorRelMapper.delete(new LambdaQueryWrapper<ProjectVendorRel>().eq(ProjectVendorRel::getProjectId, id));
        for (Long serviceProviderId : serviceProviderIds) {
            ProjectVendorRel relation = new ProjectVendorRel();
            relation.setProjectId(id);
            relation.setServiceProviderId(serviceProviderId);
            projectVendorRelMapper.insert(relation);
        }

        projectPersonRelMapper.delete(new LambdaQueryWrapper<ProjectPersonRel>().eq(ProjectPersonRel::getProjectId, id));
        for (Long personId : personIds) {
            ProjectPersonRel relation = new ProjectPersonRel();
            relation.setProjectId(id);
            relation.setPersonId(personId);
            relation.setRelationType(PersonRelationType.RESPONSIBLE.name());
            projectPersonRelMapper.insert(relation);
        }

        projectHardwareRelMapper.delete(new LambdaQueryWrapper<ProjectHardwareRel>().eq(ProjectHardwareRel::getProjectId, id));
        for (Long hardwareAssetId : hardwareAssetIds) {
            ProjectHardwareRel relation = new ProjectHardwareRel();
            relation.setProjectId(id);
            relation.setHardwareAssetId(hardwareAssetId);
            projectHardwareRelMapper.insert(relation);
        }

        auditService.record("PROJECT", id, AuditActionType.RELATION_SYNC, "Synchronized project relations", "SYSTEM");
    }

    /**
     * 删除指定主键对应的资源记录。
     */
    public void delete(Long id) {
        getById(id);
        projectInfoMapper.deleteById(id);
        auditService.record("PROJECT", id, AuditActionType.DELETE, "Deleted project " + id, "SYSTEM");
    }
}





