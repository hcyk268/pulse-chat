import httpClient, { getApiErrorMessage, unwrap } from "./httpClient.js";

const COMMUNITY_BASE = "/api/v1/community";

export { getApiErrorMessage };

function cleanParams(params) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value != null && value !== "" && value !== "all"),
  );
}

function assetUrl(asset) {
  return asset?.thumbnailUrl ?? asset?.publicUrl ?? null;
}

export function normalizeCommunitySummary(community) {
  if (!community) return null;

  const categorySlug = community.category?.slug ?? community.category ?? "all";

  return {
    ...community,
    id: community.id,
    slug: community.slug,
    name: community.name ?? "",
    description: community.description ?? "",
    category: categorySlug,
    categoryName: community.category?.name ?? categorySlug,
    tags: (community.tags ?? []).map((tag) => tag?.name ?? tag?.slug ?? tag).filter(Boolean),
    tagSlugs: (community.tags ?? []).map((tag) => tag?.slug ?? tag).filter(Boolean),
    avatarUrl: assetUrl(community.avatar),
    coverUrl: assetUrl(community.cover),
    memberCount: community.memberCount ?? 0,
    onlineCount: community.onlineCount ?? 0,
    isMember: Boolean(community.membership?.member),
    membershipRole: community.membership?.role ?? null,
    defaultChannelId: community.defaultChannelId ?? null,
  };
}

export function normalizeCommunityChannel(channel) {
  if (!channel) return null;

  return {
    ...channel,
    id: channel.id,
    slug: channel.slug,
    label: channel.name ?? channel.slug ?? "general",
    unread: channel.unreadCount ?? 0,
  };
}

export function normalizeCommunityMember(member) {
  const user = member?.user;
  if (!user) return null;

  return {
    id: user.id,
    username: user.username ?? "",
    displayName: user.displayName || user.username || "Unknown user",
    avatarUrl: user.avatarUrl ?? null,
    role: member.role ?? null,
    status: member.status ?? null,
    joinedAt: member.joinedAt ?? null,
    presence: member.presence ?? { isOnline: false, lastActiveAt: member.lastSeenAt ?? null },
  };
}

export function normalizeCommunityDetail(detail) {
  if (!detail) return null;

  return {
    community: normalizeCommunitySummary(detail.community),
    channels: (detail.channels ?? []).map(normalizeCommunityChannel).filter(Boolean),
    members: (detail.members ?? []).map(normalizeCommunityMember).filter(Boolean),
  };
}

export async function getCommunityCategories() {
  return unwrap(await httpClient.get(`${COMMUNITY_BASE}/categories`));
}

export async function getCommunityTags() {
  return unwrap(await httpClient.get(`${COMMUNITY_BASE}/tags`));
}

export async function getCommunities({ limit = 20, category, tag, query } = {}) {
  return unwrap(
    await httpClient.get(`${COMMUNITY_BASE}/communities`, {
      params: cleanParams({ limit, category, tag, q: query }),
    }),
  );
}

export async function createCommunity(request) {
  return unwrap(await httpClient.post(`${COMMUNITY_BASE}/communities`, request));
}

export async function getCommunity(slug) {
  return unwrap(
    await httpClient.get(`${COMMUNITY_BASE}/communities/${encodeURIComponent(slug)}`),
  );
}

export async function updateCommunity(communityId, request) {
  return unwrap(
    await httpClient.patch(
      `${COMMUNITY_BASE}/communities/${encodeURIComponent(communityId)}`,
      request,
    ),
  );
}

export async function joinCommunity(communityId) {
  return unwrap(
    await httpClient.post(`${COMMUNITY_BASE}/communities/${encodeURIComponent(communityId)}/join`),
  );
}

export async function leaveCommunity(communityId) {
  return unwrap(
    await httpClient.post(`${COMMUNITY_BASE}/communities/${encodeURIComponent(communityId)}/leave`),
  );
}

export async function createCommunityChannel(communityId, request) {
  return unwrap(
    await httpClient.post(
      `${COMMUNITY_BASE}/communities/${encodeURIComponent(communityId)}/channels`,
      request,
    ),
  );
}

export async function updateCommunityChannel(channelId, request) {
  return unwrap(
    await httpClient.patch(`${COMMUNITY_BASE}/channels/${encodeURIComponent(channelId)}`, request),
  );
}
