import { isSameId } from "../../utils/chat.js";

export const DEFAULT_USER_ACCENT = "from-cyan-300 to-emerald-400";

export const EMPTY_CURRENT_USER = {
  id: null,
  backendId: null,
  username: "",
  email: "",
  displayName: "You",
  avatarUrl: null,
  bio: "",
  accountStatus: null,
  accent: DEFAULT_USER_ACCENT,
  createdAt: null,
  updatedAt: null,
};

export function createClientId() {
  if (typeof crypto !== "undefined" && crypto.randomUUID) {
    return crypto.randomUUID();
  }

  return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, (char) =>
    (Number(char) ^ ((Math.random() * 16) >> (Number(char) / 4))).toString(16),
  );
}

export function toCurrentUser(user) {
  const userId = user?.id ?? null;

  return {
    ...EMPTY_CURRENT_USER,
    ...user,
    id: userId,
    backendId: userId,
    displayName: user?.displayName || user?.username || EMPTY_CURRENT_USER.displayName,
    bio: user?.bio ?? "",
    accent: user?.accent ?? DEFAULT_USER_ACCENT,
  };
}

export function toContact(user) {
  if (!user) return null;

  const userId = user.id ?? user.userId ?? null;

  return {
    id: userId,
    backendId: userId,
    username: user.username ?? "",
    email: user.email ?? "",
    displayName: user.displayName || user.username || "Unknown user",
    avatarUrl: user.avatarUrl ?? null,
    role: user.role ?? (user.directConversationId ? "Existing direct chat" : "Active user"),
    bio: user.bio ?? "",
    accent: user.accent ?? "from-sky-300 to-blue-500",
    presence: user.presence ?? { isOnline: false, lastActiveAt: null },
    directConversationId: user.directConversationId ?? null,
  };
}

export function toMemberContact(member) {
  const contact = toContact(member);
  if (!contact) return null;

  return {
    ...contact,
    role: member.role ?? contact.role,
    joinedAt: member.joinedAt ?? null,
    leftAt: member.leftAt ?? null,
    status: member.status ?? null,
  };
}

export function toGroupDisplayContact(conversation) {
  return {
    id: `group-${conversation.id}`,
    backendId: null,
    username: "group",
    email: "",
    displayName: conversation.title || conversation.name || "Group chat",
    avatarUrl: conversation.avatarUrl ?? null,
    role: `${conversation.participantCount ?? conversation.participants?.length ?? 0} members`,
    bio: "",
    accent: "from-amber-300 to-rose-500",
    presence: { isOnline: false, lastActiveAt: null },
    directConversationId: null,
  };
}

export function getConversationDisplayContact(conversation) {
  if (conversation.type === "GROUP" || conversation.peer === null) {
    return toGroupDisplayContact(conversation);
  }

  return toContact(conversation.otherParticipant ?? conversation.peer) ?? toGroupDisplayContact(conversation);
}

export function getConversationContacts(conversation) {
  const participants = (conversation.participants ?? [])
    .map(toMemberContact)
    .filter(Boolean);
  const peer = toContact(conversation.otherParticipant ?? conversation.peer);

  return peer ? [peer, ...participants] : participants;
}

export function mergeContacts(previousContacts, nextContacts) {
  const byId = new Map(previousContacts.map((contact) => [String(contact.id), contact]));

  nextContacts.filter(Boolean).forEach((contact) => {
    byId.set(String(contact.id), {
      ...byId.get(String(contact.id)),
      ...contact,
    });
  });

  return Array.from(byId.values());
}

export function getMessageSenderId(message) {
  return message.sender?.id ?? message.senderId ?? null;
}

export function normalizeMessage(message) {
  return {
    id: message.id,
    clientMessageId: message.clientMessageId,
    conversationId: message.conversationId,
    senderId: getMessageSenderId(message),
    sender: message.sender,
    content: message.content,
    replyTo: message.replyTo ?? null,
    attachments: message.attachments ?? [],
    messageType: message.messageType,
    status: message.status,
    createdAt: message.createdAt,
    editedAt: message.editedAt,
    deletedBy: message.deletedBy,
    deletedAt: message.deletedAt,
    deliveredAt: message.deliveredAt,
    readAt: message.readAt,
  };
}

export function normalizeLastMessage(lastMessage) {
  if (!lastMessage) return null;

  return {
    ...lastMessage,
    senderId: getMessageSenderId(lastMessage),
    contentPreview: lastMessage.deletedAt ? "Message deleted" : lastMessage.contentPreview,
  };
}

export function dedupeById(items) {
  return Array.from(new Map(items.map((item) => [String(item.id), item])).values());
}

export function getMessageIndex(messages, messageId) {
  return messages.findIndex((message) => isSameId(message.id, messageId));
}

export function getMessagePreview(message) {
  if (!message) return "";
  if (message.deletedAt) return "Message deleted";
  if (message.content ?? message.contentPreview) return message.content ?? message.contentPreview;

  const attachmentCount = message.attachments?.length ?? 0;
  if (attachmentCount === 1) return message.attachments[0]?.fileName || "Attachment";
  if (attachmentCount > 1) return `${attachmentCount} attachments`;

  return "";
}

export function getPinnedMessageIds(pinResponse) {
  return (pinResponse?.items ?? [])
    .map((pin) => pin?.message?.id)
    .filter((messageId) => messageId != null)
    .map(String);
}

export function normalizePin(pin) {
  return {
    pinId: pin.pinId,
    message: normalizeMessage(pin.message),
    pinnedBy: pin.pinnedBy,
    pinnedAt: pin.pinnedAt,
  };
}

export function normalizeReactionGroups(response) {
  return (response?.items ?? []).map((group) => ({
    emoji: group.emoji,
    count: group.count ?? 0,
    reactedByMe: Boolean(group.reactedByMe),
    users: group.users ?? [],
  }));
}

export function normalizeConversation(conversation, existingMessages = []) {
  const displayContact = getConversationDisplayContact(conversation);
  const participantContacts = getConversationContacts(conversation);
  const lastMessage = normalizeLastMessage(conversation.lastMessage);
  const isGroup = conversation.type === "GROUP";
  const currentUserStatus = conversation.currentUserStatus ?? conversation.status ?? null;

  return {
    id: conversation.id,
    type: conversation.type,
    title: displayContact.displayName,
    avatarUrl: isGroup ? conversation.avatarUrl ?? null : displayContact.avatarUrl,
    otherParticipantId: isGroup ? null : displayContact.id,
    otherParticipant: displayContact,
    participants: participantContacts,
    participantCount: conversation.participantCount ?? participantContacts.length,
    currentUserRole: conversation.currentUserRole ?? null,
    currentUserStatus,
    isPendingInvitation:
      currentUserStatus === "PENDING" || Boolean(conversation.isPendingInvitation),
    createdBy: conversation.createdBy ?? null,
    unreadCount: conversation.unreadCount ?? 0,
    pinned: false,
    muted: false,
    lastMessage,
    lastMessageAt:
      conversation.lastMessageAt ??
      lastMessage?.createdAt ??
      conversation.updatedAt ??
      conversation.createdAt,
    createdAt: conversation.createdAt,
    updatedAt: conversation.updatedAt,
    messages: existingMessages,
  };
}

export function getNormalizedConversationContacts(conversation) {
  return conversation.participants.length
    ? conversation.participants
    : [conversation.otherParticipant];
}
