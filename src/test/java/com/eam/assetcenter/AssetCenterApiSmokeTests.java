package com.eam.assetcenter;

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

    private long createDepartment(String suffix) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", "DEPT-" + suffix);
        payload.put("name", "测试部门-" + suffix);
        payload.put("status", "ACTIVE");
        return asLong(getDataNode(performPost("/api/v1/departments", payload)).path("id"));
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
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("name", "测试人员-" + suffix);
        payload.put("employeeNo", "EMP-" + suffix);
        payload.put("mobile", "1380000" + String.format("%04d", SEQUENCE.getAndIncrement()));
        payload.put("departmentId", departmentId);
        payload.put("status", "ACTIVE");
        return asLong(getDataNode(performPost("/api/v1/persons", payload)).path("id"));
    }

    private long createServiceProvider(String suffix) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", "SP-" + suffix);
        payload.put("name", "测试服务商-" + suffix);
        payload.put("type", "SERVICE_PROVIDER");
        payload.put("status", "ACTIVE");
        payload.put("ratingLevel", "A");
        return asLong(getDataNode(performPost("/api/v1/service-providers", payload)).path("id"));
    }

    private long createInformationSystem(String suffix) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", "SYS-" + suffix);
        payload.put("name", "测试系统-" + suffix);
        payload.put("systemType", "SUPPORT_SYSTEM");
        payload.put("status", "ACTIVE");
        payload.put("remark", "闭环联调用例");
        return asLong(getDataNode(performPost("/api/v1/information-systems", payload)).path("id"));
    }

    private long createProject(String suffix) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("code", "PRJ-" + suffix);
        payload.put("name", "测试项目-" + suffix);
        payload.put("projectType", "NEW_BUILD");
        payload.put("projectStatus", "PLANNING");
        payload.put("remark", "闭环联调用例");
        return asLong(getDataNode(performPost("/api/v1/projects", payload)).path("id"));
    }

    private long createHardwareAsset(String suffix, long departmentId, long locationId) throws Exception {
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
        return asLong(getDataNode(performPost("/api/v1/hardware-assets", payload)).path("id"));
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

    private MvcResult performPut(String url, Object payload) throws Exception {
        return mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private JsonNode getDataNode(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(content);
        assertTrue(root.path("success").asBoolean());
        return root.path("data");
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
