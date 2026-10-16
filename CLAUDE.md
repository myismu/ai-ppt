# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 仓库定位

AI-PPT 业务系统仓库：只包含业务侧代码（`ai-service`、`ppt-service` 与 AI-PPT 前端）。

用户中心、文件服务、统一网关、公共库在平台仓库 **cloud-platform**（`github.com/myismu/cloud-platform`）—— 通过**接口调用**（部署的服务）与 **Maven 制品**（common）使用，**本仓库不包含其代码**。修改平台能力时去平台仓库改，不要在本地复制。

## 模块

| 模块 | 职责 | 端口 |
|---|---|---|
| `service/ai-service` | AI 服务：大模型接入（文本/生图）、文档转 PPT 大纲 | 8083 |
| `service/ppt-service` | PPT 服务：项目持久化、生成编排、模板配置 | 8084 |
| `Web/AI-PPT` | 编辑器前端（PPTist 二次开发） | 3336 |

## 依赖的平台制品

- 父 pom：`com.exambaby:cloud`（来自 GitHub Packages，`<relativePath/>` 强制走制品仓库）
- 公共库：`com.exambaby:common`、平台契约库：`com.exambaby:api`（文件服务客户端；版本均由父 pom 的 `platform.libs.version` 统一管理）
- 仓库内契约库：`ai-ppt-api` 模块（AI 服务客户端，reactor 依赖，不发布制品）
- 本地：`~/.m2/settings.xml` 声明制品仓库与凭据；CI：`mvn -s .mvn/ci-settings.xml`（凭据经 GITHUB_TOKEN 环境变量注入）

## 配置与依赖

- 运行时配置全部来自 **Nacos**（平台提供的开发命名空间），`naocs/nacos-dev/` 下为 `ai-service.yaml`、`ppt-service.yaml` 两个模板。
- 中间件与平台服务（Nacos / MySQL / Redis / 网关 / 用户中心 / 文件服务）由平台仓库提供；**本地开发先起平台，再起业务服务**。
- 业务数据库脚本：`SQL/ai_db.sql`、`SQL/ppt.sql`。

## 构建与启动

```bash
mvn clean package
# 启动顺序：AiApplication → PptApplication（网关等平台服务需已就绪）
```

前端：`cd Web/AI-PPT && pnpm install && pnpm dev`

## 关键机制

- **AI 模型配置**：持久化在 `ai_db.ai_model_config` 表（provider / apiFormat / modelName / baseUrl / apiKey），`ai-service` 运行时动态读取，按 `apiFormat`（openai / anthropic）路由适配器。
- **流式响应**：AI 生成走 SSE，网关侧已配置 `streaming-media-types` 与 `X-Accel-Buffering`（平台仓库维护）。
- **调用平台服务**：经统一网关的 HTTP 接口（服务名寻址由 Nacos 提供）。`ppt-service` 中手写的 `AiServiceClient` / `FileServiceClient` 属于遗留实现，后续应收编为平台 `api` 契约库，新代码不要照此写法。

## 安全约定

- 不提交真实密钥、API Key、数据库密码；配置走环境变量或 Nacos。
- 新增配置项时同步更新 `naocs/nacos-dev/` 模板与 README。
