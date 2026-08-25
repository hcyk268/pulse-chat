import { Plus, X } from "lucide-react";
import { classNames } from "../../components/shared/utils";
import { useTranslation } from "../../i18n/useTranslation.js";
import { formatCompactNumber } from "../../utils/formatters";

export default function ChannelSidebar({
  activeChannelId,
  channels = [],
  community,
  drawerOpen = false,
  onCloseDrawer,
  onCreateChannel,
  onSelect,
  unreadByChannel = {},
  canManageChannels = false,
}) {
  const { t } = useTranslation();

  return (
    <aside className={classNames("workspace-sidebar", drawerOpen && "is-open")}>
      <div className="workspace-sidebar__title">
        <div>
          <strong>{community?.name ?? "Community"}</strong>
          <span>{t("community.members", { count: formatCompactNumber(community?.memberCount) })}</span>
        </div>
        <div className="workspace-sidebar__tools">
          {canManageChannels ? (
            <button
              className="icon-button"
              type="button"
              aria-label={t("community.channel.add")}
              title={t("community.channel.add")}
              onClick={onCreateChannel}
            >
              <Plus size={18} />
            </button>
          ) : null}
          <button
            className="icon-button show-sm"
            type="button"
            aria-label={t("community.closeChannels")}
            onClick={onCloseDrawer}
          >
            <X size={18} />
          </button>
        </div>
      </div>
      <nav aria-label={t("community.channels")}>
        {channels.map((channel) => {
          const unread = unreadByChannel[channel.id] ?? 0;

          return (
            <button
              key={channel.id}
              className={classNames(channel.id === activeChannelId && "is-active")}
              type="button"
              onClick={() => onSelect?.(channel.id)}
            >
              <span># {channel.label}</span>
              {unread > 0 ? <em>{unread}</em> : null}
            </button>
          );
        })}
      </nav>
    </aside>
  );
}
