RAG 接口文档

 总览
差异整理：
 	1.	传输模型不同：现有接口全走 SSE 流（无标准 JSON 完整响应）；WeKnora 风格以 JSON/REST 为主，仅搜索返回一次性结果。直接替换会影响前端流式渲染与“进度/思考过程”显示。 ￼  ￼
	2.	职责边界不同：现有接口把检索+生成封在 /unified_chat//query 内；WeKnora 把知识入库/检索独立出来，不含对话生成，需要在网关/中台拼装“检索→LLM 生成”。 ￼  ￼
	3.	入库方式不同：现有“个人知识库”直接传本地文件路径数组（Demo 级）；WeKnora 提供正式入库端点（文件/URL、异步 Job、可查询状态）。 ￼  ￼
	4.	平台级要素：WeKnora 定义了鉴权/多租户/分页/错误码/速率限制/CORS；之前的API完全没接
	5.	替换工作：需要做一个兼容适配层，把旧前端期望的 SSE 事件流（process/streaming/final_answer）映射到“检索(JSON)+生成(流)”的组合；同时接入新的入库生命周期与 Job 轮询。

	•	Base URL（对外）：https://<your-host>（保持不变）
	•	传输：对外仍为 SSE；内部适配 WeKnora 使用 JSON/REST。现有事件类型与顺序保持一致。 ￼
	•	核心流程（use_personal_knowledge=true）：
入库（URL/文件，去重、可能异步）→ 混合检索（TopK + 阈值，可重排）→ LLM 生成答案 → SSE 回放。检索与入库由适配层调用 WeKnora 完成。 ￼

⸻

1）统一聊天接口（兼容层对外）

1.1 路径与方法

POST /unified_chat
Accept: text/event-stream
Content-Type: application/json

外形与事件流与旧版一致。 ￼

1.2 请求体（与旧版对比）

interface UnifiedChatRequest {
  user_id: string;
  messages: Array<{ role: "user"|"assistant"|"system"; content: string }>;
  model?: string;                    // 默认 "default"
  stream?: boolean;                  // 默认 true
  use_personal_knowledge?: boolean;  // 默认 false
  objects?: string[];                // 兼容旧“本地路径数组”（Demo）
  file?: string[];                   // 普通对话附加内容（不入库）
  agent_config?: AgentConfig;

  // ---------- 兼容层新增 ----------
  kb_id?: string;                    // 【新增】目标知识库；不传走默认库（支持“多库”能力预留） —— 支持指定知识域检索。 [oai_citation:5‡api.md](sediment://file_000000007bcc71faba45a3e16be6b839)
  urls?: string[];                   // 【新增】URL 入库源；用于替代 objects 的本地路径 —— 支持生产化在线文档入库。 [oai_citation:6‡api.md](sediment://file_000000007bcc71faba45a3e16be6b839)
  match_count?: number;              // 【新增】默认 6，范围 1–20 —— 支持控制 TopK。 [oai_citation:7‡api.md](sediment://file_000000007bcc71faba45a3e16be6b839)
  rerank?: boolean;                  // 【新增】默认 false —— 支持可选重排。 [oai_citation:8‡api.md](sediment://file_000000007bcc71faba45a3e16be6b839)
}

必填规则（兼容）
	•	【变更】个人知识库上传文件方式：
    1、 当 use_personal_knowledge=true 时，objects 或者 urls 传值
    2、 为False的时候。走file?: string[]这个判定逻辑，识别为不入库的临时文件，考虑复用不入库的分析逻辑---现在这套，解析好的文本塞进提示词里，简单粗暴/或者依然延用URL的处理方式，做个临时索引，定时删除或者缓存

1.3 SSE 返回（外形不变）
	•	通用对话/智能体（use_personal_knowledge=false）
仍为简化流：

data: {"content": "片段"}\n\n
...
data: {"content": "[DONE]"}\n\n

（与旧版一致。） ￼

	•	个人知识库 RAG（use_personal_knowledge=true）
事件类型与示例：

data: {"type":"process","content":"开始处理个人知识库…"}\n\n
data: {"type":"process","content":"已接收文档，解析/向量化中…(42%)"}\n\n   // 【增强约定】基于内部 Job 进度合成
data: {"type":"process","content":"检索完成，命中 6 条"}\n\n
data: {"type":"streaming","content":"根据公司制度…"}\n\n
...
data: {"type":"final_answer","content":"完整答案","sources":[
  {"document_id":"doc_123","document_title":"policy.pdf","page":5,"text":"…","score":0.87}
]}\n\n

（事件名字/结构与旧版一致。） ￼

