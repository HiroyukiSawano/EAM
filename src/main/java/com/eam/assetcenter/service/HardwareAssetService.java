package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.enums.HardwareCategory;
import com.eam.assetcenter.common.enums.HardwareStatus;
import com.eam.assetcenter.common.enums.LifecycleActionType;
import com.eam.assetcenter.common.enums.PersonRelationType;
import com.eam.assetcenter.common.exception.BusinessException;
import com.eam.assetcenter.domain.entity.AssetHardware;
import com.eam.assetcenter.domain.entity.AssetHardwarePersonRel;
import com.eam.assetcenter.domain.entity.AssetHardwareQueryTerminal;
import com.eam.assetcenter.domain.entity.AssetHardwareSelfServiceTerminal;
import com.eam.assetcenter.domain.entity.AssetHardwareServer;
import com.eam.assetcenter.domain.entity.AssetHardwareSystemRel;
import com.eam.assetcenter.domain.entity.AssetHardwareTicketTerminal;
import com.eam.assetcenter.domain.entity.AssetHardwareVendorRel;
import com.eam.assetcenter.domain.entity.AssetLifecycleRecord;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwarePersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareQueryTerminalMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareSelfServiceTerminalMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareServerMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareSystemRelMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareTicketTerminalMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareVendorRelMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetLifecycleRecordMapper;
import com.eam.assetcenter.web.request.HardwareAssetUpsertRequest;
import com.eam.assetcenter.web.request.HardwareBatchImportRequest;
import com.eam.assetcenter.web.request.HardwareLifecycleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 硬件资产业务服务，负责台账、关联关系和生命周期管理。
 */
@Service
@RequiredArgsConstructor
public class HardwareAssetService {

    private final AssetHardwareMapper assetHardwareMapper;
    private final AssetHardwareServerMapper serverMapper;
    private final AssetHardwareQueryTerminalMapper queryTerminalMapper;
    private final AssetHardwareTicketTerminalMapper ticketTerminalMapper;
    private final AssetHardwareSelfServiceTerminalMapper selfServiceTerminalMapper;
    private final AssetHardwareSystemRelMapper hardwareSystemRelMapper;
    private final AssetHardwarePersonRelMapper hardwarePersonRelMapper;
    private final AssetHardwareVendorRelMapper hardwareVendorRelMapper;
    private final AssetLifecycleRecordMapper assetLifecycleRecordMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    /**
     * 新增资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public AssetHardware create(HardwareAssetUpsertRequest request) {
        supportService.ensureUniqueAssetCode(request.getAssetCode(), null);
        supportService.ensureDepartmentExists(request.getDepartmentId());
        supportService.ensureLocationExists(request.getLocationId());
        AssetHardware assetHardware = toAsset(request);
        assetHardware.setHardwareStatus(HardwareStatus.REGISTERED.name());
        assetHardwareMapper.insert(assetHardware);
        upsertSubtype(assetHardware.getId(), request);
        createLifecycleRecord(assetHardware.getId(), null, HardwareStatus.REGISTERED, LifecycleActionType.REGISTER, "Initial registration", "SYSTEM");
        auditService.record("HARDWARE_ASSET", assetHardware.getId(), AuditActionType.CREATE, "Created hardware asset " + assetHardware.getAssetCode(), "SYSTEM");
        return assetHardware;
    }

    /**
     * 更新指定主键对应的资源记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public AssetHardware update(Long id, HardwareAssetUpsertRequest request) {
        AssetHardware existing = getById(id);
        supportService.ensureUniqueAssetCode(request.getAssetCode(), id);
        supportService.ensureDepartmentExists(request.getDepartmentId());
        supportService.ensureLocationExists(request.getLocationId());
        AssetHardware assetHardware = toAsset(request);
        assetHardware.setId(id);
        assetHardware.setHardwareStatus(existing.getHardwareStatus());
        assetHardwareMapper.updateById(assetHardware);
        upsertSubtype(id, request);
        auditService.record("HARDWARE_ASSET", id, AuditActionType.UPDATE, "Updated hardware asset " + request.getAssetCode(), "SYSTEM");
        return getById(id);
    }

    /**
     * 根据主键查询资源记录，不存在时抛出业务异常。
     */
    public AssetHardware getById(Long id) {
        AssetHardware assetHardware = assetHardwareMapper.selectById(id);
        if (assetHardware == null) {
            throw new BusinessException("Hardware asset not found: " + id);
        }
        return assetHardware;
    }

