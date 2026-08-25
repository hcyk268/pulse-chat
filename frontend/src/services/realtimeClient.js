import { Client } from "@stomp/stompjs";
import { refreshAuthSession } from "../api/httpClient.js";
import { runtimeConfig } from "../config/runtimeConfig.js";
import { getFreshAccessToken } from "../utils/authStorage.js";

const IDLE_DEACTIVATE_DELAY = 250;
const PUBLIC_TOPIC_PREFIXES = ["/topic/market/", "/topic/community/"];

const channels = new Map();
const statusListeners = new Set();
const pendingPublishes = new Map();
const MAX_PENDING_PUBLISHES = 200;
let client;
let status = "idle";
let deactivateTimer;
let authenticatedConnection = false;

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

function isPublicTopic(destination) {
  return PUBLIC_TOPIC_PREFIXES.some((prefix) => destination?.startsWith(prefix));
}

function needsAuthenticatedConnection(destination) {
  return !isPublicTopic(destination);
}

function canUseDestination(destination) {
  return !needsAuthenticatedConnection(destination) || authenticatedConnection;
}

function attachChannel(destination, channel) {
  if (!client?.connected || channel.subscription || !canUseDestination(destination)) return;

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

function hasAuthenticatedRealtimeWork() {
  if (pendingPublishes.size > 0) return true;

  return [...channels.keys()].some(needsAuthenticatedConnection);
}

async function resolveConnectHeaders() {
  let accessToken = getFreshAccessToken();

  if (!accessToken && hasAuthenticatedRealtimeWork()) {
    const session = await refreshAuthSession().catch(() => null);
    accessToken = session?.accessToken ?? getFreshAccessToken();
  }

  return accessToken ? { Authorization: `Bearer ${accessToken}` } : {};
}

function publishPendingMessages() {
  if (!authenticatedConnection) return;

  pendingPublishes.forEach((body, destination) => {
    client.publish({ destination, body });
  });
  pendingPublishes.clear();
}

function reconnectActiveClient() {
  if (!client?.active) return;

  detachChannels();
  authenticatedConnection = false;

  void client.deactivate().then(() => {
    if (channels.size > 0) {
      client.activate();
      return;
    }

    emitStatus("idle");
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
      client.connectHeaders = await resolveConnectHeaders();
      emitStatus("connecting");
    },
    onConnect: () => {
      authenticatedConnection = Boolean(client.connectHeaders?.Authorization);
      emitStatus("connected");
      channels.forEach((channel, destination) => attachChannel(destination, channel));
      publishPendingMessages();
    },
    onDisconnect: () => {
      authenticatedConnection = false;
      emitStatus("disconnected");
    },
    onWebSocketClose: () => {
      detachChannels();
      authenticatedConnection = false;
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
  if (stompClient.connected && needsAuthenticatedConnection(destination) && !authenticatedConnection) {
    reconnectActiveClient();
  } else {
    attachChannel(destination, channel);
  }
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

export function publishRealtime(destination, body, { queueIfDisconnected = false } = {}) {
  if (!destination) return false;
  const serializedBody = JSON.stringify(body ?? {});

  if (!client?.connected || (needsAuthenticatedConnection(destination) && !authenticatedConnection)) {
    if (queueIfDisconnected) {
      if (pendingPublishes.size >= MAX_PENDING_PUBLISHES) {
        pendingPublishes.delete(pendingPublishes.keys().next().value);
      }
      pendingPublishes.set(destination, serializedBody);
    }

    if (client?.connected && needsAuthenticatedConnection(destination) && !authenticatedConnection) {
      reconnectActiveClient();
    }

    return false;
  }

  client.publish({ destination, body: serializedBody });
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
  pendingPublishes.clear();
  if (!client) return;

  detachChannels();
  authenticatedConnection = false;

  if (!client.active) return;

  void client.deactivate().then(() => {
    if (channels.size > 0) {
      client.activate();
      return;
    }

    emitStatus("idle");
  });
}
