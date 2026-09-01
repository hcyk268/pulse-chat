import {
  ArrowLeft,
  Check,
  PanelLeft,
  Pencil,
  Settings,
  UserPlus,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import {
  createCommunityChannel,
  getApiErrorMessage,
  getCommunity,
  getCommunityCategories,
  getCommunityTags,
  joinCommunity,
  leaveCommunity,
  normalizeCommunityChannel,
  normalizeCommunityDetail,
  updateCommunity,
  updateCommunityChannel,
} from "../api/communityApi";
import TraderLayout from "../components/layout/TraderLayout";
import Alert from "../components/shared/Alert";
import { classNames } from "../components/shared/utils";
import ChannelFormDialog from "../features/community/ChannelFormDialog";
import CommunityFormDialog from "../features/community/CommunityFormDialog";
import {
  canManageChannels,
  canManageCommunity,
} from "../features/community/communityManagement.js";
import ChannelSidebar from "../features/workspace/ChannelSidebar";
import useCommunityChannelChat from "../features/community/useCommunityChannelChat.js";
import MemberPanel from "../features/workspace/MemberPanel";
import MessageStream from "../features/workspace/MessageStream";
import WorkspaceShell from "../features/workspace/WorkspaceShell";
import { useWorkspaceDrawer } from "../features/workspace/useWorkspaceDrawer";
import { useTranslation } from "../i18n/useTranslation.js";
import { useAuthGate } from "../features/auth/AuthGateContext.jsx";
import { useAppDispatch, useAppSelector } from "../store/hooks";
import { selectCurrentUser, selectIsAuthenticated } from "../store/slices/authSlice";
import {
  selectActiveChannelId,
  selectChannelUnread,
  setActiveChannel,
  incrementChannelUnread,
  upsertJoinedCommunity,
} from "../store/slices/communitySlice";

const EMPTY_ARRAY = Object.freeze([]);


export default function CommunityDetailPage() {
  const { slug } = useParams();
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const { openAuth } = useAuthGate();
  const location = useLocation();
  const [aiDraft] = useState(() => location.state?.aiDraft ?? "");

  useEffect(() => {
    if (location.state?.aiDraft) navigate(location.pathname, { replace: true, state: {} });
  }, [location.pathname, location.state?.aiDraft, navigate]);
  const { open: drawerOpen, openDrawer, closeDrawer } = useWorkspaceDrawer();
  const currentUser = useAppSelector(selectCurrentUser);
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const activeChannelId = useAppSelector(selectActiveChannelId);
  const localUnreadByChannel = useAppSelector(selectChannelUnread);
  const [detail, setDetail] = useState(null);
  const [categoryOptions, setCategoryOptions] = useState([]);
  const [tagOptions, setTagOptions] = useState([]);
  const [managementDialog, setManagementDialog] = useState(null);
  const [loadingDetail, setLoadingDetail] = useState(true);
  const [membershipSaving, setMembershipSaving] = useState(false);
  const [error, setError] = useState("");

  const community = detail?.community ?? null;
  const channels = detail?.channels ?? EMPTY_ARRAY;
  const activeChannel =
    channels.find((channel) => String(channel.id) === String(activeChannelId)) ??
    channels[0] ??
    null;
  const conversationId = activeChannel?.conversationId ?? null;
  const isJoined = Boolean(community?.isMember);
  const membershipRole = community?.membershipRole ?? null;
  const isOwner = membershipRole === "OWNER";
  const mayManageCommunity = canManageCommunity(membershipRole);
  const mayManageChannels = canManageChannels(membershipRole);
  const currentUserId = currentUser?.id ?? null;
  const unreadByChannel = useMemo(
    () => ({
      ...Object.fromEntries(channels.map((channel) => [channel.id, channel.unread])),
      ...localUnreadByChannel,
    }),
    [channels, localUnreadByChannel],
  );
  const handleOtherConversationMessage = useCallback(
    (messageConversationId) => {
      const channel = channels.find((item) =>
        String(item.conversationId) === String(messageConversationId),
      );
      if (channel?.id != null) dispatch(incrementChannelUnread(channel.id));
    },
    [channels, dispatch],
  );
  const handlePresenceUpdated = useCallback((presence) => {
    if (presence?.userId == null) return;
    setDetail((current) =>
      current
        ? {
            ...current,
            members: (current.members ?? []).map((member) =>
              String(member.id) === String(presence.userId)
                ? {
                    ...member,
                    presence: {
                      isOnline: Boolean(presence.isOnline),
                      lastActiveAt: presence.lastActiveAt ?? null,
                    },
                  }
                : member,
            ),
          }
        : current,
    );
  }, []);



  const {
    deleteMessage,
    dismissError: dismissChatError,
    editMessage,
    error: chatError,
    hasOlderMessages,
    loadOlderMessages,
    loadReadReceipts,
    loading: loadingMessages,
    loadingOlder,
    messages,
    notifyTyping,
    pinMessage,
    sendChannelMessage,
    sending,
    toggleReaction,
    typingUsers,
    unpinMessage,
  } = useCommunityChannelChat({
    conversationId,
    currentUser,
    onOtherConversationMessage: handleOtherConversationMessage,
    onPresenceUpdated: handlePresenceUpdated,
    t,
  });



  useEffect(() => {
    let active = true;
    setLoadingDetail(true);
    setError("");

    getCommunity(slug)
      .then((response) => {
        if (!active) return;
        const normalized = normalizeCommunityDetail(response);
        setDetail(normalized);

        const defaultChannelId =
          normalized?.channels.find((channel) => channel.defaultChannel)?.id ??
          normalized?.community?.defaultChannelId ??
          normalized?.channels[0]?.id;

        if (defaultChannelId != null) dispatch(setActiveChannel(defaultChannelId));
        if (normalized?.community?.id != null) {
          dispatch(
            upsertJoinedCommunity({
              communityId: normalized.community.id,
              joined: normalized.community.isMember,
            }),
          );
        }
      })
      .catch((requestError) => {
        if (active) setError(getApiErrorMessage(requestError, t, "errors.communityDetail"));
      })
      .finally(() => {
        if (active) setLoadingDetail(false);
      });

    return () => {
      active = false;
    };
  }, [dispatch, slug, t]);

  useEffect(() => {
    let active = true;
    Promise.allSettled([getCommunityCategories(), getCommunityTags()]).then(
      ([categoriesResult, tagsResult]) => {
        if (!active) return;
        if (categoriesResult.status === "fulfilled") {
          setCategoryOptions(categoriesResult.value ?? []);
        }
        if (tagsResult.status === "fulfilled") {
          setTagOptions(tagsResult.value ?? []);
        }
      },
    );
    return () => {
      active = false;
    };
  }, []);

  const handleToggleJoin = useCallback(async () => {
    if (!community?.id || isOwner || membershipSaving) return;

    if (!isAuthenticated) {
      openAuth({
        kind: "community.join",
        title: t("guest.community.joinTitle", { community: community.name }),
        description: t("guest.community.joinDescription", { community: community.name }),
        payload: { communityId: community.id, slug },
      }, async () => {
        setMembershipSaving(true);
        try {
          const nextDetail = normalizeCommunityDetail(await joinCommunity(community.id));
          if (nextDetail) setDetail(nextDetail);
          dispatch(upsertJoinedCommunity({ communityId: community.id, joined: true }));
        } catch (requestError) {
          setError(getApiErrorMessage(requestError, t, "errors.communityJoin"));
        } finally {
          setMembershipSaving(false);
        }
      });
      return;
    }

    const wasJoined = community.isMember;
    const optimisticCommunity = {
      ...community,
      isMember: !wasJoined,
      membershipRole: wasJoined ? null : community.membershipRole,
      memberCount: Math.max(0, (community.memberCount ?? 0) + (wasJoined ? -1 : 1)),
    };

    setMembershipSaving(true);
    setDetail((current) => (current ? { ...current, community: optimisticCommunity } : current));
    dispatch(upsertJoinedCommunity({ communityId: community.id, joined: !wasJoined }));
    setError("");

    try {
      if (wasJoined) {
        await leaveCommunity(community.id);
      } else {
        const nextDetail = normalizeCommunityDetail(await joinCommunity(community.id));
        if (nextDetail) setDetail(nextDetail);
      }
    } catch (requestError) {
      setDetail((current) => (current ? { ...current, community } : current));
      dispatch(upsertJoinedCommunity({ communityId: community.id, joined: wasJoined }));
      setError(getApiErrorMessage(requestError, t, "errors.communityJoin"));
    } finally {
      setMembershipSaving(false);
    }
  }, [
    community,
    dispatch,
    isAuthenticated,
    isOwner,
    membershipSaving,
    openAuth,
    slug,
    t,
  ]);

  function handleCommunitySaved(response) {
    const updated = normalizeCommunityDetail(response);
    if (!updated) return;
    setDetail(updated);
    if (updated.community?.slug && updated.community.slug !== slug) {
      navigate(`/community/${updated.community.slug}`, { replace: true });
    }
  }

  function handleChannelCreated(response) {
    const channel = normalizeCommunityChannel(response);
    if (!channel) return;
    setDetail((current) =>
      current ? { ...current, channels: [...(current.channels ?? []), channel] } : current,
    );
    dispatch(setActiveChannel(channel.id));
  }

  function handleChannelUpdated(response) {
    const channel = normalizeCommunityChannel(response);
    if (!channel) return;
    setDetail((current) =>
      current
        ? {
            ...current,
            channels: (current.channels ?? []).map((item) =>
              String(item.id) === String(channel.id) ? channel : item,
            ),
          }
        : current,
    );
    dispatch(setActiveChannel(channel.id));
  }

  if (loadingDetail) {
    return (
      <TraderLayout active="community-detail" appFrame>
        <main className="page-shell">
          <div className="market-page-state">{t("common.loading")}</div>
        </main>
      </TraderLayout>
    );
  }

  if (!community) {
    return (
      <TraderLayout active="community">
        <main className="page-shell">
          <div className="market-page-state">
            {t("community.notFound")}{" "}
            <Link to="/community">{t("community.notFoundAction")}</Link>
          </div>
        </main>
      </TraderLayout>
    );
  }

  const canPost =
    isJoined &&
    conversationId &&
    (!activeChannel?.readOnly || mayManageChannels);

  return (
    <TraderLayout active="community-detail" appFrame>
      <WorkspaceShell className="community-workspace">
        <ChannelSidebar
          activeChannelId={activeChannel?.id}
          channels={channels}
          community={community}
          drawerOpen={drawerOpen}
          onCloseDrawer={closeDrawer}
          onCreateChannel={() => setManagementDialog({ type: "create-channel" })}
          onSelect={(channelId) => {
            dispatch(setActiveChannel(channelId));
            closeDrawer();
          }}
          unreadByChannel={unreadByChannel}
          canManageChannels={mayManageChannels}
        />

        {drawerOpen ? (
          <button
            className="workspace-drawer-backdrop"
            type="button"
            aria-label={t("community.closeChannels")}
            onClick={closeDrawer}
          />
        ) : null}

        <section className="workspace-main">
          <header className="workspace-header">
            <Link className="icon-button show-sm" to="/community" aria-label={t("community.back")}>
              <ArrowLeft size={18} />
            </Link>
            <button
              className="icon-button show-sm"
              type="button"
              aria-label={t("community.openChannels")}
              aria-expanded={drawerOpen}
              onClick={openDrawer}
            >
              <PanelLeft size={19} />
            </button>
            <div>
              <h1># {activeChannel?.label ?? community.name}</h1>
              <p>{activeChannel?.description || community.description}</p>
            </div>
            <div className="community-workspace__actions">
              {mayManageChannels && activeChannel ? (
                <button
                  className="icon-button"
                  type="button"
                  aria-label={t("community.channel.edit")}
                  title={t("community.channel.edit")}
                  onClick={() =>
                    setManagementDialog({ type: "edit-channel", channel: activeChannel })
                  }
                >
                  <Pencil size={17} />
                </button>
              ) : null}
              {mayManageCommunity ? (
                <button
                  className="icon-button"
                  type="button"
                  aria-label={t("community.edit")}
                  title={t("community.edit")}
                  onClick={() => setManagementDialog({ type: "edit-community" })}
                >
                  <Settings size={17} />
                </button>
              ) : null}
              <button
                className={classNames(
                  "button",
                  isJoined ? "button--ghost" : "button--primary",
                )}
                type="button"
                disabled={membershipSaving || isOwner}
                onClick={handleToggleJoin}
              >
                {isJoined ? <Check size={16} /> : <UserPlus size={16} />}
                {isOwner
                  ? t("community.owner")
                  : isJoined
                    ? t("community.joined")
                    : t("community.join")}
              </button>
            </div>
          </header>

          {error ? <Alert onDismiss={() => setError("")}>{error}</Alert> : null}
          {chatError ? <Alert onDismiss={dismissChatError}>{chatError}</Alert> : null}

          <MessageStream
            key={activeChannel?.id ?? "empty"}
            conversationId={conversationId}
            communitySlug={community.slug}
            currentUserId={currentUserId}
            initialDraft={aiDraft}
            emptyLabel={t("community.channelEmpty")}
            hasOlderMessages={hasOlderMessages}
            loading={loadingMessages}
            loadingOlder={loadingOlder}
            messages={messages}
            onDelete={isJoined ? deleteMessage : null}
            onEdit={isJoined ? editMessage : null}
            onLoadOlder={loadOlderMessages}
            onGuestAction={handleToggleJoin}
            onPin={isJoined ? pinMessage : null}
            onReact={isJoined ? toggleReaction : null}
            onReadReceipts={isJoined ? loadReadReceipts : null}
            onTyping={canPost ? notifyTyping : null}
            onUnpin={isJoined ? unpinMessage : null}
            onSend={canPost ? sendChannelMessage : null}
            placeholder={
              isJoined
                ? t("community.messageChannel", {
                    channel: activeChannel?.label ?? community.name,
                  })
                : t("community.joinToPost")
            }
            readOnly={!isAuthenticated}
            participants={detail?.members ?? []}
            sending={sending}
            typingUsers={typingUsers}
          />
        </section>

        <MemberPanel members={detail?.members ?? []} titleKey="chat.members" />
      </WorkspaceShell>

      {managementDialog?.type === "edit-community" ? (
        <CommunityFormDialog
          mode="edit"
          community={community}
          categories={categoryOptions}
          tags={tagOptions}
          onClose={() => setManagementDialog(null)}
          onSaved={handleCommunitySaved}
          onSubmit={(request) => updateCommunity(community.id, request)}
        />
      ) : null}

      {managementDialog?.type === "create-channel" ? (
        <ChannelFormDialog
          mode="create"
          onClose={() => setManagementDialog(null)}
          onSaved={handleChannelCreated}
          onSubmit={(request) => createCommunityChannel(community.id, request)}
        />
      ) : null}

      {managementDialog?.type === "edit-channel" ? (
        <ChannelFormDialog
          key={managementDialog.channel.id}
          mode="edit"
          channel={managementDialog.channel}
          onClose={() => setManagementDialog(null)}
          onSaved={handleChannelUpdated}
          onSubmit={(request) =>
            updateCommunityChannel(managementDialog.channel.id, request)
          }
        />
      ) : null}
    </TraderLayout>
  );
}

