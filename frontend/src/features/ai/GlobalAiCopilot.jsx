import {
  ArrowUpRight, Bot, Hash, MessageSquareText, Send, Sparkles, Tag, X,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { askSmartAssistant, getApiErrorMessage } from "../../api/aiApi.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAppSelector } from "../../store/hooks";
import { selectActiveConversation } from "../../store/slices/workspaceSlice";
import { selectIsAuthenticated } from "../../store/slices/authSlice";
import Alert from "../../components/shared/Alert";

function routeContext(pathname, conversationId) {
  const coinMatch = pathname.match(/^\/coins\/([^/]+)/);
  const communityMatch = pathname.match(/^\/community\/([^/]+)/);
  if (coinMatch) return { symbol: decodeURIComponent(coinMatch[1]).toUpperCase() };
  if (communityMatch) return { communitySlug: decodeURIComponent(communityMatch[1]) };
  if (pathname === "/chat" && conversationId) return { conversationId: Number(conversationId) };
  return {};
}

function contextQuery(context) {
  const query = new URLSearchParams();
  if (context.symbol) query.set("symbol", context.symbol);
  if (context.conversationId) query.set("conversationId", context.conversationId);
  if (context.communitySlug) query.set("community", context.communitySlug);
  const value = query.toString();
  return value ? "?" + value : "";
}

export default function GlobalAiCopilot() {
  const { t } = useTranslation();
  const location = useLocation();
  const activeConversationId = useAppSelector(selectActiveConversation);
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const inputRef = useRef(null);
  const [open, setOpen] = useState(false);
  const [question, setQuestion] = useState("");
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const context = useMemo(
    () => routeContext(location.pathname, activeConversationId),
    [activeConversationId, location.pathname],
  );

  const suggestedPrompt = useMemo(() => {
    if (context.symbol) return t("ai.global.prompt.market", { symbol: context.symbol });
    if (context.conversationId) return t("ai.global.prompt.chat");
    if (context.communitySlug) return t("ai.global.prompt.community");
    return t("ai.global.prompt.default");
  }, [context, t]);


  useEffect(() => {
    if (!open) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const focusFrame = requestAnimationFrame(() => inputRef.current?.focus());

    function closeOnEscape(event) {
      if (event.key === "Escape") setOpen(false);
    }
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      cancelAnimationFrame(focusFrame);
      document.body.style.overflow = previousOverflow;
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [open]);

  async function submit(event) {
    event.preventDefault();
    const prompt = question.trim();
    if (!prompt || loading) return;
    setLoading(true);
    setError("");
    try {
      const response = await askSmartAssistant({ question: prompt, ...context });
      setHistory((current) => [...current, {
        id: String(Date.now()),
        question: prompt,
        answer: response?.answer ?? "",
      }]);
      setQuestion("");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.ai"));
    } finally {
      setLoading(false);
    }
  }

  const ContextIcon = context.symbol
    ? Tag
    : context.conversationId
      ? MessageSquareText
      : context.communitySlug
        ? Hash
        : Sparkles;
  const contextLabel = context.symbol
    ? t("ai.context.assetBadge", { symbol: context.symbol })
    : context.conversationId
      ? t("ai.global.context.chat")
      : context.communitySlug
        ? "#" + context.communitySlug
        : t("ai.global.context.page");

  if (!isAuthenticated) return null;

  return (
    <>
      <button
        className="global-ai-fab"
        type="button"
        aria-label={t("ai.global.open")}
        aria-expanded={open}
        onClick={() => setOpen(true)}
      >
        <Sparkles size={23} />
      </button>

      {open ? (
        <div className="global-ai-layer">
          <button className="global-ai-backdrop" type="button" aria-label={t("common.dismiss")} onClick={() => setOpen(false)} />
          <aside className="global-ai-drawer" role="dialog" aria-modal="true" aria-label={t("ai.global.title")}>
            <header>
              <div className="global-ai-brand"><span><Sparkles size={19} /></span><div><strong>{t("ai.global.title")}</strong><small>{t("ai.global.subtitle")}</small></div></div>
              <button className="icon-button" type="button" aria-label={t("common.dismiss")} onClick={() => setOpen(false)}><X size={19} /></button>
            </header>

            <div className="global-ai-context">
              <ContextIcon size={14} /> {contextLabel}
            </div>

            <div className="global-ai-messages" aria-live="polite">
              {history.length === 0 ? (
                <div className="global-ai-empty">
                  <span><Bot size={24} /></span>
                  <h2>{t("ai.global.greeting")}</h2>
                  <p>{t("ai.global.greetingDescription")}</p>
                  <button type="button" onClick={() => { setQuestion(suggestedPrompt); inputRef.current?.focus(); }}>
                    <Sparkles size={16} /><span>{suggestedPrompt}</span><ArrowUpRight size={15} />
                  </button>
                </div>
              ) : null}
              {history.map((entry) => (
                <article key={entry.id}>
                  <div className="global-ai-user">{entry.question}</div>
                  <div className="global-ai-answer"><span><Sparkles size={15} /></span><p>{entry.answer}</p></div>
                </article>
              ))}
              {loading ? <div className="global-ai-answer" role="status"><span><Sparkles size={15} /></span><p>{t("ai.pending.assistant")}</p></div> : null}
            </div>

            {error ? <Alert onDismiss={() => setError("")}>{error}</Alert> : null}

            <footer>
              <form onSubmit={submit}>
                <textarea
                  ref={inputRef}
                  rows={2}
                  value={question}
                  placeholder={t("ai.global.placeholder")}
                  onChange={(event) => setQuestion(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" && !event.shiftKey) {
                      event.preventDefault();
                      event.currentTarget.form?.requestSubmit();
                    }
                  }}
                />
                <button type="submit" disabled={!question.trim() || loading} aria-label={t("ai.assistant.submit")}><Send size={17} /></button>
              </form>
              <Link to={"/ai" + contextQuery(context)} onClick={() => setOpen(false)}>
                {t("ai.global.openWorkspace")} <ArrowUpRight size={14} />
              </Link>
            </footer>
          </aside>
        </div>
      ) : null}
    </>
  );
}




