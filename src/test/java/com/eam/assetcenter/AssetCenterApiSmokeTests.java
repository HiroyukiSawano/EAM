package com.eam.assetcenter;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.eam.assetcenter.domain.entity.ServiceProviderPersonRel;
import com.eam.assetcenter.infrastructure.mapper.ServiceProviderPersonRelMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 资产中心核心接口冒烟测试，覆盖组织、项目、软件、硬件的关键闭环。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AssetCenterApiSmokeTests {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServiceProviderPersonRelMapper serviceProviderPersonRelMapper;

    @Test
    void organizationSupportOptionsShouldWork() throws Exception {
        String suffix = uniqueSuffix("ORG");
        long departmentId = createDepartment(suffix);
        long locationId = createLocation(suffix);
        long personId = createPerson(suffix, departmentId);
        long serviceProviderId = createServiceProvider(suffix);

        assertTrue(arrayContainsId(getDataNode(performGet("/api/v1/departments/options")), departmentId));
        assertTrue(arrayContainsId(getDataNode(performGet("/api/v1/locations/options")), locationId));
        assertTrue(arrayContainsId(getDataNode(performGet("/api/v1/persons/options")), personId));
        assertTrue(arrayContainsId(getDataNode(performGet("/api/v1/service-providers/options")), serviceProviderId));
    }

    @Test
    void personOptionsShouldSupportInactiveCodeValues() throws Exception {
        String suffix = uniqueSuffix("PERSON-OPT");
        long departmentId = createDepartment(suffix);
        long personId = createPerson(suffix, departmentId, "INACTIVE");

        JsonNode personOptions = getDataNode(performGet("/api/v1/persons/options"));
        assertTrue(arrayContainsId(personOptions, personId));
    }

    @Test
    void informationSystemAndServiceProviderOptionsShouldSupportInactiveCodeValues() throws Exception {
        String suffix = uniqueSuffix("OPTION-STATUS");
        long informationSystemId = createInformationSystem(suffix, "INACTIVE");
        long serviceProviderId = createServiceProvider(suffix, "INACTIVE");

        JsonNode informationSystemOptions = getDataNode(performGet("/api/v1/information-systems/options"));
        JsonNode serviceProviderOptions = getDataNode(performGet("/api/v1/service-providers/options"));
        assertTrue(arrayContainsId(informationSystemOptions, informationSystemId));
        assertTrue(arrayContainsId(serviceProviderOptions, serviceProviderId));
    }

    @Test
    void statusDictionaryShouldReturnAllGroups() throws Exception {
        JsonNode dictionaries = getDataNode(performGet("/api/v1/dictionaries/statuses"));

        assertEquals("正常", findDictionaryLabel(dictionaries.path("departmentStatus"), "ACTIVE"));
        assertEquals("停用", findDictionaryLabel(dictionaries.path("personStatus"), "INACTIVE"));
        assertEquals("规划中", findDictionaryLabel(dictionaries.path("projectStatus"), "PLANNING"));
        assertEquals("已登记", findDictionaryLabel(dictionaries.path("hardwareStatus"), "REGISTERED"));
        assertTrue(dictionaries.path("serviceProviderStatus").isArray());
        assertTrue(dictionaries.path("informationSystemStatus").isArray());
    }

    @Test
    void invalidStatusValuesShouldBeRejected() throws Exception {
        String suffix = uniqueSuffix("INVALID");
        long departmentId = createDepartment(suffix);
        long locationId = createLocation(suffix);

        JsonNode departmentRoot = readRoot(performPostExpectFailure("/api/v1/departments", createDepartmentPayload(suffix + "-BAD", "INVALID")));
        assertTrue(departmentRoot.path("message").asText().contains("部门状态不合法"));

        JsonNode personRoot = readRoot(performPostExpectFailure("/api/v1/persons", createPersonPayload(suffix, departmentId, "INVALID")));
        assertTrue(personRoot.path("message").asText().contains("人员状态不合法"));

        JsonNode serviceProviderRoot = readRoot(performPostExpectFailure("/api/v1/service-providers", createServiceProviderPayload(suffix, "INVALID")));
        assertTrue(serviceProviderRoot.path("message").asText().contains("服务商状态不合法"));

        JsonNode informationSystemRoot = readRoot(performPostExpectFailure("/api/v1/information-systems", createInformationSystemPayload(suffix, "INVALID")));
        assertTrue(informationSystemRoot.path("message").asText().contains("信息系统状态不合法"));

        JsonNode projectRoot = readRoot(performPostExpectFailure("/api/v1/projects", createProjectPayload(suffix, "INVALID")));
        assertTrue(projectRoot.path("message").asText().contains("项目状态不合法"));

        JsonNode hardwareRoot = readRoot(performGetExpectFailure("/api/v1/hardware-assets?pageNo=1&pageSize=10&hardwareStatus=INVALID"));
        assertTrue(hardwareRoot.path("message").asText().contains("硬件状态不合法"));

        JsonNode createdHardware = getDataNode(performPost("/api/v1/hardware-assets", createHardwareAssetPayload(suffix, departmentId, locationId)));
        assertTrue(asLong(createdHardware.path("id")) > 0);
    }

    @Test
    void projectAndInformationSystemRelationsShouldBeClosedLoop() throws Exception {
        String suffix = uniqueSuffix("PRJ");
        long departmentId = createDepartment(suffix);
        long personId = createPerson(suffix, departmentId);
        long serviceProviderId = createServiceProvider(suffix);
        long informationSystemId = createInformationSystem(suffix);
        long projectId = createProject(suffix);

        Map<String, Object> projectRelations = new LinkedHashMap<String, Object>();
        projectRelations.put("informationSystemIds", new long[]{informationSystemId});
        projectRelations.put("serviceProviderIds", new long[]{serviceProviderId});
        projectRelations.put("personIds", new long[]{personId});
        projectRelations.put("hardwareAssetIds", new long[0]);
        getDataNode(performPut("/api/v1/projects/" + projectId + "/relations", projectRelations));

        JsonNode projectDetail = getDataNode(performGet("/api/v1/projects/" + projectId));
        assertEquals(projectId, asLong(projectDetail.path("project").path("id")));
        assertTrue(arrayContainsId(projectDetail.path("informationSystemIds"), informationSystemId));
        assertTrue(arrayContainsId(projectDetail.path("serviceProviderIds"), serviceProviderId));
        assertTrue(arrayContainsId(projectDetail.path("personIds"), personId));

        Map<String, Object> systemRelations = new LinkedHashMap<String, Object>();
        systemRelations.put("serviceProviderIds", new long[]{serviceProviderId});
        systemRelations.put("personIds", new long[]{personId});
        systemRelations.put("projectIds", new long[]{projectId});
        getDataNode(performPut("/api/v1/information-systems/" + informationSystemId + "/relations", systemRelations));

        JsonNode systemDetail = getDataNode(performGet("/api/v1/information-systems/" + informationSystemId));
        assertEquals(informationSystemId, asLong(systemDetail.path("informationSystem").path("id")));
        assertTrue(arrayContainsId(systemDetail.path("serviceProviderIds"), serviceProviderId));
        assertTrue(arrayContainsId(systemDetail.path("personIds"), personId));
        assertTrue(arrayContainsId(systemDetail.path("projectIds"), projectId));

        JsonNode projectPage = getDataNode(performGet("/api/v1/projects?pageNo=1&pageSize=10&keyword=PRJ-" + suffix));
        JsonNode systemPage = getDataNode(performGet("/api/v1/information-systems?pageNo=1&pageSize=10&keyword=SYS-" + suffix));
        assertEquals(projectId, asLong(projectPage.path("records").path(0).path("id")));
        assertEquals(informationSystemId, asLong(systemPage.path("records").path(0).path("id")));
    }

    @Test
    void hardwareOptionsAndRelationsShouldBeClosedLoop() throws Exception {
        String suffix = uniqueSuffix("HW");
        long departmentId = createDepartment(suffix);
        long locationId = createLocation(suffix);
        long personId = createPerson(suffix, departmentId);
        long serviceProviderId = createServiceProvider(suffix);
        long informationSystemId = createInformationSystem(suffix);
        long hardwareId = createHardwareAsset(suffix, departmentId, locationId);

        assertTrue(arrayContainsId(getDataNode(performGet("/api/v1/hardware-assets/options")), hardwareId));

        getDataNode(performPut("/api/v1/hardware-assets/" + hardwareId + "/systems", idsPayload(informationSystemId)));
        getDataNode(performPut("/api/v1/hardware-assets/" + hardwareId + "/owners", idsPayload(personId)));
        getDataNode(performPut("/api/v1/hardware-assets/" + hardwareId + "/vendors", idsPayload(serviceProviderId)));

        JsonNode hardwareDetail = getDataNode(performGet("/api/v1/hardware-assets/" + hardwareId));
        assertEquals(hardwareId, asLong(hardwareDetail.path("hardwareAsset").path("id")));
        assertTrue(arrayContainsId(hardwareDetail.path("informationSystemIds"), informationSystemId));
        assertTrue(arrayContainsId(hardwareDetail.path("ownerIds"), personId));
        assertTrue(arrayContainsId(hardwareDetail.path("vendorIds"), serviceProviderId));
    }

    @Test
    void organizationRelationSyncShouldWork() throws Exception {
        String suffix = uniqueSuffix("ORGREL");
        long departmentId = createDepartment(suffix);
        long locationId = createLocation(suffix);
        long personId = createPerson(suffix, departmentId);
        long serviceProviderId = createServiceProvider(suffix);
        long informationSystemId = createInformationSystem(suffix);
        long hardwareId = createHardwareAsset(suffix, departmentId, locationId);

        Map<String, Object> providerRelations = new LinkedHashMap<String, Object>();
        providerRelations.put("hardwareAssetIds", new long[]{hardwareId});
        providerRelations.put("informationSystemIds", new long[]{informationSystemId});
        providerRelations.put("personIds", new long[]{personId});
        getDataNode(performPut("/api/v1/service-providers/" + serviceProviderId + "/relations", providerRelations));

        JsonNode providerDetail = getDataNode(performGet("/api/v1/service-providers/" + serviceProviderId));
        assertTrue(arrayContainsId(providerDetail.path("hardwareAssetIds"), hardwareId));
        assertTrue(arrayContainsId(providerDetail.path("informationSystemIds"), informationSystemId));
        assertTrue(arrayContainsId(providerDetail.path("personIds"), personId));

        Map<String, Object> personRelations = new LinkedHashMap<String, Object>();
        personRelations.put("hardwareAssetIds", new long[]{hardwareId});
        personRelations.put("informationSystemIds", new long[]{informationSystemId});
        getDataNode(performPut("/api/v1/persons/" + personId + "/relations", personRelations));

        JsonNode personDetail = getDataNode(performGet("/api/v1/persons/" + personId));
        assertTrue(arrayContainsId(personDetail.path("hardwareAssetIds"), hardwareId));
        assertTrue(arrayContainsId(personDetail.path("informationSystemIds"), informationSystemId));
    }

    @Test
    void personShouldRejectRelatedServiceProvidersAcrossEntryPoints() throws Exception {
        String suffix = uniqueSuffix("PERSON-SP");
        long departmentId = createDepartment(suffix);
        long primaryServiceProviderId = createServiceProvider(suffix + "-PRIMARY");
        long relatedServiceProviderId = createServiceProvider(suffix + "-RELATED");

        Map<String, Object> createPayload = createPersonPayload(suffix, departmentId, "ACTIVE");
        createPayload.put("serviceProviderId", primaryServiceProviderId);
        createPayload.put("personType", "OPS");
        createPayload.put("relatedServiceProviderIds", new long[]{relatedServiceProviderId});
        JsonNode createRoot = readRoot(performPostExpectFailure("/api/v1/persons", createPayload));
        assertTrue(createRoot.path("message").asText().contains("只能归属一个服务商"));

        Map<String, Object> validPayload = createPersonPayload(suffix + "-VALID", departmentId, "ACTIVE");
        validPayload.put("serviceProviderId", primaryServiceProviderId);
        validPayload.put("personType", "OPS");
        long personId = asLong(getDataNode(performPost("/api/v1/persons", validPayload)).path("id"));

        Map<String, Object> updatePayload = createPersonPayload(suffix + "-EDIT", departmentId, "ACTIVE");
        updatePayload.put("serviceProviderId", primaryServiceProviderId);
        updatePayload.put("personType", "DEV");
        updatePayload.put("relatedServiceProviderIds", new long[]{relatedServiceProviderId});
        JsonNode updateRoot = readRoot(performPutExpectFailure("/api/v1/persons/" + personId, updatePayload));
        assertTrue(updateRoot.path("message").asText().contains("只能归属一个服务商"));

        Map<String, Object> relationPayload = new LinkedHashMap<String, Object>();
        relationPayload.put("hardwareAssetIds", new long[0]);
        relationPayload.put("informationSystemIds", new long[0]);
        relationPayload.put("projectIds", new long[0]);
        relationPayload.put("relatedServiceProviderIds", new long[]{relatedServiceProviderId});
        JsonNode relationRoot = readRoot(performPutExpectFailure("/api/v1/persons/" + personId + "/relations", relationPayload));
        assertTrue(relationRoot.path("message").asText().contains("只能归属一个服务商"));
    }

    @Test
    void personSaveShouldClearLegacyRelatedServiceProviderRecords() throws Exception {
        String suffix = uniqueSuffix("PERSON-CLEAN");
        long departmentId = createDepartment(suffix);
        long primaryServiceProviderId = createServiceProvider(suffix + "-PRIMARY");
        long legacyRelatedServiceProviderId = createServiceProvider(suffix + "-LEGACY");

        Map<String, Object> createPayload = createPersonPayload(suffix, departmentId, "ACTIVE");
        createPayload.put("serviceProviderId", primaryServiceProviderId);
        createPayload.put("personType", "OPS");
        long personId = asLong(getDataNode(performPost("/api/v1/persons", createPayload)).path("id"));

        ServiceProviderPersonRel legacyRelation = new ServiceProviderPersonRel();
        legacyRelation.setPersonId(personId);
        legacyRelation.setServiceProviderId(legacyRelatedServiceProviderId);
        serviceProviderPersonRelMapper.insert(legacyRelation);
        assertEquals(1L, serviceProviderPersonRelMapper.selectCount(
                Wrappers.<ServiceProviderPersonRel>lambdaQuery().eq(ServiceProviderPersonRel::getPersonId, personId)).longValue());

        Map<String, Object> updatePayload = createPersonPayload(suffix + "-EDIT", departmentId, "ACTIVE");
        updatePayload.put("serviceProviderId", primaryServiceProviderId);
        updatePayload.put("personType", "DEV");
        getDataNode(performPut("/api/v1/persons/" + personId, updatePayload));

        assertEquals(0L, serviceProviderPersonRelMapper.selectCount(
                Wrappers.<ServiceProviderPersonRel>lambdaQuery().eq(ServiceProviderPersonRel::getPersonId, personId)).longValue());

        JsonNode personDetail = getDataNode(performGet("/api/v1/persons/" + personId));
        assertEquals(0, personDetail.path("relatedServiceProviderIds").size());
    }

    @Test
    void personHardwareConflictShouldFailWithoutPartialWrite() throws Exception {
        String suffix = uniqueSuffix("CONFLICT");
        long departmentId = createDepartment(suffix);
        long locationId = createLocation(suffix);
        long ownerAId = createPerson(suffix + "-A", departmentId);
        long ownerBId = createPerson(suffix + "-B", departmentId);
        long hardwareId = createHardwareAsset(suffix, departmentId, locationId);

        Map<String, Object> ownerARelations = new LinkedHashMap<String, Object>();
        ownerARelations.put("hardwareAssetIds", new long[]{hardwareId});
        ownerARelations.put("informationSystemIds", new long[0]);
        getDataNode(performPut("/api/v1/persons/" + ownerAId + "/relations", ownerARelations));

        Map<String, Object> ownerBRelations = new LinkedHashMap<String, Object>();
        ownerBRelations.put("hardwareAssetIds", new long[]{hardwareId});
        ownerBRelations.put("informationSystemIds", new long[0]);
        MvcResult conflictResult = performPutExpectFailure("/api/v1/persons/" + ownerBId + "/relations", ownerBRelations);
        JsonNode conflictRoot = readRoot(conflictResult);
        assertTrue(conflictRoot.path("message").asText().contains("已分配其他负责人"));

        JsonNode ownerADetail = getDataNode(performGet("/api/v1/persons/" + ownerAId));
        assertTrue(arrayContainsId(ownerADetail.path("hardwareAssetIds"), hardwareId));

        JsonNode ownerBDetail = getDataNode(performGet("/api/v1/persons/" + ownerBId));
        assertEquals(0, ownerBDetail.path("hardwareAssetIds").size());
    }

    @Test
    void deleteServiceProviderShouldFailWhenPersonRelationExists() throws Exception {
        String suffix = uniqueSuffix("SPDEL");
        long departmentId = createDepartment(suffix);
        long personId = createPerson(suffix, departmentId);
        long serviceProviderId = createServiceProvider(suffix);

        Map<String, Object> providerRelations = new LinkedHashMap<String, Object>();
        providerRelations.put("hardwareAssetIds", new long[0]);
        providerRelations.put("informationSystemIds", new long[0]);
        providerRelations.put("personIds", new long[]{personId});
        getDataNode(performPut("/api/v1/service-providers/" + serviceProviderId + "/relations", providerRelations));

        JsonNode deleteRoot = readRoot(performDeleteExpectFailure("/api/v1/service-providers/" + serviceProviderId));
        assertTrue(deleteRoot.path("message").asText().contains("人员关联"));
    }

    private long createDepartment(String suffix) throws Exception {
        return asLong(getDataNode(performPost("/api/v1/departments", createDepartmentPayload(suffix, "ACTIVE"))).path("id"));
    }

    private long createLocation(String suffix) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", "LOC-" + suffix);
        payload.put("name", "测试位置-" + suffix);
        payload.put("site", "主园区");
        payload.put("building", "A座");
        payload.put("floor", "3F");
        payload.put("area", "机房");
        payload.put("addressDetail", "闭环联调测试位置");
        return asLong(getDataNode(performPost("/api/v1/locations", payload)).path("id"));
    }

    private long createPerson(String suffix, long departmentId) throws Exception {
        return createPerson(suffix, departmentId, "ACTIVE");
    }

    private long createPerson(String suffix, long departmentId, String status) throws Exception {
        return asLong(getDataNode(performPost("/api/v1/persons", createPersonPayload(suffix, departmentId, status))).path("id"));
    }

    private long createServiceProvider(String suffix) throws Exception {
        return createServiceProvider(suffix, "ACTIVE");
    }

    private long createServiceProvider(String suffix, String status) throws Exception {
        return asLong(getDataNode(performPost("/api/v1/service-providers", createServiceProviderPayload(suffix, status))).path("id"));
    }

    private long createInformationSystem(String suffix) throws Exception {
        return createInformationSystem(suffix, "ACTIVE");
    }

    private long createInformationSystem(String suffix, String status) throws Exception {
        return asLong(getDataNode(performPost("/api/v1/information-systems", createInformationSystemPayload(suffix, status))).path("id"));
    }

    private long createProject(String suffix) throws Exception {
        return asLong(getDataNode(performPost("/api/v1/projects", createProjectPayload(suffix, "PLANNING"))).path("id"));
    }

    private long createHardwareAsset(String suffix, long departmentId, long locationId) throws Exception {
        return asLong(getDataNode(performPost("/api/v1/hardware-assets", createHardwareAssetPayload(suffix, departmentId, locationId))).path("id"));
    }

    private Map<String, Object> createDepartmentPayload(String suffix, String status) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", "DEPT-" + suffix);
        payload.put("name", "测试部门-" + suffix);
        payload.put("status", status);
        return payload;
    }

    private Map<String, Object> createPersonPayload(String suffix, long departmentId, String status) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("name", "测试人员-" + suffix);
        payload.put("employeeNo", "EMP-" + suffix);
        payload.put("mobile", "1380000" + String.format("%04d", SEQUENCE.getAndIncrement()));
        payload.put("departmentId", departmentId);
        payload.put("status", status);
        return payload;
    }

    private Map<String, Object> createServiceProviderPayload(String suffix, String status) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", "SP-" + suffix);
        payload.put("name", "测试服务商-" + suffix);
        payload.put("type", "SERVICE_PROVIDER");
        payload.put("status", status);
        payload.put("ratingLevel", "A");
        return payload;
    }

    private Map<String, Object> createInformationSystemPayload(String suffix, String status) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", "SYS-" + suffix);
        payload.put("name", "测试系统-" + suffix);
        payload.put("systemType", "SUPPORT_SYSTEM");
        payload.put("status", status);
        payload.put("remark", "闭环联调用例");
        return payload;
    }

    private Map<String, Object> createProjectPayload(String suffix, String projectStatus) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", "PRJ-" + suffix);
        payload.put("name", "测试项目-" + suffix);
        payload.put("projectType", "NEW_BUILD");
        payload.put("projectStatus", projectStatus);
        payload.put("remark", "闭环联调用例");
        return payload;
    }

    private Map<String, Object> createHardwareAssetPayload(String suffix, long departmentId, long locationId) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("assetCode", "HW-" + suffix);
        payload.put("assetName", "测试硬件-" + suffix);
        payload.put("hardwareCategory", "SERVER");
        payload.put("departmentId", departmentId);
        payload.put("locationId", locationId);
        payload.put("managementIp", "10.0.0." + (10 + SEQUENCE.getAndIncrement()));
        payload.put("businessIp", "172.16.0." + (10 + SEQUENCE.getAndIncrement()));
        payload.put("cpuModel", "Xeon Gold");
        payload.put("cpuCores", 8);
        payload.put("memoryGb", 32);
        payload.put("enabledDate", "2026-03-23");
        payload.put("remark", "闭环联调用例");
        payload.put("operatingSystem", "CentOS 7");
        payload.put("diskGb", 512);
        payload.put("virtualization", "VMware");
        return payload;
    }

    private Map<String, Object> idsPayload(long id) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("ids", new long[]{id});
        return payload;
    }

    private MvcResult performGet(String url) throws Exception {
        return mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
    }

    private MvcResult performPost(String url, Object payload) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
    }

    private MvcResult performPostExpectFailure(String url, Object payload) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andReturn();
    }

    private MvcResult performPut(String url, Object payload) throws Exception {
        return mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private MvcResult performPutExpectFailure(String url, Object payload) throws Exception {
        return mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andReturn();
    }

    private MvcResult performGetExpectFailure(String url) throws Exception {
        return mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andReturn();
    }

    private MvcResult performDeleteExpectFailure(String url) throws Exception {
        return mockMvc.perform(delete(url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andReturn();
    }

    private JsonNode getDataNode(MvcResult result) throws Exception {
        JsonNode root = readRoot(result);
        assertTrue(root.path("success").asBoolean());
        return root.path("data");
    }

    private JsonNode readRoot(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(content);
    }

    private boolean arrayContainsId(JsonNode arrayNode, long expectedId) {
        if (!arrayNode.isArray()) {
            return false;
        }
        for (JsonNode node : arrayNode) {
            if (asLong(node.path("id").isMissingNode() ? node : node.path("id")) == expectedId) {
                return true;
            }
        }
        return false;
    }

    private String findDictionaryLabel(JsonNode arrayNode, String value) {
        if (!arrayNode.isArray()) {
            return null;
        }
        for (JsonNode node : arrayNode) {
            if (value.equals(node.path("value").asText())) {
                return node.path("label").asText();
            }
        }
        return null;
    }

    private long asLong(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return -1L;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        return Long.parseLong(node.asText());
    }

    private String uniqueSuffix(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + SEQUENCE.getAndIncrement();
    }
}
