import { Plus, Search, X } from "lucide-react";
import { useMemo, useState } from "react";
import Avatar from "../../components/shared/Avatar";
import { classNames } from "../../components/shared/utils";
import { getMessagePreview } from "../../domain/chat/normalizers.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { clampPreview, formatShortTime } from "../../utils/formatters";
import { filterConversations, isSameId } from "../../utils/chat.js";

export default function ConversationSidebar({
  activeConversationId,
  conversations = [],
  drawerOpen = false,
  hasMore = false,
  loading = false,
  loadingMore = false,
  onCloseDrawer,
  onNewConversation,
  onLoadMore,
  onSelect,
}) {
  const { t } = useTranslation();
  const [query, setQuery] = useState("");
  const visibleConversations = useMemo(
    () => filterConversations(conversations, query),
    [conversations, query],
  );

  return (
    <aside className={classNames("workspace-sidebar", "chat-list", drawerOpen && "is-open")}>
      <div className="workspace-sidebar__title">
        <strong>{t("chat.title")}</strong>
        <div className="workspace-sidebar__tools">
          <button
            className="icon-button"
            type="button"
            aria-label={t("chat.newConversation")}
            onClick={onNewConversation}
          >
            <Plus size={18} />
          </button>
          <button
            className="icon-button show-sm"
            type="button"
            aria-label={t("chat.closeConversations")}
            onClick={onCloseDrawer}
          >
            <X size={18} />
          </button>
        </div>
      </div>

      <label className="sidebar-search">
        <Search size={17} />
        <input
          type="search"
          placeholder={t("chat.searchPlaceholder")}
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
      </label>

      <nav aria-label={t("chat.conversations")}>
        {visibleConversations.map((conversation) => (
          <button
            key={conversation.id}
            className={classNames(isSameId(conversation.id, activeConversationId) && "is-active")}
            type="button"
            onClick={() => onSelect?.(conversation.id)}
          >
            <Avatar
              name={conversation.title}
              seed={conversation.id}
              src={conversation.avatarUrl}
            />
            <span>
              <strong>{conversation.title}</strong>
              <small>
                {clampPreview(getMessagePreview(conversation.lastMessage), 38) || t("chat.noPreview")}
              </small>
            </span>
            <span className="chat-list__meta">
              <time>{formatShortTime(conversation.lastMessageAt)}</time>
              {conversation.unreadCount > 0 ? <em>{conversation.unreadCount}</em> : null}
            </span>
          </button>
        ))}

        {hasMore && !query.trim() ? (
          <button
            className="button button--ghost chat-list__load-more"
            type="button"
            disabled={loadingMore}
            onClick={onLoadMore}
          >
            {loadingMore ? t("common.loading") : t("chat.loadMoreConversations")}
          </button>
        ) : null}

        {loading && conversations.length === 0 ? (
          <p className="sidebar-empty">{t("chat.loadingConversations")}</p>
        ) : null}

        {!loading && conversations.length === 0 ? (
          <p className="sidebar-empty">{t("chat.noConversations")}</p>
        ) : null}

        {!loading && conversations.length > 0 && visibleConversations.length === 0 ? (
          <p className="sidebar-empty">{t("chat.noMatch")}</p>
        ) : null}
      </nav>
    </aside>
  );
}
