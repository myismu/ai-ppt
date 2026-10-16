# AI-PPT

[![CI](https://github.com/myismu/ai-ppt/actions/workflows/ci.yml/badge.svg)](https://github.com/myismu/ai-ppt/actions/workflows/ci.yml)

AI-PPT 是一个智能 PPT 生成与在线编辑系统，支持从主题或文档生成大纲、流式生成幻灯片、在线编辑、项目管理、云端保存以及 PPTX 导出。

## 架构说明（重要）

本仓库只包含 AI-PPT **业务侧**的代码。用户中心、文件服务、统一网关与公共库由平台仓库 [cloud-platform](../cloud-platform)（GitHub: `myismu/cloud-platform`）提供：

| 能力 | 来源（平台仓库） | 本仓库的使用方式 |
|---|---|---|
| 用户中心（认证 / 用户 / 角色） | `user-hub-service` | 接口调用（经统一网关） |
| 文件服务（上传 / 静态资源 /images /templates /projects） | `file-service` | 接口调用（经统一网关） |
| 统一网关（路由 / 鉴权 / CORS） | `gateway` | 所有请求的统一入口 |
| 公共库 `common` | 发布的 Maven 制品 | `pom.xml` 依赖 |

平台代码只有一份源码（在 cloud-platform 仓库），本仓库不复制、不维护其实现。

## 项目能力

- AI 生成 PPT 大纲 / AI 根据大纲生成幻灯片内容
- 长文档解析并生成 PPT 大纲
- AI 文案润色、扩写、缩写、翻译、总结
- AI 图片生成
- Web 在线 PPT 编辑
- PPT 项目创建、保存、加载、收藏、回收站管理
- PPTX、JSON、图片、PDF 等格式导出

## 目录结构

```text
.
├── Web/AI-PPT/          # 编辑器前端（Vue 3，端口 3336）
├── service/
│   ├── ai-service/      # AI 服务：大模型接入、生图、文档转 PPT（端口 8083）
│   └── ppt-service/     # PPT 服务：项目持久化、生成编排（端口 8084）
├── SQL/                 # 业务数据库脚本（ai_db / ppt_db）
├── naocs/nacos-dev/     # 业务服务的 Nacos 配置模板
├── .mvn/ci-settings.xml # CI 专用 Maven 配置（声明平台制品仓库）
└── pom.xml              # Maven 父工程（继承平台父 pom）
```

## 本地启动

### 0. 前置：启动平台（含中间件）

```bash
cd ../cloud-platform
docker-compose up -d --build     # Nacos / MySQL / Redis / 网关 / 用户中心 / 文件服务
```

也可以在 IDE 中按平台 README 的说明本地启动。

### 1. 启动业务服务

```bash
mvn clean package
```

依次启动 `AiApplication`、`PptApplication`；对应的 Nacos 配置从平台的开发命名空间读取（本仓库 `naocs/nacos-dev/` 为模板）。

### 2. 启动前端

```bash
cd Web/AI-PPT
pnpm install
pnpm dev
```

访问 http://127.0.0.1:3336 （`/api`、`/templates`、`/projects`、`/img`、`/image` 都代理到平台网关 8080）。

## Maven 依赖说明

父 pom（`com.exambaby:cloud`）、公共库 `common`、契约库 `api`（文件服务客户端）来自平台发布的 GitHub Packages 制品：

- 本地开发：`~/.m2/settings.xml` 中声明制品仓库与凭据（配置片段见 cloud-platform 的 README；GitHub 的 Maven 注册表需要凭据：用户名填 GitHub 账号，密码填具备 `read:packages` 权限的令牌）
- CI：使用 `.mvn/ci-settings.xml`

## 核心接口

### PPT 服务

- `POST /ppt/create` 创建项目 · `POST /ppt/save` 保存 · `GET /ppt/list` 列表
- `GET /ppt/detail/{id}` 详情 · `DELETE /ppt/delete/{id}` 回收站 · `PUT /ppt/restore/{id}` 恢复
- `GET /ppt/templates` 模板列表 · `POST /ppt/outline` 流式生成大纲 · `POST /ppt/generate` 流式生成内容

### AI 服务

- `GET /ai/models` 模型列表 · `POST /ai/writing` 文本流式处理 · `POST /ai/document-to-ppt` 文档转大纲 · `POST /ai/image` 生图

## 配置安全说明

- 仓库内敏感值均为占位符，真实值通过环境变量或 Nacos 私有配置注入
- 不提交真实 `.env`、API Key、数据库密码

## 说明

前端编辑器基于 PPTist 二次开发，保留 Web 幻灯片编辑、演示与导出能力，并接入 AI 生成与后端微服务。请遵守原项目及本仓库的开源协议要求。
