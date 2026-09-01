import { ArrowUpRight, Bot, LineChart, LockKeyhole, Send, Sparkles } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import PageShell from "../../components/layout/PageShell.jsx";
import TraderLayout from "../../components/layout/TraderLayout.jsx";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAuthGate } from "../auth/AuthGateContext.jsx";

const promptDefinitions = [
  { key: "btc", icon: LineChart },
  { key: "market", icon: Sparkles },
  { key: "risk", icon: Bot },
];

export default function GuestAiDemo() {
  const { t } = useTranslation();
  const { openAuth } = useAuthGate();
  const [selectedPrompt, setSelectedPrompt] = useState(null);
  const [answer, setAnswer] = useState("");
  const [streaming, setStreaming] = useState(false);
  const prompts = useMemo(
    () => promptDefinitions.map((prompt) => ({ ...prompt, label: t(`guest.ai.prompt.${prompt.key}`) })),
    [t],
  );

  useEffect(() => {
    if (!selectedPrompt) return undefined;
    const fullAnswer = t(`guest.ai.answer.${selectedPrompt}`);
    const reduceMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
    if (reduceMotion) {
      setAnswer(fullAnswer);
      setStreaming(false);
      return undefined;
    }

    let index = 0;
    setAnswer("");
    setStreaming(true);
    const timer = window.setInterval(() => {
      index = Math.min(fullAnswer.length, index + 3);
      setAnswer(fullAnswer.slice(0, index));
      if (index >= fullAnswer.length) {
        window.clearInterval(timer);
        setStreaming(false);
      }
    }, 14);
    return () => window.clearInterval(timer);
  }, [selectedPrompt, t]);

  function openRegistration() {
    openAuth({
      kind: "ai",
      mode: "register",
      title: t("guest.ai.authTitle"),
      description: t("guest.ai.authDescription"),
    });
  }

  const selectedLabel = prompts.find((prompt) => prompt.key === selectedPrompt)?.label;

  return (
    <TraderLayout active="ai">
      <PageShell
        eyebrow={t("ai.eyebrow")}
        title={t("ai.copilot.title")}
        description={t("guest.ai.description")}
        action={<span className="guest-mode-badge"><LockKeyhole size={14} /> {t("guest.demoMode")}</span>}
      >
        <section className="ai-copilot-workspace guest-ai-demo">
          <header className="ai-copilot-hero">
            <div className="ai-orb" aria-hidden="true"><Sparkles size={30} /></div>
            <div>
              <h2>{t("guest.ai.welcome")}</h2>
              <p>{t("guest.ai.welcomeDescription")}</p>
            </div>
          </header>

          <div className="ai-conversation" aria-live="polite">
            {!selectedPrompt ? (
              <div className="ai-empty-state">
                <span>{t("guest.ai.choosePrompt")}</span>
                <div className="ai-quick-prompts">
                  {prompts.map(({ key, label, icon: Icon }) => (
                    <button key={key} type="button" onClick={() => setSelectedPrompt(key)}>
                      <Icon size={18} /><span>{label}</span><ArrowUpRight size={15} />
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <article className="ai-turn">
                <div className="ai-user-message"><span>{selectedLabel}</span></div>
                <div className="ai-assistant-message">
                  <div className="ai-message-avatar"><Sparkles size={18} /></div>
                  <div className="ai-message-body">
                    <p>{answer}{streaming ? <i className="ai-stream-caret" /> : null}</p>
                    {!streaming ? <small className="guest-ai-demo__label">{t("guest.ai.sampleLabel")}</small> : null}
                  </div>
                </div>
              </article>
            )}
          </div>

          <div className="ai-composer-wrap guest-ai-gate">
            <button className="ai-smart-composer" type="button" onClick={openRegistration}>
              <span className="ai-add-context"><LockKeyhole size={19} /></span>
              <span className="guest-ai-gate__copy">
                <strong>{selectedPrompt ? t("guest.ai.unlockTitle") : t("guest.ai.customTitle")}</strong>
                <small>{t("guest.ai.unlockBody")}</small>
              </span>
              <span className="ai-send-button"><Send size={18} /></span>
            </button>
            <p className="ai-disclaimer">{t("guest.ai.disclaimer")}</p>
          </div>
        </section>
      </PageShell>
    </TraderLayout>
  );
}
