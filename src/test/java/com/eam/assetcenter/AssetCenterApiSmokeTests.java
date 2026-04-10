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
    void serviceProviderShouldRejectPersonsOwnedByOtherProvidersAcrossEntryPoints() throws Exception {
        String suffix = uniqueSuffix("SP-PERSON");
        long departmentId = createDepartment(suffix);
        long ownerProviderId = createServiceProvider(suffix + "-OWNER");
        long targetProviderId = createServiceProvider(suffix + "-TARGET");

        Map<String, Object> personPayload = createPersonPayload(suffix, departmentId, "ACTIVE");
        personPayload.put("serviceProviderId", ownerProviderId);
        personPayload.put("personType", "OPS");
        long ownedPersonId = asLong(getDataNode(performPost("/api/v1/persons", personPayload)).path("id"));

        Map<String, Object> createPayload = createServiceProviderPayload(suffix + "-CREATE", "ACTIVE");
        createPayload.put("personIds", new long[]{ownedPersonId});
        JsonNode createRoot = readRoot(performPostExpectFailure("/api/v1/service-providers", createPayload));
        assertTrue(createRoot.path("message").asText().contains("已归属其他服务商"));

        Map<String, Object> updatePayload = createServiceProviderPayload(suffix + "-TARGET-EDIT", "ACTIVE");
        updatePayload.put("personIds", new long[]{ownedPersonId});
        JsonNode updateRoot = readRoot(performPutExpectFailure("/api/v1/service-providers/" + targetProviderId, updatePayload));
        assertTrue(updateRoot.path("message").asText().contains("已归属其他服务商"));

        Map<String, Object> relationPayload = new LinkedHashMap<String, Object>();
        relationPayload.put("hardwareAssetIds", new long[0]);
        relationPayload.put("informationSystemIds", new long[0]);
        relationPayload.put("projectIds", new long[0]);
        relationPayload.put("personIds", new long[]{ownedPersonId});
        JsonNode relationRoot = readRoot(performPutExpectFailure("/api/v1/service-providers/" + targetProviderId + "/relations", relationPayload));
        assertTrue(relationRoot.path("message").asText().contains("已归属其他服务商"));

        JsonNode ownerPersonDetail = getDataNode(performGet("/api/v1/persons/" + ownedPersonId));
        assertEquals(ownerProviderId, ownerPersonDetail.path("person").path("serviceProviderId").asLong());

        JsonNode ownerProviderDetail = getDataNode(performGet("/api/v1/service-providers/" + ownerProviderId));
        assertTrue(arrayContainsId(ownerProviderDetail.path("personIds"), ownedPersonId));

        JsonNode targetProviderDetail = getDataNode(performGet("/api/v1/service-providers/" + targetProviderId));
        assertTrue(!arrayContainsId(targetProviderDetail.path("personIds"), ownedPersonId));

        Map<String, Object> ownerRelationPayload = new LinkedHashMap<String, Object>();
        ownerRelationPayload.put("hardwareAssetIds", new long[0]);
        ownerRelationPayload.put("informationSystemIds", new long[0]);
        ownerRelationPayload.put("projectIds", new long[0]);
        ownerRelationPayload.put("personIds", new long[]{ownedPersonId});
        getDataNode(performPut("/api/v1/service-providers/" + ownerProviderId + "/relations", ownerRelationPayload));

        JsonNode ownerProviderDetailAfterSave = getDataNode(performGet("/api/v1/service-providers/" + ownerProviderId));
        assertTrue(arrayContainsId(ownerProviderDetailAfterSave.path("personIds"), ownedPersonId));
    }

    @Test
    void serviceProviderUpdateShouldClearOptionalFieldsAndOwnedPersons() throws Exception {
        String suffix = uniqueSuffix("SP-CLEAR");
        long departmentId = createDepartment(suffix);

        Map<String, Object> createPayload = createServiceProviderPayload(suffix, "ACTIVE");
        createPayload.put("shortName", "简称-" + suffix);
        createPayload.put("enterpriseNature", "PRIVATE");
        createPayload.put("vendorLevel", "STRATEGIC_PARTNER");
        createPayload.put("businessContact", "联系人-" + suffix);
        createPayload.put("businessPhone", "1380000" + String.format("%04d", SEQUENCE.getAndIncrement()));
        createPayload.put("remark", "待清空备注");
        createPayload.put("cooperationScopes", new String[]{"SOFTWARE_DEVELOPMENT"});
        long serviceProviderId = asLong(getDataNode(performPost("/api/v1/service-providers", createPayload)).path("id"));

        Map<String, Object> personPayload = createPersonPayload(suffix, departmentId, "ACTIVE");
        personPayload.put("serviceProviderId", serviceProviderId);
        personPayload.put("personType", "OPS");
        long personId = asLong(getDataNode(performPost("/api/v1/persons", personPayload)).path("id"));

        Map<String, Object> updatePayload = new LinkedHashMap<String, Object>();
        updatePayload.put("code", "SP-" + suffix);
        updatePayload.put("name", "测试服务商-" + suffix);
        updatePayload.put("shortName", "");
        updatePayload.put("logoUrl", null);
        updatePayload.put("unifiedSocialCreditCode", null);
        updatePayload.put("cooperationScopes", new String[]{"SOFTWARE_DEVELOPMENT"});
        updatePayload.put("enterpriseNature", null);
        updatePayload.put("vendorLevel", null);
        updatePayload.put("score", 5);
        updatePayload.put("businessContact", "");
        updatePayload.put("businessPhone", "");
        updatePayload.put("status", "ACTIVE");
        updatePayload.put("remark", null);
        updatePayload.put("personIds", new long[0]);
        updatePayload.put("informationSystemIds", new long[0]);
        updatePayload.put("hardwareAssetIds", new long[0]);
        getDataNode(performPut("/api/v1/service-providers/" + serviceProviderId, updatePayload));

        JsonNode providerDetail = getDataNode(performGet("/api/v1/service-providers/" + serviceProviderId));
        JsonNode provider = providerDetail.path("serviceProvider");
        assertEquals("", provider.path("shortName").asText());
        assertNullOrMissing(provider, "enterpriseNature");
        assertNullOrMissing(provider, "vendorLevel");
        assertEquals("", provider.path("businessContact").asText());
        assertEquals("", provider.path("businessPhone").asText());
        assertNullOrMissing(provider, "remark");
        assertEquals(0, providerDetail.path("personIds").size());

        JsonNode personDetail = getDataNode(performGet("/api/v1/persons/" + personId));
        assertNullOrMissing(personDetail.path("person"), "serviceProviderId");
    }

    @Test
    void personUpdateShouldClearOptionalFields() throws Exception {
        String suffix = uniqueSuffix("PERSON-CLEAR");
        long departmentId = createDepartment(suffix);
        long serviceProviderId = createServiceProvider(suffix);

        Map<String, Object> createPayload = createPersonPayload(suffix, departmentId, "ACTIVE");
        createPayload.put("gender", "男");
        createPayload.put("idCardNo", "441202199205040012");
        createPayload.put("photoUrl", "https://example.com/avatar.png");
        createPayload.put("account", "acct-" + suffix);
        createPayload.put("serviceProviderId", serviceProviderId);
        createPayload.put("personType", "OPS");
        createPayload.put("hasOpsAccount", true);
        long personId = asLong(getDataNode(performPost("/api/v1/persons", createPayload)).path("id"));

        Map<String, Object> updatePayload = new LinkedHashMap<String, Object>();
        updatePayload.put("name", "测试人员-" + suffix);
        updatePayload.put("gender", null);
        updatePayload.put("idCardNo", null);
        updatePayload.put("mobile", null);
        updatePayload.put("employeeNo", null);
        updatePayload.put("photoUrl", null);
        updatePayload.put("account", null);
        updatePayload.put("departmentId", null);
        updatePayload.put("serviceProviderId", null);
        updatePayload.put("personType", null);
        updatePayload.put("status", "ACTIVE");
        updatePayload.put("hasOpsAccount", false);
        updatePayload.put("informationSystemIds", new long[0]);
        updatePayload.put("hardwareAssetIds", new long[0]);
        updatePayload.put("relatedServiceProviderIds", new long[0]);
        getDataNode(performPut("/api/v1/persons/" + personId, updatePayload));

        JsonNode person = getDataNode(performGet("/api/v1/persons/" + personId)).path("person");
        assertNullOrMissing(person, "gender");
        assertNullOrMissing(person, "idCardNo");
        assertNullOrMissing(person, "mobile");
        assertNullOrMissing(person, "employeeNo");
        assertNullOrMissing(person, "photoUrl");
        assertNullOrMissing(person, "account");
        assertNullOrMissing(person, "departmentId");
        assertNullOrMissing(person, "serviceProviderId");
        assertNullOrMissing(person, "personType");
        assertTrue(!person.path("hasOpsAccount").asBoolean());
    }

    @Test
    void informationSystemUpdateShouldClearOptionalFields() throws Exception {
        String suffix = uniqueSuffix("SYS-CLEAR");
        long departmentId = createDepartment(suffix);
        long ownerPersonId = createPerson(suffix, departmentId);

        Map<String, Object> createPayload = createInformationSystemPayload(suffix, "ACTIVE");
        createPayload.put("versionNo", "v1.0.0");
        createPayload.put("deploymentArchitecture", "CLUSTER");
        createPayload.put("ownerPersonId", ownerPersonId);
        createPayload.put("contactPhone", "1380000" + String.format("%04d", SEQUENCE.getAndIncrement()));
        createPayload.put("remark", "待清空系统备注");
        long informationSystemId = asLong(getDataNode(performPost("/api/v1/information-systems", createPayload)).path("id"));

        Map<String, Object> updatePayload = new LinkedHashMap<String, Object>();
        updatePayload.put("code", "SYS-" + suffix);
        updatePayload.put("name", "测试系统-" + suffix);
        updatePayload.put("systemType", "BASIC_SUPPORT");
        updatePayload.put("versionNo", null);
        updatePayload.put("deploymentArchitecture", null);
        updatePayload.put("ownerPersonId", null);
        updatePayload.put("contactPhone", null);
        updatePayload.put("status", "ACTIVE");
        updatePayload.put("remark", null);
        updatePayload.put("serviceProviderIds", new long[0]);
        updatePayload.put("personIds", new long[0]);
        updatePayload.put("hardwareAssetIds", new long[0]);
        getDataNode(performPut("/api/v1/information-systems/" + informationSystemId, updatePayload));

        JsonNode informationSystem = getDataNode(performGet("/api/v1/information-systems/" + informationSystemId)).path("informationSystem");
        assertNullOrMissing(informationSystem, "versionNo");
        assertNullOrMissing(informationSystem, "deploymentArchitecture");
        assertNullOrMissing(informationSystem, "ownerPersonId");
        assertNullOrMissing(informationSystem, "contactPhone");
        assertNullOrMissing(informationSystem, "remark");
    }

    @Test
    void hardwareAssetUpdateShouldClearOptionalFields() throws Exception {
        String suffix = uniqueSuffix("HW-CLEAR");
        long departmentId = createDepartment(suffix);
        long locationId = createLocation(suffix);

        Map<String, Object> createPayload = createHardwareAssetPayload(suffix, departmentId, locationId);
        createPayload.put("hardwareModel", "PowerEdge R750");
        createPayload.put("physicalLocation", "A座机房 3排 2柜");
        createPayload.put("networkEnvironment", "政务外网");
        createPayload.put("operatingSystem", "CentOS 7");
        createPayload.put("purchaseDate", "2026-03-24");
        createPayload.put("businessIp", "172.16.0.10");
        createPayload.put("cpuModel", "Xeon Gold");
        createPayload.put("cpuCores", 16);
        createPayload.put("memoryGb", 64);
        createPayload.put("remark", "待清空硬件备注");
        long hardwareAssetId = asLong(getDataNode(performPost("/api/v1/hardware-assets", createPayload)).path("id"));

        Map<String, Object> updatePayload = createHardwareAssetPayload(suffix, departmentId, locationId);
        updatePayload.put("hardwareModel", null);
        updatePayload.put("physicalLocation", null);
        updatePayload.put("networkEnvironment", null);
        updatePayload.put("operatingSystem", null);
        updatePayload.put("purchaseDate", null);
        updatePayload.put("businessIp", null);
        updatePayload.put("cpuModel", null);
        updatePayload.put("cpuCores", null);
        updatePayload.put("memoryGb", null);
        updatePayload.put("remark", null);
        getDataNode(performPut("/api/v1/hardware-assets/" + hardwareAssetId, updatePayload));

        JsonNode hardwareAsset = getDataNode(performGet("/api/v1/hardware-assets/" + hardwareAssetId)).path("hardwareAsset");
        assertNullOrMissing(hardwareAsset, "hardwareModel");
        assertNullOrMissing(hardwareAsset, "physicalLocation");
        assertNullOrMissing(hardwareAsset, "networkEnvironment");
        assertNullOrMissing(hardwareAsset, "operatingSystem");
        assertNullOrMissing(hardwareAsset, "purchaseDate");
        assertNullOrMissing(hardwareAsset, "businessIp");
        assertNullOrMissing(hardwareAsset, "cpuModel");
        assertNullOrMissing(hardwareAsset, "cpuCores");
        assertNullOrMissing(hardwareAsset, "memoryGb");
        assertNullOrMissing(hardwareAsset, "remark");
    }

    @Test
    void projectUpdateShouldClearOptionalFields() throws Exception {
        String suffix = uniqueSuffix("PRJ-CLEAR");

        Map<String, Object> createPayload = createProjectPayload(suffix, "PLANNING");
        createPayload.put("approvalBatchNo", "APP-" + suffix);
        createPayload.put("projectBudget", 120.5);
        createPayload.put("contractAmount", 118.2);
        createPayload.put("ownerName", "项目负责人");
        createPayload.put("ownerPhone", "1380000" + String.format("%04d", SEQUENCE.getAndIncrement()));
        createPayload.put("approvalDate", "2026-03-20");
        createPayload.put("startDate", "2026-03-21");
        createPayload.put("initialDeliveryDate", "2026-03-22");
        createPayload.put("endDate", "2026-03-23");
        createPayload.put("warrantyEndDate", "2027-03-23");
        createPayload.put("stage", "实施中");
        createPayload.put("paymentCycleName", "首付款");
        createPayload.put("paymentRatio", 30);
        createPayload.put("paymentAmount", 35.46);
        createPayload.put("plannedPaymentDate", "2026-04-01");
        createPayload.put("actualPaymentDate", "2026-04-02");
        createPayload.put("paymentStatus", "PAID");
        createPayload.put("remark", "待清空项目备注");
        long projectId = asLong(getDataNode(performPost("/api/v1/projects", createPayload)).path("id"));

        Map<String, Object> updatePayload = new LinkedHashMap<String, Object>();
        updatePayload.put("code", "PRJ-" + suffix);
        updatePayload.put("name", "测试项目-" + suffix);
        updatePayload.put("projectType", "NEW_BUILD");
        updatePayload.put("projectStatus", "PLANNING");
        updatePayload.put("approvalBatchNo", null);
        updatePayload.put("projectBudget", null);
        updatePayload.put("contractAmount", null);
        updatePayload.put("ownerName", null);
        updatePayload.put("ownerPhone", null);
        updatePayload.put("approvalDate", null);
        updatePayload.put("startDate", null);
        updatePayload.put("initialDeliveryDate", null);
        updatePayload.put("endDate", null);
        updatePayload.put("warrantyEndDate", null);
        updatePayload.put("stage", null);
        updatePayload.put("paymentCycleName", null);
        updatePayload.put("paymentRatio", null);
        updatePayload.put("paymentAmount", null);
        updatePayload.put("plannedPaymentDate", null);
        updatePayload.put("actualPaymentDate", null);
        updatePayload.put("paymentStatus", null);
        updatePayload.put("remark", null);
        updatePayload.put("documents", new Object[0]);
        updatePayload.put("personIds", new long[0]);
        updatePayload.put("informationSystemIds", new long[0]);
        updatePayload.put("hardwareAssetIds", new long[0]);
        getDataNode(performPut("/api/v1/projects/" + projectId, updatePayload));

        JsonNode project = getDataNode(performGet("/api/v1/projects/" + projectId)).path("project");
        assertNullOrMissing(project, "approvalBatchNo");
        assertNullOrMissing(project, "projectBudget");
        assertNullOrMissing(project, "contractAmount");
        assertNullOrMissing(project, "ownerName");
        assertNullOrMissing(project, "ownerPhone");
        assertNullOrMissing(project, "approvalDate");
        assertNullOrMissing(project, "startDate");
        assertNullOrMissing(project, "initialDeliveryDate");
        assertNullOrMissing(project, "endDate");
        assertNullOrMissing(project, "warrantyEndDate");
        assertNullOrMissing(project, "stage");
        assertNullOrMissing(project, "paymentCycleName");
        assertNullOrMissing(project, "paymentRatio");
        assertNullOrMissing(project, "paymentAmount");
        assertNullOrMissing(project, "plannedPaymentDate");
        assertNullOrMissing(project, "actualPaymentDate");
        assertNullOrMissing(project, "paymentStatus");
        assertNullOrMissing(project, "remark");
    }

    @Test
    void listPagesShouldSupportNewUtilityFilters() throws Exception {
        String suffix = uniqueSuffix("LIST-FILTER");
        long departmentAId = createDepartment(suffix + "-A");
        long departmentBId = createDepartment(suffix + "-B");
        long ownerPersonId = createPerson(suffix + "-OWNER", departmentAId);

        long strategicProviderId = createServiceProviderWithVendorLevel(suffix + "-SP-A", "STRATEGIC_PARTNER");
        long generalProviderId = createServiceProviderWithVendorLevel(suffix + "-SP-B", "GENERAL_SUPPLIER");

        long departmentAPersonId = createPersonWithDepartmentAndType(suffix + "-PERSON-A", departmentAId, "OPS");
        long departmentBPersonId = createPersonWithDepartmentAndType(suffix + "-PERSON-B", departmentBId, "DEV");

        long paidProjectId = createProjectWithPaymentStatus(suffix + "-PRJ-A", "PAID");
        long pendingProjectId = createProjectWithPaymentStatus(suffix + "-PRJ-B", "PENDING");

        long clusterSystemId = createInformationSystemWithArchitecture(suffix + "-SYS-A", ownerPersonId, "CLUSTER");
        long singleSystemId = createInformationSystemWithArchitecture(suffix + "-SYS-B", ownerPersonId, "SINGLE");

        JsonNode providerPage = getDataNode(performGet("/api/v1/service-providers?pageNo=1&pageSize=10&vendorLevel=STRATEGIC_PARTNER&keyword=SP-" + suffix + "-SP"));
        assertTrue(arrayContainsId(providerPage.path("records"), strategicProviderId));
        assertTrue(!arrayContainsId(providerPage.path("records"), generalProviderId));
        assertTrue(providerPage.path("total").asLong() >= 1L);

        JsonNode personPage = getDataNode(performGet("/api/v1/persons?pageNo=1&pageSize=10&departmentId=" + departmentAId + "&keyword=" + suffix + "-PERSON"));
        assertTrue(arrayContainsId(personPage.path("records"), departmentAPersonId));
        assertTrue(!arrayContainsId(personPage.path("records"), departmentBPersonId));
        assertTrue(personPage.path("total").asLong() >= 1L);

        JsonNode projectPage = getDataNode(performGet("/api/v1/projects?pageNo=1&pageSize=10&paymentStatus=PAID&keyword=PRJ-" + suffix + "-PRJ"));
        assertTrue(arrayContainsId(projectPage.path("records"), paidProjectId));
        assertTrue(!arrayContainsId(projectPage.path("records"), pendingProjectId));
        assertTrue(projectPage.path("total").asLong() >= 1L);

        JsonNode informationSystemPage = getDataNode(performGet("/api/v1/information-systems?pageNo=1&pageSize=10&deploymentArchitecture=CLUSTER&keyword=SYS-" + suffix + "-SYS"));
        assertTrue(arrayContainsId(informationSystemPage.path("records"), clusterSystemId));
        assertTrue(!arrayContainsId(informationSystemPage.path("records"), singleSystemId));
        assertTrue(informationSystemPage.path("total").asLong() >= 1L);
    }

    @Test
    void listPagesShouldRejectInvalidNewUtilityFilters() throws Exception {
        String suffix = uniqueSuffix("LIST-FILTER-BAD");
        long departmentId = createDepartment(suffix);

        JsonNode providerRoot = readRoot(performGetExpectFailure("/api/v1/service-providers?pageNo=1&pageSize=10&vendorLevel=INVALID"));
        assertTrue(providerRoot.path("message").asText().contains("服务商等级不合法"));

        JsonNode personRoot = readRoot(performGetExpectFailure("/api/v1/persons?pageNo=1&pageSize=10&departmentId=" + (departmentId + 999999)));
        assertTrue(personRoot.path("message").asText().contains("Department not found"));

        JsonNode projectRoot = readRoot(performGetExpectFailure("/api/v1/projects?pageNo=1&pageSize=10&paymentStatus=INVALID"));
        assertTrue(projectRoot.path("message").asText().contains("项目付款状态不合法"));

        JsonNode informationSystemRoot = readRoot(performGetExpectFailure("/api/v1/information-systems?pageNo=1&pageSize=10&deploymentArchitecture=INVALID"));
        assertTrue(informationSystemRoot.path("message").asText().contains("部署架构不合法"));
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

    private long createServiceProviderWithVendorLevel(String suffix, String vendorLevel) throws Exception {
        Map<String, Object> payload = createServiceProviderPayload(suffix, "ACTIVE");
        payload.put("vendorLevel", vendorLevel);
        return asLong(getDataNode(performPost("/api/v1/service-providers", payload)).path("id"));
    }

    private long createInformationSystem(String suffix) throws Exception {
        return createInformationSystem(suffix, "ACTIVE");
    }

    private long createInformationSystem(String suffix, String status) throws Exception {
        return asLong(getDataNode(performPost("/api/v1/information-systems", createInformationSystemPayload(suffix, status))).path("id"));
    }

    private long createInformationSystemWithArchitecture(String suffix, long ownerPersonId, String deploymentArchitecture) throws Exception {
        Map<String, Object> payload = createInformationSystemPayload(suffix, "ACTIVE");
        payload.put("ownerPersonId", ownerPersonId);
        payload.put("deploymentArchitecture", deploymentArchitecture);
        return asLong(getDataNode(performPost("/api/v1/information-systems", payload)).path("id"));
    }

    private long createProject(String suffix) throws Exception {
        return asLong(getDataNode(performPost("/api/v1/projects", createProjectPayload(suffix, "PLANNING"))).path("id"));
    }

    private long createProjectWithPaymentStatus(String suffix, String paymentStatus) throws Exception {
        Map<String, Object> payload = createProjectPayload(suffix, "PLANNING");
        payload.put("paymentStatus", paymentStatus);
        return asLong(getDataNode(performPost("/api/v1/projects", payload)).path("id"));
    }

    private long createPersonWithDepartmentAndType(String suffix, long departmentId, String personType) throws Exception {
        Map<String, Object> payload = createPersonPayload(suffix, departmentId, "ACTIVE");
        payload.put("personType", personType);
        return asLong(getDataNode(performPost("/api/v1/persons", payload)).path("id"));
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
        payload.put("systemType", "BASIC_SUPPORT");
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
        payload.put("hardwareType", "SERVER");
        payload.put("departmentId", departmentId);
        payload.put("locationId", locationId);
        payload.put("managementIp", "10.0.0." + (10 + SEQUENCE.getAndIncrement()));
        payload.put("businessIp", "172.16.0." + (10 + SEQUENCE.getAndIncrement()));
        payload.put("cpuModel", "Xeon Gold");
        payload.put("cpuCores", 8);
        payload.put("memoryGb", 32);
        payload.put("enabledDate", "2026-03-23");
        payload.put("ownerName", "硬件负责人-" + suffix);
        payload.put("contactPhone", "1390000" + String.format("%04d", SEQUENCE.getAndIncrement()));
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

    private void assertNullOrMissing(JsonNode parent, String fieldName) {
        JsonNode fieldNode = parent.path(fieldName);
        assertTrue(fieldNode.isNull() || fieldNode.isMissingNode(),
                "Expected field '" + fieldName + "' to be null or missing, but was: " + fieldNode);
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