1.4 业务行为（由兼容层实现）
	1.	入库阶段（仅在 use_personal_knowledge=true 且提供 urls 或 objects 时执行）
	•	URL 入库：逐个 urls 调用 【内部】 POST /knowledge-bases/{kb_id}/documents/url。通常异步，立即返回 202 + job_id。兼容层轮询 【内部】 GET /jobs/{job_id}，并将进度转成 process 事件输出。 ￼
	•	文件入库：若仍使用 objects（本地路径，仅 Demo），兼容层负责实际上传并调用 【内部】 POST /knowledge-bases/{kb_id}/documents/file（multipart）。 ￼
	•	去重处理：若服务端判定上传内容与库内已存在，则 【内部】 返回 409 duplicate_file。【增强约定】：该响应必须包含已存在的 document_id；兼容层据此输出一条 process 事件：“文档已存在，跳过入库”，并继续检索（不视为错误）。 ￼
	2.	检索阶段
	•	调用 【内部】 POST /knowledge-bases/{kb_id}/search/hybrid，参数取 match_count/rerank（未提供时用默认 6/false）。返回 chunk 命中（含 knowledge_id/content/score/metadata）。 ￼
	•	兼容层将命中条目映射为 final_answer.sources[]（字段映射见 §5）。 ￼
	3.	生成阶段
	•	使用命中片段 + 对话历史调用你们既有 LLM，边出边播 streaming；结束输出 final_answer。该形态与旧版保持一致。 ￼

1.5 典型请求（最小改动）

curl -X POST https://<your-host>/unified_chat \
  -H "Content-Type: application/json" \
  -d '{
    "user_id":"u123",
    "messages":[{"role":"user","content":"公司的请假流程是什么？"}],
    "use_personal_knowledge": true,
    "kb_id": "kb_hr",                 // 【新增】可选
    "urls": ["https://intra/hr/policy.pdf"],  // 【新增】可选
    "match_count": 6,                 // 【新增】可选
    "stream": true
  }'


⸻

2）企业知识库接口（兼容层对外）

2.1 路径与方法

POST /query
Accept: text/event-stream
Content-Type: application/json

2.2 请求体（与旧版对比）

interface QueryRequest {
  user_id: string;
  question: string;
  model?: string;      // 默认 "default"
  kb_id?: string;      // 【新增】目标知识库
  match_count?: number;// 【新增】TopK（默认 6）
  rerank?: boolean;    // 【新增】是否重排（默认 false）
}

以上新增参数用于把 WeKnora 的混合检索能力向上暴露（不传亦可使用默认）。 ￼

2.3 SSE 返回（外形不变）
	•	事件类型：process / reasoning / final_answer（与旧版一致）。检索与来源生成由兼容层完成。 ￼

⸻

3）智能体配置接口（对外不变）

POST /prompt_fill
Content-Type: application/json

该接口与旧版一致，不依赖 WeKnora。 ￼

⸻

4）错误与边界（对外表现）

场景	内部 WeKnora 行为	对外兼容层行为
文档重复	409 duplicate_file，【增强约定】响应体含 document_id	输出一条 process：“文档已存在，跳过入库”，继续检索
入库耗时	202 Accepted + job_id（需 GET /jobs/{job_id}）	连续输出 process 进度，完成后进入检索
非法格式/过大	unsupported_media_type / payload_too_large	输出 error 事件并终止
鉴权/限流	401/403/429（含 Retry-After 等）	输出 error 并在日志透传 trace_id

（WeKnora 标准错误模型与分页协议见其文档；兼容层对外仍是 SSE。） ￼

⸻

5）来源字段映射（命中 → sources）

WeKnora 命中字段	对外 final_answer.sources[]
knowledge_id	document_id
metadata.file_name（或二次查询）	document_title
metadata.page	page
content	text
score（0–1，越大越相关）	score

该映射由兼容层完成，保证前端无感。 ￼  ￼

⸻

6）【内部】适配层 ↔ WeKnora 对接规范

以下为兼容层与 WeKnora 的内部协议，便于后端对接与联调；客户端无需感知。

6.1 文件入库（含去重与文档 ID 透出）
	•	POST /knowledge-bases/{kb_id}/documents/file
Headers：X-API-Key, X-Tenant-ID；Content-Type: multipart/form-data。
表单：file(必填)、metadata(JSON 字符串)、enable_multimodal(bool)。 ￼

响应语义（统一 Envelope）
	•	200（≤30s 完成）或 202：

{
  "success": true,
  "data": { "document_id":"doc_123","status":"processing|ready","file_name":"contract.pdf" }
}


	•	409 duplicate_file：【增强约定】 为支持上层**“已存在即跳过”，当检测重复时，响应体必须包含已存在文档的 document_id**：

{
  "success": false,
  "code": "duplicate_file",
  "message": "File already exists in this knowledge base.",
  "details": { "document_id": "doc_123" },
  "trace_id": "..." 
}

