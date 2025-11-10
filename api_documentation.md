# RAG系统接口技术文档

## 目录
1. [系统架构概述](#系统架构概述)
2. [统一聊天接口](#1-统一聊天接口)
3. [企业知识库接口](#2-企业知识库接口)
4. [智能体配置接口](#3-智能体配置接口)
5. [数据模型定义](#4-数据模型定义)
6. [错误处理](#5-错误处理)
7. [接口调用示例](#6-接口调用示例)

---

## 系统架构概述

本系统提供了一套完整的RAG（Retrieval-Augmented Generation）解决方案，包含企业知识库、个人知识库和通用对话功能。

### 接口架构
- **企业知识库**：通过独立的 `/query` 接口提供服务
- **统一聊天接口**：通过 `/unified_chat` 接口统一处理：
  - 通用对话（普通聊天）
  - 个人知识库RAG
  - 智能体对话
- **智能体配置**：通过 `/prompt_fill` 接口优化智能体设定

### SSE（Server-Sent Events）通信机制

系统所有接口均采用SSE格式返回数据，实现服务端到客户端的实时流式传输。

#### SSE基本格式
```
data: {JSON数据}\n\n
data: {JSON数据}\n\n
data: {结束标记}\n\n
```

#### SSE数据包结构
每个数据包都以`data: `开头，后跟JSON格式的数据，以`\n\n`结尾。客户端需要：
1. 建立HTTP连接，设置Accept: text/event-stream
2. 逐行读取响应流
3. 解析以"data: "开头的行
4. 将行内容解析为JSON对象
5. 根据内容或类型字段处理数据

### 核心特性
- **统一SSE响应**：所有接口均通过SSE格式返回数据
- **灵活路由机制**：统一聊天接口根据参数自动选择处理模块
- **流式响应支持**：支持实时流式输出和一次性输出
- **智能体配置**：支持自定义AI助手角色和行为
- **Token限制管理**：自动检查和限制输入长度（默认8192 tokens）
- **多格式文档支持**：支持txt、pdf、pptx等格式

---

## 1. 统一聊天接口

### 接口地址
```
POST /unified_chat
```

### 功能描述
统一聊天接口整合了三大功能模块：
1. **通用对话**：普通AI对话，支持文件上传
2. **个人知识库RAG**：基于用户私有文档的问答
3. **智能体对话**：使用特定角色设定的AI助手

根据`use_personal_knowledge`参数自动路由到相应功能。

### 请求参数

| 参数名 | 类型 | 必填 | 默认值 | 描述 |
|--------|------|------|--------|------|
| user_id | string | 是 | - | 用户唯一标识（如用户工号） |
| messages | array | 是 | - | 对话历史消息列表 |
| model | string | 否 | "default" | 模型选择，支持"default"、"reasoning"等 |
| stream | boolean | 否 | true | 控制模型输出方式（true=流式，false=一次性） |
| use_personal_knowledge | boolean | 否 | false | 是否使用个人知识库 |
| objects | array | 条件必填 | null | 个人知识库文件列表（use_personal_knowledge=true时必填） |
| file | array | 否 | null | 普通对话的附加文件内容 |
| agent_config | object | 否 | null | 智能体配置信息 |

### 通用SSE响应格式

统一聊天接口根据不同的功能模式返回不同格式的SSE数据：

#### 1. 通用对话/智能体模式（use_personal_knowledge=false）
```
data: {"content": "文本片段1"}\n\n
data: {"content": "文本片段2"}\n\n
data: {"content": "文本片段3"}\n\n
data: {"content": "[DONE]"}\n\n
```

#### 2. 个人知识库RAG模式（use_personal_knowledge=true）
```
data: {"type": "process", "content": "处理进度信息"}\n\n
data: {"type": "streaming", "content": "答案片段"}\n\n
data: {"type": "final_answer", "content": "完整答案", "sources": [...]}\n\n
data: {"type": "error", "content": "错误信息"}\n\n
```

### 使用场景示例

#### 场景1：普通对话

**请求示例：**
```json
{
    "user_id": "用户id",
    "messages": [
        {"role": "user", "content": "什么是机器学习？"}
    ],
    "model": "default",
    "stream": true,
    "use_personal_knowledge": false
}
```

**响应说明：**
通用对话模式下，系统返回简化的流式响应格式：
- 数据格式：`data: {"content": "文本片段"}\n\n`
- 结束标记：`data: {"content": "[DONE]"}\n\n`

#### 场景2：带文件的对话

**请求示例：**
```json
{
    "user_id": "用户id",
    "messages": [
        {"role": "user", "content": "请分析这份数据"}
    ],
    "file": ["CSV数据内容：\nDate,Sales\n2024-01,1000\n2024-02,1200"],
    "use_personal_knowledge": false
}
```

**响应说明：**
与场景1相同，文件内容会被自动附加到用户消息中处理。

#### 场景3：个人知识库RAG

**请求示例：**
```json
{
    "user_id": "用户id",
    "messages": [
        {"role": "user", "content": "公司的请假流程是什么？"}
    ],
    "use_personal_knowledge": true,
    "objects": [
        "/local/documents/company_policy.pdf",
        "/local/documents/hr_handbook.txt"
    ],
    "stream": true
}
```

**响应说明：**
个人知识库模式下，系统返回更详细的响应类型：

| 响应类型 | 说明 | 示例 |
|---------|------|------|
| process | 处理进度信息 | `data: {"type": "process", "content": "开始加载个人知识库文档..."}\n\n` |
| streaming | 流式输出的答案片段 | `data: {"type": "streaming", "content": "根据文档"}\n\n` |
| final_answer | 最终完整答案及来源 | `data: {"type": "final_answer", "content": "完整答案", "sources": [...]}\n\n` |
| error | 错误信息 | `data: {"type": "error", "content": "文件加载失败"}\n\n` |

**完整响应流程示例：**
```
data: {"type": "process", "content": "开始加载个人知识库文档..."}\n\n
data: {"type": "process", "content": "文档加载完成，用时 1.2s"}\n\n
data: {"type": "process", "content": "正在构建向量数据库..."}\n\n
data: {"type": "process", "content": "向量数据库构建完成，用时 2.3s"}\n\n
data: {"type": "process", "content": "正在检索相关文档..."}\n\n
data: {"type": "process", "content": "文档检索完成，用时 0.5s"}\n\n
data: {"type": "process", "content": "整合检索信息到对话上下文..."}\n\n
data: {"type": "process", "content": "开始生成答案..."}\n\n
data: {"type": "streaming", "content": "根据"}\n\n
data: {"type": "streaming", "content": "文档内容，"}\n\n
data: {"type": "streaming", "content": "请假流程如下..."}\n\n
data: {"type": "process", "content": "答案生成完成，用时 3.5s"}\n\n
data: {"type": "final_answer", "content": "完整答案内容", "sources": [{"document_id": "0", "document_title": "company_policy.pdf", "page": 5, "text": "相关文本...", "score": -1.0}]}\n\n
data: {"type": "process", "content": "本次查询总耗时 7.5s"}\n\n
```

#### 场景4：智能体对话

**请求示例：**
```json
{
    "user_id": "用户id",
    "messages": [
        {"role": "user", "content": "帮我分析销售数据"}
    ],
    "agent_config": {
        "agent_name": "销售分析师",
        "agent_setting": "你是一位专业的销售数据分析师，擅长从销售数据中发现趋势和机会",
        "agent_description": "专业销售数据分析服务"
    },
    "use_personal_knowledge": false
}
```

**响应说明：**
智能体模式下的响应格式与场景1相同，但回答会基于设定的角色和专业知识。

### 个人知识库处理流程
当`use_personal_knowledge=true`时，系统执行以下流程：

1. **文档加载**：从本地指定路径加载文档（Demo环境）
2. **向量化处理**：将文档切分为chunks并向量化
3. **构建索引**：创建临时向量数据库
4. **相似度检索**：根据用户问题检索相关片段
5. **答案生成**：结合检索结果和对话历史生成答案

支持的文件格式：
- `.txt` - 纯文本文件
- `.pdf` - PDF文档  
- `.pptx` - PowerPoint演示文稿

*注：Demo环境下，文档从本地路径加载，无需MinIO等对象存储服务。*

---

## 2. 企业知识库接口

### 接口地址
```
POST /query
```

### 功能描述
企业知识库RAG接口，用于查询企业内部知识库，支持完整的RAG流程。

### 请求参数

| 参数名 | 类型 | 必填 | 默认值 | 描述 |
|--------|------|------|--------|------|
| user_id | string | 是 | - | 用户唯一标识 |
| question | string | 是 | - | 用户问题 |
| model | string | 否 | "default" | 模型选择 |

### SSE响应格式

**响应类型说明：**

| 响应类型 | 说明 | 使用阶段 |
|---------|------|----------|
| process | 处理进度和中间结果 | 全流程 |
| reasoning | 模型思考过程 | 仅reasoning模型 |
| final_answer | 最终答案及来源 | 流程结束 |

**完整响应流程示例：**
```
data: {"type": "process", "content": "开始处理请求..."}\n\n
data: {"type": "process", "content": "[Start] 开始对问题进行分类..."}\n\n
data: {"type": "process", "content": "[End] 问题的类别为：1: 技术问题\n分类原因：用户询问技术相关内容"}\n\n
data: {"type": "process", "content": "[Start] 开始对问题进行优化..."}\n\n
data: {"type": "process", "content": "[Result] 优化后的问题: 如何配置公司VPN连接？"}\n\n
data: {"type": "process", "content": "[Result] 需要采集的信息: \n  - VPN配置步骤\n  - 所需软件\n  - 认证方式"}\n\n
data: {"type": "process", "content": "[End] 已完成问题优化"}\n\n
data: {"type": "process", "content": "[Start] 开始第 1 轮查询..."}\n\n
data: {"type": "process", "content": "[Start] 检索资料库..."}\n\n
data: {"type": "process", "content": "[Result] 取回资料: \n  - ContextSource(Document: VPN配置指南.pdf, Page: 3, Score: 0.95)"}\n\n
data: {"type": "process", "content": "[End] 完成资料库检索"}\n\n
data: {"type": "process", "content": "[Start] 开始总结答案..."}\n\n
data: {"type": "process", "content": "[Result] 生成答案: VPN配置步骤如下..."}\n\n
data: {"type": "process", "content": "[End] 已完成总结答案"}\n\n
data: {"type": "reasoning", "content": "思考过程..."}\n\n
data: {"type": "final_answer", "content": "最终答案内容", "sources": [{"document_id": "doc_001", "document_title": "VPN配置指南.pdf", "page": 3, "text": "相关文本...", "score": 0.95}]}\n\n
```

### 处理流程
1. **问题分类**：判断问题类型和所属领域
2. **问题优化**：优化用户问题，提取关键信息点
3. **上下文检索**：从企业知识库检索相关文档
4. **答案生成**：基于检索结果生成专业答案

---

## 3. 智能体配置接口

### 接口地址
```
POST /prompt_fill
```

### 功能描述
智能体配置优化接口，用于生成和优化AI助手的角色设定。系统会自动：
- 优化用户提供的智能体设定
- 自动生成合适的智能体名称（如未提供）
- 自动生成简洁的智能体描述（如未提供）

### 请求参数

| 参数名 | 类型 | 必填 | 默认值 | 描述 |
|--------|------|------|--------|------|
| agent_name | string | 否 | 自动生成 | 智能体名称 |
| agent_setting | string | 是 | - | 智能体设定描述 |
| agent_description | string | 否 | 自动生成 | 智能体简介（20-40字） |

### 响应格式（JSON）

```json
{
    "agent_name": "数据分析专家",
    "agent_setting": "- 角色: 数据分析专家\n- 背景: 用户在面对复杂的数据集时...\n- 身份: 你是一位在数据分析领域...\n- 技能: 你具备强大的数据处理能力...\n- 目标: 为用户提供精准的数据分析结果...\n- 限制: 你应确保分析过程的科学性...\n- 输出结构: 以结构化的报告形式呈现...\n- 工作流:\n  1. 明确用户需求\n  2. 收集和整理数据\n  3. 应用数据分析方法\n  4. 解读分析结果",
    "agent_description": "专业的数据分析和可视化服务，提供深度洞察"
}
```

---

## 4. 数据模型定义

### 请求数据模型

#### UnifiedChatRequest - 统一聊天请求
```typescript
interface UnifiedChatRequest {
    user_id: string;                    // 用户唯一标识
    messages: Array<{                   // 对话历史
        role: string;                   // "user" | "assistant" | "system"
        content: string;                // 消息内容
    }>;
    model?: string;                     // 模型选择，默认"default"
    stream?: boolean;                   // 流式输出，默认true
    use_personal_knowledge?: boolean;   // 是否使用个人知识库，默认false
    objects?: string[];                 // 个人知识库文件列表
    file?: string[];                    // 普通对话的文件内容
    agent_config?: AgentConfig;         // 智能体配置
}
```

#### QueryRequest - 企业知识库查询请求
```typescript
interface QueryRequest {
    user_id: string;                    // 用户唯一标识
    question: string;                   // 用户问题
    model?: string;                     // 模型选择，默认"default"
}
```

#### AgentConfig - 智能体配置
```typescript
interface AgentConfig {
    agent_name?: string;                // 智能体名称，默认自动生成
    agent_setting: string;              // 智能体设定（必填）
    agent_description?: string;         // 智能体描述，默认自动生成
}
```

### 响应数据模型

#### StreamResponse - 流式响应（企业知识库/个人知识库）
```typescript
interface StreamResponse {
    type: string;                       // 响应类型："process" | "streaming" | "reasoning" | "final_answer" | "error"
    content: string;                    // 响应内容
    sources?: ContextSource[];          // 来源信息（可选）
}
```

#### ChatStreamResponse - 聊天流式响应（通用对话）
```typescript
interface ChatStreamResponse {
    content: string;                    // 响应内容片段或"[DONE]"结束标记
}
```

#### ContextSource - 上下文来源
```typescript
interface ContextSource {
    document_id: string;                // 文档ID
    document_title: string;             // 文档标题
    page?: number;                      // 页码（可选）
    text?: string;                      // 文本内容（可选）
    score?: number;                     // 相关性分数，默认-1.0
}
```

#### ChatMessage - 对话消息
```typescript
interface ChatMessage {
    role: string;                       // "system" | "user" | "assistant"
    content: string;                    // 消息内容
}
```

---

## 5. 错误处理

### 常见错误响应

#### Token限制错误
```json
{
    "error": "输入token超过限制",
    "token_count": 12000,
    "token_limit": 8192
}
```

#### 文件格式错误
```json
{
    "error": "仅支持 txt / pdf / pptx 文件"
}
```

#### 参数缺失错误
```json
{
    "error": "启用个人知识库时必须提供 objects 参数"
}
```

#### 文件加载失败（SSE格式）
```
data: {"type": "error", "content": "文件 /local/documents/file.pdf 加载失败: File not found"}\n\n
```

#### 服务器内部错误
```json
{
    "error": "服务器内部错误，请稍后再试"
}
```

---

## 6. 接口调用示例

### 场景1：普通对话（统一聊天接口）

```bash
curl -X POST http://127.0.0.1:20000/unified_chat \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "test_user_123",
    "messages": [
      { "role": "user", "content": "什么是机器学习？" }
    ],
    "model": "default"
  }'
```

**SSE响应处理伪代码：**
```
function handleChat(url, payload):
    response = http.post(url, payload, stream=true)
    
    for line in response.stream_lines():
        if line.startswith("data: "):
            data = json.parse(line[6:])
            
            if data.content == "[DONE]":
                break
            else:
                appendToOutput(data.content)
```

### 场景2：带文件的对话

```bash
curl -X POST http://127.0.0.1:20000/unified_chat \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "用户id",
    "messages": [{"role": "user", "content": "请分析这份数据"}],
    "file": ["Date,Sales\n2024-01,1000\n2024-02,1200"],
    "use_personal_knowledge": false,
    "stream": true
  }'
```

### 场景3：个人知识库RAG

```bash
curl -X POST "http://127.0.0.1:20000/unified_chat" \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "test_user_123",
    "messages": [
      { "role": "user", "content": "怎么使用langgraph开发？" }
    ],
    "model": "default",
    "stream": true,
    "use_personal_knowledge": true,
    "objects": [
      "routes/personal_knowledge_base/demo_files/model_fine_tuning.txt",
      "routes/personal_knowledge_base/demo_files/研究 LangGraph 的综合指南_.pdf"
    ],
    "file": null
  }'
```

**SSE响应处理伪代码：**
```
function handlePersonalRAG(url, payload):
    response = http.post(url, payload, stream=true)
    
    for line in response.stream_lines():
        if line.startswith("data: "):
            data = json.parse(line[6:])
            
            switch data.type:
                case "process":
                    showProgress(data.content)
                case "streaming":
                    appendToOutput(data.content)
                case "final_answer":
                    displayAnswer(data.content)
                    displaySources(data.sources)
                case "error":
                    handleError(data.content)
```

### 场景4：智能体对话

```bash
curl -X POST http://127.0.0.1:20000/unified_chat \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "用户id",
    "messages": [{"role": "user", "content": "我不开心"}],
    "agent_config": {
        "agent_name": "安慰大师",
        "agent_setting": "你是一位专业的心理学家",
        "agent_description": "你是一位专业的心理学家，擅长安慰人"
    },
    "use_personal_knowledge": false,
    "stream": true
  }'
```

### 企业知识库查询

```bash
curl -X POST http://127.0.0.1:20000/query \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "用户id",
    "question": "deepseek-r1使用了什么架构？",
    "model": "default"
  }'
```

**SSE响应处理伪代码：**
```
function handleEnterpriseQuery(url, payload):
    response = http.post(url, payload, stream=true)
    
    for line in response.stream_lines():
        if line.startswith("data: "):
            data = json.parse(line[6:])
            
            switch data.type:
                case "process":
                    showProgress(data.content)
                case "reasoning":
                    showThinking(data.content)
                case "final_answer":
                    displayAnswer(data.content)
                    displaySources(data.sources)
```

### 智能体配置生成

```bash
curl -X POST http://127.0.0.1:20000/prompt_fill \
  -H "Content-Type: application/json" \
  -d '{
    "agent_setting": "法律顾问，精通公司法和劳动法"
  }'
```

### 注意事项

1. **Token管理**：单条消息不超过8192 tokens
2. **文件大小**：个人知识库单个文件建议不超过10MB
3. **并发限制**：建议单用户并发请求不超过5个
4. **超时设置**：流式请求建议设置30秒超时
5. **重试策略**：网络错误时建议指数退避重试
6. **SSE连接**：保持长连接，处理网络中断重连

---

## 联系与支持

如需技术支持或有任何问题，请联系技术团队。 