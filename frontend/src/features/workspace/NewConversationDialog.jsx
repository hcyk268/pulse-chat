import { Search, Users } from "lucide-react";
import { useEffect, useState } from "react";
import { getApiErrorMessage, searchUsers } from "../../api/chatApi.js";
import Avatar from "../../components/shared/Avatar";
import Modal from "../../components/shared/Modal";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { useTranslation } from "../../i18n/useTranslation.js";
import { formatPresence } from "../../utils/formatters";

const MIN_QUERY_LENGTH = 2;

export default function NewConversationDialog({ onClose, onSelectUser, onCreateGroup }) {
  const { t } = useTranslation();
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState("");
  const [creatingId, setCreatingId] = useState(null);
  const debouncedQuery = useDebouncedValue(query.trim(), 300);

  useEffect(() => {
    if (debouncedQuery.length < MIN_QUERY_LENGTH) {
      setResults([]);
      setSearching(false);
      return undefined;
    }

    let ignore = false;
    setSearching(true);
    setError("");

    searchUsers({ query: debouncedQuery, limit: 10 })
      .then((response) => {
        if (!ignore) setResults(response?.items ?? []);
      })
      .catch((apiError) => {
        if (!ignore) setError(getApiErrorMessage(apiError, t, "errors.userSearch"));
      })
      .finally(() => {
        if (!ignore) setSearching(false);
      });

    return () => {
      ignore = true;
    };
  }, [debouncedQuery, t]);

  async function handleSelect(user) {
    setCreatingId(user.id);
    setError("");

    try {
      await onSelectUser(user);
      onClose();
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, t, "errors.openConversation"));
    } finally {
      setCreatingId(null);
    }
  }

  return (
    <Modal
      title={t("chat.dialog.title")}
      description={t("chat.dialog.description")}
      onClose={onClose}
    >
        {onCreateGroup ? (
          <button className="button button--ghost" type="button" onClick={onCreateGroup}>
            <Users size={16} /> {t("chat.group.createTitle")}
          </button>
        ) : null}
      <label className="sidebar-search">
        <Search size={17} />
        <input
          type="search"
          placeholder={t("chat.dialog.searchPlaceholder")}
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          aria-label={t("chat.dialog.searchAria")}
        />
      </label>

      {error ? (
        <p className="form-error" role="alert">
          {error}
        </p>
      ) : null}

      <div className="user-result-list">
        {results.map((user) => (
          <button
            key={user.id}
            type="button"
            disabled={creatingId != null}
            onClick={() => handleSelect(user)}
          >
            <Avatar
              name={user.displayName || user.username}
              seed={user.username ?? user.id}
              src={user.avatarUrl}
            />
            <span>
              <strong>{user.displayName || user.username}</strong>
              <small>
                @{user.username} · {formatPresence(user.presence, t)}
              </small>
            </span>
            <span className="user-result-list__action">
              {creatingId === user.id
                ? t("common.opening")
                : user.directConversationId
                  ? t("chat.dialog.open")
                  : t("chat.dialog.message")}
            </span>
          </button>
        ))}

        {searching ? <p className="sidebar-empty">{t("common.searching")}</p> : null}

        {!searching && debouncedQuery.length >= MIN_QUERY_LENGTH && results.length === 0 ? (
          <p className="sidebar-empty">{t("chat.dialog.noResults")}</p>
        ) : null}

        {debouncedQuery.length < MIN_QUERY_LENGTH ? (
          <p className="sidebar-empty">{t("chat.dialog.minChars", { count: MIN_QUERY_LENGTH })}</p>
        ) : null}
      </div>
    </Modal>
  );
}
