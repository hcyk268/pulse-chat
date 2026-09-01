import {
  ArrowUpRight,
  AtSign,
  Bell,
  Eye,
  Mail,
  MailOpen,
  ShieldCheck,
  Trash2,
  TrendingUp,
  Users,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import Avatar from "../../components/shared/Avatar.jsx";
import { classNames } from "../../components/shared/utils.js";
import {
  NOTIFICATION_TYPE_KEYS,
  resolveNotificationAction,
} from "../../domain/notifications/notifications.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { formatRelativeTime } from "../../utils/formatters.js";

const typeIcons = {
  PRICE_ALERT: TrendingUp,
  MENTION: AtSign,
  COMMUNITY: Users,
  SYSTEM: ShieldCheck,
};

export default function NotificationCard({
  busy,
  item,
  onDelete,
  onOpen,
  onToggleRead,
}) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const Icon = typeIcons[item.type] ?? Bell;
  const actorName = item.actor?.displayName || item.actor?.username;
  const action = resolveNotificationAction(item);

  function handleActionClick() {
    if (!item.read) onToggleRead(item);
    if (action?.path) navigate(action.path);
  }

  return (
    <article className={classNames("notification-card", !item.read && "is-unread")}>
      {item.actor ? (
        <Avatar
          name={actorName}
          seed={item.actor.username ?? item.actor.id}
          src={item.actor.avatarUrl}
        />
      ) : (
        <span>
          <Icon size={19} />
        </span>
      )}

      <div>
        <small>{t(NOTIFICATION_TYPE_KEYS[item.type] ?? item.type)}</small>
        <button
          className="notification-card__title"
          type="button"
          onClick={() => onOpen(item.id)}
          disabled={busy}
        >
          {item.title}
        </button>
        {item.body ? <p>{item.body}</p> : null}
        <div className="notification-card__foot">
          <time dateTime={item.createdAt}>{formatRelativeTime(item.createdAt)}</time>
          {action ? (
            <button type="button" onClick={handleActionClick} disabled={busy}>
              <ArrowUpRight size={14} />
              {t(action.labelKey)}
            </button>
          ) : null}
          <button type="button" onClick={() => onOpen(item.id)} disabled={busy}>
            <Eye size={14} />
            {t("notifications.viewDetail")}
          </button>
          <button type="button" onClick={() => onToggleRead(item)} disabled={busy}>
            {item.read ? <Mail size={14} /> : <MailOpen size={14} />}
            {t(item.read ? "notifications.markUnread" : "notifications.markRead")}
          </button>
          <button
            className="notification-card__delete"
            type="button"
            onClick={() => onDelete(item)}
            disabled={busy}
          >
            <Trash2 size={14} />
            {t("notifications.delete")}
          </button>
        </div>
      </div>

      {!item.read ? <em /> : null}
    </article>
  );
}
