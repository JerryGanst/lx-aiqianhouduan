import { fileURLToPath, URL } from "node:url";

import { crx } from "@crxjs/vite-plugin";
import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";

import manifest from "./manifest.config";

export default defineConfig({
    plugins: [vue(), crx({ manifest })],
    resolve: {
        alias: {
            "@": fileURLToPath(new URL("./src", import.meta.url))
        }
    },
    build: {
        outDir: "dist"
    }
});
