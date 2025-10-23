<p align="center">
  <a href="http://nestjs.com/" target="blank"><img src="./apps/web/public/pwa-512x512.png" width="90" alt="Nest Logo" /></a>
</p>

<h1 align="center">FastbuildAI</h1>

<p align="center">
  Fast build your AI application
</p>

<p align="center">
  <a href="https://nestjs.com/"><img src="https://img.shields.io/badge/NestJS-11.x-ea2845" alt="NestJS" /></a>
  <a href="https://typeorm.io/"><img src="https://img.shields.io/badge/Typeorm-0.3.x-ef4100" alt="NestJS" /></a>
  <a href="https://www.postgresql.org/"><img src="https://img.shields.io/badge/PostgreSQL-17.x-29527d" alt="NestJS" /></a>
  <a href="https://www.typescriptlang.org/"><img src="https://img.shields.io/badge/TypeScript-5.x-3178c6" alt="TypeScript" /></a>
  <a href="https://turbo.build/"><img src="https://img.shields.io/badge/Turbo-2.x-6d5cb3" alt="Turbo" /></a>
  <a href="https://vuejs.org/"><img src="https://img.shields.io/badge/Vue.js-3.x-3aaf78" alt="Vue.js" /></a>
  <a href="https://vitejs.dev/"><img src="https://img.shields.io/badge/vite-6.x-646cff" alt="Vite" /></a>
  <a href="https://ui.nuxt.com/"><img src="https://img.shields.io/badge/NuxtUI-3.x-00b95f" alt="NuxtUI" /></a>
  <a href="https://nuxt.com/"><img src="https://img.shields.io/badge/NuxtJS-3.x-00b95f" alt="NuxtJS" /></a>
</p>

<p align="center">
<a href="http://ai.fastbuildai.com/" target="_blank">Demo Online</a>｜
<a href="https://www.fastbuildai.com/">Website</a>｜
<a href="./README.zh-CN.md">中文文档</a>
</p>

## Get Started

From the project root directory, run:

```bash
# Copy the example configuration file
cp .env.production.local.example .env.production.local

# Start the application using Docker
docker compose -p fastbuildai --env-file ./.env.production.local -f ./docker/docker-compose.yml up -d
```

Wait for **2–3 minutes** until all services are up and running.

Once started, you can access the application at:

```
http://localhost:4090
```

**Default Super Admin Account**  

- **Username:** `admin`  
- **Password:** `FastbuildAI&123456`  

## N8N Workflow Integration

1. Enable the workflow bridge in `.env.production.local`:

    ```env
    N8N_ENABLED=true
    N8N_WORKFLOW_USER_REGISTER_PATH=fastbuildai/user-registered
    # Optional security if your webhook requires it:
    # N8N_WEBHOOK_AUTH_HEADER=Authorization
    # N8N_WEBHOOK_AUTH_TOKEN=Bearer <token>
    ```

2. Boot the N8N service together with the stack:

    ```bash
    docker compose -p fastbuildai --env-file ./.env.production.local -f ./docker/docker-compose.yml up -d n8n
    ```

3. Visit `http://localhost:5678` (defaults: `admin` / `fastbuildai`) and import the sample workflow at `workflows/n8n/user-registered.workflow.json`.

4. Activate the workflow (optional) and register a new FastbuildAI user to emit the `user.registered` event into N8N. The workflow echoes the payload, ready for you to extend with downstream automations.

## Features

- ✅ **AI Chat** – Multi-model AI conversation.
- ✅ **MCP Invocation** – Supports Model Context Protocol (MCP).
- ✅ **User Recharge** – User balance and payment system.
- ✅ **Model Management** – Manage and deploy AI models.
- ✅ **Knowledge Base** – Centralized AI knowledge.
- ✅ **Intelligent Agents** – Autonomous agents for tasks.
- ⬜ **Workflow** – AI task automation.
- ⬜ **Plugin System** – Extend functionality with plugins.

## Screenshots

![image](./docs/screenshots/1.png)
![image](./docs/screenshots/2.png)
![image](./docs/screenshots/3.png)
![image](./docs/screenshots/4.png)
![image](./docs/screenshots/5.png)
![image](./docs/screenshots/6.png)
![image](./docs/screenshots/7.png)
![image](./docs/screenshots/8.png)
![image](./docs/screenshots/9.png)
![image](./docs/screenshots/10.png)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=FastbuildAI/FastbuildAI&type=Date)](https://www.star-history.com/#FastbuildAI/FastbuildAI&Date)

## License

[Apache License 2.0](./LICENSE)
