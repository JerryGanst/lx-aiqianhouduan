# FastbuildAI 企业微信机器人快速接入指南

本文档说明如何在本地使用 Docker 启动 FastbuildAI，并通过 Cloudflare Tunnel 将服务暴露给企业微信以完成回调验证。

## 1. 准备环境变量

1. 复制示例文件：

```bash
cp docker/.env.wecom.example docker/.env.wecom.local
```

2. 编辑 `docker/.env.wecom.local`，填入企业微信后台的 `Token` 与 `EncodingAESKey`。如企业微信返回的回调载荷包含 `CorpID` 或 `receiveid`，可以填写在 `WECOM_BOT_RECEIVE_ID`，若为空则保持空字符串。
3. 若希望机器人主动回复，还需配置企业凭证：
   - `WECOM_BOT_CORP_ID`：企业微信 CorpID；
   - `WECOM_BOT_CORP_SECRET`：自建应用或智能机器人的 Secret；
   - （可选）`WECOM_BOT_API_BASE_URL`：企微 API 域名，默认为官方地址。

## 2. 启动 FastbuildAI

```bash
cd docker
docker compose --env-file .env.wecom.local up -d redis postgres nodejs nginx
```

> 首次启动会执行依赖安装与构建，耗时稍长。完成后后端监听在 `http://127.0.0.1:8080`（Nginx 代理到 Node 4090 端口）。

## 3. 暴露公网地址

若暂时没有正式域名，可使用 Cloudflare Tunnel：

```bash
cloudflared tunnel --url http://localhost:8080
```

命令会输出形如 `https://<随机子域>.trycloudflare.com` 的地址，供企业微信填写回调 URL。

## 4. 配置企业微信

在企业微信后台「API 设置」中：

- URL：`https://<随机子域>.trycloudflare.com/wecom/callback`
- Token / EncodingAESKey：填写与 `.env.wecom.local` 中一致的值

点击「验证」后应该返回成功。

## 5. 调试回调

FastbuildAI 在 `apps/server/src/modules/web/wecom-bot` 下提供基础的 GET/POST 回调控制器：

- GET `/wecom/callback`：校验 URL
- POST `/wecom/callback`：解密消息并暂时返回 `success`
- 已集成 `WecomBotSenderService`，在 `POST` 回调中会自动调用企业微信 `aibot/send` 接口向触发用户回复文本，确保 `.env` 中配置了 `WECOM_BOT_CORP_ID` 与 `WECOM_BOT_CORP_SECRET`。

日志记录位于 `docker/nodejs` 容器中，可以用 `docker logs -f fastbuildai-nodejs` 查看。其中已预留 `WecomBotService.encryptResponse` 方便后续接入 FastbuildAI 的模型能力。

## 6. 停止服务

```bash
docker compose --env-file .env.wecom.local down
```

如使用 Cloudflare Tunnel，记得退出对应进程。


