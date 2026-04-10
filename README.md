## 系统说明

- 基于 Spring Cloud 、Spring Boot、 OAuth2 的 RBAC **企业快速开发平台**， 同时支持微服务架构和单体架构
- 提供对 Spring Authorization Server 生产级实践，支持多种安全授权模式
- 提供对常见容器化方案支持 Kubernetes、Rancher2 、Kubesphere、EDAS、SAE 支持

#### 分支说明

- jdk17: java17/21 + springboot 3.5 + springcloud 2025

## 快速开始

### 核心依赖

| 依赖                         | 版本       |
|-----------------------------|------------|
| Spring Boot                 | 3.5.11     |
| Spring Cloud                | 2025.0.1   |
| Spring Cloud Alibaba        | 2025.0.0.0 |
| Spring Authorization Server | 1.5.6      |
| Mybatis Plus                | 3.5.16     |
| Vue                         | 3.5.13     |
| Element Plus                | 2.13.1     |
| Nacos                       | 3.1.0      |


### 中间件
| 名称                         | 版本       |
|-----------------------------|------------|
| RocketMQ                    | 5.3.0      |
| Msql                        | 5.7        |
| Redis                       | 6.2        |
| TDengine                    | 3.3.6.13   |
| Seata                       | 1.7.1      |
| Sentinel                    | 1.8.7      |

### 模块说明

```lua
nqboard
├── nqboard-boot -- 单体模式启动器[9999]
├── nqboard-auth -- 授权服务提供[3000]
└── nqboard-common -- 系统公共模块
     ├── nqboard-common-bom -- 全局依赖管理控制
     ├── nqboard-common-core -- 公共工具类核心包
     ├── nqboard-common-datasource -- 动态数据源包
     ├── nqboard-common-log -- 日志服务
     ├── nqboard-common-oss -- 文件上传工具类
     ├── nqboard-common-mybatis -- mybatis 扩展封装
     ├── nqboard-common-seata -- 分布式事务
     ├── nqboard-common-security -- 安全工具类
     ├── nqboard-common-swagger -- 接口文档
     ├── nqboard-common-feign -- feign 扩展封装
     └── nqboard-common-xss -- xss 安全封装
└── nqboard-device -- iot设备管理模块
     └── nqboard-device-api -- iot设备管理模块公共api模块
     ├── nqboard-device-biz -- iot设备管理模块业务处理模块
├── nqboard-register -- Nacos Server
├── nqboard-gateway -- Spring Cloud Gateway网关
└── nqboard-upms -- 通用用户权限管理模块
     └── nqboard-upms-api -- 通用用户权限管理系统公共api模块
     └── nqboard-upms-biz -- 通用用户权限管理系统业务处理模块
└── nqboard-visual
     └── nqboard-visual-monitor -- 服务监控
     ├── nqboard-visual-codegen -- 图形化代码生成
     └── nqboard-visual-quartz -- 定时任务管理台
└── nqboard-workflow
     └── nqboard-workflow-api -- 工作流管理模块公共api模块
     ├── nqboard-workflow-biz -- 工作流管理模块业务处理模块
```
## 项目截图
![](./nqboard-device/nqboard-device-biz/src/main/resources/img/demo.gif)
<table>
    <tr>
        <td><img src="https://gitee.com/hgbcode/nqboard/raw/master/nqboard-device/nqboard-device-biz/src/main/resources/img/1.png"/></td>
        <td><img src="https://gitee.com/hgbcode/nqboard/raw/master/nqboard-device/nqboard-device-biz/src/main/resources/img/2.png"/></td>
    </tr> 
   <tr>
        <td><img src="https://gitee.com/hgbcode/nqboard/raw/master/nqboard-device/nqboard-device-biz/src/main/resources/img/3.png"/></td>
        <td><img src="https://gitee.com/hgbcode/nqboard/raw/master/nqboard-device/nqboard-device-biz/src/main/resources/img/4.png"/></td>
    </tr> 
   <tr>
        <td><img src="https://gitee.com/hgbcode/nqboard/raw/master/nqboard-device/nqboard-device-biz/src/main/resources/img/5.png"/></td>
        <td><img src="https://gitee.com/hgbcode/nqboard/raw/master/nqboard-device/nqboard-device-biz/src/main/resources/img/6.png"/></td>
    </tr> 
    <tr>
        <td><img src="https://gitee.com/hgbcode/nqboard/raw/master/nqboard-device/nqboard-device-biz/src/main/resources/img/7.png"/></td>
        <td><img src="https://gitee.com/hgbcode/nqboard/raw/master/nqboard-device/nqboard-device-biz/src/main/resources/img/8.png"/></td>
    </tr> 
	<tr>
        <td><img src="https://gitee.com/hgbcode/nqboard/raw/master/nqboard-device/nqboard-device-biz/src/main/resources/img/9.png"/></td>
    </tr>
</table>
## 开源共建

### 开源协议

nqboard 开源软件遵循 [Apache 2.0 协议](https://www.apache.org/licenses/LICENSE-2.0.html)。
允许商业使用，但务必保留类作者、Copyright 信息。

![](https://minio.pigx.top/oss/1655474288.jpg)
