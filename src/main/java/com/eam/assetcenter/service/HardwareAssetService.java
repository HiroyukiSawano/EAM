package com.eam.assetcenter.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eam.assetcenter.common.api.PageResponse;
import com.eam.assetcenter.common.enums.AuditActionType;
import com.eam.assetcenter.common.enums.HardwareStatus;
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
import com.eam.assetcenter.domain.entity.InformationSystem;
import com.eam.assetcenter.domain.entity.Person;
import com.eam.assetcenter.domain.entity.ProjectHardwareRel;
import com.eam.assetcenter.domain.entity.ProjectInfo;
import com.eam.assetcenter.domain.entity.ServiceProvider;
import com.eam.assetcenter.domain.entity.SystemVendorRel;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwarePersonRelMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareQueryTerminalMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareSelfServiceTerminalMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareServerMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareSystemRelMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareTicketTerminalMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetHardwareVendorRelMapper;
import com.eam.assetcenter.infrastructure.mapper.AssetLifecycleRecordMapper;
import com.eam.assetcenter.infrastructure.mapper.InformationSystemMapper;
import com.eam.assetcenter.infrastructure.mapper.PersonMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectHardwareRelMapper;
import com.eam.assetcenter.infrastructure.mapper.ProjectInfoMapper;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderMapper;
import com.eam.assetcenter.infrastructure.mapper.SystemVendorRelMapper;
import com.eam.assetcenter.web.request.HardwareAssetRelationRequest;
import com.eam.assetcenter.web.request.HardwareAssetUpsertRequest;
import com.eam.assetcenter.web.request.HardwareBatchImportRequest;
import com.eam.assetcenter.web.request.HardwareLifecycleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 硬件资产业务服务，负责新版硬件台账、统计与关联关系维护。
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
    private final InformationSystemMapper informationSystemMapper;
    private final SystemVendorRelMapper systemVendorRelMapper;
    private final PersonMapper personMapper;
    private final ProjectInfoMapper projectInfoMapper;
    private final ProjectHardwareRelMapper projectHardwareRelMapper;
    private final ServiceProviderMapper serviceProviderMapper;
    private final SupportService supportService;
    private final AuditService auditService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(HardwareAssetUpsertRequest request) {
        validateRequest(request, null);
        AssetHardware assetHardware = toEntity(request, null);
        assetHardwareMapper.insert(assetHardware);
        syncOwnerRelation(assetHardware, request.getOwnerPersonId(), request.getContactPhone(), true);
        auditService.record("HARDWARE_ASSET", assetHardware.getId(), AuditActionType.CREATE,
                "Created hardware asset " + assetHardware.getAssetCode(), "SYSTEM");
        return toHardwareView(assetHardwareMapper.selectById(assetHardware.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> update(Long id, HardwareAssetUpsertRequest request) {
        AssetHardware existing = getById(id);
        validateRequest(request, id);
        AssetHardware assetHardware = toEntity(request, existing);
        assetHardware.setId(id);
        assetHardwareMapper.updateById(assetHardware);
        syncOwnerRelation(assetHardwareMapper.selectById(id), request.getOwnerPersonId(), request.getContactPhone(), true);
        auditService.record("HARDWARE_ASSET", id, AuditActionType.UPDATE,
                "Updated hardware asset " + assetHardware.getAssetCode(), "SYSTEM");
        return toHardwareView(assetHardwareMapper.selectById(id));
    }

    public AssetHardware getById(Long id) {
        AssetHardware assetHardware = assetHardwareMapper.selectById(id);
        if (assetHardware == null) {
            throw new BusinessException("Hardware asset not found: " + id);
        }
        return assetHardware;
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("total", assetHardwareMapper.selectCount(Wrappers.<AssetHardware>lambdaQuery()));
        result.put("server", countByHardwareType("SERVER"));
        result.put("networkDevice", countByHardwareType("NETWORK_DEVICE"));
        result.put("terminalDevice", countByHardwareType("TERMINAL_DEVICE"));
        result.put("peripheral", countByHardwareType("PERIPHERAL"));
        return result;
    }

    public Map<String, Object> getDetail(Long id) {
        AssetHardware assetHardware = getById(id);
        List<Long> informationSystemIds = normalizeIds(hardwareSystemRelMapper.selectList(
                        Wrappers.<AssetHardwareSystemRel>lambdaQuery().eq(AssetHardwareSystemRel::getHardwareAssetId, id))
                .stream().map(AssetHardwareSystemRel::getInformationSystemId).collect(Collectors.toList()));
        List<Long> serviceProviderIds = normalizeIds(hardwareVendorRelMapper.selectList(
                        Wrappers.<AssetHardwareVendorRel>lambdaQuery().eq(AssetHardwareVendorRel::getHardwareAssetId, id))
                .stream().map(AssetHardwareVendorRel::getServiceProviderId).collect(Collectors.toList()));
        List<Long> projectIds = normalizeIds(projectHardwareRelMapper.selectList(
                        Wrappers.<ProjectHardwareRel>lambdaQuery().eq(ProjectHardwareRel::getHardwareAssetId, id))
                .stream().map(ProjectHardwareRel::getProjectId).collect(Collectors.toList()));
        List<AssetHardwarePersonRel> personRelations = hardwarePersonRelMapper.selectList(
                Wrappers.<AssetHardwarePersonRel>lambdaQuery()
                        .eq(AssetHardwarePersonRel::getHardwareAssetId, id)
                        .orderByAsc(AssetHardwarePersonRel::getRelationType)
                        .orderByAsc(AssetHardwarePersonRel::getId));

        List<Long> ownerIds = normalizeIds(personRelations.stream()
                .filter(item -> PersonRelationType.RESPONSIBLE.name().equals(item.getRelationType()))
                .map(AssetHardwarePersonRel::getPersonId)
                .collect(Collectors.toList()));
        List<Long> personIds = normalizeIds(personRelations.stream()
                .filter(item -> PersonRelationType.USER.name().equals(item.getRelationType()))
                .map(AssetHardwarePersonRel::getPersonId)
                .collect(Collectors.toList()));

        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("hardwareAsset", toHardwareView(assetHardware));
        detail.put("personIds", personIds);
        detail.put("ownerIds", ownerIds);
        detail.put("informationSystemIds", informationSystemIds);
        detail.put("projectIds", projectIds);
        detail.put("serviceProviderIds", serviceProviderIds);
        detail.put("vendorIds", serviceProviderIds);
        detail.put("persons", buildPersonSummaries(personRelations));
        detail.put("softwareAssets", buildInformationSystemSummaries(informationSystemIds));
        detail.put("projects", buildProjectSummaries(projectIds));
        detail.put("serviceProviders", buildServiceProviderSummaries(serviceProviderIds));
        detail.put("subtypeDetail", null);
        detail.put("lifecycleRecords", assetLifecycleRecordMapper.selectList(
                Wrappers.<AssetLifecycleRecord>lambdaQuery()
                        .eq(AssetLifecycleRecord::getHardwareAssetId, id)
                        .orderByDesc(AssetLifecycleRecord::getActionTime)));
        return detail;
    }

    public PageResponse<Map<String, Object>> page(int pageNo, int pageSize, String keyword, String hardwareType,
                                                  String hardwareCategory, String hardwareStatus, Long locationId) {
        String resolvedType = StringUtils.hasText(hardwareType) ? hardwareType : hardwareCategory;
        supportService.ensureHardwareTypeValid(resolvedType);
        supportService.ensureHardwareStatusValid(hardwareStatus);

        LambdaQueryWrapper<AssetHardware> wrapper = new LambdaQueryWrapper<AssetHardware>()
                .and(StringUtils.hasText(keyword), query -> query
                        .like(AssetHardware::getAssetCode, keyword)
                        .or().like(AssetHardware::getAssetName, keyword)
                        .or().like(AssetHardware::getHardwareIp, keyword)
                        .or().like(AssetHardware::getHardwareBrand, keyword))
                .eq(StringUtils.hasText(resolvedType), AssetHardware::getHardwareType, resolvedType)
                .eq(StringUtils.hasText(hardwareStatus), AssetHardware::getHardwareStatus, hardwareStatus)
                .eq(locationId != null, AssetHardware::getLocationId, locationId)
                .orderByAsc(AssetHardware::getAssetCode);

        Page<AssetHardware> page = assetHardwareMapper.selectPage(new Page<AssetHardware>(pageNo, pageSize), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream().map(this::toHardwareView).collect(Collectors.toList());
        return new PageResponse<Map<String, Object>>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    public List<AssetHardware> options() {
        return assetHardwareMapper.selectList(new LambdaQueryWrapper<AssetHardware>().orderByAsc(AssetHardware::getAssetCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncRelations(Long id, HardwareAssetRelationRequest request) {
        AssetHardware assetHardware = getById(id);
        syncPersonRelations(id, validatePersonIds(request.getPersonIds()));
        syncSystems(id, validateInformationSystemIds(request.getInformationSystemIds()));
        syncProjects(id, validateProjectIds(request.getProjectIds()));
        syncVendors(id, validateServiceProviderIds(request.getServiceProviderIds()));
        syncOwnerRelation(assetHardware, assetHardware.getOwnerPersonId(), assetHardware.getContactPhone(), false);
        auditService.record("HARDWARE_ASSET", id, AuditActionType.RELATION_SYNC,
                "Synchronized hardware relations", "SYSTEM");
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncSystems(Long id, List<Long> systemIds) {
        getById(id);
        List<Long> safeIds = validateInformationSystemIds(systemIds);
        hardwareSystemRelMapper.delete(Wrappers.<AssetHardwareSystemRel>lambdaQuery().eq(AssetHardwareSystemRel::getHardwareAssetId, id));
        for (Long systemId : safeIds) {
            AssetHardwareSystemRel relation = new AssetHardwareSystemRel();
            relation.setHardwareAssetId(id);
            relation.setInformationSystemId(systemId);
            hardwareSystemRelMapper.insert(relation);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncOwners(Long id, List<Long> ownerIds) {
        AssetHardware assetHardware = getById(id);
        List<Long> safeIds = normalizeIds(ownerIds);
        if (safeIds.size() > 1) {
            throw new BusinessException("Only one responsible owner is allowed");
        }
        Long ownerPersonId = safeIds.isEmpty() ? null : safeIds.get(0);
        if (ownerPersonId != null) {
            supportService.ensurePersonExists(ownerPersonId);
        }
        syncOwnerRelation(assetHardware, ownerPersonId, null, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncVendors(Long id, List<Long> vendorIds) {
        getById(id);
        List<Long> safeIds = validateServiceProviderIds(vendorIds);
        hardwareVendorRelMapper.delete(Wrappers.<AssetHardwareVendorRel>lambdaQuery().eq(AssetHardwareVendorRel::getHardwareAssetId, id));
        for (Long vendorId : safeIds) {
            AssetHardwareVendorRel relation = new AssetHardwareVendorRel();
            relation.setHardwareAssetId(id);
            relation.setServiceProviderId(vendorId);
            hardwareVendorRelMapper.insert(relation);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeLifecycle(Long id, HardwareLifecycleRequest request) {
        getById(id);
        throw new BusinessException("硬件新版正式入口不支持生命周期操作");
    }

    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> batchImport(HardwareBatchImportRequest request) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        List<HardwareAssetUpsertRequest> items = request == null ? Collections.<HardwareAssetUpsertRequest>emptyList() : request.getItems();
        if (items == null) {
            return result;
        }
        for (HardwareAssetUpsertRequest item : items) {
            result.add(create(item));
        }
        return result;
    }

    public String exportCsv() {
        List<AssetHardware> list = assetHardwareMapper.selectList(new LambdaQueryWrapper<AssetHardware>().orderByAsc(AssetHardware::getAssetCode));
        Map<Long, String> ownerNameMap = loadPersonNameMap(list.stream().map(AssetHardware::getOwnerPersonId).collect(Collectors.toList()));
        StringBuilder builder = new StringBuilder();
        builder.append("assetCode,hardwareIp,assetName,hardwareBrand,hardwareType,ownerName,hardwareStatus\n");
        for (AssetHardware item : list) {
            builder.append(safe(item.getAssetCode())).append(',')
                    .append(safe(item.getHardwareIp())).append(',')
                    .append(safe(item.getAssetName())).append(',')
                    .append(safe(item.getHardwareBrand())).append(',')
                    .append(safe(item.getHardwareType())).append(',')
                    .append(safe(ownerNameMap.get(item.getOwnerPersonId()))).append(',')
                    .append(safe(item.getHardwareStatus()))
                    .append('\n');
        }
        return builder.toString();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AssetHardware existing = getById(id);
        serverMapper.delete(Wrappers.<AssetHardwareServer>lambdaQuery().eq(AssetHardwareServer::getHardwareAssetId, id));
        queryTerminalMapper.delete(Wrappers.<AssetHardwareQueryTerminal>lambdaQuery().eq(AssetHardwareQueryTerminal::getHardwareAssetId, id));
        ticketTerminalMapper.delete(Wrappers.<AssetHardwareTicketTerminal>lambdaQuery().eq(AssetHardwareTicketTerminal::getHardwareAssetId, id));
        selfServiceTerminalMapper.delete(Wrappers.<AssetHardwareSelfServiceTerminal>lambdaQuery().eq(AssetHardwareSelfServiceTerminal::getHardwareAssetId, id));
        hardwareSystemRelMapper.delete(Wrappers.<AssetHardwareSystemRel>lambdaQuery().eq(AssetHardwareSystemRel::getHardwareAssetId, id));
        hardwarePersonRelMapper.delete(Wrappers.<AssetHardwarePersonRel>lambdaQuery().eq(AssetHardwarePersonRel::getHardwareAssetId, id));
        hardwareVendorRelMapper.delete(Wrappers.<AssetHardwareVendorRel>lambdaQuery().eq(AssetHardwareVendorRel::getHardwareAssetId, id));
        projectHardwareRelMapper.delete(Wrappers.<ProjectHardwareRel>lambdaQuery().eq(ProjectHardwareRel::getHardwareAssetId, id));
        assetLifecycleRecordMapper.delete(Wrappers.<AssetLifecycleRecord>lambdaQuery().eq(AssetLifecycleRecord::getHardwareAssetId, id));
        assetHardwareMapper.deleteById(id);
        auditService.record("HARDWARE_ASSET", id, AuditActionType.DELETE,
                "Deleted hardware asset " + existing.getAssetCode(), "SYSTEM");
    }

    private void validateRequest(HardwareAssetUpsertRequest request, Long excludeId) {
        supportService.ensureUniqueAssetCode(request.getAssetCode(), excludeId);
        supportService.ensureLocationExists(request.getLocationId());
        supportService.ensureHardwareTypeValid(request.getHardwareType());
        supportService.ensureHardwareStatusValid(request.getHardwareStatus());
        supportService.ensurePersonExists(request.getOwnerPersonId());
    }

    private AssetHardware toEntity(HardwareAssetUpsertRequest request, AssetHardware existing) {
        AssetHardware assetHardware = new AssetHardware();
        assetHardware.setAssetCode(request.getAssetCode());
        assetHardware.setAssetName(request.getAssetName());
        assetHardware.setHardwareIp(firstNonBlank(request.getHardwareIp(), request.getManagementIp()));
        assetHardware.setHardwareModel(request.getHardwareModel());
        assetHardware.setHardwareBrand(request.getHardwareBrand());
        assetHardware.setHardwareType(request.getHardwareType());
        assetHardware.setHardwareCategory(request.getHardwareType());
        assetHardware.setPhysicalLocation(request.getPhysicalLocation());
        assetHardware.setLocationId(request.getLocationId());
        assetHardware.setNetworkEnvironment(request.getNetworkEnvironment());
        assetHardware.setOperatingSystem(request.getOperatingSystem());
        assetHardware.setHardwareStatus(StringUtils.hasText(request.getHardwareStatus())
                ? request.getHardwareStatus()
                : existing == null ? HardwareStatus.RUNNING.name() : existing.getHardwareStatus());
        assetHardware.setPurchaseDate(request.getPurchaseDate());
        assetHardware.setOwnerPersonId(request.getOwnerPersonId());
        assetHardware.setContactPhone(request.getContactPhone());
        assetHardware.setRemark(request.getRemark());

        assetHardware.setManagementIp(firstNonBlank(request.getManagementIp(), request.getHardwareIp()));
        assetHardware.setBusinessIp(request.getBusinessIp());
        assetHardware.setCpuModel(request.getCpuModel());
        assetHardware.setCpuCores(request.getCpuCores());
        assetHardware.setMemoryGb(request.getMemoryGb());
        assetHardware.setEnabledDate(request.getEnabledDate() != null ? request.getEnabledDate() : request.getPurchaseDate());
        assetHardware.setDepartmentId(existing == null ? null : existing.getDepartmentId());
        return assetHardware;
    }

    private Map<String, Object> toHardwareView(AssetHardware assetHardware) {
        Map<Long, String> ownerNameMap = loadPersonNameMap(Collections.singletonList(assetHardware.getOwnerPersonId()));
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("id", assetHardware.getId());
        view.put("assetCode", assetHardware.getAssetCode());
        view.put("assetName", assetHardware.getAssetName());
        view.put("hardwareIp", assetHardware.getHardwareIp());
        view.put("hardwareModel", assetHardware.getHardwareModel());
        view.put("hardwareBrand", assetHardware.getHardwareBrand());
        view.put("hardwareType", assetHardware.getHardwareType());
        view.put("hardwareCategory", assetHardware.getHardwareCategory());
        view.put("physicalLocation", assetHardware.getPhysicalLocation());
        view.put("locationId", assetHardware.getLocationId());
        view.put("networkEnvironment", assetHardware.getNetworkEnvironment());
        view.put("operatingSystem", assetHardware.getOperatingSystem());
        view.put("hardwareStatus", assetHardware.getHardwareStatus());
        view.put("purchaseDate", assetHardware.getPurchaseDate());
        view.put("ownerPersonId", assetHardware.getOwnerPersonId());
        view.put("ownerName", ownerNameMap.get(assetHardware.getOwnerPersonId()));
        view.put("contactPhone", assetHardware.getContactPhone());
        view.put("remark", assetHardware.getRemark());
        view.put("managementIp", assetHardware.getManagementIp());
        view.put("businessIp", assetHardware.getBusinessIp());
        view.put("cpuModel", assetHardware.getCpuModel());
        view.put("cpuCores", assetHardware.getCpuCores());
        view.put("memoryGb", assetHardware.getMemoryGb());
        view.put("enabledDate", assetHardware.getEnabledDate());
        view.put("createdAt", assetHardware.getCreatedAt());
        view.put("updatedAt", assetHardware.getUpdatedAt());
        return view;
    }

    private void syncOwnerRelation(AssetHardware assetHardware, Long ownerPersonId, String contactPhone, boolean updateEntity) {
        if (ownerPersonId != null) {
            supportService.ensurePersonExists(ownerPersonId);
        }
        if (updateEntity) {
            assetHardware.setOwnerPersonId(ownerPersonId);
            if (StringUtils.hasText(contactPhone)) {
                assetHardware.setContactPhone(contactPhone);
            } else if (ownerPersonId != null) {
                Person owner = personMapper.selectById(ownerPersonId);
                assetHardware.setContactPhone(owner == null ? assetHardware.getContactPhone() : owner.getMobile());
            }
            assetHardwareMapper.updateById(assetHardware);
        }
        hardwarePersonRelMapper.delete(Wrappers.<AssetHardwarePersonRel>lambdaQuery()
                .eq(AssetHardwarePersonRel::getHardwareAssetId, assetHardware.getId())
                .eq(AssetHardwarePersonRel::getRelationType, PersonRelationType.RESPONSIBLE.name()));
        if (ownerPersonId == null) {
            return;
        }
        AssetHardwarePersonRel relation = new AssetHardwarePersonRel();
        relation.setHardwareAssetId(assetHardware.getId());
        relation.setPersonId(ownerPersonId);
        relation.setRelationType(PersonRelationType.RESPONSIBLE.name());
        hardwarePersonRelMapper.insert(relation);
    }

    private void syncPersonRelations(Long hardwareAssetId, List<Long> personIds) {
        hardwarePersonRelMapper.delete(Wrappers.<AssetHardwarePersonRel>lambdaQuery()
                .eq(AssetHardwarePersonRel::getHardwareAssetId, hardwareAssetId)
                .eq(AssetHardwarePersonRel::getRelationType, PersonRelationType.USER.name()));
        for (Long personId : personIds) {
            AssetHardwarePersonRel relation = new AssetHardwarePersonRel();
            relation.setHardwareAssetId(hardwareAssetId);
            relation.setPersonId(personId);
            relation.setRelationType(PersonRelationType.USER.name());
            hardwarePersonRelMapper.insert(relation);
        }
    }

    private void syncProjects(Long hardwareAssetId, List<Long> projectIds) {
        projectHardwareRelMapper.delete(Wrappers.<ProjectHardwareRel>lambdaQuery().eq(ProjectHardwareRel::getHardwareAssetId, hardwareAssetId));
        for (Long projectId : projectIds) {
            ProjectHardwareRel relation = new ProjectHardwareRel();
            relation.setHardwareAssetId(hardwareAssetId);
            relation.setProjectId(projectId);
            projectHardwareRelMapper.insert(relation);
        }
    }

    private List<Map<String, Object>> buildPersonSummaries(List<AssetHardwarePersonRel> relations) {
        List<Long> personIds = relations.stream().map(AssetHardwarePersonRel::getPersonId).collect(Collectors.toList());
        Map<Long, Person> personMap = personMapper.selectList(Wrappers.<Person>lambdaQuery().in(Person::getId, normalizeIds(personIds)))
                .stream()
                .collect(Collectors.toMap(Person::getId, item -> item));
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (AssetHardwarePersonRel relation : relations) {
            Person person = personMap.get(relation.getPersonId());
            if (person == null) {
                continue;
            }
            Map<String, Object> summary = new LinkedHashMap<String, Object>();
            summary.put("id", person.getId());
            summary.put("name", person.getName());
            summary.put("employeeNo", person.getEmployeeNo());
            summary.put("mobile", person.getMobile());
            summary.put("relationType", relation.getRelationType());
            summary.put("relationLabel", PersonRelationType.RESPONSIBLE.name().equals(relation.getRelationType()) ? "设备负责人" : "关联人员");
            result.add(summary);
        }
        return result;
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
        Map<Long, Long> vendorIdMap = systemVendorRelMapper.selectList(
                        Wrappers.<SystemVendorRel>lambdaQuery().in(SystemVendorRel::getInformationSystemId, normalizedIds))
                .stream()
                .collect(Collectors.toMap(SystemVendorRel::getInformationSystemId, SystemVendorRel::getServiceProviderId, (left, right) -> left));
        Map<Long, String> vendorNameMap = loadServiceProviderNameMap(vendorIdMap.values().stream().collect(Collectors.toList()));
        Map<Long, String> ownerNameMap = loadPersonNameMap(systemMap.values().stream()
                .map(InformationSystem::getOwnerPersonId)
                .collect(Collectors.toList()));
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Long id : normalizedIds) {
            InformationSystem item = systemMap.get(id);
            if (item == null) {
                continue;
            }
            Map<String, Object> summary = new LinkedHashMap<String, Object>();
            summary.put("id", item.getId());
            summary.put("code", item.getCode());
            summary.put("name", item.getName());
            summary.put("systemType", item.getSystemType());
            summary.put("serviceProviderName", vendorNameMap.get(vendorIdMap.get(item.getId())));
            summary.put("ownerName", ownerNameMap.get(item.getOwnerPersonId()));
            result.add(summary);
        }
        return result;
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
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Long id : normalizedIds) {
            ProjectInfo item = projectMap.get(id);
            if (item == null) {
                continue;
            }
            Map<String, Object> summary = new LinkedHashMap<String, Object>();
            summary.put("id", item.getId());
            summary.put("code", item.getCode());
            summary.put("name", item.getName());
            summary.put("projectType", item.getProjectType());
            summary.put("projectStatus", item.getProjectStatus());
            summary.put("ownerName", item.getOwnerName());
            result.add(summary);
        }
        return result;
    }

    private List<Map<String, Object>> buildServiceProviderSummaries(List<Long> serviceProviderIds) {
        List<Long> normalizedIds = normalizeIds(serviceProviderIds);
        if (normalizedIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ServiceProvider> providerMap = serviceProviderMapper.selectList(
                        Wrappers.<ServiceProvider>lambdaQuery().in(ServiceProvider::getId, normalizedIds))
                .stream()
                .collect(Collectors.toMap(ServiceProvider::getId, item -> item));
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Long id : normalizedIds) {
            ServiceProvider item = providerMap.get(id);
            if (item == null) {
                continue;
            }
            Map<String, Object> summary = new LinkedHashMap<String, Object>();
            summary.put("id", item.getId());
            summary.put("code", item.getCode());
            summary.put("name", item.getName());
            summary.put("unifiedSocialCreditCode", item.getUnifiedSocialCreditCode());
            summary.put("businessContact", item.getBusinessContact());
            summary.put("businessPhone", item.getBusinessPhone());
            result.add(summary);
        }
        return result;
    }

    private Long countByHardwareType(String hardwareType) {
        return assetHardwareMapper.selectCount(Wrappers.<AssetHardware>lambdaQuery().eq(AssetHardware::getHardwareType, hardwareType));
    }

    private List<Long> validatePersonIds(List<Long> personIds) {
        List<Long> normalizedIds = normalizeIds(personIds);
        for (Long personId : normalizedIds) {
            supportService.ensurePersonExists(personId);
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

    private List<Long> validateProjectIds(List<Long> projectIds) {
        List<Long> normalizedIds = normalizeIds(projectIds);
        for (Long projectId : normalizedIds) {
            supportService.ensureProjectExists(projectId);
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

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream().filter(item -> item != null).distinct().collect(Collectors.toList());
    }

    private Map<Long, String> loadPersonNameMap(List<Long> personIds) {
        List<Long> normalizedIds = normalizeIds(personIds);
        if (normalizedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return personMapper.selectList(Wrappers.<Person>lambdaQuery().in(Person::getId, normalizedIds))
                .stream()
                .collect(Collectors.toMap(Person::getId, Person::getName));
    }

    private Map<Long, String> loadServiceProviderNameMap(List<Long> serviceProviderIds) {
        List<Long> normalizedIds = normalizeIds(serviceProviderIds);
        if (normalizedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return serviceProviderMapper.selectList(Wrappers.<ServiceProvider>lambdaQuery().in(ServiceProvider::getId, normalizedIds))
                .stream()
                .collect(Collectors.toMap(ServiceProvider::getId, ServiceProvider::getName));
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(",", " ");
    }

    private String firstNonBlank(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }
}
