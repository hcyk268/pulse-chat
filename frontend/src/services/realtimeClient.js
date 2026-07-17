import { Client } from "@stomp/stompjs";
import { getAccessToken } from "../utils/authStorage";
import { REALTIME_HOST, REALTIME_URL } from "./apiConfig";

const USER_EVENTS_DESTINATION = "/user/queue/events";
const USER_ERRORS_DESTINATION = "/user/queue/errors";

function parseMessageBody(message) {
  if (!message?.body) return null;

  try {
    return JSON.parse(message.body);
  } catch (error) {
    throw new Error(error.message || "Could not parse realtime event.", { cause: error });
  }
}

export function createRealtimeClient({ onEvent, onStatus, onError }) {
  let client = null;
  let shouldReconnect = true;
  let hasConnected = false;

  function setStatus(status) {
    onStatus?.(status);
  }

  function buildClient() {
    const accessToken = getAccessToken();

    return new Client({
      brokerURL: REALTIME_URL,
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
        host: REALTIME_HOST,
      },
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      reconnectDelay: 1000,
      maxWebSocketChunkSize: 8 * 1024,
      onConnect() {
        hasConnected = true;
        setStatus("connected");

        client?.subscribe(USER_EVENTS_DESTINATION, (message) => {
          try {
            onEvent?.(parseMessageBody(message));
          } catch (error) {
            onError?.(error.message || "Could not parse realtime event.");
          }
        });

        client?.subscribe(USER_ERRORS_DESTINATION, (message) => {
          try {
            const body = parseMessageBody(message);
            onError?.(body?.message || "Realtime request failed.");
          } catch (error) {
            onError?.(error.message || "Realtime request failed.");
          }
        });
      },
      onStompError(frame) {
        onError?.(frame.body || frame.headers?.message || "Realtime connection error.");
      },
      onWebSocketError() {
        onError?.("Realtime connection error.");
      },
      onWebSocketClose() {
        if (!shouldReconnect) {
          setStatus("idle");
          return;
        }

        setStatus(hasConnected ? "reconnecting" : "connecting");
      },
    });
  }

  function connect() {
    const accessToken = getAccessToken();
    if (!accessToken) {
      setStatus("idle");
      return;
    }

    shouldReconnect = true;
    hasConnected = false;
    setStatus("connecting");
    client?.deactivate();
    client = buildClient();
    client.activate();
  }

  function disconnect() {
    shouldReconnect = false;
    const currentClient = client;
    client = null;
    currentClient?.deactivate();
    setStatus("idle");
  }

  function publishJson(destination, payload) {
    if (!client?.connected) return false;

    client.publish({
      destination,
      body: JSON.stringify(payload),
      headers: {
        "content-type": "application/json",
      },
    });
    return true;
  }

  return {
    connect,
    disconnect,
    sendTypingStatus(conversationId, typing) {
      if (!conversationId) return false;

      return publishJson(`/app/conversations/${conversationId}/typing`, { typing });
    },
    sendMessageDelivered(messageId) {
      if (!messageId) return false;

      return publishJson(`/app/messages/${messageId}/delivered`, {});
    },
  };
}
