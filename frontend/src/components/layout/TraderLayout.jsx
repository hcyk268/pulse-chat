import TraderHeader from "./TraderHeader";
import TraderFooter from "./TraderFooter";
import GlobalAiCopilot from "../../features/ai/GlobalAiCopilot";
import { useTranslation } from "../../i18n/useTranslation.js";
import { classNames } from "../shared/utils";

export default function TraderLayout({ active = "market", children, appFrame = false }) {
  const { t } = useTranslation();

  return (
    <div className={classNames("trader-app", appFrame && "trader-app--frame")}>
      <a className="skip-link" href="#main">
        {t("a11y.skipToContent")}
      </a>
      <TraderHeader active={active} />
      {children}
      {!appFrame ? <TraderFooter /> : null}
      {active !== "ai" ? <GlobalAiCopilot /> : null}
    </div>
  );
}

