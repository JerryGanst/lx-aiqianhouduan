import { defineManifest } from "@crxjs/vite-plugin";

export default defineManifest({
    manifest_version: 3,
    name: "Fastbuild AI Assistant",
    version: "0.0.1",
    description: "Chrome extension entrypoint for Fastbuild AI.",
    action: {
        default_title: "Fastbuild AI Assistant",
        default_popup: "index.html"
    },
    permissions: ["storage"],
    host_permissions: []
});