该约定用于让兼容层在 SSE 中输出“文档已存在”并直接进入检索，无需再列表查询匹配。此行为建立在 WeKnora 的错误码与 Envelope 之上。 ￼

	•	其他错误：unsupported_media_type、payload_too_large、validation_error、server_error。 ￼

6.2 URL 入库（异步）
	•	POST /knowledge-bases/{kb_id}/documents/url
Body：{ "url": "http(s)://...", "enable_multimodal": false, "metadata": {...} }。
典型响应：202 + job_id。兼容层需轮询 GET /jobs/{job_id} 获取 state/progress，并据此合成 process 事件。 ￼

6.3 混合检索
	•	POST /knowledge-bases/{kb_id}/search/hybrid
Body：{ "query_text": "...", "match_count": 6, "vector_threshold": 0.3, "keyword_threshold": 0.1, "rerank": false }。
返回 data[] 为 chunk 列表（含 id/knowledge_id/content/score/match_type/metadata）。Score 越大越相关（0–1）。 ￼

6.4 任务查询
	•	GET /jobs/{job_id} → { "state":"queued|running|succeeded|failed", "progress": 0.0~1.0 }。 ￼

⸻

7）安全与速率（内部对接要点）
	•	所有内部调用均需 X-API-Key 与 X-Tenant-ID，并遵循 429 限流头（Retry-After 等）。 ￼
	•	兼容层应将 trace_id 写入服务日志，便于排障。 ￼

⸻

8）示例

8.1 /unified_chat（URL 入库 + RAG）

curl -N -X POST https://<your-host>/unified_chat \
  -H "Content-Type: application/json" \
  -d '{
    "user_id":"u123",
    "messages":[{"role":"user","content":"公司的请假流程是什么？"}],
    "use_personal_knowledge": true,
    "kb_id":"kb_hr",
    "urls":["https://intra/hr/policy.pdf"],
    "match_count":6,
    "stream": true
  }'

SSE 关键片段（可能顺序）：

data: {"type":"process","content":"已接收文档，解析/向量化中…(15%)"} 

data: {"type":"process","content":"文档已存在，跳过入库（doc_123）"}     // 来自 409 duplicate_file 的增强约定

data: {"type":"process","content":"检索完成，命中 6 条"}
data: {"type":"streaming","content":"根据公司制度…"}
data: {"type":"final_answer","content":"…","sources":[{...}]}

（事件形态与旧版说明一致。） ￼

8.2 【内部】重复文件入库（返回 document_id）

curl -X POST $BASE/knowledge-bases/kb_123/documents/file \
  -H "X-API-Key: $API_KEY" -H "X-Tenant-ID: $TENANT_ID" \
  -F "file=@./policy.pdf"

409 响应（增强约定）：

{
  "success": false,
  "code": "duplicate_file",
  "message": "File already exists in this knowledge base.",
  "details": { "document_id": "doc_123" },
  "trace_id": "b7f7a0d9..."
}

（基于 WeKnora 的错误模型，补充了 document_id 便于上层继续检索。） ￼

⸻

9）与旧文档的差异清单（最小改动）
	1.	/unified_chat
	•	【新增】 kb_id、urls、match_count、rerank 四个可选字段，用于知识库选择、URL 入库与检索控制。 ￼
	•	【变更】 use_personal_knowledge=true 时，objects 与 urls 二选一（旧版仅 objects）。 ￼
	•	【增强约定】 入库重复时不报错，SSE 输出“文档已存在”后继续检索。
	2.	/query
	•	【新增】 kb_id、match_count、rerank 三个可选字段，映射 WeKnora 的混合检索能力。 ￼
	3.	内部能力
	•	【内部】 将 GET 检索改为符合 WeKnora 的 POST /knowledge-bases/{kb_id}/search/hybrid（需 JSON body）。 ￼
	•	【内部】 入库异步化与 Job 轮询（202 + job_id / GET /jobs/{job_id}）。 ￼
	•	【增强约定】 409 duplicate_file 响应包含 details.document_id。


10）后续逐步工作（快速上线后改造）


阶段 1
	•	前端从 objects 迁移到 urls 或真实 文件上传（前端文件转存 → 兼容层转调 /documents/file）。 ￼
	•	建立统一错误映射与用户友好提示（429 超限、媒体类型不支持、大小超限）。 ￼
	•	在 SSE 首包写入 trace_id（日志串联）。

阶段 2
	•	增强来源：document_title/page/text 从 metadata 或二次查询补齐，可提供“点击下载原文”（内部 GET /documents/{id}/download）。 ￼
	•	支持跨库检索（内部 POST /search/hybrid + knowledge_base_ids[]）。 ￼
	•	在管理端提供“入库任务中心”（轮询 Job + 失败重试）。
