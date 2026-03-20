# 信息化资产管理中心后端

## 请先读这里

这是一个面向“信息化资产管理中心”的后端项目，目标是让新的会话、代理或开发人员第一次进入仓库时，先通过这份文档快速建立正确上下文，再继续阅读代码。

- 项目名称：信息化资产管理中心后端
- 当前范围：组织资源、项目资源、软件资源、硬件资源
- 明确排除：本次不做数据资源模块
- 技术基线：JDK 8、Spring Boot 2.7.18、MyBatis-Plus 3.5.7、Flyway
- 当前状态：后端骨架、核心接口、Swagger 页面均已可用

## 业务边界规则

本项目当前只覆盖以下四个资源域：

- 组织资源：部门、服务商、人员、位置
- 项目资源：项目主数据及其与系统、服务商、人员、硬件的关联
- 软件资源：信息系统主数据及其关联关系
- 硬件资源：硬件资产主表、子类型扩展表、关联关系、生命周期、审计

以下内容不在本次建设范围内：

- 数据资源模块
- 审批流
- 多租户
- 自动编号中心
- 前端界面

## 代码结构规则

当前包结构的职责约定如下，后续会话默认沿用，不建议轻易打乱：

- `config`：基础配置，如安全、OpenAPI、MyBatis-Plus
- `common`：统一响应、异常、枚举等公共能力
- `domain.entity`：领域实体
- `infrastructure.mapper`：MyBatis-Plus 持久层接口
- `service`：业务逻辑与跨表聚合
- `web.controller`：REST 接口入口
- `web.request`：请求对象

如果继续开发，优先沿用当前分层和命名方式，不做无必要的大重构。

## 实现现状规则

当前代码已经落地的能力包括：

- 主数据 CRUD：部门、位置、服务商、人员、项目、信息系统、硬件资产
- 关联关系维护：项目、系统、硬件之间的多对多关系已打通
- 硬件生命周期：支持注册、入库、分配、变更、闲置、维护、下线、报废
- 审计记录：资源创建、更新、删除、关系同步、生命周期动作都会记录
- 中文注释：主要类型、关键服务方法、请求对象、实体字段已补中文说明
- Swagger 中文化：控制器分组、接口摘要、核心模型说明已补充

## 接口规则

接口层统一遵循以下约定：

- 基础前缀统一为 `/api/v1`
- 统一响应对象为 `ApiResponse`
- 分页响应统一为 `PageResponse`
- Swagger 地址为 `/swagger-ui.html`
- OpenAPI JSON 地址为 `/v3/api-docs`

当前主要入口接口：

- `/api/v1/departments`
- `/api/v1/locations`
- `/api/v1/service-providers`
- `/api/v1/persons`
- `/api/v1/projects`
- `/api/v1/information-systems`
- `/api/v1/hardware-assets`

硬件扩展接口：

- `PUT /api/v1/hardware-assets/{id}/systems`
- `PUT /api/v1/hardware-assets/{id}/owners`
- `PUT /api/v1/hardware-assets/{id}/vendors`
- `POST /api/v1/hardware-assets/{id}/lifecycle`
- `POST /api/v1/hardware-assets/import`
- `GET /api/v1/hardware-assets/export`

## 开发约束规则

后续会话默认遵守以下约束：

- 代码注释优先使用中文
- 生成或修改代码时，必须补充清晰、准确的中文注释，重点说明业务意图、关键字段、核心流程和必要边界条件
- 生成或修改接口时，Swagger 的分组、接口摘要、参数说明和模型说明也必须保持中文
- 优先保持 JDK 8 兼容
- 不引入 Spring Boot 3 或 Jakarta 风格迁移
- 不新增数据资源相关表、接口、流程
- 不在没有必要的情况下改动当前分层结构
- 新增接口时优先沿用现有 `controller + service + mapper + entity/request` 模式

## 运行与验证规则

本地运行和验证默认按以下方式理解：

- 本地默认数据源为 H2，配置见 `src/main/resources/application.yml`
- 数据库初始化脚本由 Flyway 管理，脚本位于 `src/main/resources/db/migration/V1__init_schema.sql`
- 如需切换生产数据库，按 MySQL 方向修改 `application.yml` 中的数据源配置
- 常用验证命令：`mvn test`
- Swagger 用于接口浏览和联调，不替代自动化测试

## 已知特殊处理

当前项目有几项需要特别记住的上下文：

- Swagger UI 通过项目内静态页面兜底，避免旧版 SpringDoc UI 路由兼容问题
- `springdoc-openapi-ui` 当前使用 `1.5.2`
- 本地 Swagger 页面地址为 `/swagger-ui.html`
- Maven 仓库环境之前出现过握手问题，但当前项目已经可以正常构建和测试
- 当前默认安全配置允许接口访问，后续如果补 JWT/权限，需要在现有 `SecurityConfig` 基础上扩展

## 关键文件

下个会话如果需要快速接手，优先看这几个文件：

- `src/main/resources/application.yml`
- `src/main/resources/db/migration/V1__init_schema.sql`
- `src/main/java/com/eam/assetcenter/service/HardwareAssetService.java`
- `src/main/java/com/eam/assetcenter/web/controller/HardwareAssetController.java`
- `src/main/java/com/eam/assetcenter/config/SecurityConfig.java`

## 下个会话建议起手动作

建议新的会话按下面顺序进入项目：

1. 先阅读本 README，确认业务边界和技术约束
2. 再看 `application.yml`、`V1__init_schema.sql`、`HardwareAssetService.java`
3. 如需继续开发，优先补 DTO / VO 分层
4. 如需继续开发，优先补 权限 / JWT
5. 如需继续开发，优先补更完整的接口测试
6. 如需继续开发，优先补更细的业务校验

## 额外说明

- 本 README 既服务于人工阅读，也服务于新的 AI 会话建立上下文
- 如果 README 与代码不一致，以代码实现为准，并应优先修正文档
