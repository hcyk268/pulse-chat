import { getAccessToken } from "../utils/authStorage";

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080").replace(
  /\/$/,
  "",
);

const REALTIME_URL =
  import.meta.env.VITE_WS_URL ||
  `${API_BASE_URL.replace(/^http/i, "ws")}/ws`;
const REALTIME_HOST = new URL(API_BASE_URL).host;

const USER_EVENTS_DESTINATION = "/user/queue/events";
const USER_ERRORS_DESTINATION = "/user/queue/errors";

function buildFrame(command, headers = {}, body = "") {
  const headerLines = Object.entries(headers)
    .filter(([, value]) => value !== undefined && value !== null)
    .map(([key, value]) => `${key}:${value}`);

  return `${command}\n${headerLines.join("\n")}\n\n${body}\0`;
}

function parseFrame(rawFrame) {
  const [headerBlock, ...bodyParts] = rawFrame.split("\n\n");
  const [command, ...headerLines] = headerBlock.split("\n").filter(Boolean);
  const headers = {};

  headerLines.forEach((line) => {
    const separatorIndex = line.indexOf(":");
    if (separatorIndex === -1) return;

    headers[line.slice(0, separatorIndex)] = line.slice(separatorIndex + 1);
  });

  return {
    body: bodyParts.join("\n\n"),
    command,
    headers,
  };
}

export function createRealtimeClient({ onEvent, onStatus, onError }) {
  let socket = null;
  let heartbeatTimer = null;
  let reconnectTimer = null;
  let shouldReconnect = true;
  let reconnectAttempt = 0;

  function setStatus(status) {
    onStatus?.(status);
  }

  function clearTimers() {
    window.clearInterval(heartbeatTimer);
    window.clearTimeout(reconnectTimer);
    heartbeatTimer = null;
    reconnectTimer = null;
  }

  function sendFrame(command, headers = {}, body = "") {
    if (!socket || socket.readyState !== WebSocket.OPEN) return;
    socket.send(buildFrame(command, headers, body));
  }

  function subscribeToEvents() {
    sendFrame("SUBSCRIBE", {
      ack: "auto",
      destination: USER_EVENTS_DESTINATION,
      id: "user-events",
    });
    sendFrame("SUBSCRIBE", {
      ack: "auto",
      destination: USER_ERRORS_DESTINATION,
      id: "user-errors",
    });
  }

  function sendJson(destination, payload) {
    sendFrame(
      "SEND",
      {
        "content-type": "application/json",
        destination,
      },
      JSON.stringify(payload),
    );
  }

  function scheduleReconnect() {
    if (!shouldReconnect) return;

    const delay = Math.min(1000 * 2 ** reconnectAttempt, 10000);
    reconnectAttempt += 1;
    setStatus("reconnecting");
    reconnectTimer = window.setTimeout(connect, delay);
  }

  function handleMessage(event) {
    const frames = String(event.data)
      .split("\0")
      .map((frame) => frame.trim())
      .filter(Boolean);

    frames.forEach((rawFrame) => {
      const frame = parseFrame(rawFrame);

      if (frame.command === "CONNECTED") {
        reconnectAttempt = 0;
        setStatus("connected");
        subscribeToEvents();
        heartbeatTimer = window.setInterval(() => {
          if (socket?.readyState === WebSocket.OPEN) {
            socket.send("\n");
          }
        }, 10000);
        return;
      }

      if (frame.command === "MESSAGE") {
        try {
          onEvent?.(JSON.parse(frame.body));
        } catch (error) {
          onError?.(error.message || "Could not parse realtime event.");
        }
        return;
      }

      if (frame.command === "ERROR") {
        onError?.(frame.body || "Realtime connection error.");
      }
    });
  }

  function connect() {
    const accessToken = getAccessToken();
    if (!accessToken) {
      setStatus("idle");
      return;
    }

    shouldReconnect = true;
    clearTimers();
    setStatus(reconnectAttempt > 0 ? "reconnecting" : "connecting");
    socket = new WebSocket(REALTIME_URL, ["v12.stomp"]);

    socket.addEventListener("open", () => {
      sendFrame("CONNECT", {
        Authorization: `Bearer ${accessToken}`,
        "accept-version": "1.2",
        "heart-beat": "10000,10000",
        host: REALTIME_HOST,
      });
    });

    socket.addEventListener("message", handleMessage);

    socket.addEventListener("error", () => {
      onError?.("Realtime connection error.");
    });

    socket.addEventListener("close", () => {
      clearTimers();
      socket = null;

      if (shouldReconnect) {
        scheduleReconnect();
      } else {
        setStatus("idle");
      }
    });
  }

  function disconnect() {
    shouldReconnect = false;
    clearTimers();
    sendFrame("DISCONNECT");
    socket?.close();
    socket = null;
    setStatus("idle");
  }

  return {
    connect,
    disconnect,
    sendTypingStatus(conversationId, typing) {
      if (!conversationId) return;

      sendJson(`/app/conversations/${conversationId}/typing`, { typing });
    },
  };
}
