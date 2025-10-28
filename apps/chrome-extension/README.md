# Fastbuild AI Chrome Extension

This workspace package bootstraps the dependencies for building a Chrome extension that embeds the Fastbuild AI experience.

## Getting started

Install dependencies from the workspace root (already done if you ran `pnpm install` after adding this package):

```bash
pnpm install
```

Start the extension development server with hot reload:

```bash
pnpm --filter chrome-extension dev
```

The `@crxjs/vite-plugin` development server exposes an HMR-ready extension at `http://localhost:5173`. Visit `chrome://extensions`, enable *Developer mode*, then use *Load unpacked* and pick the temporary build directory that the dev server prints in the terminal when it starts.

Build a production-ready bundle:

```bash
pnpm --filter chrome-extension build
```

The generated `dist/` folder contains the final extension assets you can load manually or upload to the Chrome Web Store.
