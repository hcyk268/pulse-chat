export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080").replace(
  /\/$/,
  "",
);

const APP_ORIGIN =
  typeof window !== "undefined" && window.location?.origin
    ? window.location.origin
    : "http://localhost:5173";

export const ABSOLUTE_API_BASE_URL = new URL(API_BASE_URL, APP_ORIGIN)
  .toString()
  .replace(/\/$/, "");

export const REALTIME_URL =
  import.meta.env.VITE_WS_URL ||
  `${ABSOLUTE_API_BASE_URL.replace(/^http/i, "ws")}/ws`;

export const REALTIME_HOST = new URL(ABSOLUTE_API_BASE_URL).host;
