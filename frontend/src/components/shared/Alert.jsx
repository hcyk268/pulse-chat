import { X } from "lucide-react";
import { classNames } from "./utils";
import { useTranslation } from "../../i18n/useTranslation.js";

/**
 * One alert for the whole app: an error that cannot be dismissed or retried
 * strands the user on a broken screen.
 */
export default function Alert({ children, onDismiss, onRetry, variant = "page" }) {
  const { t } = useTranslation();

  return (
    <div className={classNames("app-alert", `app-alert--${variant}`)} role="alert">
      <span className="app-alert__message">{children}</span>
      {onRetry ? (
        <button className="app-alert__action" type="button" onClick={onRetry}>
          {t("common.retry")}
        </button>
      ) : null}
      {onDismiss ? (
        <button
          className="app-alert__dismiss"
          type="button"
          aria-label={t("common.dismiss")}
          onClick={onDismiss}
        >
          <X size={15} />
        </button>
      ) : null}
    </div>
  );
}
