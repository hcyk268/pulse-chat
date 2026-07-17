import { isSameId } from "../../utils/chat";
import {
  acceptGroupInvitation as acceptGroupInvitationApi,
  addGroupMembers as addGroupMembersApi,
  createDirectConversation,
  createGroupConversation as createGroupConversationApi,
  leaveGroup as leaveGroupApi,
  rejectGroupInvitation as rejectGroupInvitationApi,
  removeGroupMember as removeGroupMemberApi,
  updateGroupMemberRole as updateGroupMemberRoleApi,
  updateGroupProfile as updateGroupProfileApi,
} from "../../services/conversationApi";
import { searchUsers as searchUsersApi, updateMe } from "../../services/userApi";
import { isPersistentSession, saveAuthSession } from "../../utils/authStorage";
import { mergeConversationList } from "./conversationState";
import { mergeContacts, normalizeConversation, toContact, toCurrentUser } from "./normalizers";

export function createConversationActionsDomain({
  conversations,
  setConversations,
  setContacts,
  setMessagePagingByConversation,
  setIsStartingConversation,
  setStartConversationError,
  setChatActionError,
  setCurrentUser,
  setAuthSession,
  setUserSearchResults,
  setIsSearchingUsers,
  setUserSearchError,
  mergeNormalizedConversationContacts,
  loadConversation,
}) {
  async function runChatAction(fallbackMessage, action, fallbackValue = null) {
    setChatActionError("");

    try {
      return await action();
    } catch (error) {
      setChatActionError(error.message || fallbackMessage);
      return fallbackValue;
    }
  }

  function applyConversationResponse(conversationResponse, existingMessages = null) {
    const existing = conversations.find(
      (item) => String(item.id) === String(conversationResponse.id),
    );
    const normalizedConversation = normalizeConversation(
      conversationResponse,
      existingMessages ?? existing?.messages ?? [],
    );

    mergeNormalizedConversationContacts(normalizedConversation);
    setConversations((previous) => mergeConversationList(previous, [normalizedConversation]));
    return normalizedConversation;
  }

  async function startConversation(targetUserId) {
    const existing = conversations.find(
      (conversation) => String(conversation.otherParticipantId) === String(targetUserId),
    );

    if (existing) return existing.id;

    setIsStartingConversation(true);
    setStartConversationError("");

    try {
      const response = await createDirectConversation(Number(targetUserId));
      const normalizedConversation = applyConversationResponse(response, []);
      setMessagePagingByConversation((previous) => ({
        ...previous,
        [normalizedConversation.id]: null,
      }));
      return normalizedConversation.id;
    } catch (error) {
      setStartConversationError(error.message || "Could not start conversation.");
      return null;
    } finally {
      setIsStartingConversation(false);
    }
  }

  async function startGroupConversation({ name, avatarUrl = null, memberIds }) {
    setIsStartingConversation(true);
    setStartConversationError("");

    try {
      const response = await createGroupConversationApi({
        name: name.trim(),
        avatarUrl: avatarUrl?.trim() || null,
        memberIds: memberIds.map(Number),
      });
      const normalizedConversation = applyConversationResponse(response, []);
      setMessagePagingByConversation((previous) => ({
        ...previous,
        [normalizedConversation.id]: null,
      }));
      return normalizedConversation.id;
    } catch (error) {
      setStartConversationError(error.message || "Could not create group.");
      return null;
    } finally {
      setIsStartingConversation(false);
    }
  }

  async function addMembersToGroup(conversationId, memberIds) {
    if (!conversationId || memberIds.length === 0) return null;

    return runChatAction("Could not add group members.", async () => {
      const response = await addGroupMembersApi(conversationId, memberIds.map(Number));
      return applyConversationResponse(response);
    });
  }

  async function acceptGroupInvitation(conversationId) {
    if (!conversationId) return null;

    return runChatAction("Could not accept invitation.", async () => {
      const response = await acceptGroupInvitationApi(conversationId);
      const acceptedConversation = applyConversationResponse(response);
      await loadConversation(conversationId, { force: true });
      return acceptedConversation;
    });
  }

  async function rejectGroupInvitation(conversationId) {
    if (!conversationId) return false;

    return runChatAction("Could not reject invitation.", async () => {
      await rejectGroupInvitationApi(conversationId);
      setConversations((previous) =>
        previous.filter((conversation) => !isSameId(conversation.id, conversationId)),
      );
      return true;
    }, false);
  }

  async function updateGroup(conversationId, profile) {
    if (!conversationId) return null;

    return runChatAction("Could not update group.", async () => {
      const response = await updateGroupProfileApi(conversationId, {
        name: profile.name?.trim() || null,
        avatarUrl: profile.avatarUrl?.trim() || null,
      });
      return applyConversationResponse(response);
    });
  }

  async function updateGroupMemberRole(conversationId, memberId, role) {
    if (!conversationId || !memberId || !role) return null;

    return runChatAction("Could not update member role.", async () => {
      const response = await updateGroupMemberRoleApi(conversationId, memberId, role);
      return applyConversationResponse(response);
    });
  }

  async function removeMemberFromGroup(conversationId, memberId) {
    if (!conversationId || !memberId) return null;

    return runChatAction("Could not remove member.", async () => {
      const response = await removeGroupMemberApi(conversationId, memberId);
      return applyConversationResponse(response);
    });
  }

  async function leaveCurrentGroup(conversationId) {
    if (!conversationId) return false;

    return runChatAction("Could not leave group.", async () => {
      await leaveGroupApi(conversationId);
      setConversations((previous) =>
        previous.filter((conversation) => !isSameId(conversation.id, conversationId)),
      );
      return true;
    }, false);
  }

  async function updateProfile(nextProfile) {
    const displayName = nextProfile.displayName?.trim();
    const avatarUrl = nextProfile.avatarUrl?.trim();
    const bio = nextProfile.bio?.trim();
    const user = await updateMe({
      displayName: displayName || null,
      avatarUrl: avatarUrl ?? null,
      bio: bio ?? null,
    });

    setCurrentUser(toCurrentUser(user));
    setAuthSession((previous) => {
      if (!previous) return previous;

      const nextAuthSession = { ...previous, user };
      saveAuthSession(nextAuthSession, isPersistentSession());
      return nextAuthSession;
    });

    return user;
  }

  async function searchUsers(query) {
    const normalized = query.trim();

    if (!normalized) {
      setUserSearchResults([]);
      return [];
    }

    setIsSearchingUsers(true);
    setUserSearchError("");

    try {
      const response = await searchUsersApi(normalized, { limit: 20 });
      const results = (response.items ?? []).map(toContact);
      setUserSearchResults(results);
      setContacts((previous) => mergeContacts(previous, results));
      return results;
    } catch (error) {
      setUserSearchError(error.message || "Could not search users.");
      setUserSearchResults([]);
      return [];
    } finally {
      setIsSearchingUsers(false);
    }
  }

  function clearUserSearch() {
    setUserSearchResults([]);
    setUserSearchError("");
    setIsSearchingUsers(false);
  }

  return {
    startConversation,
    startGroupConversation,
    addMembersToGroup,
    acceptGroupInvitation,
    rejectGroupInvitation,
    updateGroup,
    updateGroupMemberRole,
    removeMemberFromGroup,
    leaveCurrentGroup,
    updateProfile,
    searchUsers,
    clearUserSearch,
  };
}
