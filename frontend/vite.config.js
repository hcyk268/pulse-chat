import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import { buildRuntimeConfig } from "./src/config/runtimeConfig.js";

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), "");

  if (command === "build") {
    buildRuntimeConfig({
      apiBaseUrl: env.VITE_API_BASE_URL,
      websocketUrl: env.VITE_WS_URL,
      appOrigin: "https://chat.example.com",
      isProduction: true,
    });
  }

  return {
    plugins: [react()],
  };
});
