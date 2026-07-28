import { Client } from "@stomp/stompjs";
import { runtimeConfig } from "../config/runtimeConfig.js";
import { getAccessToken } from "../utils/authStorage.js";

const IDLE_DEACTIVATE_DELAY = 250;

const channels = new Map();
const statusListeners = new Set();
let client;
let status = "idle";
let deactivateTimer;

function emitStatus(nextStatus) {
  status = nextStatus;
  statusListeners.forEach((listener) => listener(nextStatus));
}

function parseEnvelope(message) {
  try {
    return JSON.parse(message.body);
  } catch {
    return null;
  }
}

function attachChannel(destination, channel) {
  if (!client?.connected || channel.subscription) return;

  channel.subscription = client.subscribe(destination, (message) => {
    const envelope = parseEnvelope(message);
    if (!envelope) return;
    channel.listeners.forEach((listener) => listener(envelope));
  });
}

function detachChannels() {
  channels.forEach((channel) => {
    channel.subscription = null;
  });
}

function ensureClient() {
  if (client) return client;

  client = new Client({
    brokerURL: runtimeConfig.realtimeUrl,
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    connectionTimeout: 10000,
    beforeConnect: async () => {
      const accessToken = getAccessToken();
      client.connectHeaders = accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
      emitStatus("connecting");
    },
    onConnect: () => {
      emitStatus("connected");
      channels.forEach((channel, destination) => attachChannel(destination, channel));
    },
    onDisconnect: () => emitStatus("disconnected"),
    onWebSocketClose: () => {
      detachChannels();
      emitStatus("disconnected");
    },
    onStompError: () => emitStatus("error"),
    onWebSocketError: () => emitStatus("error"),
  });

  return client;
}

export function subscribeRealtimeTopic(destination, listener) {
  if (!destination || typeof listener !== "function") return () => {};

  if (deactivateTimer) {
    clearTimeout(deactivateTimer);
    deactivateTimer = undefined;
  }

  let channel = channels.get(destination);
  if (!channel) {
    channel = { listeners: new Set(), subscription: null };
    channels.set(destination, channel);
  }

  channel.listeners.add(listener);
  const stompClient = ensureClient();
  attachChannel(destination, channel);
  if (!stompClient.active) stompClient.activate();

  return () => {
    const currentChannel = channels.get(destination);
    if (!currentChannel) return;

    currentChannel.listeners.delete(listener);
    if (currentChannel.listeners.size > 0) return;

    currentChannel.subscription?.unsubscribe();
    channels.delete(destination);

    if (channels.size === 0 && client?.active) {
      deactivateTimer = setTimeout(() => {
        if (channels.size === 0 && client?.active) {
          void client.deactivate().finally(() => emitStatus("idle"));
        }
      }, IDLE_DEACTIVATE_DELAY);
    }
  };
}

export function publishRealtime(destination, body) {
  if (!destination || !client?.connected) return false;

  client.publish({ destination, body: JSON.stringify(body ?? {}) });
  return true;
}

export function observeRealtimeStatus(listener) {
  statusListeners.add(listener);
  listener(status);
  return () => statusListeners.delete(listener);
}

/**
 * The access token travels in the STOMP CONNECT frame, so an existing socket keeps
 * the identity it was opened with. Sign-in and sign-out must therefore reconnect.
 */
export function resetRealtimeConnection() {
  if (!client) return;

  detachChannels();

  if (!client.active) return;

  void client.deactivate().then(() => {
    if (channels.size > 0) {
      client.activate();
      return;
    }

    emitStatus("idle");
  });
}
