# 信息化资产管理中心后端

## 项目定位

这是“信息化资产管理中心”的后端项目，当前目标不是重做骨架，而是在现有基础上继续完成新版资源模块交付、接口联调和演示稳定。

- 当前范围：组织资源、项目资源、软件资源、硬件资源
- 明确不做：数据资源模块
- 技术基线：JDK 8、Spring Boot 2.7.18、MyBatis-Plus 3.5.7、Flyway

## 当前业务现状

### 组织资源

组织资源已完成新版交付所需的核心接口升级：

- 服务商新版字段与统计接口
- 服务商合作范围关系维护
- 人员 `serviceProviderId`、`personType`、`hasOpsAccount`
- 人员详情聚合与关系同步
- 图片上传接口，供服务商 Logo 等页面使用

### 项目资源

项目资源已完成新版交付所需的核心接口升级：

- 项目新版字段扩展
- 项目分页查询与统计接口
- 项目详情摘要聚合
- 项目文档真实上传与 `project_document` 落库
- 项目关系维护继续支持人员 / 软件 / 硬件 / 服务商

### 软件资源 / 硬件资源

软件资源、硬件资源已完成新版交付所需的核心接口升级：

- 信息系统新版字段扩展：版本号、部署架构、负责人、联系电话
- 信息系统详情与保存支持人员 / 服务商 / 硬件关联维护
- 硬件资产新版字段扩展：IP、型号、品牌、类型、位置、网络环境、操作系统、采购日期、负责人、联系电话
- 硬件详情与保存支持人员 / 软件 / 项目 / 服务商关联维护
- 历史软件类型、硬件分类 / 状态数据已迁移兼容到新版枚举

## 代码结构

当前分层默认沿用：

- `config`：基础配置
- `common`：统一响应、异常、枚举
- `domain.entity`：领域实体
- `infrastructure.mapper`：持久层接口
- `service`：业务逻辑与跨表聚合
- `web.controller`：REST 接口入口
- `web.request`：请求对象

默认不要打乱现有分层，不做无必要的大重构。

## 接口约定

- 基础前缀：`/api/v1`
- 统一响应：`ApiResponse`
- 分页响应：`PageResponse`
- Swagger：`/swagger-ui.html`
- OpenAPI JSON：`/v3/api-docs`

当前主要资源入口：

- `/api/v1/departments`
- `/api/v1/locations`
- `/api/v1/service-providers`
- `/api/v1/persons`
- `/api/v1/projects`
- `/api/v1/information-systems`
- `/api/v1/hardware-assets`

新增或升级的重要接口：

- `GET /api/v1/service-providers/stats`
- `GET /api/v1/persons/stats`
- `GET /api/v1/projects/stats`
- `POST /api/v1/files/images`
- `GET /api/v1/files/images/{fileName}`
- `POST /api/v1/files`
- `GET /api/v1/files/documents/{fileName}`

## 关键迁移

当前需要重点记住的增量迁移：

- `V4__upgrade_organization_person_and_provider.sql`
- `V5__add_person_ops_account.sql`
- `V6__upgrade_project_resource_delivery.sql`
- `V7__upgrade_information_system_resource_delivery.sql`
- `V8__upgrade_hardware_resource_delivery.sql`

其中 `V6` 主要完成：

- `project_info` 新版字段扩展
- `project_document` 表创建
- 旧项目类型和状态向新版枚举迁移

后续表结构变更继续新增 `V9 / V10 / ...`，不要回改旧迁移。

## 文件上传

当前后端已提供两类上传：

- 图片上传
  - `POST /api/v1/files/images`
  - 默认限制 2MB
  - 主要用于 Logo、头像等
- 通用文件上传
  - `POST /api/v1/files`
  - 默认限制 100MB
  - 主要用于项目文档

默认本地文件存储目录：

- `${user.dir}/data/uploads`

公开访问路径：

- 图片：`/api/v1/files/images/{fileName}`
- 文档：`/api/v1/files/documents/{fileName}`

## 运行与验证

本地默认配置以 `src/main/resources/application.yml` 为准：

- 默认数据库：MySQL `127.0.0.1:3307/eam`
- 仍保留 H2 示例配置，便于切换
- Flyway 默认开启

常用命令：

- `mvn test`
- `mvn -q -DskipTests compile`

## 开发约束

后续会话默认遵守以下规则：

- 注释和 Swagger 说明保持中文
- 保持 JDK 8 兼容
- 不引入 Spring Boot 3 / Jakarta 迁移
- 不新增数据资源相关表和接口
- 优先沿用 `controller + service + mapper + entity/request` 模式
- 文档与代码冲突时，以代码为准，并及时更新 README

## 当前建议重点

推荐后续优先级：

1. 继续支撑软件 / 硬件新版细节优化与联调
2. 继续补齐跨模块关联、聚合回显和校验细节
3. 最后再清理旧协议和补更细的 DTO / VO、测试与校验

## 下个会话建议优先阅读

- `src/main/java/com/eam/assetcenter/web/controller/ProjectController.java`
- `src/main/java/com/eam/assetcenter/service/ProjectService.java`
- `src/main/java/com/eam/assetcenter/web/controller/FileController.java`
- `src/main/java/com/eam/assetcenter/service/FileStorageService.java`
- `src/main/java/com/eam/assetcenter/web/request/ProjectUpsertRequest.java`
- `src/main/java/com/eam/assetcenter/domain/entity/ProjectDocument.java`
- `src/main/resources/application.yml`
- `src/main/resources/db/migration/V6__upgrade_project_resource_delivery.sql`

## 额外说明

- 当前 README 主要服务于后续新会话快速建立真实上下文
- 如果继续推进项目资源，请优先检查项目详情聚合、文档上传、关系同步和枚举回显，不要重复回退到旧接口口径
