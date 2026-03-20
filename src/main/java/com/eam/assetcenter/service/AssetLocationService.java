package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.AssetHardware;
import com.eam.assetcenter.domain.entity.AssetLocation;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetLocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 资产位置业务服务，负责位置主数据维护。
 */
@Service
@RequiredArgsConstructor
public class AssetLocationService {

    private final AssetLocationMapper assetLocationMapper;
    private final AssetHardwareMapper assetHardwareMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public AssetLocation create(AssetLocation assetLocation) {
        supportService.ensureUniqueLocationCode(assetLocation.getCode(), null);
        assetLocationMapper.insert(assetLocation);
        auditService.record("LOCATION", assetLocation.getId(), AuditActionType.CREATE, "Created location " + assetLocation.getCode(), "SYSTEM");
        return assetLocation;
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public AssetLocation update(Long id, AssetLocation assetLocation) {
        getById(id);
        supportService.ensureUniqueLocationCode(assetLocation.getCode(), id);
        assetLocation.setId(id);
        assetLocationMapper.updateById(assetLocation);
        auditService.record("LOCATION", id, AuditActionType.UPDATE, "Updated location " + assetLocation.getCode(), "SYSTEM");
        return getById(id);
    }

    /**
     * 根据主键查询资源记录，不存在时抛出业务异常。
     */
    public AssetLocation getById(Long id) {
        AssetLocation assetLocation = assetLocationMapper.selectById(id);
        if (assetLocation == null) {
            throw new BusinessException("Location not found: " + id);
        }
        return assetLocation;
    }

    /**
     * 按条件分页查询资源列表。
     */
    public PageResponse<AssetLocation> page(int pageNo, int pageSize, String keyword) {
        LambdaQueryWrapper<AssetLocation> wrapper = new LambdaQueryWrapper<AssetLocation>()
                .and(keyword != null && !keyword.trim().isEmpty(),
                        q -> q.like(AssetLocation::getCode, keyword).or().like(AssetLocation::getName, keyword))
                .orderByAsc(AssetLocation::getCode);
        return PageResponse.from(assetLocationMapper.selectPage(new Page<AssetLocation>(pageNo, pageSize), wrapper));
    }

    /**
     * 删除指定主键对应的资源记录，删除前检查是否被硬件资产引用。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        getById(id);
        // 检查是否被硬件资产引用
        Long hardwareCount = assetHardwareMapper.selectCount(new LambdaQueryWrapper<AssetHardware>().eq(AssetHardware::getLocationId, id));
        if (hardwareCount > 0) {
            throw new BusinessException("该位置下仍有 " + hardwareCount + " 件硬件资产，无法删除");
        }
        assetLocationMapper.deleteById(id);
        auditService.record("LOCATION", id, AuditActionType.DELETE, "Deleted location " + id, "SYSTEM");
    }
}





