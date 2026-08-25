import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import { buildRuntimeConfig } from "./src/config/runtimeConfig.js";

export default defineConfig(({ command, mode, isSsrBuild }) => {
  const env = loadEnv(mode, process.cwd(), "");

  if (command === "build" && !isSsrBuild && env.VITE_API_BASE_URL) {
    buildRuntimeConfig({
      apiBaseUrl: env.VITE_API_BASE_URL,
      websocketUrl: env.VITE_WS_URL,
      enableAdminDemo: env.VITE_ENABLE_ADMIN_DEMO === "true",
      appOrigin: "https://chat.example.com",
      isProduction: true,
    });
  }

  return {
    plugins: [react()],
    // The render smoke test bundles for Node; it is not a deployable build, so
    // it must not inherit the production endpoint requirements.
    define: isSsrBuild ? { "import.meta.env.PROD": "false" } : undefined,
  };
});
