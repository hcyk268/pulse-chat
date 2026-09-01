import { Activity, MessageCircleMore, PanelLeft, Plus, Sparkles } from "lucide-react";
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { getApiErrorMessage, summarizeConversation } from "../api/aiApi.js";
import TraderLayout from "../components/layout/TraderLayout";
import Alert from "../components/shared/Alert";
import Avatar from "../components/shared/Avatar";
import Modal from "../components/shared/Modal";
import { classNames } from "../components/shared/utils";
import ConversationSidebar from "../features/workspace/ConversationSidebar";
import MemberPanel from "../features/workspace/MemberPanel";
import GroupConversationDialog from "../features/workspace/GroupConversationDialog";
import MessageStream from "../features/workspace/MessageStream";
import NewConversationDialog from "../features/workspace/NewConversationDialog";
import WorkspaceShell from "../features/workspace/WorkspaceShell";
import { useChatWorkspace } from "../features/workspace/useChatWorkspace";
import { useWorkspaceDrawer } from "../features/workspace/useWorkspaceDrawer";
import { getNormalizedConversationContacts } from "../domain/chat/normalizers.js";
import { realtimeStatusKey } from "../features/market/realtimeStatus.js";
import { useTranslation } from "../i18n/useTranslation.js";
import GuestChatPreview from "../features/chat/GuestChatPreview.jsx";
import { useAppSelector } from "../store/hooks.js";
import { selectIsAuthenticated } from "../store/slices/authSlice.js";
import { formatPresence } from "../utils/formatters";

function describeConversation(conversation, t) {
  if (!conversation) return t("chat.noneSelected.body");
  if (conversation.type === "GROUP") {
    return t("chat.groupMembers", { count: conversation.participantCount });
  }

  return formatPresence(conversation.otherParticipant?.presence, t);
}

