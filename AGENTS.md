# CLAUDE.md — NQBoard 项目开发手册

> 本文件是 NQBoard 项目的权威开发约定，面向本仓库内的所有编码任务。
> **自定义指令优先级：本文件 < 用户本次对话的显式指令。**

---

## 项目概述

NQBoard 是基于 **Spring Cloud Alibaba / Spring Boot 3** 的企业级快速开发平台（衍生自 pig4cloud 架构），同时支持**微服务**与**单体**两种部署形态，落地了 Spring Authorization Server（OAuth2.0，多种授权模式）、Flowable 工作流、IoT 设备管理、量化（quanta）等业务域。

| 技术             | 版本          | 技术                | 版本          |
|------------------|---------------|---------------------|---------------|
| Spring Boot      | 3.5.11        | MyBatis-Plus        | 3.5.16        |
| Spring Cloud     | 2025.0.1      | Flowable            | 7.x(流程)     |
| Spring Cloud Alibaba | 2025.0.0.0 | RocketMQ            | 5.3.0         |
| Spring Authorization Server | 1.5.6 | MySQL            | 8.0.32        |
| Nacos            | 3.1.0         | Redis               | 6.2           |
| Vue / Element Plus | 3.5.13 / 2.13.1 | TDengine / Seata / Sentinel | 3.3.6 / 2.1.0 / 1.8.7 |
| JDK              | 17            | Maven               | （UTF-8）     |

- 基础能力：RBAC 权限、OAuth2 授权、统一返回体 `R<T>`、代码生成（visual-codegen）、Quartz 定时任务、监控。
- 本仓库含微服务全部模块；`nqboard-boot` 是单体启动器（端口 9999）。

---

## 核心模块（微服务）

```lua
nqboard
├── nqboard-boot       单体模式启动器 [9999]
├── nqboard-auth       授权服务 [3000]
├── nqboard-common     公共模块（BOM 统一依赖管理）→ 详见下方
├── nqboard-register   Nacos 注册/配置中心
├── nqboard-gateway    Spring Cloud Gateway 网关（统一鉴权 / 限流 / 路由转发）
├── nqboard-upms       用户权限管理（RBAC / 机构 / 数据权限）
│    ├── nqboard-upms-api   公共 API 模块（entity/dto/vo/feign）
│    └── nqboard-upms-biz   业务实现（controller/service/mapper）
├── nqboard-device     IoT 设备管理（分类/产品/物模型/点位/驱动）
│    ├── nqboard-device-api
│    └── nqboard-device-biz
├── nqboard-workflow   工作流（Flowable 封装，流程定义/实例/任务/监听/表达式）
│    ├── nqboard-workflow-api
│    └── nqboard-workflow-biz
├── nqboard-quanta     量化交易模块（新骨架，尚未实现业务）[6007]
│    ├── nqboard-quanta-api
│    └── nqboard-quanta-biz
└── nqboard-visual     运维可视化
     ├── nqboard-visual-monitor   服务监控
     ├── nqboard-visual-codegen   图形化代码生成
     └── nqboard-visual-quartz    定时任务管理台
```

> 网关路由约定：`/auth/**`→auth、`/admin/**`→upms、`/device/**`→device、`/quanta/**`→quanta、`/gen/**`→codegen、`/job/**`→quartz。

---

## 框架层包结构

每个业务域（如 device、upms、workflow、quanta）遵循 **api / biz 双层**拆分，包路径为 `com.mx.<域>`（quanta 为 `com.mx.nqboard.quanta`）。

```lua
<模块>-api        # 供其他服务引用的公共契约
  └── com.mx.<域>.api
       ├── entity      # 数据库实体（MyBatis-Plus @TableName）
       ├── dto         # 入参对象
       ├── vo          # 出参对象
       ├── constant    # 业务常量
       ├── enums       # 业务枚举
       └── feign       # 跨服务调用接口（@FeignClient）

<模块>-biz        # 业务实现（可引用 api）
  └── com.mx.<域>
       ├── controller  # REST 接口层
       ├── service     # 业务接口
       │    └── impl   # 业务实现（继承 ServiceImpl<Mapper, Entity>）
       ├── mapper      # 数据访问（继承 BaseMapper<Entity>）
       ├── consumer    # MQ 消费者 / 事件消费
       ├── config      # 模块内配置
       └── utils       # 内部工具
```

命名规范（全项目统一）：
- 类 `UpperCamelCase`；方法/参数 `lowerCamelCase`；常量 `UPPER_UNDERSCORE`。
- DTO 以 `XxxDTO`、VO 以 `XxxVO`、实体以 `XxxEntity`、枚举以 `XxxEnum`、常量类以 `XxxConstants`、Feign 以 `RemoteXxxService` 结尾。

---

## 核心架构

### CRUD 分层（标准样板 · 以 IotCategoryController 为参考）

