package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.AssetHardwareVendorRel;
import com.eam.assetcenter.domain.entity.ProjectVendorRel;
import com.eam.assetcenter.domain.entity.ServiceProviderPersonRel;
import com.eam.assetcenter.domain.entity.ServiceProvider;
import com.eam.assetcenter.domain.entity.SystemVendorRel;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareVendorRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectVendorRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderPersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.SystemVendorRelMapper;
import com.eam.assetcenter.web.request.ServiceProviderRelationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
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
    private final AssetHardwareVendorRelMapper hardwareVendorRelMapper;
    private final SystemVendorRelMapper systemVendorRelMapper;
    private final ProjectVendorRelMapper projectVendorRelMapper;
    private final ServiceProviderPersonRelMapper serviceProviderPersonRelMapper;
    private final AuditService auditService;
    private final SupportService supportService;

    /**
     * 新增资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceProvider create(ServiceProvider serviceProvider) {
        supportService.ensureUniqueServiceProviderCode(serviceProvider.getCode(), null);
        supportService.ensureCommonStatusValid(serviceProvider.getStatus(), "服务商");
        serviceProviderMapper.insert(serviceProvider);
        auditService.record("SERVICE_PROVIDER", serviceProvider.getId(), AuditActionType.CREATE, "Created provider " + serviceProvider.getCode(), "SYSTEM");
        return serviceProvider;
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public ServiceProvider update(Long id, ServiceProvider serviceProvider) {
        getById(id);
        supportService.ensureUniqueServiceProviderCode(serviceProvider.getCode(), id);
        supportService.ensureCommonStatusValid(serviceProvider.getStatus(), "服务商");
        serviceProvider.setId(id);
        serviceProviderMapper.updateById(serviceProvider);
        auditService.record("SERVICE_PROVIDER", id, AuditActionType.UPDATE, "Updated provider " + serviceProvider.getCode(), "SYSTEM");
        return getById(id);
    }

    /**
     * 根据主键查询资源记录，不存在时抛出业务异常。
     */
    public ServiceProvider getById(Long id) {
        ServiceProvider serviceProvider = serviceProviderMapper.selectById(id);
        if (serviceProvider == null) {
            throw new BusinessException("Service provider not found: " + id);
        }
        return serviceProvider;
    }

    /**
     * 查询资源详情，并聚合相关联的数据。
     */
    public Map<String, Object> getDetail(Long id) {
        Map<String, Object> detail = new HashMap<String, Object>();
        detail.put("serviceProvider", getById(id));
        detail.put("hardwareAssetIds", hardwareVendorRelMapper.selectList(
                new LambdaQueryWrapper<AssetHardwareVendorRel>().eq(AssetHardwareVendorRel::getServiceProviderId, id))
                .stream().map(AssetHardwareVendorRel::getHardwareAssetId).collect(Collectors.toList()));
        detail.put("informationSystemIds", systemVendorRelMapper.selectList(
                new LambdaQueryWrapper<SystemVendorRel>().eq(SystemVendorRel::getServiceProviderId, id))
                .stream().map(SystemVendorRel::getInformationSystemId).collect(Collectors.toList()));
        detail.put("personIds", serviceProviderPersonRelMapper.selectList(
                new LambdaQueryWrapper<ServiceProviderPersonRel>().eq(ServiceProviderPersonRel::getServiceProviderId, id))
                .stream().map(ServiceProviderPersonRel::getPersonId).collect(Collectors.toList()));
        detail.put("projectIds", projectVendorRelMapper.selectList(
                new LambdaQueryWrapper<ProjectVendorRel>().eq(ProjectVendorRel::getServiceProviderId, id))
                .stream().map(ProjectVendorRel::getProjectId).collect(Collectors.toList()));
        return detail;
    }

    /**
     * 按条件分页查询资源列表。
     */
    public PageResponse<ServiceProvider> page(int pageNo, int pageSize, String keyword, String type, String status) {
        if (status != null && !status.trim().isEmpty()) {
            supportService.ensureCommonStatusValid(status, "服务商");
        }
        LambdaQueryWrapper<ServiceProvider> wrapper = new LambdaQueryWrapper<ServiceProvider>()
                .and(keyword != null && !keyword.trim().isEmpty(),
                        q -> q.like(ServiceProvider::getCode, keyword).or().like(ServiceProvider::getName, keyword))
                .eq(type != null && !type.trim().isEmpty(), ServiceProvider::getType, type)
                .eq(status != null && !status.trim().isEmpty(), ServiceProvider::getStatus, status)
                .orderByAsc(ServiceProvider::getCode);
        return PageResponse.from(serviceProviderMapper.selectPage(new Page<ServiceProvider>(pageNo, pageSize), wrapper));
    }

    /**
     * 查询可用于下拉选择的资源列表。
     */
    public List<ServiceProvider> options() {
        return serviceProviderMapper.selectList(new LambdaQueryWrapper<ServiceProvider>().orderByAsc(ServiceProvider::getCode));
    }

    /**
     * 同步服务商的关联关系数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncRelations(Long id, ServiceProviderRelationRequest request) {
        getById(id);
        List<Long> hardwareAssetIds = request.getHardwareAssetIds() == null ? Collections.<Long>emptyList() : request.getHardwareAssetIds();
        List<Long> informationSystemIds = request.getInformationSystemIds() == null ? Collections.<Long>emptyList() : request.getInformationSystemIds();
        List<Long> personIds = request.getPersonIds() == null ? Collections.<Long>emptyList() : request.getPersonIds();

        for (Long hardwareAssetId : hardwareAssetIds) {
            supportService.ensureHardwareExists(hardwareAssetId);
        }
        for (Long informationSystemId : informationSystemIds) {
            supportService.ensureInformationSystemExists(informationSystemId);
        }
        for (Long personId : personIds) {
            supportService.ensurePersonExists(personId);
        }

        hardwareVendorRelMapper.delete(new LambdaQueryWrapper<AssetHardwareVendorRel>().eq(AssetHardwareVendorRel::getServiceProviderId, id));
        for (Long hardwareAssetId : hardwareAssetIds) {
            AssetHardwareVendorRel relation = new AssetHardwareVendorRel();
            relation.setHardwareAssetId(hardwareAssetId);
            relation.setServiceProviderId(id);
            hardwareVendorRelMapper.insert(relation);
        }

        systemVendorRelMapper.delete(new LambdaQueryWrapper<SystemVendorRel>().eq(SystemVendorRel::getServiceProviderId, id));
        for (Long informationSystemId : informationSystemIds) {
            SystemVendorRel relation = new SystemVendorRel();
            relation.setInformationSystemId(informationSystemId);
            relation.setServiceProviderId(id);
            systemVendorRelMapper.insert(relation);
        }

        serviceProviderPersonRelMapper.delete(new LambdaQueryWrapper<ServiceProviderPersonRel>().eq(ServiceProviderPersonRel::getServiceProviderId, id));
        for (Long personId : personIds) {
            ServiceProviderPersonRel relation = new ServiceProviderPersonRel();
            relation.setServiceProviderId(id);
            relation.setPersonId(personId);
            serviceProviderPersonRelMapper.insert(relation);
        }

        auditService.record("SERVICE_PROVIDER", id, AuditActionType.RELATION_SYNC, "Synchronized service provider relations", "SYSTEM");
    }

    /**
     * 删除指定主键对应的资源记录，删除前检查是否被关联表引用。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        // 检查是否被硬件资产引用
        Long hwCount = hardwareVendorRelMapper.selectCount(new LambdaQueryWrapper<AssetHardwareVendorRel>().eq(AssetHardwareVendorRel::getServiceProviderId, id));
        if (hwCount > 0) {
            throw new BusinessException("该服务商仍被 " + hwCount + " 件硬件资产关联，无法删除");
        }
        // 检查是否被信息系统引用
        Long sysCount = systemVendorRelMapper.selectCount(new LambdaQueryWrapper<SystemVendorRel>().eq(SystemVendorRel::getServiceProviderId, id));
        if (sysCount > 0) {
            throw new BusinessException("该服务商仍被 " + sysCount + " 个信息系统关联，无法删除");
        }
        // 检查是否被人员引用
        Long personCount = serviceProviderPersonRelMapper.selectCount(new LambdaQueryWrapper<ServiceProviderPersonRel>().eq(ServiceProviderPersonRel::getServiceProviderId, id));
        if (personCount > 0) {
            throw new BusinessException("该服务商仍被 " + personCount + " 名人员关联，无法删除");
        }
        // 检查是否被项目引用
        Long projCount = projectVendorRelMapper.selectCount(new LambdaQueryWrapper<ProjectVendorRel>().eq(ProjectVendorRel::getServiceProviderId, id));
        if (projCount > 0) {
            throw new BusinessException("该服务商仍被 " + projCount + " 个项目关联，无法删除");
        }
        serviceProviderMapper.deleteById(id);
        auditService.record("SERVICE_PROVIDER", id, AuditActionType.DELETE, "Deleted provider " + id, "SYSTEM");
    }
}





