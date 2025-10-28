以下是您要给 CODE X 复制的完整文档，可直接粘贴进去。

⸻

Outlook Add-in (Office.js) 邮件 AI 摘要 项目文档

一、项目背景

董事长经常 2–3 天未查看邮箱，积压 1000 + 封未读邮件。
目标是在 Outlook 中集成 AI 助手，实现：
	•	邮件线程自动摘要；
	•	行动项提取；
	•	重要度排序；
	•	不落地、不存储原邮件正文或附件；
	•	可跨 Web、Windows、Mac、iOS 客户端一致运行。

最终目标：让高管在 Outlook 阅读窗或任务窗格内，一键得到摘要与行动建议，完全不离开邮箱。

⸻

二、项目用途与价值

功能	作用	目标指标
摘要生成	对长线程生成 3–5 条要点	阅读时间 ↓ 70 %
行动项识别	自动提取 TODO / 负责人 / 截止日	漏办率 ↓ > 60 %
重要度排序	结合发件人与内容权重	优先处理率 ↑ 50 %
零持久化	服务端仅存 metadata	满足 DLP / 隐私 合规
跨端一致	一套 Add-in 代码覆盖 Web/Win/Mac/iOS	运维成本 ↓ 90 %


⸻

三、目录结构

docs/outlook-addin-starter/
├─ manifest.xml                  # 加载项清单（SourceLocation=https://localhost:3000/taskpane.html）
├─ package.json                  # npm/pnpm 脚本与依赖
├─ src/
│  └─ server.js                  # HTTPS dev server（Express + self-signed cert）
└─ public/
   ├─ taskpane.html              # 任务窗格 UI (加载 Office.js)
   ├─ taskpane.js                # 前端逻辑：读取当前邮件 → 调 AI 接口 → 渲染结果
   ├─ commands.html              # Ribbon 命令（预留）
   ├─ icon-32.png / icon-80.png # 图标占位


⸻

四、运行步骤（macOS）
	1.	启动本地服务

cd docs/outlook-addin-starter
pnpm install        # 首次安装依赖
pnpm start          # 启动 https://localhost:3000

	2.	在 Outlook 加载 Add-in

	•	Outlook Web：齿轮 → 管理加载项 → 我的加载项 → 上传 manifest.xml。
	•	Outlook 桌面：文件 → 管理加载项 → 添加 → 选择 manifest.xml。

	3.	打开任意邮件 → 右侧任务窗格 → 点击“对当前邮件生成摘要”或“提取行动项”。

⸻

五、接入后端 mail-assistant API

修改 public/taskpane.js：

// 替换本地假接口
const res = await fetch('https://your-server/api/mail-assistant/summary', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ subject, text: body, mode })
});

后端要求：
	1.	CORS：允许来源 https://localhost:3000（开发）或正式 Add-in 域。
	2.	零持久化：正文/附件只在内存中处理；日志仅含 userId | mailIdHash | action | latency | status。
	3.	返回格式：

{ "result": "摘要内容", "latency_ms": 1234, "model": "mail-assistant" }


⸻

六、后续增强任务

A. 启用 SSO/NAA 获取 Graph 令牌
	1.	在 Azure 注册应用（单租户），记录 clientId。
	2.	在 manifest.xml 中增加：

<WebApplicationInfo>
  <Id>YOUR-AAD-APP-CLIENT-ID</Id>
  <Resource>api://YOUR-AAD-APP-CLIENT-ID</Resource>
  <Scopes><Scope>Mail.Read</Scope></Scopes>
</WebApplicationInfo>

	3.	前端使用 OfficeRuntime.auth.getAccessToken() ；
	4.	后端使用 OBO (On-Behalf-Of) 流交换 Graph 访问令牌；
	5.	调 /me/messages 、/attachments 接口实现 批量与附件功能。

B. 未读批量初筛 (≤ 200/批)
	•	Graph 接口：GET /me/messages?$filter=isRead eq false&$top=50 分页到 200；
	•	批量传输到 /summarize/batch ；AI 并发 ≈ 20；首批 P95 ≤ 50 s 返回。

C. 附件流式解析
	•	任务窗格增加“总结附件”按钮。
	•	通过 Graph 令牌 GET /attachments/$value (≤ 20 MB)；
	•	以流方式上传 /parse/attachment → 返回纯文本 → 再调用 /summarize/text?mode=attachment-summary。

⸻

七、验收指标（上线标准）

项目	目标值
单封摘要	P50 1–2 s / 封  P95 ≤ 5 s
批量初筛	≤ 200 封/批  首批 P95 ≤ 50 s
附件	PDF/DOCX/PPTX ≤ 20 MB  成功率 ≥ 99 %
安全	服务端 0 持久化；令牌 会话级 TTL ≤ 5 min
权限	最小 Mail.Read；启写入再 Mail.ReadWrite
稳定	Graph 429 退避 重试 成功率 ≥ 95 %


⸻

八、执行顺序（六步即可）
	1.	pnpm start → 侧载 manifest → 验证两按钮。
	2.	替换 /summarize/text 为 真实 API。
	3.	后端开放 CORS 与 POST 接口。
	4.	确认 返回 JSON 格式正确。
	5.	埋点 元数据 日志 验证 latency。
	6.	注册 Azure 应用 → 启 SSO/NAA → 实现 批量/附件。

⸻

九、关键数据约束
	•	性能 ：单封 P50 1–2 s ；批量 P95 ≤ 50 s。
	•	令牌 ：SSO/NAA 获取 Graph 令牌 ；Callback Token 有效期 ≤ 5 min。
	•	附件 ：PDF/DOCX/PPTX ≤ 20 MB ；流式 解析；不落盘。
	•	审计 ：仅记录 元数据 ； 日志脱敏。

⸻

十、未来扩展
	•	支持 “AI 智能回复 / 会议摘要 / 任务同步”。
	•	引入 Graph 事件触发器 自动推送 摘要日报。
	•	与 企业 知识库 RAG 联动，实现跨邮件检索与知识归档。

