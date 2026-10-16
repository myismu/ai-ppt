# AI-PPT

AI-PPT 是一个基于 Vue 3 + Spring Cloud 的智能 PPT 生成与在线编辑系统，支持从主题或文档生成大纲、流式生成幻灯片、在线编辑、项目管理、云端保存以及 PPTX 导出。

本仓库整理为可独立部署的 AI-PPT 项目，包含前端编辑器、AI 服务、PPT 服务、文件服务、网关、用户认证服务、公共模块、数据库脚本与 Nacos 配置，不包含题库系统前端与题库服务后端。

## 项目能力

- AI 生成 PPT 大纲
- AI 根据大纲生成幻灯片内容
- 长文档解析并生成 PPT 大纲
- AI 文案润色、扩写、缩写、翻译、总结
- AI 图片生成
- Web 在线 PPT 编辑
- PPT 项目创建、保存、加载、收藏、回收站管理
- 项目资源存储统计
- PPTX、JSON、图片、PDF 等格式导出
- 基于网关的前后端分离部署

## 技术栈

### 前端

- Vue 3
- TypeScript
- Vite
- Pinia
- Vue Router
- Sass
- ProseMirror
- ECharts
- pptxgenjs
- html-to-image

### 后端

- Java 17
- Spring Boot 3.5
- Spring Cloud 2025
- Spring Cloud Alibaba
- Nacos
- Sentinel
- OpenFeign
- WebFlux
- MyBatis-Plus
- MySQL
- Redis
- Log4j2
- Apache PDFBox
- Apache POI

## 目录结构

```text
.
├── Web/
│   ├── AI-PPT/                 # AI-PPT 前端工程
│   └── UserHub-Web/            # 用户认证/用户中心前端
├── service/
│   ├── ai-service/             # AI 能力服务
│   ├── ppt-service/            # PPT 项目与生成编排服务
│   ├── file-service/           # 文件上传、访问与资源统计服务
│   └── user-hub-service/       # 用户认证与用户中心服务
├── gateway/                    # Spring Cloud Gateway 网关
├── common/                     # 公共模块
├── api/                        # Feign/API 模块
├── SQL/                        # 数据库脚本
├── naocs/                      # Nacos 配置示例
├── doc/                        # 项目文档
├── docker-compose.yml          # 基础部署编排示例
└── pom.xml                     # Maven 父工程
```

## 环境要求

- Node.js >= 20
- pnpm 或 npm
- JDK >= 17
- Maven >= 3.8
- MySQL
- Redis
- Nacos

## 快速启动

### 1. 初始化数据库

根据 `SQL/` 目录下的脚本创建数据库表与初始化数据。

常见脚本包括：

- `SQL/ai_db.sql`
- `SQL/user_hub.sql`

请根据你的数据库环境调整库名、账号和密码。

### 2. 启动基础组件

准备并启动：

- MySQL
- Redis
- Nacos

也可以参考根目录的 `docker-compose.yml` 自行调整后启动相关基础服务。

### 3. 导入 Nacos 配置

参考 `naocs/nacos-dev/` 下的配置文件，为以下服务准备配置：

- gateway
- user-hub-service
- file-service
- ai-service
- ppt-service

部署前请重点修改：

- MySQL 地址、账号、密码
- Redis 地址和密码
- JWT Secret
- 文件上传路径和访问地址
- AI 模型供应商、模型名称、API Key、Base URL

不要将真实密钥提交到公开仓库。

### 4. 启动后端服务

在项目根目录构建后端：

```bash
mvn clean package
```

然后依次启动：

1. `gateway`
2. `service/user-hub-service`
3. `service/file-service`
4. `service/ai-service`
5. `service/ppt-service`

也可以在 IDE 中直接启动各模块的 Spring Boot 启动类。

### 5. 启动 AI-PPT 前端

```bash
cd Web/AI-PPT
pnpm install
pnpm dev
```

默认访问：

```text
http://127.0.0.1:5173/
```

生产构建：

```bash
pnpm build
```

### 6. 启动用户中心前端

如果需要登录、注册、用户中心等页面，可启动：

```bash
cd Web/UserHub-Web
pnpm install
pnpm dev
```

## 前端说明

AI-PPT 前端详细说明见：

```text
Web/AI-PPT/README.md
```

该目录包含编辑器源码、前端构建配置、Dockerfile、Nginx 配置和前端环境变量示例。

## 核心接口

### PPT 服务

- `POST /ppt/create`：创建 PPT 项目
- `POST /ppt/save`：保存 PPT 项目
- `GET /ppt/list`：获取项目列表
- `GET /ppt/detail/{id}`：获取项目详情
- `DELETE /ppt/delete/{id}`：移入回收站
- `PUT /ppt/restore/{id}`：恢复项目
- `DELETE /ppt/hardDelete/{id}`：彻底删除项目
- `PUT /ppt/star/{id}`：收藏/取消收藏
- `GET /ppt/templates`：获取 PPT 模板
- `POST /ppt/outline`：流式生成 PPT 大纲
- `POST /ppt/generate`：流式生成幻灯片内容
- `POST /ppt/writing`：AI 文案处理
- `POST /ppt/image`：AI 图片生成

### AI 服务

- `GET /ai/models`：获取可用模型列表
- `POST /ai/writing`：AI 文本流式处理
- `POST /ai/document-to-ppt`：文档转 PPT 大纲
- `POST /ai/image`：AI 生图

## 配置安全说明

本项目需要配置数据库、Redis、JWT、AI 模型密钥等敏感信息。公开部署或提交代码前，请确保：

- 不提交真实 `.env` 文件
- 不提交真实 API Key
- 不提交生产数据库密码
- 不提交上传目录中的用户文件
- 使用环境变量或 Nacos 私有配置管理敏感信息

## 说明

前端编辑器基于 PPTist 二次开发，保留了 Web 幻灯片编辑、演示和导出等核心能力，并在此基础上接入 AI 生成、项目持久化和后端微服务能力。

请遵守原项目及本仓库相关开源协议要求，保留必要的版权与许可证声明。
