import { useMemo } from "react";
import { getTranslator } from "./index.js";
import { useAppSelector } from "../store/hooks";
import { selectLocale } from "../store/slices/uiSlice";

export function useTranslation() {
  const locale = useAppSelector(selectLocale);
  const t = useMemo(() => getTranslator(locale), [locale]);

  return { t, locale };
}

export default useTranslation;
