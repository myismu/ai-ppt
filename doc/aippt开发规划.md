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
  - `AnthropicAdapter`: 封装 WebClient 调用大模型的 Anthropic Messages 协议。
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

为了配合微服务架构落地，数据库将根据服务模块进行拆分。

### 4.1 `ppt-service` 库表设计

#### 1. `ppt_project` (用户 PPT 项目主表)
| 字段名 | 数据类型 | 约束/索引 | 说明 |
| :--- | :--- | :--- | :--- |
| `id` | bigint | 主键 | 项目唯一标识，建议雪花算法 |
| `user_id` | bigint | 普通索引 | 归属用户 ID (关联用户中心) |
| `title` | varchar(128) | - | PPT 标题 |
| `cover_url` | varchar(512) | - | 封面图地址 |
| `content_json` | longtext | - | 幻灯片完整 JSON 数据 (或者存 OSS URL) |
| `status` | tinyint(1) | - | 状态: 0-草稿, 1-已发布, 2-已归档, -1-已删除 |
| `create_time` | datetime | - | 创建时间 |
| `update_time` | datetime | - | 更新时间 |

#### 2. `ppt_template` (PPT 模板库表)
| 字段名 | 数据类型 | 约束/索引 | 说明 |
| :--- | :--- | :--- | :--- |
| `id` | bigint | 主键 | 模板 ID |
| `name` | varchar(64) | - | 模板名称 (如: 极简极夜黑) |
| `category` | varchar(32) | 字典索引 | 类别标识 (business, education 等) |
| `preview_url` | varchar(512) | - | 模板预览图 |
| `data_json` | longtext | - | 模板标准元素定义 JSON |
| `is_vip` | tinyint(1) | - | 是否付费/VIP 模板: 0-免费, 1-VIP |
| `sort_order` | int | - | 排序权重 |

#### 3. `ppt_material` (用户素材库表)
| 字段名 | 数据类型 | 约束/索引 | 说明 |
| :--- | :--- | :--- | :--- |
| `id` | bigint | 主键 | 素材 ID |
| `user_id` | bigint | 普通索引 | 用户 ID |
| `type` | varchar(16) | - | 素材类型: image, audio, video |
| `url` | varchar(512) | - | 资源实际地址 (OSS) |
| `size` | int | - | 文件大小 (字节) |
| `create_time` | datetime | - | 上传时间 |

```
-- ==========================================================
-- 数据库: ppt_db (ppt-service 对应数据库)
-- ==========================================================
CREATE DATABASE IF NOT EXISTS `ppt_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ppt_db`;

-- 1. 用户 PPT 项目主表
CREATE TABLE `ppt_project` (
  `id` bigint(20) NOT NULL COMMENT '项目唯一标识，建议雪花算法',
  `user_id` bigint(20) NOT NULL COMMENT '归属用户 ID (关联用户中心)',
  `title` varchar(128) NOT NULL DEFAULT '未命名演示文稿' COMMENT 'PPT 标题',
  `cover_url` varchar(512) DEFAULT NULL COMMENT '封面图地址',
  `content_json` longtext COMMENT '幻灯片完整 JSON 数据',
  `status` tinyint(2) NOT NULL DEFAULT '0' COMMENT '状态: 0-草稿, 1-已发布, 2-已归档, -1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户 PPT 项目主表';

-- 2. PPT 模板库表
CREATE TABLE `ppt_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '模板 ID',
  `name` varchar(64) NOT NULL COMMENT '模板名称 (如: 极简极夜黑)',
  `category` varchar(32) NOT NULL COMMENT '类别标识 (business, education 等)',
  `preview_url` varchar(512) DEFAULT NULL COMMENT '模板预览图 URL',
  `file_url` varchar(512) NOT NULL COMMENT '模板标准元素定义 JSON 文件所在的 OSS/静态资源 URL',
  `is_vip` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否付费/VIP 模板: 0-免费, 1-VIP',
  `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '排序权重',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PPT 模板库表';

-- 3. 用户素材库表
CREATE TABLE `ppt_material` (
  `id` bigint(20) NOT NULL COMMENT '素材 ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `type` varchar(16) NOT NULL COMMENT '素材类型: image, audio, video',
  `url` varchar(512) NOT NULL COMMENT '资源实际地址 (OSS)',
  `size` int(11) NOT NULL DEFAULT '0' COMMENT '文件大小 (字节)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户素材库表';

```



### 4.2 `ai-service` 库表设计

#### 1. `ai_generation_record` (AI 调用统计与审计表)
| 字段名 | 数据类型 | 约束/索引 | 说明 |
| :--- | :--- | :--- | :--- |
| `id` | bigint | 主键 | 记录 ID |
| `user_id` | bigint | 普通索引 | 用户 ID |
| `task_type` | varchar(32) | - | 任务类型: outline, content, polish, draw |
| `prompt` | text | - | 用户输入的原始指令 |
| `model_used` | varchar(32) | - | 使用的模型 (如 mimo-v2-pro) |
| `tokens_consumed`| int | - | 消耗的 Token 数量 (用于成本核算) |
| `status` | tinyint(1) | - | 状态: 0-失败, 1-成功 |
| `create_time` | datetime | 普通索引 | 调用时间 |

#### 2. `ai_prompt_template` (系统内置提示词表)
| 字段名 | 数据类型 | 约束/索引 | 说明 |
| :--- | :--- | :--- | :--- |
| `id` | bigint | 主键 | 提示词 ID |
| `scene_code` | varchar(32) | 唯一索引 | 场景编码 (如: outline_gen, img_prompt_enhance) |
| `system_prompt` | text | - | 系统级人设指令 |
| `user_prompt` | text | - | 用户输入模板 (含 ${变量} 占位符) |
| `is_active` | tinyint(1) | - | 是否启用 |



```
-- ==========================================================
-- 数据库: ai_db (ai-service 对应数据库)
-- ==========================================================
CREATE DATABASE IF NOT EXISTS `ai_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ai_db`;

-- 1. AI 调用统计与审计表
CREATE TABLE `ai_generation_record` (
  `id` bigint(20) NOT NULL COMMENT '记录 ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `task_type` varchar(32) NOT NULL COMMENT '任务类型: outline, content, polish, draw',
  `prompt` text NOT NULL COMMENT '用户输入的原始指令',
  `model_used` varchar(32) NOT NULL COMMENT '使用的模型 (如 mimo-v2-pro)',
  `tokens_consumed` int(11) NOT NULL DEFAULT '0' COMMENT '消耗的 Token 数量 (用于成本核算)',
  `status` tinyint(2) NOT NULL DEFAULT '1' COMMENT '状态: 0-失败, 1-成功',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 调用统计与审计表';

-- 2. 系统内置提示词表
CREATE TABLE `ai_prompt_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '提示词 ID',
  `scene_code` varchar(32) NOT NULL COMMENT '场景编码 (如: outline_gen, img_prompt_enhance)',
  `system_prompt` text COMMENT '系统级人设指令',
  `user_prompt` text NOT NULL COMMENT '用户输入模板 (含 ${变量} 占位符)',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用: 0-禁用, 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_code` (`scene_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统内置提示词表';

```



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