function AuthenticatedChatPage() {
  const {
    acceptInvitation,
    activeConversation,
    activeTypingUsers,
    cancelUpload,
    changeMemberRole,
    conversations,
    createGroup,
    currentUser,
    deleteActiveMessage,
    dismissError,
    editActiveMessage,
    error,
    hasOlderMessages,
    hasMoreConversations,
    loadOlderMessages,
    loadOlderConversations,
    loadingConversations,
    loadingMessages,
    inviteMembers,
    leaveActiveGroup,
    loadMessageReadReceipts,
    loadingOlder,
    loadingOlderConversations,
    notifyTyping,
    pinActiveMessage,
    realtimeStatus,
    rejectInvitation,
    removeMember,
    saveGroupProfile,
    selectConversation,
    sendDraft,
    sending,
    uploadProgress,
    toggleReaction,
    startDirectConversation,
    unpinActiveMessage,
  } = useChatWorkspace();
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const [aiDraft] = useState(() => location.state?.aiDraft ?? "");

  useEffect(() => {
    if (location.state?.aiDraft) navigate(location.pathname, { replace: true, state: {} });
  }, [location.pathname, location.state?.aiDraft, navigate]);
  const { open: drawerOpen, openDrawer, closeDrawer } = useWorkspaceDrawer();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [groupDialogMode, setGroupDialogMode] = useState(null);
  const [summaryDialog, setSummaryDialog] = useState({ open: false, loading: false, error: "", result: null });

  const members = activeConversation
    ? getNormalizedConversationContacts(activeConversation).filter(Boolean)
    : [];
  const headerTitle = activeConversation?.title ?? t("chat.title");

  async function openConversationSummary() {
    if (!activeConversation?.id) return;

    setSummaryDialog({ open: true, loading: true, error: "", result: null });
    try {
      const result = await summarizeConversation(activeConversation.id, { limit: 20 });
      setSummaryDialog((current) =>
        current.open ? { open: true, loading: false, error: "", result } : current,
      );
    } catch (requestError) {
      setSummaryDialog((current) =>
        current.open
          ? {
              open: true,
              loading: false,
              error: getApiErrorMessage(requestError, t, "errors.ai"),
              result: null,
            }
          : current,
      );
    }
  }

  async function handleSelectUser(user) {
    if (user.directConversationId) {
      selectConversation(user.directConversationId);
      return;
    }

    await startDirectConversation(user.id);
  }

  function handleSelectConversation(conversationId) {
    selectConversation(conversationId);
    closeDrawer();
  }

  return (
    <TraderLayout active="chat" appFrame>
      <WorkspaceShell className="chat-workspace">
        <ConversationSidebar
          activeConversationId={activeConversation?.id ?? null}
          conversations={conversations}
          hasMore={hasMoreConversations}
          drawerOpen={drawerOpen}
          loading={loadingConversations}
          loadingMore={loadingOlderConversations}
          onCloseDrawer={closeDrawer}
          onNewConversation={() => {
            closeDrawer();
            setDialogOpen(true);
          }}
          onLoadMore={loadOlderConversations}
          onSelect={handleSelectConversation}
        />

        {drawerOpen ? (
          <button
            className="workspace-drawer-backdrop"
            type="button"
            aria-label={t("chat.closeConversations")}
            onClick={closeDrawer}
          />
        ) : null}

        <section className="workspace-main">
          <header className="workspace-header">
            <button
              className="icon-button show-sm"
              type="button"
              aria-label={t("chat.openConversations")}
              aria-expanded={drawerOpen}
              onClick={openDrawer}
            >
              <PanelLeft size={19} />
            </button>
            {activeConversation ? (
              <Avatar
                name={headerTitle}
                seed={activeConversation.id}
                src={activeConversation.avatarUrl}
              />
            ) : (
              <span className="workspace-header__placeholder" aria-hidden="true">
                <MessageCircleMore size={20} />
              </span>
            )}
            <div className="workspace-header__identity">
              <h1>{headerTitle}</h1>
              <p>
                {loadingConversations
                  ? t("common.loading")
                  : describeConversation(activeConversation, t)}
              </p>
            </div>
            <div>
              {activeConversation ? (
                <button
                  className="button button--ghost workspace-header__ai"
                  type="button"
                  onClick={openConversationSummary}
                >
                  <Sparkles size={16} />
                  <span>{t("chat.ai.summarize")}</span>
                </button>
              ) : null}
              <span
                className={classNames("realtime-status", `realtime-status--${realtimeStatus}`)}
              >
                <Activity size={15} />
                <span>{t(realtimeStatusKey(realtimeStatus))}</span>
              </span>
            </div>
          </header>

          {error ? (
            <Alert variant="workspace" onDismiss={dismissError}>
              {error}
            </Alert>
          ) : null}

          {activeConversation ? (
            <MessageStream
              key={activeConversation.id}
              conversationId={activeConversation.id}
              currentUserId={currentUser?.id ?? null}
              initialDraft={aiDraft}
              hasOlderMessages={hasOlderMessages}
              loading={loadingMessages}
              loadingOlder={loadingOlder}
              messages={activeConversation.messages ?? []}
              participants={activeConversation.participants ?? (activeConversation.otherParticipant ? [activeConversation.otherParticipant] : [])}
              onLoadOlder={loadOlderMessages}
              onCancelUpload={cancelUpload}
              onSend={sendDraft}
              onTyping={notifyTyping}
              onPin={pinActiveMessage}
              onUnpin={unpinActiveMessage}
              onReact={toggleReaction}
              onEdit={editActiveMessage}
              onDelete={deleteActiveMessage}
              onReadReceipts={loadMessageReadReceipts}
              sending={sending}
              typingUsers={activeTypingUsers}
              uploadProgress={uploadProgress}
            />
          ) : (
            <div className="chat-welcome">
              <div className="chat-welcome__art" aria-hidden="true">
                <span>
                  <MessageCircleMore size={36} strokeWidth={1.7} />
                </span>
                <i />
                <i />
                <i />
              </div>
              <div className="chat-welcome__copy">
                <h2>{t("chat.noneSelected.title")}</h2>
                <p>{t("chat.noneSelected.body")}</p>
              </div>
              <button
                className="button button--primary chat-welcome__action"
                type="button"
                onClick={() => setDialogOpen(true)}
              >
                <Plus size={17} />
                {t("chat.newConversation")}
              </button>
            </div>
          )}
        </section>

        {activeConversation ? (
          <MemberPanel
            excludeId={currentUser?.id ?? null}
            members={members}
            isGroup={activeConversation.type === "GROUP"}
            canManage={activeConversation.currentUserRole === "OWNER"}
            isPendingInvitation={activeConversation.isPendingInvitation}
            onAcceptInvitation={acceptInvitation}
            onRejectInvitation={rejectInvitation}
            onInvite={() => setGroupDialogMode("invite")}
            onEditProfile={() => {
              const name = window.prompt(t("chat.group.name"), activeConversation.title);
              if (name == null) return;
              const avatarUrl = window.prompt(t("chat.group.avatarUrl"), activeConversation.avatarUrl ?? "");
              saveGroupProfile({ name: name.trim(), avatarUrl: avatarUrl?.trim() || null });
            }}
            onLeave={() => {
              if (window.confirm(t("chat.group.confirmLeave"))) leaveActiveGroup();
            }}
            onRemoveMember={(memberId) => {
              if (window.confirm(t("chat.group.confirmRemove"))) removeMember(memberId);
            }}
            onChangeRole={changeMemberRole}
          />
        ) : null}
      </WorkspaceShell>

      {summaryDialog.open ? (
        <Modal
          title={t("chat.ai.summaryTitle", { conversation: headerTitle })}
          description={t("chat.ai.summaryDescription")}
          onClose={() => setSummaryDialog((current) => ({ ...current, open: false }))}
        >
          {summaryDialog.loading ? (
            <div className="ai-dialog-state">
              <Sparkles size={20} /> {t("ai.pending.summary")}
            </div>
          ) : null}
          {summaryDialog.error ? <Alert>{summaryDialog.error}</Alert> : null}
          {summaryDialog.result ? (
            <div className="ai-dialog-result">
              <p>{summaryDialog.result.summary}</p>
              {summaryDialog.result.highlights?.length ? (
                <section>
                  <h3>{t("ai.summary.highlights")}</h3>
                  <ul>
                    {summaryDialog.result.highlights.map((item, index) => (
                      <li key={String(index)}>{item}</li>
                    ))}
                  </ul>
                </section>
              ) : null}
              {summaryDialog.result.actionItems?.length ? (
                <section>
                  <h3>{t("ai.summary.actionItems")}</h3>
                  <ul>
                    {summaryDialog.result.actionItems.map((item, index) => (
                      <li key={String(index)}>{item}</li>
                    ))}
                  </ul>
                </section>
              ) : null}
            </div>
          ) : null}
        </Modal>
      ) : null}

      {dialogOpen ? (
        <NewConversationDialog
          onClose={() => setDialogOpen(false)}
          onCreateGroup={() => {
            setDialogOpen(false);
            setGroupDialogMode("create");
          }}
          onSelectUser={handleSelectUser}
        />
      ) : null}

      {groupDialogMode ? (
        <GroupConversationDialog
          mode={groupDialogMode}
          existingMemberIds={(activeConversation?.participants ?? []).map((member) => member.id)}
          onClose={() => setGroupDialogMode(null)}
          onCreate={(payload) =>
            groupDialogMode === "invite" ? inviteMembers(payload.memberIds) : createGroup(payload)
          }
        />
      ) : null}
    </TraderLayout>
  );
}
export default function ChatPage() {
  const isAuthenticated = useAppSelector(selectIsAuthenticated);

  if (!isAuthenticated) {
    return (
      <TraderLayout active="chat" appFrame>
        <GuestChatPreview />
      </TraderLayout>
    );
  }

  return <AuthenticatedChatPage />;
}