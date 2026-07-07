import { useRef } from "react";

export function useLatestRef(value) {
  const valueRef = useRef(value);
  valueRef.current = value;
  return valueRef;
}
