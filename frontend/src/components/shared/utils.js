export function classNames(...values) {
  return values.filter(Boolean).join(" ");
}

export function initials(name, fallback = "?") {
  const parts = String(name ?? "")
    .split(/\s+/)
    .filter(Boolean);

  if (parts.length === 0) return fallback;

  return parts
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
}

/**
 * A curated hue ring, so identity colours stay harmonious instead of landing on
 * muddy yellow-greens the way `hash % 360` would.
 */
const IDENTITY_HUES = [214, 196, 170, 142, 96, 45, 25, 8, 340, 316, 286, 258];

export function identityHue(seed) {
  const value = String(seed ?? "");
  let hash = 0;

  for (let index = 0; index < value.length; index += 1) {
    hash = (hash * 31 + value.charCodeAt(index)) % 1000003;
  }

  return IDENTITY_HUES[hash % IDENTITY_HUES.length];
}

/** Extra `active` keys that should light up a primary nav entry. */
export const pageGroups = {
  market: ["market", "watchlist", "coin"],
  community: ["community", "community-detail"],
  chat: ["chat"],
  ai: ["ai"],
  profile: ["profile", "notifications"],
};