这是全项目增删改查的统一范式，**新模块 CRUD 必须照此实现，禁止另起炉灶**。

```text
Controller (REST)           → 入参校验、权限、日志注解，只做参数组装
    ↓ @RequiredArgsConstructor 注入 Service
Service  extends IService<Entity>
    ↓
ServiceImpl extends ServiceImpl<Mapper, Entity>  实现业务逻辑
    ↓
Mapper    extends BaseMapper<Entity>
    ↓
Entity    继承 BaseEntity（自动填充创建人/时间等）
```

Controller 五个标准接口（对照 `IotCategoryController`）：

| 方法 | 路径 | 说明 | 关键注解 |
|------|------|------|----------|
| 分页 | `GET /page ` | MyBatis-Plus `Page` 分页 | `@ParameterObject` |
| 条件查询 | `GET /details` | `list(Wrappers.query(entity))` 通用条件查询 | |
| 新增 | `POST` | `save(entity)` | `@Validated @RequestBody`、`@SysLog("...")`、`@HasPermission("域_表_ add")` |
| 修改 | `PUT` | `updateById(entity)` | 同新增 |
| 删除 | `DELETE` | `removeBatchByIds(CollUtil.toList(ids))`，入参 `Long[] ids` | |
| 导出 | `GET /export` | `BeanUtil.copyToList(sourceList, XxxExportVO.class)` | `@ResponseExcel` |

配套注解/能力（全部**已存在，必须复用**）：
- 返回统一封装：`R.ok(...)` / `R.failed(...)`（`com.mx.nqboard.common.core.util.R`）。
- 日志：`com.mx.nqboard.common.log.annotation.SysLog`（方法级打点）。
- 权限：`com.mx.nqboard.common.security.annotation.HasPermission`（按钮级鉴权）。
- 服务间白名单：`com.mx.nqboard.common.security.annotation.Inner`（内部接口免鉴权）。
- 参数校验：`@Validated` + JSR380（`@NotBlank`/`@NotNull`）。
- 接口文档：knife4j，类加 `@Tag`、方法加 `@Operation`，Controller 加 `@SecurityRequirement(name = HttpHeaders.AUTHORIZATION)`。
- 条件构造：一律用 MyBatis-Plus `LambdaQueryWrapper` / `Wrappers.lambdaQuery()` / `Wrappers.query()`，**禁止手拼 SQL**。
- 导出：`ResponseExcel`（pig4cloud-plugin-excel）+ `@ExcelDictFormat` / `@ExcelProperty` 字段注解。

> Feign 调用方写法：新增业务 `Controller` 前，先确认同域 `-api` 模块是否已有 `RemoteXxxService`；跨域调用用 `OpenFeign`，接口标注 `@FeignClient(contextId = "xxx", value = ServiceNameConstants.XXX)` 并启用 `EnableNqBoardFeignClients`。

---

## API 模块划分

- `-api` 模块 = **对外契约**（entity/dto/vo/enums/constant/feign），被其他域与前端网关依赖，**不得包含业务逻辑与 Mapper/Service 实现**。
- `-biz` 模块 = **内部实现**（controller/service/mapper/consumer/config）。
- 新业务先定契约（api 模块），再写实现（biz 模块），保持依赖方向单向：`biz → api`。
- 跨服务接口一律 `Feign`；接口文档聚合通过网关 `/v3/api-docs` 代理。

---

## nqboard-common 公共包说明（重要）

`nqboard-common` 是**公共能力底座**，所有业务模块均依赖，按职责拆分子模块。**编码前先确认所需能力在公共包是否已封装，禁止重复实现。**

| 子模块 | 定位与核心内容 |
|--------|----------------|
| `nqboard-common-bom` | 全局依赖版本管理（dependencyManagement），各模块统一引用，避免版本冲突 |
| `nqboard-common-core` | **核心基础包**：统一返回 `R<T>`、常量（`CommonConstants`/`CacheConstants`/`SecurityConstants`/`ServiceNameConstants`/`DriverConstants`/`StringConstants`）、异常体系（`CheckedException`/`DeniedException`/`ReadPointException`/`WritePointException`/`ImportFileException`/`FlowErrorException` 等 + `ErrorCodes`）、Jackson 时间序列化模块、RedisTemplate/RestTemplate 配置、WebMvc 配置、工具类（`RedisUtils`/`WebUtils`/`SpringContextHolder`/`MsgUtils`/`RetOps`/`ClassUtils`） |
| `nqboard-common-datasource` | 动态数据源（多数据源切换、`@DSTransactional` 场景），主库/演进库 Provider 与路由 |
| `nqboard-common-log` | 操作日志：`@SysLog` 注解 + `SysLogAspect` 切面 + `SysLogEvent` 异步落库 |
| `nqboard-common-mybatis` | MyBatis-Plus 增强：`BaseEntity`（审计字段自动填充）、分页拦截器、JSON 数组 TypeHandler、SQL 参数解析 |
| `nqboard-common-security` | OAuth2 安全能力：`@HasPermission`/`@Inner` 注解、`SecurityUtils`、资源服务自动配置、UserDetails/权限解析 |
| `nqboard-common-feign` | OpenFeign 增强：`@Inner` 内部请求透传、Sentinel 降级兜底、无 Token 调用支持 |
| `nqboard-common-swagger` | knife4j/OpenAPI 文档聚合配置 |
| `nqboard-common-xss` | XSS 防注入（Jackson 反序列化清洗、表单与 JSON 两条链路） |
| `nqboard-common-oss` | 对象存储/文件上传抽象（`OssTemplate`）与本地文件模板 |
| `nqboard-common-websocket` | WebSocket 长连接：session 管理、消息分发（本地/Redis 广播）、JSON/文本消息处理器 |
| `nqboard-common-rocketmq` | RocketMQ 自动配置 |
| `nqboard-common-excel` | Excel 导出能力（连同 `ResponseExcel`，含远程字典项解析） |
| `nqboard-common-seata` | 分布式事务（Seata）自动配置 |

