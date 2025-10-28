# 邮件助理 API 使用说明

为了支撑董事长收件箱的快速分流，本项目新增了 `MailAssistantModule`，使用 Fastbuild AI 对 Exchange Online 邮件进行临时摘要，避免在服务器端持久化正文或附件。

## 前提条件

- Azure AD 应用已经完成 OAuth 授权，客户端可以为每个用户获取 Microsoft Graph 访问令牌；
- Fastbuild AI 控制台中已配置可用的对话模型，并记录模型 ID；
- 在服务器环境变量中设置 `MAIL_ASSISTANT_MODEL_ID=<模型ID>`，或在请求体中显式传入 `modelId`。

## 接口信息

- 路径：`POST /api/mail-assistant/summary`
- 认证：继承 Web API 默认的认证机制；
- 请求体：

```json
{
  "accessToken": "Graph Access Token",
  "folders": [
    { "id": "inbox", "name": "Inbox" },
    { "id": "sentitems", "name": "Sent" }
  ],
  "top": 20,
  "includeRead": false,
  "modelId": "可选，覆盖默认模型"
}
```

> `folders` 可选，为空时默认读取 Inbox。`top` 控制每个文件夹的最大邮件数量（1-50）。

## 返回结构

```json
{
  "metadata": {
    "totalMessages": 12,
    "folders": [
      { "id": "inbox", "name": "Inbox", "messageCount": 12 }
    ],
    "processedMessageIds": ["...", "..."]
  },
  "summary": {
    "globalSummary": "……",
    "folderSummaries": [
      {
        "folderId": "inbox",
        "folderName": "Inbox",
        "summary": "……",
        "actionItems": ["……"],
        "urgentMessages": ["邮件ID1", "邮件ID2"]
      }
    ],
    "suggestedActions": ["……"],
    "followUpReminders": ["……"]
  },
  "rawResponse": "{...}"
}
```

其中 `rawResponse` 保留原始 AI 输出以便调试，客户端可酌情忽略。

## 开发者注意事项

- 服务器端仅接收访问令牌并实时调用 Graph，未把邮件正文写入数据库或磁盘；
- 若 AI 返回的 JSON 结构异常，会自动退化为纯文本摘要并写入 `summary.globalSummary`；
- 错误日志不会包含邮件内容，满足“不得持久化正文/附件”的要求；
- Chrome 扩展或其它客户端可直接调用该 API，并在本地缓存处理结果。

## 请求方式与环境变量

- **Graph 模式**：提交 `accessToken`，可选携带 `folders/top/includeRead/modelId`，后端会调用 Microsoft Graph 拉取邮件。
- **直接文本模式**：若任务窗格已经拿到正文，可仅提交 `subject/text/mode`（`mode` 支持 `thread-summary`、`actions` 等自定义值），不需要 Graph 令牌。
- **返回结果**：除结构化的 `metadata`/`summary` 外，额外提供 `result`（便于 Outlook 直接展示）以及 `latencyMs`/`latency_ms`。
- **CORS 设置**：在服务器环境配置
  ```bash
  SERVER_CORS_ENABLED=true
  SERVER_CORS_ORIGIN=https://localhost:3000
  ```
  发布时将 `ORIGIN` 换成正式域名。
- **任务窗格代理**：`docs/outlook-addin-starter/src/server.js` 读取以下变量将请求透传到后端：

  | 变量 | 说明 | 默认值 |
  | --- | --- | --- |
  | `MAIL_ASSISTANT_API_URL` | 后端接口地址 | `https://localhost:4090/api/mail-assistant/summary` |
  | `MAIL_ASSISTANT_API_KEY` | （可选）注入 `x-api-key` 鉴权头 | 空 |
  | `MAIL_ASSISTANT_API_BEARER` | （可选）注入 `Authorization: Bearer ...` | 空 |

  任务窗格仍向 `/summarize/text` 发请求，Express 服务会转发到上述 URL。若后端暂不可用，会自动回落到本地假摘要，便于离线演示。
