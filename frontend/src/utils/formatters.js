export function formatShortTime(value) {
  if (!value) return "";

  const date = new Date(value);
  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();

  if (isToday) {
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  }

  return date.toLocaleDateString([], { month: "short", day: "numeric" });
}

export function formatLongTime(value) {
  if (!value) return "";

  return new Date(value).toLocaleString([], {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatPresence(presence) {
  if (!presence) return "Offline";
  if (presence.isOnline) return "Online now";

  return `Last active ${formatShortTime(presence.lastActiveAt)}`;
}

export function initials(name = "") {
  return name
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join("");
}

export function clampPreview(text = "", max = 74) {
  if (text.length <= max) return text;

  return `${text.slice(0, max - 1).trim()}...`;
}