    /**
     * 查询资源详情，并聚合相关联的数据。
     */
    public Map<String, Object> getDetail(Long id) {
        AssetHardware assetHardware = getById(id);
        Map<String, Object> detail = new HashMap<String, Object>();
        detail.put("hardwareAsset", assetHardware);
        detail.put("subtypeDetail", findSubtypeDetail(assetHardware));
        detail.put("informationSystemIds", hardwareSystemRelMapper.selectList(
                        new LambdaQueryWrapper<AssetHardwareSystemRel>().eq(AssetHardwareSystemRel::getHardwareAssetId, id))
                .stream().map(AssetHardwareSystemRel::getInformationSystemId).collect(Collectors.toList()));
        detail.put("ownerIds", hardwarePersonRelMapper.selectList(
                        new LambdaQueryWrapper<AssetHardwarePersonRel>()
                                .eq(AssetHardwarePersonRel::getHardwareAssetId, id)
                                .eq(AssetHardwarePersonRel::getRelationType, PersonRelationType.RESPONSIBLE.name()))
                .stream().map(AssetHardwarePersonRel::getPersonId).collect(Collectors.toList()));
        detail.put("vendorIds", hardwareVendorRelMapper.selectList(
                        new LambdaQueryWrapper<AssetHardwareVendorRel>().eq(AssetHardwareVendorRel::getHardwareAssetId, id))
                .stream().map(AssetHardwareVendorRel::getServiceProviderId).collect(Collectors.toList()));
        detail.put("lifecycleRecords", assetLifecycleRecordMapper.selectList(
                new LambdaQueryWrapper<AssetLifecycleRecord>().eq(AssetLifecycleRecord::getHardwareAssetId, id).orderByDesc(AssetLifecycleRecord::getActionTime)));
        return detail;
    }

    /**
     * 按条件分页查询资源列表。
     */
    public PageResponse<AssetHardware> page(int pageNo, int pageSize, String keyword, String hardwareCategory, String hardwareStatus,
                                            Long departmentId, Long locationId) {
        LambdaQueryWrapper<AssetHardware> wrapper = new LambdaQueryWrapper<AssetHardware>()
                .and(keyword != null && !keyword.trim().isEmpty(),
                        q -> q.like(AssetHardware::getAssetCode, keyword).or().like(AssetHardware::getAssetName, keyword))
                .eq(hardwareCategory != null && !hardwareCategory.trim().isEmpty(), AssetHardware::getHardwareCategory, hardwareCategory)
                .eq(hardwareStatus != null && !hardwareStatus.trim().isEmpty(), AssetHardware::getHardwareStatus, hardwareStatus)
                .eq(departmentId != null, AssetHardware::getDepartmentId, departmentId)
                .eq(locationId != null, AssetHardware::getLocationId, locationId)
                .orderByAsc(AssetHardware::getAssetCode);
        return PageResponse.from(assetHardwareMapper.selectPage(new Page<AssetHardware>(pageNo, pageSize), wrapper));
    }

    /**
     * 查询可用于下拉选择的硬件资产列表。
     */
    public List<AssetHardware> options() {
        return assetHardwareMapper.selectList(new LambdaQueryWrapper<AssetHardware>()
                .orderByAsc(AssetHardware::getAssetCode));
    }

