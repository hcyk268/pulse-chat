import { createSlice, nanoid } from "@reduxjs/toolkit";
import {
  communityCategories,
  communityChannels,
  communityMessagesByChannel,
} from "../../data/traderHubData.js";

const seedUnread = Object.fromEntries(
  communityChannels.map((channel) => [channel.id, channel.unread]),
);

const seedMessages = Object.fromEntries(
  Object.entries(communityMessagesByChannel).map(([channelId, messages]) => [
    channelId,
    [...messages],
  ]),
);

const communitySlice = createSlice({
  name: "community",
  initialState: {
    activeCategory: communityCategories[0],
    query: "",
    joinedIds: [],
    activeChannelId: communityChannels[0]?.id ?? "general",
    unreadByChannel: seedUnread,
    messagesByChannel: seedMessages,
  },
  reducers: {
    setCommunityCategory(state, action) {
      state.activeCategory = action.payload || communityCategories[0];
    },
    setCommunityQuery(state, action) {
      state.query = action.payload;
    },
    toggleJoinCommunity(state, action) {
      const communityId = action.payload;
      if (communityId == null) return;

      state.joinedIds = state.joinedIds.some((id) => String(id) === String(communityId))
        ? state.joinedIds.filter((id) => String(id) !== String(communityId))
        : [...state.joinedIds, communityId];
    },
    setJoinedCommunityIds(state, action) {
      state.joinedIds = action.payload ?? [];
    },
    upsertJoinedCommunity(state, action) {
      const { communityId, joined } = action.payload ?? {};
      if (communityId == null) return;

      const exists = state.joinedIds.some((id) => String(id) === String(communityId));
      if (joined && !exists) state.joinedIds.push(communityId);
      if (!joined && exists) {
        state.joinedIds = state.joinedIds.filter((id) => String(id) !== String(communityId));
      }
    },
    setActiveChannel(state, action) {
      state.activeChannelId = action.payload;
      state.unreadByChannel[action.payload] = 0;
    },
    incrementChannelUnread(state, action) {
      const channelId = action.payload;
      if (channelId == null || String(channelId) === String(state.activeChannelId)) return;
      state.unreadByChannel[channelId] = (state.unreadByChannel[channelId] ?? 0) + 1;
    },
    postChannelMessage: {
      prepare({ channelId, author, content }) {
        return {
          payload: {
            channelId,
            message: {
              id: `c-${nanoid(8)}`,
              senderId: author,
              sender: { displayName: author },
              content,
              createdAt: new Date().toISOString(),
              pending: false,
            },
          },
        };
      },
      reducer(state, action) {
        const { channelId, message } = action.payload;
        state.messagesByChannel[channelId] = [
          ...(state.messagesByChannel[channelId] ?? []),
          message,
        ];
      },
    },
  },
});

export const {
  setCommunityCategory,
  setCommunityQuery,
  toggleJoinCommunity,
  setJoinedCommunityIds,
  upsertJoinedCommunity,
  setActiveChannel,
  incrementChannelUnread,
  postChannelMessage,
} = communitySlice.actions;

export const selectCommunityCategory = (state) => state.community.activeCategory;
export const selectCommunityQuery = (state) => state.community.query;
export const selectJoinedCommunityIds = (state) => state.community.joinedIds;
export const selectActiveChannelId = (state) => state.community.activeChannelId;
export const selectChannelUnread = (state) => state.community.unreadByChannel;
export const selectChannelMessages = (channelId) => (state) =>
  state.community.messagesByChannel[channelId] ?? [];

/** Pure filter shared by the discovery page and its tests. */
export function filterCommunities(items, { category, query }) {
  const normalizedQuery = (query ?? "").trim().toLowerCase();

  return items.filter((community) => {
    const communityCategory = community.category?.slug ?? community.category;
    const matchesCategory = !category || category === "all" || communityCategory === category;
    if (!matchesCategory) return false;
    if (!normalizedQuery) return true;

    return [community.name, community.description, ...(community.tags ?? [])]
      .join(" ")
      .toLowerCase()
      .includes(normalizedQuery);
  });
}

export default communitySlice.reducer;