**common 使用铁律：**
1. 返回体一律 `R<T>`，不自定义返回结构。
2. 业务异常抛 common 异常体系，由全局异常处理器统一转换，禁止在业务里到处 try-catch 返回。
3. 实体统一继承 `BaseEntity`。
4. 缓存统一走 `RedisUtils`，key 遵循 `CacheConstants`，过期时间合理设置，防穿透/击穿/雪崩。
5. 敏感信息（密钥、AppSecret、IP、端口）一律走 Nacos 配置 + 环境变量，**禁止硬编码在源码**。

---

## 常用命令

```bash
# 编译（跳过测试）
mvn -q clean install -DskipTests

# 仅编译指定模块及其依赖
mvn clean install -pl nqboard-common/nqboard-common-bom -am -DskipTests

# 构建并启动某个微服务（示例：quanta）
mvn spring-boot:run -pl nqboard-quanta/nqboard-quanta-biz -am

# 测试
mvn test

# 打包（可配合 docker）
mvn clean package -DskipTests
mvn package -DskipTests -Ddocker.image.prefix=<image-prefix>
```

> 依赖中间件（MySQL、Redis、Nacos、RocketMQ、Seata、TDengine）通过根目录 `docker-compose.yml` 一键拉起，启动业务前先确保中间件就绪。

---

## 构建与测试

- 编译环境：JDK 17、Maven、UTF-8。
- 全局依赖由 `nqboard-common-bom` 统一管理，新增依赖版本先在此声明。
- 配置文件经 **jasypt** 加密，密钥由环境变量注入，源码中不留明文。
- **测试规范**：
    - 核心 Service 与流程编排（FlowService/节点/策略/事件）必须补充单元/集成测试；
    - 测试覆盖：正常流、边界值、空值、权限拦截、幂等与重试、异常回滚、非法状态流转；
    - 接口文档对应的 CRUD 建议至少验证分页与导出两条链路。

---

## 开发规范（最高优先级，必须严格遵守）

### 禁止自动提交

- **AI/Copilot 一律不得自动执行 `git add / git commit / git push`。**
- 所有变更由开发者本人审阅后手动提交；需要机器人辅助时，必须显式征得用户同意并说明将执行的 git 命令。
- 提交信息格式（与现有仓库风格保持一致，二选一）：
    - 常规：`<type>(<scope>): <描述>`，如 `<feat>(qyabta): 新增量化模块`
    - 或中文：`类型：<类型> 描述：<描述>`

### 基本规范

1. 严格遵循现有 api/biz 包结构，**不随意新建包、不改变项目目录**，不重复实现 common 已有能力。
2. 输入参数使用 `@Validated` + JSR380 校验；全局异常统一处理，禁止 try-catch 泛滥。
3. 禁止手拼 SQL，统一 MyBatis-Plus 条件构造；涉及多表/复杂场景先评估是否该建立聚合仓储。
4. 日志规范：关键节点 `info`、异常 `error`，**禁止 `System.out` / e.printStackTrace**。
5. 代码优雅简洁，可抽象工具方法则抽取；禁止冗余、重复逻辑。
6. 不硬编码密钥/地址等配置；多线程/Redis/异步需说明线程安全与风险。
7. 实体统一继承 `BaseEntity`；跨模块契约一律放 `-api`。

### 编码前知识查阅（PKR，必须先查询再动手）

- 编码任何功能前，**必须先检索/查阅项目已有知识**（详见「知识管理」），确认是否已有可复用能力、既定模式与分层约束，再开始编码。这是最高优先级的前置步骤。

### 计划模式约束（先规划后编码）

- 涉及多文件、多模块或复杂流程（尤其是 quanta 的 FlowService 编排）时，**先给出实现规划（涉及文件、改动点、顺序、风险），经确认后再落代码**，避免破坏既有架构约定。

---
