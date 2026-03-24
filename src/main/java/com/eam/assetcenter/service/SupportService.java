package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eam.assetcenter.common.enums.CommonStatus;
import com.eam.assetcenter.common.enums.HardwareStatus;
import com.eam.assetcenter.common.enums.ProjectStatus;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.AssetHardware;
import com.eam.assetcenter.domain.entity.AssetLocation;
import com.eam.assetcenter.domain.entity.Department;
import com.eam.assetcenter.domain.entity.InformationSystem;
import com.eam.assetcenter.domain.entity.Person;
import com.eam.assetcenter.domain.entity.ProjectInfo;
import com.eam.assetcenter.domain.entity.ServiceProvider;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetLocationMapper;
import com.eam.assetcenter.infrastructure.mapper.DepartmentMapper;
import com.eam.assetcenter.infrastructure.mapper.InformationSystemMapper;
import com.eam.assetcenter.infrastructure.mapper.PersonMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectInfoMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 通用校验服务，封装跨模块存在性与唯一性校验。
 */
@Service
@RequiredArgsConstructor
public class SupportService {

    private final DepartmentMapper departmentMapper;
    private final AssetLocationMapper assetLocationMapper;
    private final ServiceProviderMapper serviceProviderMapper;
    private final PersonMapper personMapper;
    private final InformationSystemMapper informationSystemMapper;
    private final ProjectInfoMapper projectInfoMapper;
    private final AssetHardwareMapper assetHardwareMapper;

    /**
     * 校验部门是否存在。
     */
    public void ensureDepartmentExists(Long departmentId) {
        if (departmentId != null && departmentMapper.selectById(departmentId) == null) {
            throw new BusinessException("Department not found: " + departmentId);
        }
    }

    /**
     * 校验位置是否存在。
     */
    public void ensureLocationExists(Long locationId) {
        if (locationId != null && assetLocationMapper.selectById(locationId) == null) {
            throw new BusinessException("Location not found: " + locationId);
        }
    }

    /**
     * 校验服务商是否存在。
     */
    public void ensureServiceProviderExists(Long id) {
        if (id != null && serviceProviderMapper.selectById(id) == null) {
            throw new BusinessException("Service provider not found: " + id);
        }
    }

    /**
     * 校验人员是否存在。
     */
    public void ensurePersonExists(Long id) {
        if (id != null && personMapper.selectById(id) == null) {
            throw new BusinessException("Person not found: " + id);
        }
    }

    /**
     * 校验信息系统是否存在。
     */
    public void ensureInformationSystemExists(Long id) {
        if (id != null && informationSystemMapper.selectById(id) == null) {
            throw new BusinessException("Information system not found: " + id);
        }
    }

    /**
     * 校验项目是否存在。
     */
    public void ensureProjectExists(Long id) {
        if (id != null && projectInfoMapper.selectById(id) == null) {
            throw new BusinessException("Project not found: " + id);
        }
    }

    /**
     * 校验硬件资产是否存在。
     */
    public void ensureHardwareExists(Long id) {
        if (id != null && assetHardwareMapper.selectById(id) == null) {
            throw new BusinessException("Hardware asset not found: " + id);
        }
    }

    /**
     * 校验部门编码是否唯一。
     */
    public void ensureUniqueDepartmentCode(String code, Long excludeId) {
        ensureUnique(departmentMapper.selectOne(new LambdaQueryWrapper<Department>().eq(Department::getCode, code)), excludeId, "Department code already exists");
    }

    /**
     * 校验位置编码是否唯一。
     */
    public void ensureUniqueLocationCode(String code, Long excludeId) {
        ensureUnique(assetLocationMapper.selectOne(new LambdaQueryWrapper<AssetLocation>().eq(AssetLocation::getCode, code)), excludeId, "Location code already exists");
    }

    /**
     * 校验服务商编码是否唯一。
     */
    public void ensureUniqueServiceProviderCode(String code, Long excludeId) {
        ensureUnique(serviceProviderMapper.selectOne(new LambdaQueryWrapper<ServiceProvider>().eq(ServiceProvider::getCode, code)), excludeId, "Service provider code already exists");
    }

    /**
     * 校验信息系统编码是否唯一。
     */
    public void ensureUniqueSystemCode(String code, Long excludeId) {
        ensureUnique(informationSystemMapper.selectOne(new LambdaQueryWrapper<InformationSystem>().eq(InformationSystem::getCode, code)), excludeId, "Information system code already exists");
    }

    /**
     * 校验项目编码是否唯一。
     */
    public void ensureUniqueProjectCode(String code, Long excludeId) {
        ensureUnique(projectInfoMapper.selectOne(new LambdaQueryWrapper<ProjectInfo>().eq(ProjectInfo::getCode, code)), excludeId, "Project code already exists");
    }

    /**
     * 校验硬件资产编码是否唯一。
     */
    public void ensureUniqueAssetCode(String code, Long excludeId) {
        ensureUnique(assetHardwareMapper.selectOne(new LambdaQueryWrapper<AssetHardware>().eq(AssetHardware::getAssetCode, code)), excludeId, "Hardware asset code already exists");
    }

    /**
     * 校验通用状态是否合法。
     */
    public void ensureCommonStatusValid(String status, String resourceLabel) {
        if (!CommonStatus.isValid(status)) {
            throw new BusinessException(resourceLabel + "状态不合法: " + status);
        }
    }

    /**
     * 校验项目状态是否合法。
     */
    public void ensureProjectStatusValid(String status) {
        if (!ProjectStatus.isValid(status)) {
            throw new BusinessException("项目状态不合法: " + status);
        }
    }

    /**
     * 校验硬件状态是否合法。
     */
    public void ensureHardwareStatusValid(String status) {
        if (!HardwareStatus.isValid(status)) {
            throw new BusinessException("硬件状态不合法: " + status);
        }
    }

    private void ensureUnique(Object entity, Long excludeId, String message) {
        if (entity == null) {
            return;
        }
        Long currentId = null;
        if (entity instanceof Department) {
            currentId = ((Department) entity).getId();
        } else if (entity instanceof AssetLocation) {
            currentId = ((AssetLocation) entity).getId();
        } else if (entity instanceof ServiceProvider) {
            currentId = ((ServiceProvider) entity).getId();
        } else if (entity instanceof InformationSystem) {
            currentId = ((InformationSystem) entity).getId();
        } else if (entity instanceof ProjectInfo) {
            currentId = ((ProjectInfo) entity).getId();
        } else if (entity instanceof AssetHardware) {
            currentId = ((AssetHardware) entity).getId();
        }
        if (excludeId == null || !excludeId.equals(currentId)) {
            throw new BusinessException(message);
        }
    }
}





