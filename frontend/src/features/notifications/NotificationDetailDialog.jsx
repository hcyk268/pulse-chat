import { ArrowUpRight, Mail, MailOpen, Trash2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import Modal from "../../components/shared/Modal.jsx";
import Avatar from "../../components/shared/Avatar.jsx";
import {
  NOTIFICATION_TARGET_KEYS,
  NOTIFICATION_TYPE_KEYS,
  resolveNotificationAction,
} from "../../domain/notifications/notifications.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { formatLongTime } from "../../utils/formatters.js";

function DetailRow({ label, children }) {
  if (!children) return null;

  return (
    <div>
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}

export default function NotificationDetailDialog({
  busy,
  item,
  onClose,
  onDelete,
  onToggleRead,
}) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const actorName = item.actor?.displayName || item.actor?.username;
  const targetLabel = item.targetType
    ? t(NOTIFICATION_TARGET_KEYS[item.targetType] ?? item.targetType)
    : "";
  const action = resolveNotificationAction(item);

  function handleActionClick() {
    if (!item.read) onToggleRead(item);
    if (action?.path) {
      onClose?.();
      navigate(action.path);
    }
  }

  return (
    <Modal
      title={item.title}
      description={t(NOTIFICATION_TYPE_KEYS[item.type] ?? item.type)}
      onClose={busy ? undefined : onClose}
    >
      <div className="notification-detail">
        {actorName ? (
          <div className="notification-detail__actor">
            <Avatar
              name={actorName}
              seed={item.actor?.username ?? item.actor?.id}
              src={item.actor?.avatarUrl}
              size="sm"
            />
            <div>
              <small>{t("notifications.detail.actor")}</small>
              <strong>{actorName}</strong>
            </div>
          </div>
        ) : null}

        {item.body ? <p>{item.body}</p> : null}

        <dl className="notification-detail__meta">
          <DetailRow label={t("notifications.detail.status")}>
            {t(item.read ? "notifications.status.read" : "notifications.status.unread")}
          </DetailRow>
          <DetailRow label={t("notifications.detail.created")}>
            {formatLongTime(item.createdAt)}
          </DetailRow>
          <DetailRow label={t("notifications.detail.updated")}>
            {formatLongTime(item.updatedAt)}
          </DetailRow>
          <DetailRow label={t("notifications.detail.readAt")}>
            {formatLongTime(item.readAt)}
          </DetailRow>
          <DetailRow label={t("notifications.detail.target")}>
            {targetLabel && item.targetId ? `${targetLabel} #${item.targetId}` : targetLabel}
          </DetailRow>
          <DetailRow label={t("notifications.detail.source")}>
            {item.sourceType && item.sourceId
              ? `${item.sourceType} #${item.sourceId}`
              : item.sourceType}
          </DetailRow>
        </dl>

        <div className="dialog-form__actions">
          {action ? (
            <button
              className="button button--primary"
              type="button"
              onClick={handleActionClick}
              disabled={busy}
            >
              <ArrowUpRight size={16} />
              {t(action.labelKey)}
            </button>
          ) : null}
          <button
            className="button button--ghost"
            type="button"
            onClick={() => onToggleRead(item)}
            disabled={busy}
          >
            {item.read ? <Mail size={16} /> : <MailOpen size={16} />}
            {t(item.read ? "notifications.markUnread" : "notifications.markRead")}
          </button>
          <button
            className="button notification-delete-button"
            type="button"
            onClick={() => onDelete(item)}
            disabled={busy}
          >
            <Trash2 size={16} />
            {t("notifications.delete")}
          </button>
        </div>
      </div>
    </Modal>
  );
}
