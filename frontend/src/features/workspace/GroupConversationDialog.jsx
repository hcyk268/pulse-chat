import { Check, Search, Users } from "lucide-react";
import { useEffect, useState } from "react";
import Avatar from "../../components/shared/Avatar";
import Modal from "../../components/shared/Modal";
import { getApiErrorMessage, searchUsers } from "../../api/chatApi.js";
import { useDebouncedValue } from "../../hooks/useDebouncedValue";
import { useTranslation } from "../../i18n/useTranslation.js";

const MIN_QUERY_LENGTH = 2;

export default function GroupConversationDialog({ mode = "create", existingMemberIds = [], onClose, onCreate }) {
  const { t } = useTranslation();
  const isInvite = mode === "invite";
  const [name, setName] = useState("");
  const [avatarUrl, setAvatarUrl] = useState("");
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [selected, setSelected] = useState([]);
  const [searching, setSearching] = useState(false);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState("");
  const debouncedQuery = useDebouncedValue(query.trim(), 250);

  useEffect(() => {
    if (debouncedQuery.length < MIN_QUERY_LENGTH) {
      setResults([]);
      setSearching(false);
      return undefined;
    }

    let ignore = false;
    setSearching(true);
    searchUsers({ query: debouncedQuery, limit: 20 })
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

  function toggleUser(user) {
    if (existingMemberIds.some((id) => String(id) === String(user.id))) return;
    setSelected((current) =>
      current.some((item) => String(item.id) === String(user.id))
        ? current.filter((item) => String(item.id) !== String(user.id))
        : [...current, user],
    );
  }

  async function handleSubmit(event) {
    event.preventDefault();
    if ((!isInvite && !name.trim()) || selected.length < (isInvite ? 1 : 2) || creating) return;

    setCreating(true);
    setError("");
    try {
      await onCreate(
        isInvite
          ? { memberIds: selected.map((user) => user.id) }
          : {
              name: name.trim(),
              avatarUrl: avatarUrl.trim() || null,
              memberIds: selected.map((user) => user.id),
            },
      );
      onClose();
    } catch (apiError) {
      setError(getApiErrorMessage(apiError, t, "errors.createGroup"));
    } finally {
      setCreating(false);
    }
  }

  return (
    <Modal
      title={t(isInvite ? "chat.group.inviteTitle" : "chat.group.createTitle")}
      description={t(isInvite ? "chat.group.inviteDescription" : "chat.group.createDescription")}
      onClose={onClose}
    >
      <form className="dialog-form group-dialog" onSubmit={handleSubmit} noValidate>
        <label>
          {t("chat.group.name")}
          <input
            value={name}
            maxLength={100}
            required
            placeholder={t("chat.group.namePlaceholder")}
            onChange={(event) => setName(event.target.value)}
          />
        </label>
        <label>
          {t("chat.group.avatarUrl")}
          <input
            value={avatarUrl}
            maxLength={500}
            placeholder="https://example.com/group.png"
            onChange={(event) => setAvatarUrl(event.target.value)}
          />
        </label>
        <label>
          {t("chat.group.members", { count: selected.length })}
          <div className="sidebar-search">
            <Search size={17} />
            <input
              value={query}
              placeholder={t("chat.group.memberSearch")}
              onChange={(event) => setQuery(event.target.value)}
              aria-label={t("chat.group.memberSearch")}
            />
          </div>
        </label>
        {error ? <p className="form-error" role="alert">{error}</p> : null}
        <div className="user-result-list group-dialog__results">
          {results.map((user) => {
            const isSelected = selected.some((item) => String(item.id) === String(user.id));
            return (
              <button
                key={user.id}
                className={isSelected ? "is-selected" : ""}
                type="button"
                onClick={() => toggleUser(user)}
              >
                <Avatar name={user.displayName || user.username} seed={user.id} src={user.avatarUrl} />
                <span>
                  <strong>{user.displayName || user.username}</strong>
                  <small>@{user.username}</small>
                </span>
                {isSelected ? <Check size={17} /> : <Users size={16} />}
              </button>
            );
          })}
          {searching ? <p className="sidebar-empty">{t("common.searching")}</p> : null}
          {!searching && debouncedQuery.length >= MIN_QUERY_LENGTH && results.length === 0 ? (
            <p className="sidebar-empty">{t("chat.dialog.noResults")}</p>
          ) : null}
          {debouncedQuery.length < MIN_QUERY_LENGTH ? (
            <p className="sidebar-empty">{t("chat.dialog.minChars", { count: MIN_QUERY_LENGTH })}</p>
          ) : null}
        </div>
        <div className="dialog-form__actions">
          <button className="button button--ghost" type="button" onClick={onClose}>
            {t("common.cancel")}
          </button>
          <button className="button button--primary" type="submit" disabled={(!isInvite && !name.trim()) || selected.length < (isInvite ? 1 : 2) || creating}>
            {creating ? t("common.saving") : t(isInvite ? "chat.group.invite" : "chat.group.create")}
          </button>
        </div>
      </form>
    </Modal>
  );
}