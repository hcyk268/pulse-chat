import { buildRuntimeConfig } from "../config/runtimeConfig";

const APP_ORIGIN =
  typeof window !== "undefined" && window.location?.origin
    ? window.location.origin
    : "http://localhost:5173";

const runtimeConfig = buildRuntimeConfig({
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL,
  websocketUrl: import.meta.env.VITE_WS_URL,
  appOrigin: APP_ORIGIN,
  isProduction: import.meta.env.PROD,
});

export const API_BASE_URL = runtimeConfig.apiBaseUrl;
export const ABSOLUTE_API_BASE_URL = runtimeConfig.absoluteApiBaseUrl;
export const REALTIME_URL = runtimeConfig.realtimeUrl;
export const REALTIME_HOST = runtimeConfig.realtimeHost;
