import {
  Activity,
  ArrowRight,
  LockKeyhole,
  MessageCircleMore,
  PanelLeft,
  Radio,
  RefreshCw,
  Sparkles,
  UsersRound,
} from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  getCommunities,
  getCommunity,
  normalizeCommunityDetail,
  normalizeCommunitySummary,
} from "../../api/communityApi.js";
import { classNames } from "../../components/shared/utils.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { useAuthGate } from "../auth/AuthGateContext.jsx";
import useCommunityChannelChat from "../community/useCommunityChannelChat.js";
import { realtimeStatusKey } from "../market/realtimeStatus.js";
import ChannelSidebar from "../workspace/ChannelSidebar.jsx";
import MemberPanel from "../workspace/MemberPanel.jsx";
import MessageStream from "../workspace/MessageStream.jsx";
import WorkspaceShell from "../workspace/WorkspaceShell.jsx";
import { useWorkspaceDrawer } from "../workspace/useWorkspaceDrawer.js";

const previewRooms = ["market", "bitcoin", "altcoin"];

function GuestChatLanding({ loading, onRetry, onJoin, t }) {
  return (
    <section className={classNames("guest-chat-landing", loading && "is-loading")}>
      <aside className="guest-chat-landing__rail">
        <div className="guest-chat-landing__brand">
          <span><MessageCircleMore size={20} /></span>
          <div>
            <strong>{t("guest.chat.roomsTitle")}</strong>
            <small>{t("guest.readOnly")}</small>
          </div>
        </div>
        <nav aria-label={t("guest.chat.roomsTitle")}>
          {previewRooms.map((room, index) => (
            <span key={room} className={index === 0 ? "is-active" : undefined}>
              <i aria-hidden="true" />
              # {t(`guest.chat.room.${room}`)}
            </span>
          ))}
        </nav>
        <div className="guest-chat-landing__rail-note">
          <Radio size={15} />
          <span>{t("guest.chat.liveNote")}</span>
        </div>
      </aside>

      <div className="guest-chat-landing__content">
        <header>
          <span className="guest-chat-landing__status"><i /> {t("guest.chat.previewMode")}</span>
          <span><LockKeyhole size={14} /> {t("guest.readOnly")}</span>
        </header>

        <div className="guest-chat-landing__hero">
          <div className="guest-chat-landing__copy">
            <span className="guest-chat-landing__eyebrow"><Sparkles size={15} /> {t("guest.chat.eyebrow")}</span>
            <h1>{t("guest.chat.landingTitle")}</h1>
            <p>{t("guest.chat.landingBody")}</p>
            <div className="guest-chat-landing__features">
              <span><Radio size={15} /> {t("guest.chat.featureRealtime")}</span>
              <span><UsersRound size={15} /> {t("guest.chat.featurePublic")}</span>
            </div>
            <div className="guest-chat-landing__actions">
              <button className="button button--primary" type="button" onClick={onJoin}>
                {t("guest.chat.joinAction")} <ArrowRight size={16} />
              </button>
              <button className="button button--ghost" type="button" onClick={onRetry} disabled={loading}>
                <RefreshCw size={16} className={loading ? "spin" : undefined} />
                {t("common.retry")}
              </button>
              <Link to="/market">{t("guest.chat.exploreMarket")}</Link>
            </div>
          </div>

          <div className="guest-chat-preview-card" aria-hidden="true">
            <div className="guest-chat-preview-card__head">
              <span># {t("guest.chat.room.market")}</span>
              <i />
            </div>
            {[72, 88, 64].map((width, index) => (
              <div className="guest-chat-preview-card__message" key={width}>
                <span className={`guest-chat-preview-card__avatar avatar-${index + 1}`} />
                <div>
                  <strong />
                  <p style={{ "--preview-width": `${width}%` }} />
                  <small />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

export default function GuestChatPreview() {
  const { t } = useTranslation();
  const { openAuth } = useAuthGate();
  const { open: drawerOpen, openDrawer, closeDrawer } = useWorkspaceDrawer();
  const [detail, setDetail] = useState(null);
  const [activeChannelId, setActiveChannelId] = useState(null);
  const [loadingDetail, setLoadingDetail] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [loadAttempt, setLoadAttempt] = useState(0);

  useEffect(() => {
    let active = true;
    setLoadingDetail(true);
    setLoadFailed(false);

    getCommunities({ limit: 12 })
      .then((items) => (items ?? []).map(normalizeCommunitySummary).find((item) => item?.slug))
      .then((community) => {
        if (!community?.slug) throw new Error("NO_PUBLIC_COMMUNITY");
        return getCommunity(community.slug);
      })
      .then((response) => {
        if (!active) return;
        const normalized = normalizeCommunityDetail(response);
        setDetail(normalized);
        setActiveChannelId(
          normalized?.channels.find((channel) => channel.defaultChannel)?.id ??
          normalized?.community?.defaultChannelId ??
          normalized?.channels[0]?.id ??
          null,
        );
      })
      .catch(() => {
        if (!active) return;
        setDetail(null);
        setActiveChannelId(null);
        setLoadFailed(true);
      })
      .finally(() => {
        if (active) setLoadingDetail(false);
      });

    return () => {
      active = false;
    };
  }, [loadAttempt]);

  const community = detail?.community ?? null;
  const channels = useMemo(() => detail?.channels ?? [], [detail?.channels]);
  const activeChannel = useMemo(
    () => channels.find((channel) => String(channel.id) === String(activeChannelId)) ?? channels[0] ?? null,
    [activeChannelId, channels],
  );
  const conversationId = activeChannel?.conversationId ?? null;
  const {
    error: chatError,
    hasOlderMessages,
    loadOlderMessages,
    loading,
    loadingOlder,
    messages,
    realtimeStatus,
  } = useCommunityChannelChat({ conversationId, currentUser: null, t });

  function requestParticipation() {
    openAuth({
      kind: "chat",
      mode: "register",
      title: t("guest.chat.authTitle"),
      description: t("guest.chat.authDescription"),
      payload: { conversationId },
    });
  }

  if ((loadingDetail && !detail) || loadFailed || !community || !activeChannel) {
    return (
      <GuestChatLanding
        loading={loadingDetail}
        onJoin={requestParticipation}
        onRetry={() => setLoadAttempt((current) => current + 1)}
        t={t}
      />
    );
  }

  return (
    <WorkspaceShell className="chat-workspace guest-chat-workspace">
      <ChannelSidebar
        activeChannelId={activeChannel.id}
        channels={channels}
        community={community}
        drawerOpen={drawerOpen}
        onCloseDrawer={closeDrawer}
        onSelect={(channelId) => {
          setActiveChannelId(channelId);
          closeDrawer();
        }}
      />

      {drawerOpen ? (
        <button className="workspace-drawer-backdrop" type="button" aria-label={t("community.closeChannels")} onClick={closeDrawer} />
      ) : null}

      <section className="workspace-main">
        <header className="workspace-header">
          <button className="icon-button show-sm" type="button" aria-label={t("community.openChannels")} onClick={openDrawer}>
            <PanelLeft size={19} />
          </button>
          <span className="workspace-header__placeholder" aria-hidden="true"><MessageCircleMore size={20} /></span>
          <div className="workspace-header__identity">
            <h1># {activeChannel.label}</h1>
            <p>{activeChannel.description || community.description || t("guest.chat.description")}</p>
          </div>
          <div>
            <span className="guest-mode-badge"><LockKeyhole size={14} /> {t("guest.readOnly")}</span>
            <span className={classNames("realtime-status", `realtime-status--${realtimeStatus}`)}>
              <Activity size={15} /> {t(realtimeStatusKey(realtimeStatus))}
            </span>
          </div>
        </header>

        {chatError ? (
          <div className="guest-chat-sync-note"><RefreshCw size={15} /> {t("guest.chat.syncPaused")}</div>
        ) : null}

        <MessageStream
          key={activeChannel.id}
          conversationId={conversationId}
          emptyLabel={t("guest.chat.empty")}
          hasOlderMessages={hasOlderMessages}
          loading={loading}
          loadingOlder={loadingOlder}
          messages={messages}
          onLoadOlder={loadOlderMessages}
          onGuestAction={requestParticipation}
          readOnly
        />
      </section>

      <MemberPanel members={detail.members ?? []} titleKey="chat.members" />
    </WorkspaceShell>
  );
}