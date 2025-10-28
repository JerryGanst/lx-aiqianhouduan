
import fs from "fs";
import https from "https";
import express from "express";
import selfsigned from "selfsigned";
import serveStatic from "serve-static";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = process.env.PORT || 3000;
const MAIL_ASSISTANT_API_URL =
  process.env.MAIL_ASSISTANT_API_URL || "https://localhost:4090/api/mail-assistant/summary";
const MAIL_ASSISTANT_API_KEY = process.env.MAIL_ASSISTANT_API_KEY;
const MAIL_ASSISTANT_API_BEARER = process.env.MAIL_ASSISTANT_API_BEARER;

// Basic security headers
app.use((req, res, next) => {
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("X-Frame-Options", "SAMEORIGIN");
  res.setHeader("Referrer-Policy", "no-referrer");
  next();
});

// Serve static files (taskpane.html, taskpane.js, commands.html)
app.use(serveStatic(path.join(__dirname, "../public"), { index: ["taskpane.html"] }));

// Simple placeholder AI endpoint for local testing (echo summary)
app.use(express.json({ limit: "2mb" }));
app.post("/summarize/text", async (req, res) => {
  const { subject = "", text = "", mode = "thread-summary", accessToken = "", modelId } =
    req.body || {};
  const startedAt = Date.now();

  try {
    const response = await fetch(MAIL_ASSISTANT_API_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(MAIL_ASSISTANT_API_KEY ? { "x-api-key": MAIL_ASSISTANT_API_KEY } : {}),
        ...(MAIL_ASSISTANT_API_BEARER ? { Authorization: `Bearer ${MAIL_ASSISTANT_API_BEARER}` } : {}),
      },
      body: JSON.stringify({
        subject,
        text,
        mode,
        modelId,
        accessToken,
      }),
    });

    const data = await response.json();
    const latency =
      data.latencyMs ??
      data.latency_ms ??
      Math.max(Date.now() - startedAt, 0);

    res.status(response.status).json({
      ...data,
      result:
        data.result ||
        data.summary?.globalSummary ||
        data.summary?.folderSummaries?.[0]?.summary ||
        "",
      latencyMs: latency,
      latency_ms: latency,
      model: data.model || "mail-assistant",
    });
  } catch (error) {
    const preview = text.slice(0, 400).replace(/\s+/g, " ").trim();
    const fallback = `【${mode}】Subject: ${subject.slice(0, 80)}\nPreview: ${preview}`;
    res.status(502).json({
      error: "MAIL_ASSISTANT_UNREACHABLE",
      message: error.message,
      result: fallback,
      latency_ms: Date.now() - startedAt,
      model: "local-fallback",
    });
  }
});

// Create or load self-signed cert for localhost dev
const certDir = path.join(__dirname, "../.cert");
const keyPath = path.join(certDir, "key.pem");
const certPath = path.join(certDir, "cert.pem");

if (!fs.existsSync(certDir)) fs.mkdirSync(certDir, { recursive: true });
let attrs = [{ name: "commonName", value: "localhost" }];
let pems;
if (!fs.existsSync(keyPath) || !fs.existsSync(certPath)) {
  pems = selfsigned.generate(attrs, { days: 365, keySize: 2048 });
  fs.writeFileSync(keyPath, pems.private);
  fs.writeFileSync(certPath, pems.cert);
  console.log("Generated self-signed certificate for https://localhost:" + PORT);
} else {
  pems = { private: fs.readFileSync(keyPath), cert: fs.readFileSync(certPath) };
}

https.createServer({ key: pems.private, cert: pems.cert }, app).listen(PORT, () => {
  console.log(`Outlook Add-in dev server running at https://localhost:${PORT}`);
  console.log(`Proxying summaries to ${MAIL_ASSISTANT_API_URL}`);
});