    /**
     * 同步硬件与信息系统之间的关联关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncSystems(Long id, List<Long> systemIds) {
        getById(id);
        List<Long> safeIds = systemIds == null ? Collections.<Long>emptyList() : systemIds;
        for (Long systemId : safeIds) {
            supportService.ensureInformationSystemExists(systemId);
        }
        hardwareSystemRelMapper.delete(new LambdaQueryWrapper<AssetHardwareSystemRel>().eq(AssetHardwareSystemRel::getHardwareAssetId, id));
        for (Long systemId : safeIds) {
            AssetHardwareSystemRel relation = new AssetHardwareSystemRel();
            relation.setHardwareAssetId(id);
            relation.setInformationSystemId(systemId);
            hardwareSystemRelMapper.insert(relation);
        }
        auditService.record("HARDWARE_ASSET", id, AuditActionType.RELATION_SYNC, "Synchronized hardware-system relations", "SYSTEM");
    }

    /**
     * 同步硬件负责人的关联关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncOwners(Long id, List<Long> ownerIds) {
        getById(id);
        List<Long> safeIds = ownerIds == null ? Collections.<Long>emptyList() : ownerIds;
        if (safeIds.size() > 1) {
            throw new BusinessException("Only one responsible owner is allowed");
        }
        for (Long ownerId : safeIds) {
            supportService.ensurePersonExists(ownerId);
        }
        hardwarePersonRelMapper.delete(new LambdaQueryWrapper<AssetHardwarePersonRel>()
                .eq(AssetHardwarePersonRel::getHardwareAssetId, id)
                .eq(AssetHardwarePersonRel::getRelationType, PersonRelationType.RESPONSIBLE.name()));
        for (Long ownerId : safeIds) {
            AssetHardwarePersonRel relation = new AssetHardwarePersonRel();
            relation.setHardwareAssetId(id);
            relation.setPersonId(ownerId);
            relation.setRelationType(PersonRelationType.RESPONSIBLE.name());
            hardwarePersonRelMapper.insert(relation);
        }
        auditService.record("HARDWARE_ASSET", id, AuditActionType.RELATION_SYNC, "Synchronized hardware-owner relations", "SYSTEM");
    }

    /**
     * 同步硬件与服务商之间的关联关系。
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncVendors(Long id, List<Long> vendorIds) {
        getById(id);
        List<Long> safeIds = vendorIds == null ? Collections.<Long>emptyList() : vendorIds;
        for (Long vendorId : safeIds) {
            supportService.ensureServiceProviderExists(vendorId);
        }
        hardwareVendorRelMapper.delete(new LambdaQueryWrapper<AssetHardwareVendorRel>().eq(AssetHardwareVendorRel::getHardwareAssetId, id));
        for (Long vendorId : safeIds) {
            AssetHardwareVendorRel relation = new AssetHardwareVendorRel();
            relation.setHardwareAssetId(id);
            relation.setServiceProviderId(vendorId);
            hardwareVendorRelMapper.insert(relation);
        }
        auditService.record("HARDWARE_ASSET", id, AuditActionType.RELATION_SYNC, "Synchronized hardware-vendor relations", "SYSTEM");
    }

    /**
     * 执行硬件生命周期流转动作。
     */
    @Transactional(rollbackFor = Exception.class)
    public AssetHardware executeLifecycle(Long id, HardwareLifecycleRequest request) {
        AssetHardware assetHardware = getById(id);
        HardwareStatus currentStatus = HardwareStatus.valueOf(assetHardware.getHardwareStatus());
        HardwareStatus targetStatus = transition(currentStatus, request.getAction());
        assetHardware.setHardwareStatus(targetStatus.name());
        assetHardwareMapper.updateById(assetHardware);
        createLifecycleRecord(id, currentStatus, targetStatus, request.getAction(), request.getReason(), request.getOperator());
        auditService.record("HARDWARE_ASSET", id, AuditActionType.LIFECYCLE,
                "Lifecycle action " + request.getAction().name() + " from " + currentStatus.name() + " to " + targetStatus.name(),
                auditService.defaultOperator(request.getOperator()));
        return assetHardware;
    }

