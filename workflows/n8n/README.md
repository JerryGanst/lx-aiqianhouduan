# FastbuildAI n8n Workflows

## Leave Link Dispatcher (`hook/leave-link`)

- **Flow**: Webhook → Function (JS) → Switch → Respond to Webhook
- **Import**: In n8n open *Workflows → Import from File* and pick `workflows/n8n/leave-link.workflow.json`.
- **Activation**: Keep response mode as `Last node` so the workflow returns the JSON crafted in the Function node.

### Required environment variables

- `N8N_LEAVE_JWT_SECRET`: HS256 secret used to verify the backend JWT supplied in the `Authorization: Bearer <token>` header.
- `N8N_LINK_SIGN_SECRET` *(or `N8N_LEAVE_LINK_SIGN_SECRET`)*: Secret used to produce the short-lived HMAC signature that is appended to the HR URL.

### Optional hardening

- `N8N_LEAVE_IP_WHITELIST`: Comma-separated list of source IPs (matching `x-forwarded-for`) that are allowed to hit the webhook.
- `N8N_LEAVE_JWT_ISS`: Comma-separated list of accepted JWT issuers.
- `N8N_LEAVE_JWT_AUD`: Comma-separated list of accepted JWT audiences.
- `N8N_LEAVE_LINK_TTL`: TTL in seconds for the generated link signature (defaults to 300 seconds).

### Request contract

```json
{
  "userId": "E123",
  "org": "SZ-OPS",
  "intent": "leave",
  "type": "annual",
  "locale": "zh-CN",
  "ts": 1700000000
}
```

### Sample success response

```json
{
  "title": "申请年假",
  "url": "https://hr.example.com/leave/annual?src=fastbuild-ai&trace=abc123&uid=E123&exp=1700000300&sig=...",
  "traceId": "abc123",
  "expiresAt": 1700000300
}
```

Branches handled in the Switch node today: `leave_annual`, `leave_sick`, `leave_marriage`, `leave_bereavement`, `business_trip`. Update the Function node maps if new intents/types are introduced.
