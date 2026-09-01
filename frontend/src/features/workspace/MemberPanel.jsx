import { LogOut, Pencil, Shield, UserMinus, UserPlus, X } from "lucide-react";
import Avatar from "../../components/shared/Avatar";
import { useTranslation } from "../../i18n/useTranslation.js";
import { formatPresence } from "../../utils/formatters";
import { isSameId } from "../../utils/chat.js";

export default function MemberPanel({
  members = [],
  excludeId = null,
  titleKey = "chat.participants",
  isGroup = false,
  canManage = false,
  isPendingInvitation = false,
  onAcceptInvitation,
  onRejectInvitation,
  onInvite,
  onEditProfile,
  onLeave,
  onRemoveMember,
  onChangeRole,
}) {
  const { t } = useTranslation();
  const visibleMembers = members.filter(
    (member) => member && !(excludeId != null && isSameId(member.id, excludeId)),
  );

  const onlineCount = visibleMembers.filter((member) => member.presence?.isOnline).length;

  return (
    <aside className="members-panel">
      <div className="panel-heading">
        <div>
          <h2>{t(titleKey)}</h2>
          <span className="members-panel__count">{t("common.onlineCount", { count: onlineCount })}</span>
        </div>
        {isGroup ? (
          <div className="members-panel__actions">
            {canManage ? (
              <>
                <button className="icon-button" type="button" title={t("chat.group.invite")} aria-label={t("chat.group.invite")} onClick={onInvite}>
                  <UserPlus size={15} />
                </button>
                <button className="icon-button" type="button" title={t("chat.group.editProfile")} aria-label={t("chat.group.editProfile")} onClick={onEditProfile}>
                  <Pencil size={15} />
                </button>
              </>
            ) : null}
            {isPendingInvitation ? (
              <>
                <button className="icon-button" type="button" title={t("chat.group.accept")} aria-label={t("chat.group.accept")} onClick={onAcceptInvitation}>
                  <Shield size={15} />
                </button>
                <button className="icon-button" type="button" title={t("chat.group.reject")} aria-label={t("chat.group.reject")} onClick={onRejectInvitation}>
                  <X size={15} />
                </button>
              </>
            ) : (
              <button className="icon-button" type="button" title={t("chat.group.leave")} aria-label={t("chat.group.leave")} onClick={onLeave}>
                <LogOut size={15} />
              </button>
            )}
          </div>
        ) : null}
      </div>

      {visibleMembers.map((member) => (
        <div className="member-row" key={member.id ?? member.username ?? member.displayName}>
          <Avatar
            name={member.displayName}
            seed={member.username ?? member.id ?? member.displayName}
            src={member.avatarUrl}
          />
          <div className="member-row__identity">
            <strong>{member.displayName}</strong>
            <span>{member.role ?? formatPresence(member.presence, t)}</span>
          </div>
          {canManage && member.id != null ? (
            <div className="member-row__actions">
              <button
                className="icon-button"
                type="button"
                title={t("chat.group.toggleRole")}
                aria-label={t("chat.group.toggleRole")}
                onClick={() => onChangeRole?.(member.id, member.role === "OWNER" ? "MEMBER" : "OWNER")}
              >
                <Shield size={14} />
              </button>
              <button
                className="icon-button"
                type="button"
                title={t("chat.group.removeMember")}
                aria-label={t("chat.group.removeMember")}
                onClick={() => onRemoveMember?.(member.id)}
              >
                <UserMinus size={14} />
              </button>
            </div>
          ) : null}
        </div>
      ))}

      {visibleMembers.length === 0 ? (
        <p className="sidebar-empty">{t("chat.noParticipants")}</p>
      ) : null}
    </aside>
  );
}