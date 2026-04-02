# 基于 LLM 的全栈智能 PPT 生成系统 - 微服务架构开发规划

## 一、 系统架构总览
系统基于 Spring Cloud Alibaba 微服务体系构建，旨在将前端 PPTist 的交互能力与后端 AI 大模型的生成能力深度结合。

### 1.1 技术栈现状
- **网关层**: Spring Cloud Gateway (路由转发、权限校验、限流)
- **服务治理**: Nacos (服务注册/发现、配置中心)
- **流量防护**: Sentinel (AI 接口熔断、防刷)
- **分布式事务**: Seata (涉及多服务资源分配时使用)
- **异步通信**: RocketMQ (图片异步生成通知、大数据量导出任务)
- **缓存/原子操作**: Redis (AI Token 消耗统计、生成状态追踪)
- **持久化**: MySQL (用户文档、模板元数据)

---

## 二、 服务定义与职责

### 2.1 `ai-service` (AI 能力调度中心)
**核心职责**：屏蔽底层大模型协议差异，为全系统提供统一的 AI 指令服务。
- **大模型适配器 (Adapter Pattern)**：
  - `XiaomiMimoAdapter`: 封装 WebClient 调用小米 MIMO 的 Anthropic Messages 协议。
  - `DoubaoImageAdapter`: 封装对字节跳动（火山引擎）生图接口的 REST 调用。
- **流式响应中转**：利用 Spring Boot `SseEmitter` 将大模型的流式文本中转至前端。
- **Prompt 管理**：统一维护“大纲生成”、“内容扩写”、“提示词工程化转换”等系统级 Prompt。
- **功能接口**：
  - `/ai/outline`: 生成逻辑大纲。
  - `/ai/content`: 根据大纲生成 PPT 结构化 JSONL。
  - `/ai/writing`: 文本润色、扩写、摘要。
  - `/ai/image`: 接收主题词，返回 AI 生成的图片 Base64 或本地 URL。

### 2.2 `ppt-service` (PPT 业务中心)
**核心职责**：负责 PPT 项目的生命周期管理与结构化数据处理。
- **文档管理**：用户保存、历史版本、协作分享逻辑。
- **模板引擎**：维护 PPTist 兼容的模板库（JSON 格式），根据 AI 生成的内容进行“内容-模板”组装。
- **资源处理**：
  - 接收 AI 生成的 Base64 图片，存储至云服务器本地磁盘或文件服务，映射为静态 URL。
- **功能接口**：
  - `/ppt/create`: 创建生成任务。
  - `/ppt/save`: 保存当前幻灯片 JSON。
  - `/ppt/templates`: 获取系统内置模板列表。

---

## 三、 核心业务流设计

### 3.1 异步生图协同流
由于 AI 生图耗时较长（5-15s），采用“占位-异步替换”方案：
1. `Gateway` 接收请求，转发至 `ai-service`。
2. `ai-service` 同步返回一个“生成中”状态或占位 URL。
3. `ai-service` 异步调用豆包接口，完成后将图片 Base64 写回 Redis 或通过推送通知。
4. 前端根据 `PPTElementID` 自动替换占位图。

### 3.2 文本流式处理 (SSE)
1. 前端通过网关发起 SSE 请求。
2. `Gateway` 配置 `Cross-Origin-Resource-Policy`。
3. `ai-service` 使用 `WebClient` 非阻塞请求小米 API，并持续推送 `Data` 给前端。

---

## 四、 数据库设计 (核心表)

### 4.1 `ppt_project` (项目表)
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | bigint | 主键 |
| user_id | bigint | 用户关联 ID |
| title | varchar | 项目标题 |
| cover_url | varchar | 封面缩略图 |
| content_json | longtext | 存储 PPTist 标准的幻灯片数组 JSON |
| status | tinyint | 0:草稿, 1:已完成 |

### 4.2 `ppt_template` (模板表)
| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| id | bigint | 模板 ID |
| name | varchar | 模板名称 |
| category | varchar | 教育/商务/简约 |
| data_json | longtext | 模板的标准元素定义 |

---

## 五、 开发路线图 (Milestones)

### 第一阶段：基础设施搬迁 (Current Target)
- [ ] 在 `ai-service` 中实现小米大模型 Anthropic 协议的 Java 实现（推荐使用 WebFlux/WebClient）。
- [ ] 在网关配置后端接口的路由映射，确保前端请求能正确进入 Java 服务。
- [ ] 迁移文本润色 `/ai/writing` 接口。

### 第二阶段：数据落地与生图增强
- [ ] 实现 `ppt-service` 的持久化逻辑，对接 MySQL 保存用户 PPT。
- [ ] 接入豆包生图 API，并在 Java 后端完成图片转 Base64 的中转操作以解决跨域。

### 第三阶段：运维与优化
- [ ] 接入 Sentinel 控制 AI 接口的单位时间请求频次。
- [ ] 利用 Redis 缓存高频请求的 Prompt 结果。


给 Java 开发同事的特别说明：
流式接口注意：Java 后端建议使用 SseEmitter 或者反应式编程 Flux。网关（Gateway）层必须配置对 SSE 的支持，防止响应被网关缓存拦截。
小米 API 特点：它是标准的异步 HTTP，但在 Java 里调用时，注意它的 Header 需要带上 anthropic-version 和 x-api-key。此外，流式解析需要处理 event: message_stop 这种特定的结束标志。
生图逻辑：生图接口是普通 Post，但返回的是外链 URL。你在 Java 后端需要做一个 HttpClient 下载动作，拿到字节流后转 Base64，否则前端 Canvas 无法操作图片。