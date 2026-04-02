目前已经有一套微服务架构了，技术有网关、redis，nacos服务注册和配置中心、sentinel熔断和降级、seata分布式事务，RocketMQ队列，服务已经有了用户服务，所以现在需要新增ai-service和ppt-service两个服务，实现ai ppt的功能

### 1. AI 模型的接入与管理 (ai-service)

- **流式响应（SSE）：** 建议在 `ai-service` 中使用 **Spring WebFlux** 的 `WebClient`。由于 AI 生成 10 页 PPT 的内容可能需要较长时间，传统的同步 HTTP 会导致网关超时。通过 `SseEmitter` 把 AI 吐出的 JSON 片段实时传给前端，用户体验会提升一个档次。
- **Sentinel 限流：** 既然已经搭好了 Sentinel，一定要对 AI 接口做**热点参数限流**或**QPS 限流**。AI API 的 Token 是实打实的钱，防止被恶意盗刷。
- 这个服务是“耗钱大户”和“算力消耗者”，它完全不需要关心 PPT 长什么样，它只负责一件事：**和外部大语言模型（LLM）打交道。**
  - **核心职责：** Prompt 组装、外部 API 调用、流式数据解析。
  - **数据库依赖：** 几乎不需要连 MySQL。但需要重度依赖 **Redis**（用于缓存高频生成的通用问题）和 **Sentinel**（绝对的核心，防刷限流）。
  - **需要写的核心接口 (Controller)：**
    - `POST /api/ai/generate-outline`: 接收用户输入，返回 SSE 流（让前端实现打字机效果吐出大纲）。
    - `POST /api/ai/fill-template`: 接收确认后的大纲，组装成结构化 JSON 抛给前端。
  - **架构隔离的好处：** AI 接口的响应时间是不稳定的（可能 1 秒，可能 10 秒）。把它独立出来，即使大模型服务商那边宕机了，或者请求超时了，用户依然可以正常登录，依然可以打开并编辑历史 PPT，不会导致整个系统雪崩。

### 2. PPTist 的“后端化”改造(ppt-service)

- **数据持久化钩子：** PPTist 原生是在浏览器的 `localStorage` 中存数据。需要修改它的 `store/modules/slides.ts`（Pinia 模块），在关键操作（如添加页、修改文字、AI 生成结束）后，触发一个异步的 `saveProject` Action。
- **接入 MQ：** 前端频繁的保存操作不要直接写库。可以让后端接口把 PPT 的 JSON 丢进 RocketMQ，由 `project-service` 异步写入 MySQL。这样即使数据库在高并发时反应慢，前端也不会卡顿。
- 这个服务是“数据仓库”，属于典型的 I/O 密集型服务。前端 PPTist 里的所有增删改查动作，最终都会落到这里。
  - **核心职责：** 管理用户的 PPT 草稿、历史版本、模板骨架。
  - **数据库依赖：** 强依赖 **MySQL**。
  - **需要写的核心接口 (Controller)：**
    - `GET /api/ppt/templates`: 获取预设好的几套 PPTist JSON 模板（前端根据这个来让大模型填空）。
    - `POST /api/ppt/create`: 创建一个空的或基于模板的 PPT 项目记录。
    - `GET /api/ppt/list`: 获取当前用户的 PPT 历史列表（用于在控制台展示）。
    - `GET /api/ppt/{projectId}`: 打开某个具体项目时，拉取那个巨大的 JSON 字符串。
    - `POST /api/ppt/save`: 保存 PPT 状态。
  - **高阶优化点（结合 MQ）：** 刚才提到的那个 `save` 接口，前端在用户编辑时可能会频繁调用（自动保存）。建议这个接口收到前端传来的大 JSON 后，不要直接 `UPDATE` 数据库，而是直接扔进RocketMQ，然后立马给前端返回 `200 OK`。服务内部再写一个监听器，慢慢把队列里的 JSON 刷进 MySQL。

### 3. 设计模板骨架 (Template Skeletons)

- **解耦排版与内容：** 不要让 AI 去想坐标。应该定义一套自己的 **“逻辑版式库”**。
  - 例如：`Template_01` 代表“左图右书”，`Template_02` 代表“三点式排版”。
  - 这些模板以标准的 PPTist 元素对象（包含固定的 `left`, `top`, `width`, `height`）形式存在后端的资源目录或数据库中。

### 4. 编写 Prompt (提示词工程)

- **Few-Shot Prompting：** 在提示词里给 AI 一个“样板间”。告诉它：*“必须返回如下格式的 JSON，不要解释，不要带 markdown 代码块标记...”*。
- **结构化约束：** 让 AI 只负责输出 `title` (标题), `content` (正文列表), `key_words` (搜图关键词) 以及 `recommended_template` (建议版式)。

### 5. 组装渲染 (The Merger)

这是项目最关键的“业务逻辑层”。前端已经实现了一套极其精妙的解析与组装机制（详见 `src/hooks/useAIPPT.ts`），这极大地减轻了后端的压力。我们来看看这个核心数据流是怎么跑通的：

#### 5.1 数据流与映射机制

1. **用户输入**：用户在前端输入主题“人工智能发展史”。
2. **大纲生成（ai-service）**：前端调用 `/api/ai/generate-outline`，后端组装 Prompt 请求 LLM，LLM 吐出 Markdown。后端通过 SSE 流式返回给前端，前端打字机渲染大纲。
3. **大纲编辑与模板选择**：用户在前端微调大纲，并选择了一套模板（例如 `template_1`）。
4. **幻灯片内容生成（ai-service）**：前端将用户改好的 Markdown 大纲丢给后端 `/api/ai/generate-slides-content`，请求 LLM 按页生成。
   - **这里的核心约束是：让 LLM 输出毫无排版信息的纯数据 JSONL（即 `AIPPTSlide` 规范）。**
   - 比如一页“正文（content）”，LLM 只需要吐出类似：`{"type": "content", "data": {"title": "起源", "items": [{"title": "图灵测试", "text": "1950年提出"}]}}`。它不需要知道什么字号、什么颜色、放在什么坐标。
5. **数据组装与排版（前端魔法 `useAIPPT.ts`）**：
   - 这是 PPTist 前端最核心的机制。当它收到上述的那一条 JSON 时，它会去 **被选中的模板（如 `template_1`）** 里面找类型也是 `content` 的骨架页。
   - 找到骨架页后，前端通过读取模板元素中的 `textType` 属性（如 `title`, `itemTitle`, `item`, `itemNumber` 等）来识别“占位符”。
   - 前端提取占位符的属性（坐标 `left/top`、宽高 `width/height`、颜色、最大行数等），然后把 LLM 流输过来的纯数据塞进去。它甚至会自动调用 `getAdaptedFontsize` 进行**自适应缩放字体排版**，防止文本溢出！
6. **持久化保存（ppt-service）**：组装好带有坐标的完整 `Slide` 数据对象后，丢给前端画布引擎渲染。然后（如之前规划的），由前端触发异步 Action 将整份大文件交给 `ppt-service` 通过 RocketMQ 和 MySQL 完成保存、建档和落盘。



