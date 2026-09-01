import { useEffect, useState } from "react";
import { observeRealtimeStatus, subscribeRealtimeTopic } from "../services/realtimeClient.js";
import { useLatestRef } from "./useLatestRef.js";

/**
 * Subscribes to a STOMP destination for as long as it stays truthy and reports
 * the shared connection status. Passing null keeps the component unsubscribed.
 */
export function useRealtimeTopic(destination, onEvent) {
  const eventHandlerRef = useLatestRef(onEvent);
  const [status, setStatus] = useState("idle");

  useEffect(() => observeRealtimeStatus(setStatus), []);

  useEffect(() => {
    if (!destination) return undefined;

    return subscribeRealtimeTopic(destination, (event) => eventHandlerRef.current?.(event));
  }, [destination, eventHandlerRef]);

  return status;
}

export default useRealtimeTopic;
