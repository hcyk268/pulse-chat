export const COMMUNITY_VISIBILITIES = Object.freeze(["PUBLIC", "PRIVATE", "UNLISTED"]);
export const COMMUNITY_CHANNEL_TYPES = Object.freeze([
  "TEXT",
  "ANNOUNCEMENT",
  "SIGNALS",
  "EDUCATION",
]);

const COMMUNITY_MANAGER_ROLES = new Set(["OWNER", "ADMIN"]);
const CHANNEL_MANAGER_ROLES = new Set(["OWNER", "ADMIN", "MODERATOR"]);

export function canManageCommunity(role) {
  return COMMUNITY_MANAGER_ROLES.has(role);
}

export function canManageChannels(role) {
  return CHANNEL_MANAGER_ROLES.has(role);
}

export function createCommunityForm(community) {
  return {
    name: community?.name ?? "",
    description: community?.description ?? "",
    categorySlug: community?.category === "all" ? "" : community?.category ?? "",
    visibility: community?.visibility ?? "PUBLIC",
    tagSlugs: [...(community?.tagSlugs ?? [])],
    channelName: "General",
    channelDescription: "",
    channelType: "TEXT",
    channelReadOnly: false,
  };
}

export function createChannelForm(channel) {
  return {
    name: channel?.name ?? channel?.label ?? "",
    description: channel?.description ?? "",
    type: channel?.type ?? "TEXT",
    readOnly: Boolean(channel?.readOnly),
  };
}

function optionalText(value) {
  const normalized = value?.trim();
  return normalized ? normalized : null;
}

export function buildCommunityRequest(form, { creating = false } = {}) {
  const request = {
    name: form.name.trim(),
    description: optionalText(form.description),
    categorySlug: optionalText(form.categorySlug),
    visibility: form.visibility,
    tagSlugs: [...form.tagSlugs],
  };

  if (creating) {
    request.channels = [
      {
        name: form.channelName.trim(),
        description: optionalText(form.channelDescription),
        type: form.channelType,
        readOnly: Boolean(form.channelReadOnly),
      },
    ];
  }

  return request;
}

export function buildChannelRequest(form) {
  return {
    name: form.name.trim(),
    description: optionalText(form.description),
    type: form.type,
    readOnly: Boolean(form.readOnly),
  };
}

export function toggleTag(tagSlugs, slug) {
  return tagSlugs.includes(slug)
    ? tagSlugs.filter((value) => value !== slug)
    : [...tagSlugs, slug];
}

export function containsHtmlAngleBracket(...values) {
  return values.some((value) => /[<>]/.test(value ?? ""));
}