    /**
     * 批量导入硬件资产。
     */
    @Transactional(rollbackFor = Exception.class)
    public List<AssetHardware> batchImport(HardwareBatchImportRequest request) {
        return request.getItems().stream().map(this::create).collect(Collectors.toList());
    }

    /**
     * 导出硬件资产的 CSV 数据。
     */
    public String exportCsv() {
        List<AssetHardware> list = assetHardwareMapper.selectList(new LambdaQueryWrapper<AssetHardware>().orderByAsc(AssetHardware::getAssetCode));
        StringBuilder builder = new StringBuilder();
        builder.append("assetCode,assetName,hardwareCategory,hardwareStatus,managementIp,businessIp,cpuModel,cpuCores,memoryGb\n");
        for (AssetHardware item : list) {
            builder.append(safe(item.getAssetCode())).append(',')
                    .append(safe(item.getAssetName())).append(',')
                    .append(safe(item.getHardwareCategory())).append(',')
                    .append(safe(item.getHardwareStatus())).append(',')
                    .append(safe(item.getManagementIp())).append(',')
                    .append(safe(item.getBusinessIp())).append(',')
                    .append(safe(item.getCpuModel())).append(',')
                    .append(item.getCpuCores() == null ? "" : item.getCpuCores()).append(',')
                    .append(item.getMemoryGb() == null ? "" : item.getMemoryGb())
                    .append('\n');
        }
        return builder.toString();
    }

