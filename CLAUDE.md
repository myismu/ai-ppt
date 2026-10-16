# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 仓库定位

AI-PPT 智能 PPT 生成与在线编辑平台：从主题或长文档生成大纲、流式生成幻灯片、在线编辑、项目管理、云端保存与 PPTX 导出。

本仓库自包含（题库系统在独立的 question-bank 仓库，不在此仓库内）：

- 后端：Spring Cloud 微服务（Java 17 / Spring Boot 3.5 / Spring Cloud 2025 / Spring Cloud Alibaba 2025）
- 前端：两个独立的 Vue 3 + Vite 工程（`Web/AI-PPT`、`Web/UserHub-Web`）

## 后端微服务

| 模块 | 职责 | 端口 |
|---|---|---|
| `gateway` | 网关：路由、JWT 鉴权、CORS、请求日志 | 8080 |
| `service/user-hub-service` | 用户服务：登录、注册、用户/角色/路由管理 | 8001 |
| `service/file-service` | 文件服务：上传、静态资源（/img、/image、/templates、/projects）、存储统计 | 8003 |
| `service/ai-service` | AI 服务：大模型接入（文本/生图）、文档转 PPT 大纲 | 8083 |
| `service/ppt-service` | PPT 服务：项目持久化、生成编排、模板配置 | 8084 |
| `common` | 公共模块：JwtUtil、统一返回、全局异常、登录头过滤器 | - |
| `api` | Feign 客户端接口定义 | - |

### 配置与依赖

- 运行时配置全部来自 **Nacos**（开发命名空间 `a1e2b3c4-d5e6-7f8a-9b0c-1d2e3f4a5b6c`），本地 `application-dev.yml` 只保留端口与 Nacos 地址。
- 配置模板在 `naocs/nacos-dev/`，按 Data ID 导入 Nacos 对应命名空间。仓库内所有敏感值均为占位符（如 `${MYSQL_PWD:}`、`${JWT_SECRET:}`），真实值通过环境变量注入，**不要提交任何真实密钥**。
- 中间件依赖：MySQL（`user_hub` / `ppt_db` / `ai_db` / `seata` 库）、Redis、Nacos、RocketMQ（NameServer 9876）、Seata（8091）。ppt-service 与 user-hub-service 配置了 Seata，但当前无业务代码使用全局事务。
- SQL 脚本在 `SQL/`：`user_hub.sql`、`ppt.sql`、`ai_db.sql`、`seata.sql`。

### 构建与启动

```bash
mvn clean install    # 先安装 common/api 到本地仓库
```

再依次启动各模块启动类：`GatewayApplication` → `UserHubApplication` → `FileApplication` → `AiApplication` → `PptApplication`。

### 关键机制

- **网关鉴权**：`gateway` 的 `AuthFilter` 校验 JWT（`common/JwtUtil`），并检查 Redis 在线状态（`online:{systemCode}:{userId}` 必须存在且与 token 一致）。免认证白名单见 `AuthFilter.WHITE_LIST`。
- **JWT 密钥**：`user-hub-service` 签发、`gateway` 校验，两边共用 Nacos 下发的 `jwt.secret`，修改必须全服务同步。
- **AI 模型配置**：持久化在 `ai_db.ai_model_config` 表（provider / apiFormat / modelName / baseUrl / apiKey / extraConfig），`ai-service` 运行时动态读取，按 `apiFormat`（openai / anthropic）路由到不同适配器。
- **流式响应**：AI 生成走 SSE 流式，网关开启 `streaming-media-types`（text/event-stream），并给 `/api/ppt/**`、`/api/tools/**` 路由追加 `X-Accel-Buffering: no` 响应头。

## 前端

### Web/AI-PPT

基于 PPTist 二次开发的编辑器前端。

- 命令：`pnpm install` / `pnpm dev`（端口 3336）/ `pnpm build`（vue-tsc + vite build）
- `vite.config.ts` 将 `/api`、`/templates`、`/projects`、`/img`、`/image` 代理到 `127.0.0.1:8080`（网关），因此本地联调需要先启动后端服务。
- 核心代码：`src/views/Editor`（编辑器）、`src/views/Projects`（项目管理）、`src/hooks/useAIPPT.ts`（AI 生成流程）、`src/hooks/useExport.ts`（导出）、`src/types/slides.ts`（幻灯片类型定义）。

### Web/UserHub-Web

基于 v3-admin-vite 模板的用户中心前端。

- 命令：`pnpm install` / `pnpm dev`（端口 3333）/ `pnpm build`
- 登录流程包含验证码与 token 两步（`src/pages/login/apis/index.ts`）。
- 保留模板的 Pinia user store + cookie token + 路由守卫组合。

## 部署

- `docker-compose.yml`：单机全量编排（nacos / mysql / redis / gateway / file-service / ai-service / ppt-service / 前端）。
- `.github/workflows/deploy.yml`：push 到 main 后构建后端并 SSH 部署，需要在仓库 Secrets 配置 `SERVER_HOST` / `SERVER_USER` / `SERVER_SSH_KEY`。

## 安全约定

- 不提交真实密钥、密码、生产数据库密码；配置走环境变量或 Nacos 私有配置。
- 新增配置项时同步更新 `naocs/nacos-dev/` 模板与 `README.md` 的部署说明。
