/**
 * The socket reports idle / connecting / connected / disconnected / error.
 * The UI used to render everything except "connected" as "Connecting", which
 * kept claiming a reconnect was in progress long after it had failed.
 */
export function realtimeStatusKey(status) {
  if (status === "connected") return "market.status.live";
  if (status === "connecting") return "market.status.connecting";
  if (status === "error") return "market.status.error";

  return "market.status.offline";
}

export default realtimeStatusKey;
