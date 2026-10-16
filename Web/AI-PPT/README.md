# AI-PPT

基于 Vue 3、TypeScript 与 Spring Cloud 的智能 PPT 生成与在线编辑系统。项目在 PPTist 的 Web 幻灯片编辑能力基础上，接入后端 AI 生成、文档解析、项目管理、云端保存与 PPTX 导出能力，适合用于学习、课程汇报、产品方案、工作总结等演示文稿的快速创作。

## 项目简介

AI-PPT 面向“从主题或文档快速生成可编辑 PPT”的使用场景，支持用户输入主题、上传长文档或提供创作要求，由 AI 生成 PPT 大纲和幻灯片内容，再在 Web 编辑器中继续调整版式、文字、图片、图表、动画等元素，最终导出为 PPTX 文件。

项目包含前端编辑器和后端微服务能力：

- 前端：Vue 3 + TypeScript + Vite，基于 PPTist 二次开发。
- 后端：Spring Boot / Spring Cloud 微服务，提供 PPT 项目管理、AI 文本生成、AI 生图、文档转大纲、文件存储等接口。
- AI 能力：支持文本流式生成、文案润色/扩写/摘要/翻译、AI 生图、长文档转 PPT 大纲等能力。

## 功能特性

### AI 生成 PPT

- 根据主题生成 PPT 大纲
- 根据大纲流式生成幻灯片内容
- 支持长文档解析并生成 PPT 大纲
- 支持选择语言、风格、主题和模型
- 支持 AI 文案处理：润色、扩写、缩写、翻译、总结等
- 支持 AI 图片生成并插入幻灯片

### 在线 PPT 编辑

- 支持文字、图片、形状、线条、图表、表格、音视频、公式等常见元素
- 支持拖拽、缩放、旋转、多选、组合、图层调整、对齐与分布
- 支持富文本编辑、主题设置、背景设置、备注、动画、放映等能力
- 支持撤销/重做、快捷键、右键菜单、缩略图导航
- 支持移动端基础预览与编辑

### 项目管理

- 创建、保存、加载 PPT 项目
- 项目列表与详情查看
- 收藏项目
- 回收站、恢复与彻底删除
- 统计项目关联资源的存储占用
- 支持云端持久化保存 AI 生成结果和编辑内容

### 导入导出

- 导出 PPTX
- 导出 JSON、图片、PDF 等格式
- 支持 `.pptist` 格式导入导出
- 支持将编辑器中的页面内容保存为后端项目数据

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
- Dexie

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
- Redis
- MySQL
- Log4j2
- Apache PDFBox
- Apache POI

## 目录结构

```text
.
├── Web/
│   └── AI-PPT/                 # AI-PPT 前端工程
│       ├── src/                # 前端源码
│       ├── public/             # 静态资源
│       ├── package.json        # 前端依赖与脚本
│       └── vite.config.ts      # Vite 配置
├── service/
│   ├── ai-service/             # AI 能力服务：文本生成、生图、文档转 PPT
│   ├── ppt-service/            # PPT 服务：项目管理、生成编排、模板配置
│   └── file-service/           # 文件服务：上传、资源访问、存储统计
├── gateway/                    # API 网关
├── common/                     # 公共模块
├── api/                        # Feign/API 模块
└── pom.xml                     # 后端 Maven 父工程
```

## 快速开始

### 环境要求

- Node.js >= 20
- pnpm 或 npm
- JDK >= 17
- Maven >= 3.8
- MySQL
- Redis
- Nacos

### 启动前端

进入前端目录：

```bash
cd Web/AI-PPT
```

安装依赖：

```bash
pnpm install
```

启动开发服务：

```bash
pnpm dev
```

默认访问地址：

```text
http://127.0.0.1:5173/
```

如果使用 npm，也可以执行：

```bash
npm install
npm run dev
```

### 构建前端

```bash
pnpm build
```

构建产物会输出到前端工程的 `dist/` 目录。

### 启动后端

后端为 Spring Cloud 多模块工程，启动前请先准备好 MySQL、Redis、Nacos，并根据本地环境调整各服务的 `application-dev.yml` 或 Nacos 配置。

常用启动模块包括：

- `gateway`
- `service/ai-service`
- `service/ppt-service`
- `service/file-service`
- 用户认证相关服务

可在项目根目录执行 Maven 构建：

```bash
mvn clean package
```

也可以在 IDE 中分别启动对应服务的 Spring Boot 启动类。

## 环境配置

前端开发环境配置位于：

```text
Web/AI-PPT/.env.development
```

生产环境配置位于：

```text
Web/AI-PPT/.env.production
```

当前生产环境 API 基础路径示例：

```env
VITE_APP_BASE_API=/api
```

后端需要配置的常见内容包括：

- Nacos 服务发现与配置中心地址
- MySQL 数据库连接
- Redis 连接
- 文件上传与访问路径
- AI 模型供应商、模型名称、API Key 等配置

请不要将真实密钥、生产数据库密码等敏感信息提交到公开仓库。

## 核心接口能力

### PPT 服务

`ppt-service` 主要提供：

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

`ai-service` 主要提供：

- `GET /ai/models`：获取可用模型列表
- `POST /ai/writing`：AI 文本流式处理
- `POST /ai/document-to-ppt`：文档转 PPT 大纲
- `POST /ai/image`：AI 生图

## 常用命令

### 前端

```bash
pnpm dev
pnpm build
pnpm preview
pnpm lint
```

### 后端

```bash
mvn clean package
mvn clean install
```

## 使用流程

1. 启动后端基础服务、网关和 AI/PPT/File 相关服务。
2. 启动 `Web/AI-PPT` 前端工程。
3. 在首页创建 PPT 项目，或选择已有项目继续编辑。
4. 输入主题、文档或创作要求，生成 PPT 大纲。
5. 根据大纲生成幻灯片内容。
6. 在编辑器中调整版式、文字、图片、图表和动画。
7. 保存项目或导出为 PPTX 文件。

## 项目亮点

- 前后端分离，便于独立部署和扩展
- AI 生成过程采用流式返回，前端体验更接近实时生成
- PPT 内容以结构化 JSON 保存，便于二次编辑和持久化
- 支持主题生成、文档生成、编辑器手动创作等多种创作路径
- 保留 PPTist 强大的 Web 幻灯片编辑体验
- 后端通过微服务拆分 AI、PPT、文件等能力，职责清晰

## 注意事项

- 本项目包含 AI 能力，实际使用前需要配置可用的模型服务和密钥。
- PPTX 导出会尽量还原编辑器内容，但复杂样式与 Office 原生效果可能存在差异。
- 如果公开发布仓库，请检查 `.env`、配置文件、日志和上传目录，避免泄露敏感信息。
- 本项目基于 PPTist 二次开发，请遵守原项目的开源协议要求。

## 致谢与开源协议

本项目的前端编辑器基于 [PPTist](https://github.com/pipipi-pikachu/PPTist) 二次开发。PPTist 是一个优秀的 Web 幻灯片编辑/演示应用，提供了丰富的 PPT 编辑能力。

PPTist 使用 AGPL-3.0 License。基于其代码进行修改、分发或作为网络服务提供时，请严格遵守 AGPL-3.0 协议要求，保留原作者版权声明和许可证信息。

本仓库中的相关代码请以仓库内 `LICENSE` 文件及各模块声明为准。
