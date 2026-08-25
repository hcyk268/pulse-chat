import {
  ArrowUpRight, Bot, Check, ChevronDown, ChevronRight, Copy, FileText, Hash,
  LineChart, MessageSquareText, Paperclip, Plus, Send, ShieldCheck, Sparkles,
  Tag, Terminal, Trash2, X,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { getConversations, uploadMessageAttachment } from "../api/chatApi.js";
import {
  askSmartAssistant, getApiErrorMessage, getMarketInsight,
  moderateCommunityContent, summarizeConversation,
} from "../api/aiApi.js";
import PageShell from "../components/layout/PageShell";
import TraderLayout from "../components/layout/TraderLayout";
import Alert from "../components/shared/Alert";
import { classNames } from "../components/shared/utils";
import { useTranslation } from "../i18n/useTranslation.js";
import { formatRelativeTime } from "../utils/formatters";
import GuestAiDemo from "../features/ai/GuestAiDemo.jsx";
import { useAppSelector } from "../store/hooks.js";
import { selectIsAuthenticated } from "../store/slices/authSlice.js";

const intentIcons = {
  assistant: Bot,
  market: LineChart,
  summary: MessageSquareText,
  moderation: ShieldCheck,
};

function getConversationLabel(conversation) {
  const peer = conversation.otherParticipant ?? conversation.peer;
  return (
    conversation.title || conversation.name || peer?.displayName || peer?.username ||
    "Conversation #" + conversation.id
  );
}

function responseText(entry) {
  if (entry.kind === "market") return entry.response.insight ?? "";
  if (entry.kind === "summary") return entry.response.summary ?? "";
  if (entry.kind === "moderation") return entry.response.reason ?? "";
  return entry.response.answer ?? "";
}

function ResultMeta({ result }) {
  const { t } = useTranslation();
  if (!result) return null;
  return (
    <div className="ai-meta">
      {result.model ? <span>{t("ai.meta.model", { model: result.model })}</span> : null}
      {result.generatedAt ? <span>{formatRelativeTime(result.generatedAt)}</span> : null}
      {typeof result.cacheHit === "boolean" ? (
        <span>{t(result.cacheHit ? "ai.meta.cacheHit" : "ai.meta.generated")}</span>
      ) : null}
    </div>
  );
}

function TextList({ title, items, tone }) {
  if (!items?.length) return null;
  return (
    <section className={classNames("ai-message-list", tone && "ai-message-list--" + tone)}>
      <h3>{title}</h3>
      <ul>{items.map((item, index) => <li key={String(index)}>{item}</li>)}</ul>
    </section>
  );
}

function ToolCallsSection({ toolCalls }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  if (!toolCalls?.length) return null;

  return (
    <div className="ai-tool-calls">
      <button
        type="button"
        className="ai-tool-calls__toggle"
        onClick={() => setOpen((current) => !current)}
      >
        <Terminal size={14} />
        <span>{t("ai.toolCalls.title")}</span>
        <span className="ai-tool-calls__count">{toolCalls.length}</span>
        {open ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
      </button>
      {open ? (
        <div className="ai-tool-calls__list">
          {toolCalls.map((call, index) => (
            <div className="ai-tool-call-item" key={String(index)}>
              <span className="ai-tool-call-name"><code>{call.toolName || call.name}</code></span>
              {call.argumentsJson || call.arguments ? (
                <span className="ai-tool-call-args">
                  {t("ai.toolCalls.arguments", {
                    args: typeof (call.argumentsJson || call.arguments) === "string"
                      ? call.argumentsJson || call.arguments
                      : JSON.stringify(call.argumentsJson || call.arguments),
                  })}
                </span>
              ) : null}
            </div>
          ))}
        </div>
      ) : null}
    </div>
  );
}

function AssistantResult({ entry }) {
  const { t } = useTranslation();
  const { kind, response } = entry;

  if (kind === "market" || kind === "summary") {
    const isMarket = kind === "market";
    return (
      <>
        <p>{isMarket ? response.insight : response.summary}</p>
        <div className="ai-message-grid">
          <TextList
            title={t(isMarket ? "ai.market.keyPoints" : "ai.summary.highlights")}
            items={isMarket ? response.keyPoints : response.highlights}
          />
          <TextList
            title={t(isMarket ? "ai.market.riskNotes" : "ai.summary.actionItems")}
            items={isMarket ? response.riskNotes : response.actionItems}
            tone={isMarket ? "risk" : undefined}
          />
        </div>
      </>
    );
  }

  if (kind === "moderation") {
    const decision = response.decision ?? "REVIEW";
    return (
      <>
        <div className={classNames("ai-decision", "ai-decision--" + decision.toLowerCase())}>
          <ShieldCheck size={16} />
          <strong>{t("ai.moderation.decision." + decision)}</strong>
        </div>
        <p>{response.reason}</p>
        {response.suggestedTags?.length ? (
          <div className="ai-suggested-tags">
            {response.suggestedTags.map((tag) => <span key={tag}>#{tag}</span>)}
          </div>
        ) : null}
      </>
    );
  }

  return (
    <>
      <p>{response.answer}</p>
      <ToolCallsSection toolCalls={response.toolCalls} />
    </>
  );
}

function AuthenticatedAiPage() {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const inputRef = useRef(null);
  const fileInputRef = useRef(null);
  const query = useMemo(() => new URLSearchParams(location.search), [location.search]);
  const [question, setQuestion] = useState("");
  const [intent, setIntent] = useState("assistant");
  const [contextOpen, setContextOpen] = useState(false);
  const [attachments, setAttachments] = useState([]);
  const [uploadingAttachment, setUploadingAttachment] = useState(false);
  const [attachmentProgress, setAttachmentProgress] = useState(0);
  const [context, setContext] = useState({
    symbol: query.get("symbol")?.toUpperCase() ?? "",
    conversationId: query.get("conversationId") ?? "",
    communitySlug: query.get("community") ?? "",
  });
  const [conversations, setConversations] = useState([]);
  const [loadingConversations, setLoadingConversations] = useState(true);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [copiedId, setCopiedId] = useState(null);

  const quickPrompts = useMemo(() => [
    { id: "summary", icon: MessageSquareText, label: t("ai.copilot.prompt.summary") },
    { id: "market", icon: LineChart, label: t("ai.copilot.prompt.market") },
    { id: "moderation", icon: ShieldCheck, label: t("ai.copilot.prompt.moderation") },
  ], [t]);

  useEffect(() => {
    let active = true;
    getConversations({ limit: 30 })
      .then((box) => {
        if (active) setConversations(box?.items ?? []);
      })
      .catch((requestError) => {
        if (active) setError(getApiErrorMessage(requestError, t, "errors.conversations"));
      })
      .finally(() => {
        if (active) setLoadingConversations(false);
      });
    return () => {
      active = false;
    };
  }, [t]);

  async function handleFileSelect(event) {
    const files = Array.from(event.target.files || []);
    event.target.value = "";
    if (!files.length) return;

    if (attachments.length + files.length > 5) {
      setError(t("ai.attachments.limitExceeded", { count: 5 }));
      return;
    }

    setUploadingAttachment(true);
    setError("");
    try {
      for (const file of files) {
        setAttachmentProgress(0);
        const asset = await uploadMessageAttachment(file, {
          purpose: "MESSAGE_ATTACHMENT",
          onProgress: ({ uploadedBytes, totalBytes }) => {
            if (totalBytes > 0) {
              setAttachmentProgress(Math.round((uploadedBytes / totalBytes) * 100));
            }
          },
        });
        if (asset?.id) {
          setAttachments((current) => [...current, asset]);
        }
      }
    } catch (uploadError) {
      setError(getApiErrorMessage(uploadError, t, "errors.ai"));
    } finally {
      setUploadingAttachment(false);
      setAttachmentProgress(0);
    }
  }

  function removeAttachment(assetId) {
    setAttachments((current) => current.filter((item) => item.id !== assetId));
  }

  function choosePrompt(prompt) {
    setIntent(prompt.id);
    setQuestion(prompt.id === "moderation" ? "" : prompt.label);
    if (prompt.id === "market" && !context.symbol) {
      setContext((current) => ({ ...current, symbol: "BTC" }));
    }
    if (prompt.id === "summary" && !context.conversationId) setContextOpen(true);
    inputRef.current?.focus();
  }

  function removeContext(key) {
    setContext((current) => ({ ...current, [key]: "" }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const prompt = question.trim();
    if (!prompt || loading || uploadingAttachment) return;
    if (intent === "summary" && !context.conversationId) {
      setError(t("ai.copilot.validation.conversation"));
      setContextOpen(true);
      return;
    }

    setLoading(true);
    setError("");
    const snapshot = { ...context };
    const currentAttachments = [...attachments];
    try {
      let response;
      if (intent === "market") {
        response = await getMarketInsight({ symbol: context.symbol.trim().toUpperCase() });
      } else if (intent === "summary") {
        response = await summarizeConversation(context.conversationId, { limit: 20 });
      } else if (intent === "moderation") {
        response = await moderateCommunityContent({
          content: prompt,
          communitySlug: context.communitySlug.trim(),
        });
      } else {
        response = await askSmartAssistant({
          question: prompt,
          symbol: context.symbol.trim().toUpperCase(),
          conversationId: context.conversationId ? Number(context.conversationId) : undefined,
          communitySlug: context.communitySlug.trim(),
          attachmentIds: currentAttachments.length > 0 ? currentAttachments.map((item) => item.id) : undefined,
        });
      }
      setHistory((current) => [...current, {
        id: String(Date.now()) + "-" + current.length,
        question: prompt,
        kind: intent,
        response,
        context: snapshot,
        attachments: currentAttachments,
      }]);
      setQuestion("");
      setAttachments([]);
      setIntent("assistant");
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, t, "errors.ai"));
    } finally {
      setLoading(false);
    }
  }

  async function copyEntry(entry) {
    await navigator.clipboard.writeText(responseText(entry));
    setCopiedId(entry.id);
    window.setTimeout(() => setCopiedId(null), 1600);
  }

  const selectedConversation = conversations.find(
    (item) => String(item.id) === String(context.conversationId),
  );
  const IntentIcon = intentIcons[intent];

  return (
    <TraderLayout active="ai">
      <PageShell
        eyebrow={t("ai.eyebrow")}
        title={t("ai.copilot.title")}
        description={t("ai.copilot.description")}
        action={<span className="meta-chip ai-online-chip"><span /> {t("ai.copilot.ready")}</span>}
      >
        <section className="ai-copilot-workspace">
          <header className="ai-copilot-hero">
            <div className="ai-orb" aria-hidden="true"><Sparkles size={30} /></div>
            <div>
              <h2>{t("ai.copilot.welcome")}</h2>
              <p>{t("ai.copilot.welcomeDescription")}</p>
            </div>
            {history.length ? (
              <button className="button button--ghost" type="button" onClick={() => setHistory([])}>
                <Trash2 size={15} /> {t("ai.copilot.newChat")}
              </button>
            ) : null}
          </header>

          {error ? <Alert onDismiss={() => setError("")}>{error}</Alert> : null}

          <div className="ai-conversation" aria-live="polite">
            {history.length === 0 ? (
              <div className="ai-empty-state">
                <span>{t("ai.copilot.startWith")}</span>
                <div className="ai-quick-prompts">
                  {quickPrompts.map((prompt) => {
                    const Icon = prompt.icon;
                    return (
                      <button key={prompt.id} type="button" onClick={() => choosePrompt(prompt)}>
                        <Icon size={18} /><span>{prompt.label}</span><ArrowUpRight size={15} />
                      </button>
                    );
                  })}
                </div>
              </div>
            ) : null}

            {history.map((entry) => {
              const EntryIcon = intentIcons[entry.kind];
              return (
                <article className="ai-turn" key={entry.id}>
                  <div className="ai-user-message"><span>{entry.question}</span></div>
                  <div className="ai-assistant-message">
                    <div className="ai-message-avatar"><EntryIcon size={18} /></div>
                    <div className="ai-message-body">
                      <AssistantResult entry={entry} />
                      <ResultMeta result={entry.response} />
                      <div className="ai-message-actions">
                        <button type="button" onClick={() => copyEntry(entry)}>
                          {copiedId === entry.id ? <Check size={15} /> : <Copy size={15} />}
                          {t(copiedId === entry.id ? "ai.actions.copied" : "ai.actions.copy")}
                        </button>
                        <button type="button" onClick={() => navigate("/chat", { state: { aiDraft: responseText(entry) } })}>
                          <MessageSquareText size={15} /> {t("ai.actions.sendToChat")}
                        </button>
                        {entry.context.symbol ? (
                          <button type="button" onClick={() => navigate("/coins/" + entry.context.symbol.toLowerCase())}>
                            <LineChart size={15} /> {t("ai.actions.openAsset")}
                          </button>
                        ) : null}
                        {entry.context.communitySlug ? (
                          <button
                            type="button"
                            onClick={() => navigate("/community/" + entry.context.communitySlug, {
                              state: { aiDraft: responseText(entry) },
                            })}
                          >
                            <Hash size={15} /> {t("ai.actions.useInCommunity")}
                          </button>
                        ) : null}
                      </div>
                    </div>
                  </div>
                </article>
              );
            })}

            {loading ? (
              <div className="ai-assistant-message ai-assistant-message--loading" role="status">
                <div className="ai-message-avatar"><Sparkles size={18} /></div>
                <div className="ai-thinking"><i /><i /><i /> {t("ai.pending." + intent)}</div>
              </div>
            ) : null}
          </div>

          <div className="ai-composer-wrap">
            <div className="ai-context-pills">
              {context.symbol ? (
                <span><Tag size={14} /> {t("ai.context.assetBadge", { symbol: context.symbol.toUpperCase() })}
                  <button type="button" aria-label={t("common.dismiss")} onClick={() => removeContext("symbol")}><X size={13} /></button>
                </span>
              ) : null}
              {context.conversationId ? (
                <span><MessageSquareText size={14} /> {getConversationLabel(selectedConversation ?? { id: context.conversationId })}
                  <button type="button" aria-label={t("common.dismiss")} onClick={() => removeContext("conversationId")}><X size={13} /></button>
                </span>
              ) : null}
              {context.communitySlug ? (
                <span><Hash size={14} /> {context.communitySlug}
                  <button type="button" aria-label={t("common.dismiss")} onClick={() => removeContext("communitySlug")}><X size={13} /></button>
                </span>
              ) : null}
            </div>

            {contextOpen ? (
              <div className="ai-context-picker">
                <div className="ai-context-picker__heading">
                  <div><strong>{t("ai.copilot.addContext")}</strong><span>{t("ai.copilot.contextHelp")}</span></div>
                  <button className="icon-button" type="button" aria-label={t("common.dismiss")} onClick={() => setContextOpen(false)}><X size={17} /></button>
                </div>
                <div className="ai-context-fields">
                  <label><Tag size={16} /><span>{t("ai.context.symbol")}</span>
                    <input value={context.symbol} maxLength={30} placeholder={t("ai.context.symbolPlaceholder")} onChange={(event) => setContext((current) => ({ ...current, symbol: event.target.value.toUpperCase() }))} />
                  </label>
                  <label><MessageSquareText size={16} /><span>{t("ai.context.conversation")}</span>
                    <select value={context.conversationId} disabled={loadingConversations} onChange={(event) => setContext((current) => ({ ...current, conversationId: event.target.value }))}>
                      <option value="">{loadingConversations ? t("ai.context.conversationLoading") : t("ai.context.conversationOptional")}</option>
                      {conversations.map((conversation) => <option key={conversation.id} value={conversation.id}>{getConversationLabel(conversation)}</option>)}
                    </select>
                  </label>
                  <label><Hash size={16} /><span>{t("ai.context.communitySlug")}</span>
                    <input value={context.communitySlug} maxLength={120} placeholder={t("ai.context.communityPlaceholder")} onChange={(event) => setContext((current) => ({ ...current, communitySlug: event.target.value }))} />
                  </label>
                </div>
              </div>
            ) : null}

            {attachments.length > 0 ? (
              <div className="ai-attachment-pills">
                {attachments.map((item) => (
                  <span className="ai-attachment-chip" key={item.id}>
                    <FileText size={13} />
                    <strong title={item.fileName}>{item.fileName}</strong>
                    <button
                      type="button"
                      aria-label={t("ai.attachments.remove")}
                      onClick={() => removeAttachment(item.id)}
                    >
                      <X size={12} />
                    </button>
                  </span>
                ))}
              </div>
            ) : null}

            {uploadingAttachment ? (
              <div className="ai-uploading-indicator">
                <Sparkles size={13} />
                <span>{t("ai.attachments.uploading", { percent: attachmentProgress })}</span>
              </div>
            ) : null}

            <form className="ai-smart-composer" onSubmit={handleSubmit}>
              <input
                ref={fileInputRef}
                type="file"
                multiple
                hidden
                accept="image/*,.pdf,.docx,.doc,.txt,.csv,.json,.md"
                onChange={handleFileSelect}
              />
              <button
                className="ai-add-context"
                type="button"
                aria-label={t("ai.copilot.addContext")}
                aria-expanded={contextOpen}
                onClick={() => setContextOpen((current) => !current)}
              >
                <Plus size={20} />
              </button>
              {intent === "assistant" ? (
                <button
                  className="ai-add-context"
                  type="button"
                  aria-label={t("ai.attachments.upload")}
                  disabled={uploadingAttachment || attachments.length >= 5}
                  onClick={() => fileInputRef.current?.click()}
                >
                  <Paperclip size={18} />
                </button>
              ) : null}
              <div className="ai-smart-composer__input">
                {intent !== "assistant" ? (
                  <span className={"ai-intent ai-intent--" + intent}><IntentIcon size={13} /> {t("ai.tabs." + intent)}</span>
                ) : null}
                <textarea
                  ref={inputRef}
                  value={question}
                  rows={1}
                  maxLength={8000}
                  placeholder={t(intent === "moderation" ? "ai.moderation.contentPlaceholder" : "ai.copilot.placeholder")}
                  onChange={(event) => setQuestion(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" && !event.shiftKey) {
                      event.preventDefault();
                      event.currentTarget.form?.requestSubmit();
                    }
                  }}
                />
              </div>
              <button className="ai-send-button" type="submit" disabled={!question.trim() || loading || uploadingAttachment} aria-label={t("ai.assistant.submit")}><Send size={18} /></button>
            </form>
            <p className="ai-disclaimer">{t("ai.copilot.disclaimer")}</p>
          </div>
        </section>
      </PageShell>
    </TraderLayout>
  );
}
export default function AiPage() {
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  return isAuthenticated ? <AuthenticatedAiPage /> : <GuestAiDemo />;
}