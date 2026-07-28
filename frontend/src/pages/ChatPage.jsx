import { Activity, PanelLeft } from "lucide-react";
import { useState } from "react";
import TraderLayout from "../components/layout/TraderLayout";
import Alert from "../components/shared/Alert";
import Avatar from "../components/shared/Avatar";
import { classNames } from "../components/shared/utils";
import ConversationSidebar from "../features/workspace/ConversationSidebar";
import MemberPanel from "../features/workspace/MemberPanel";
import MessageStream from "../features/workspace/MessageStream";
import NewConversationDialog from "../features/workspace/NewConversationDialog";
import WorkspaceShell from "../features/workspace/WorkspaceShell";
import { useChatWorkspace } from "../features/workspace/useChatWorkspace";
import { useWorkspaceDrawer } from "../features/workspace/useWorkspaceDrawer";
import { getNormalizedConversationContacts } from "../domain/chat/normalizers.js";
import { realtimeStatusKey } from "../features/market/realtimeStatus.js";
import { useTranslation } from "../i18n/useTranslation.js";
import { formatPresence } from "../utils/formatters";

function describeConversation(conversation, t) {
  if (!conversation) return t("chat.noneSelected.body");
  if (conversation.type === "GROUP") {
    return t("chat.groupMembers", { count: conversation.participantCount });
  }

  return formatPresence(conversation.otherParticipant?.presence, t);
}

export default function ChatPage() {
  const {
    activeConversation,
    activeTypingUsers,
    conversations,
    currentUser,
    dismissError,
    error,
    hasOlderMessages,
    loadOlderMessages,
    loadingConversations,
    loadingMessages,
    loadingOlder,
    notifyTyping,
    realtimeStatus,
    selectConversation,
    sendDraft,
    sending,
    startDirectConversation,
  } = useChatWorkspace();
  const { t } = useTranslation();
  const { open: drawerOpen, openDrawer, closeDrawer } = useWorkspaceDrawer();
  const [dialogOpen, setDialogOpen] = useState(false);

  const members = activeConversation
    ? getNormalizedConversationContacts(activeConversation).filter(Boolean)
    : [];
  const headerTitle = activeConversation?.title ?? t("chat.title");

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
          drawerOpen={drawerOpen}
          loading={loadingConversations}
          onCloseDrawer={closeDrawer}
          onNewConversation={() => {
            closeDrawer();
            setDialogOpen(true);
          }}
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
            <Avatar
              name={headerTitle}
              seed={activeConversation?.id ?? headerTitle}
              src={activeConversation?.avatarUrl}
            />
            <div>
              <h1>{headerTitle}</h1>
              <p>
                {loadingConversations
                  ? t("common.loading")
                  : describeConversation(activeConversation, t)}
              </p>
            </div>
            <div>
              <span
                className={classNames("realtime-status", `realtime-status--${realtimeStatus}`)}
              >
                <Activity size={15} />
                {t(realtimeStatusKey(realtimeStatus))}
              </span>
            </div>
          </header>

          {error ? (
            <Alert variant="workspace" onDismiss={dismissError}>
              {error}
            </Alert>
          ) : null}

          <MessageStream
            key={activeConversation?.id ?? "empty"}
            currentUserId={currentUser?.id ?? null}
            emptyLabel={activeConversation ? undefined : t("chat.noneSelected.title")}
            hasOlderMessages={hasOlderMessages}
            loading={loadingConversations || loadingMessages}
            loadingOlder={loadingOlder}
            messages={activeConversation?.messages ?? []}
            onLoadOlder={loadOlderMessages}
            onSend={activeConversation ? sendDraft : null}
            onTyping={activeConversation ? notifyTyping : null}
            sending={sending}
            typingUsers={activeTypingUsers}
          />
        </section>

        <MemberPanel excludeId={currentUser?.id ?? null} members={members} />
      </WorkspaceShell>

      {dialogOpen ? (
        <NewConversationDialog
          onClose={() => setDialogOpen(false)}
          onSelectUser={handleSelectUser}
        />
      ) : null}
    </TraderLayout>
  );
}