    /**
     * 删除指定主键对应的资源记录，同时级联清理子类型表、关联表和生命周期记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AssetHardware existing = getById(id);
        // 清理子类型扩展表
        serverMapper.delete(new LambdaQueryWrapper<AssetHardwareServer>().eq(AssetHardwareServer::getHardwareAssetId, id));
        queryTerminalMapper.delete(new LambdaQueryWrapper<AssetHardwareQueryTerminal>().eq(AssetHardwareQueryTerminal::getHardwareAssetId, id));
        ticketTerminalMapper.delete(new LambdaQueryWrapper<AssetHardwareTicketTerminal>().eq(AssetHardwareTicketTerminal::getHardwareAssetId, id));
        selfServiceTerminalMapper.delete(new LambdaQueryWrapper<AssetHardwareSelfServiceTerminal>().eq(AssetHardwareSelfServiceTerminal::getHardwareAssetId, id));
        // 清理关联关系表
        hardwareSystemRelMapper.delete(new LambdaQueryWrapper<AssetHardwareSystemRel>().eq(AssetHardwareSystemRel::getHardwareAssetId, id));
        hardwarePersonRelMapper.delete(new LambdaQueryWrapper<AssetHardwarePersonRel>().eq(AssetHardwarePersonRel::getHardwareAssetId, id));
        hardwareVendorRelMapper.delete(new LambdaQueryWrapper<AssetHardwareVendorRel>().eq(AssetHardwareVendorRel::getHardwareAssetId, id));
        // 清理生命周期记录
        assetLifecycleRecordMapper.delete(new LambdaQueryWrapper<AssetLifecycleRecord>().eq(AssetLifecycleRecord::getHardwareAssetId, id));
        // 删除主记录
        assetHardwareMapper.deleteById(id);
        auditService.record("HARDWARE_ASSET", id, AuditActionType.DELETE, "Deleted hardware asset " + existing.getAssetCode(), "SYSTEM");
    }

    private AssetHardware toAsset(HardwareAssetUpsertRequest request) {
        AssetHardware assetHardware = new AssetHardware();
        assetHardware.setAssetCode(request.getAssetCode());
        assetHardware.setAssetName(request.getAssetName());
        assetHardware.setHardwareCategory(request.getHardwareCategory().name());
        assetHardware.setLocationId(request.getLocationId());
        assetHardware.setDepartmentId(request.getDepartmentId());
        assetHardware.setManagementIp(request.getManagementIp());
        assetHardware.setBusinessIp(request.getBusinessIp());
        assetHardware.setCpuModel(request.getCpuModel());
        assetHardware.setCpuCores(request.getCpuCores());
        assetHardware.setMemoryGb(request.getMemoryGb());
        assetHardware.setEnabledDate(request.getEnabledDate());
        assetHardware.setRemark(request.getRemark());
        return assetHardware;
    }

    private Object findSubtypeDetail(AssetHardware assetHardware) {
        HardwareCategory category = HardwareCategory.valueOf(assetHardware.getHardwareCategory());
        if (category == HardwareCategory.SERVER) {
            return serverMapper.selectOne(new LambdaQueryWrapper<AssetHardwareServer>().eq(AssetHardwareServer::getHardwareAssetId, assetHardware.getId()));
        }
        if (category == HardwareCategory.QUERY_TERMINAL) {
            return queryTerminalMapper.selectOne(new LambdaQueryWrapper<AssetHardwareQueryTerminal>().eq(AssetHardwareQueryTerminal::getHardwareAssetId, assetHardware.getId()));
        }
        if (category == HardwareCategory.TICKET_TERMINAL) {
            return ticketTerminalMapper.selectOne(new LambdaQueryWrapper<AssetHardwareTicketTerminal>().eq(AssetHardwareTicketTerminal::getHardwareAssetId, assetHardware.getId()));
        }
        return selfServiceTerminalMapper.selectOne(
                new LambdaQueryWrapper<AssetHardwareSelfServiceTerminal>().eq(AssetHardwareSelfServiceTerminal::getHardwareAssetId, assetHardware.getId()));
    }

    private void upsertSubtype(Long hardwareAssetId, HardwareAssetUpsertRequest request) {
        HardwareCategory category = request.getHardwareCategory();
        if (category == HardwareCategory.SERVER) {
            serverMapper.delete(new LambdaQueryWrapper<AssetHardwareServer>().eq(AssetHardwareServer::getHardwareAssetId, hardwareAssetId));
            AssetHardwareServer detail = new AssetHardwareServer();
            detail.setHardwareAssetId(hardwareAssetId);
            detail.setOperatingSystem(request.getOperatingSystem());
            detail.setDiskGb(request.getDiskGb());
            detail.setVirtualization(request.getVirtualization());
            serverMapper.insert(detail);
            return;
        }
        if (category == HardwareCategory.QUERY_TERMINAL) {
            queryTerminalMapper.delete(new LambdaQueryWrapper<AssetHardwareQueryTerminal>().eq(AssetHardwareQueryTerminal::getHardwareAssetId, hardwareAssetId));
            AssetHardwareQueryTerminal detail = new AssetHardwareQueryTerminal();
            detail.setHardwareAssetId(hardwareAssetId);
            detail.setScreenSize(request.getScreenSize());
            detail.setTouchEnabled(Boolean.TRUE.equals(request.getTouchEnabled()) ? 1 : 0);
            detail.setDeviceModel(request.getDeviceModel());
            queryTerminalMapper.insert(detail);
            return;
        }
        if (category == HardwareCategory.TICKET_TERMINAL) {
            ticketTerminalMapper.delete(new LambdaQueryWrapper<AssetHardwareTicketTerminal>().eq(AssetHardwareTicketTerminal::getHardwareAssetId, hardwareAssetId));
            AssetHardwareTicketTerminal detail = new AssetHardwareTicketTerminal();
            detail.setHardwareAssetId(hardwareAssetId);
            detail.setPrinterModel(request.getPrinterModel());
            detail.setSupportQr(Boolean.TRUE.equals(request.getSupportQr()) ? 1 : 0);
            detail.setDeviceModel(request.getDeviceModel());
            ticketTerminalMapper.insert(detail);
            return;
        }
        selfServiceTerminalMapper.delete(new LambdaQueryWrapper<AssetHardwareSelfServiceTerminal>().eq(AssetHardwareSelfServiceTerminal::getHardwareAssetId, hardwareAssetId));
        AssetHardwareSelfServiceTerminal detail = new AssetHardwareSelfServiceTerminal();
        detail.setHardwareAssetId(hardwareAssetId);
        detail.setTerminalType(request.getTerminalType());
        detail.setScreenSize(request.getScreenSize());
        detail.setDeviceModel(request.getDeviceModel());
        selfServiceTerminalMapper.insert(detail);
    }

    private void createLifecycleRecord(Long hardwareAssetId, HardwareStatus fromStatus, HardwareStatus toStatus,
                                       LifecycleActionType actionType, String reason, String operator) {
        AssetLifecycleRecord record = new AssetLifecycleRecord();
        record.setHardwareAssetId(hardwareAssetId);
        record.setActionType(actionType.name());
        record.setFromStatus(fromStatus == null ? null : fromStatus.name());
        record.setToStatus(toStatus.name());
        record.setReason(reason);
        record.setOperator(auditService.defaultOperator(operator));
        record.setActionTime(LocalDateTime.now());
        assetLifecycleRecordMapper.insert(record);
    }

    private HardwareStatus transition(HardwareStatus currentStatus, LifecycleActionType actionType) {
        Map<LifecycleActionType, List<HardwareStatus>> allowed = new LinkedHashMap<LifecycleActionType, List<HardwareStatus>>();
        allowed.put(LifecycleActionType.IN_STOCK, Arrays.asList(HardwareStatus.REGISTERED));
        allowed.put(LifecycleActionType.ASSIGN, Arrays.asList(HardwareStatus.IN_STOCK, HardwareStatus.IDLE));
        allowed.put(LifecycleActionType.CHANGE, Arrays.asList(HardwareStatus.ASSIGNED));
        allowed.put(LifecycleActionType.IDLE, Arrays.asList(HardwareStatus.ASSIGNED, HardwareStatus.CHANGED));
        allowed.put(LifecycleActionType.MAINTAIN, Arrays.asList(HardwareStatus.ASSIGNED, HardwareStatus.IDLE, HardwareStatus.CHANGED));
        allowed.put(LifecycleActionType.OFFLINE, Arrays.asList(HardwareStatus.IDLE, HardwareStatus.MAINTAINING, HardwareStatus.CHANGED));
        allowed.put(LifecycleActionType.SCRAP, Arrays.asList(HardwareStatus.OFFLINE));

        if (actionType == LifecycleActionType.REGISTER) {
            throw new BusinessException("REGISTER action is only used during creation");
        }
        List<HardwareStatus> from = allowed.get(actionType);
        if (from == null || !from.contains(currentStatus)) {
            throw new BusinessException("Illegal lifecycle transition: " + currentStatus.name() + " -> " + actionType.name());
        }
        if (actionType == LifecycleActionType.IN_STOCK) {
            return HardwareStatus.IN_STOCK;
        }
        if (actionType == LifecycleActionType.ASSIGN) {
            return HardwareStatus.ASSIGNED;
        }
        if (actionType == LifecycleActionType.CHANGE) {
            return HardwareStatus.CHANGED;
        }
        if (actionType == LifecycleActionType.IDLE) {
            return HardwareStatus.IDLE;
        }
        if (actionType == LifecycleActionType.MAINTAIN) {
            return HardwareStatus.MAINTAINING;
        }
        if (actionType == LifecycleActionType.OFFLINE) {
            return HardwareStatus.OFFLINE;
        }
        return HardwareStatus.SCRAPPED;
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }
}





