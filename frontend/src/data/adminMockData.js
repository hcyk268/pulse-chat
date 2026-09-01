/**
 * Admin mock data. The backend exposes no admin endpoints, so every number and
 * row here is sample data — the admin shell says so on screen.
 *
 * Shapes deliberately mirror what a real endpoint would return, so swapping in
 * an API means changing the thunk, not the components.
 */

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;
const NOW = Date.now();

function timeAgo(offsetMs) {
  return new Date(NOW - offsetMs).toISOString();
}

export const ADMIN_USER_STATUSES = ["active", "pending", "suspended"];
export const ADMIN_REPORT_STATUSES = ["open", "resolved", "dismissed"];
export const ADMIN_REPORT_REASONS = ["spam", "abuse", "scam", "offTopic"];

export const adminKpis = [
  { key: "users", value: 4820, deltaPercent: 3.4 },
  { key: "activeToday", value: 1146, deltaPercent: 1.1 },
  { key: "messages24h", value: 18432, deltaPercent: -2.6 },
  { key: "openReports", value: 7, deltaPercent: 16.7 },
];

/** Seven days of signups, oldest first. Rendered as a bar row. */
export const adminSignupSeries = [
  { label: "Mon", value: 42 },
  { label: "Tue", value: 58 },
  { label: "Wed", value: 51 },
  { label: "Thu", value: 74 },
  { label: "Fri", value: 96 },
  { label: "Sat", value: 63 },
  { label: "Sun", value: 39 },
];

export const adminUsers = [
  { id: 1, username: "alextrades", displayName: "Alex Crypto", email: "alex@example.com", status: "active", role: "member", createdAt: timeAgo(320 * DAY), lastActiveAt: timeAgo(12 * MINUTE), messageCount: 1842, reportCount: 0 },
  { id: 2, username: "mayaquant", displayName: "Maya Quant", email: "maya@example.com", status: "active", role: "moderator", createdAt: timeAgo(280 * DAY), lastActiveAt: timeAgo(40 * MINUTE), messageCount: 2610, reportCount: 0 },
  { id: 3, username: "ninalevels", displayName: "Nina Levels", email: "nina@example.com", status: "active", role: "member", createdAt: timeAgo(210 * DAY), lastActiveAt: timeAgo(3 * HOUR), messageCount: 934, reportCount: 1 },
  { id: 4, username: "tomdesk", displayName: "Tom Osaka", email: "tom@example.com", status: "suspended", role: "member", createdAt: timeAgo(190 * DAY), lastActiveAt: timeAgo(9 * DAY), messageCount: 402, reportCount: 4 },
  { id: 5, username: "sarahlevels", displayName: "Sarah Connor", email: "sarah@example.com", status: "active", role: "admin", createdAt: timeAgo(365 * DAY), lastActiveAt: timeAgo(6 * MINUTE), messageCount: 3120, reportCount: 0 },
  { id: 6, username: "deskresearch", displayName: "Desk Research", email: "research@example.com", status: "active", role: "member", createdAt: timeAgo(150 * DAY), lastActiveAt: timeAgo(2 * HOUR), messageCount: 588, reportCount: 0 },
  { id: 7, username: "pumpwatcher", displayName: "Pump Watcher", email: "pump@example.com", status: "pending", role: "member", createdAt: timeAgo(2 * HOUR), lastActiveAt: null, messageCount: 0, reportCount: 0 },
  { id: 8, username: "gridbot", displayName: "Grid Bot", email: "grid@example.com", status: "suspended", role: "member", createdAt: timeAgo(64 * DAY), lastActiveAt: timeAgo(21 * DAY), messageCount: 5121, reportCount: 12 },
  { id: 9, username: "lanhtrader", displayName: "Lan Nguyen", email: "lan@example.com", status: "active", role: "member", createdAt: timeAgo(96 * DAY), lastActiveAt: timeAgo(55 * MINUTE), messageCount: 771, reportCount: 0 },
  { id: 10, username: "khoadesk", displayName: "Khoa Pham", email: "khoa@example.com", status: "active", role: "member", createdAt: timeAgo(48 * DAY), lastActiveAt: timeAgo(4 * HOUR), messageCount: 316, reportCount: 1 },
  { id: 11, username: "shortsqueeze", displayName: "Short Squeeze", email: "squeeze@example.com", status: "pending", role: "member", createdAt: timeAgo(20 * HOUR), lastActiveAt: null, messageCount: 0, reportCount: 0 },
  { id: 12, username: "macrodesk", displayName: "Macro Desk", email: "macro@example.com", status: "active", role: "moderator", createdAt: timeAgo(240 * DAY), lastActiveAt: timeAgo(28 * MINUTE), messageCount: 1980, reportCount: 0 },
];

export const adminReports = [
  { id: "rep-1", status: "open", reason: "scam", target: "message", excerpt: "Guaranteed 40% weekly returns, DM me for the private group link.", reportedUser: "gridbot", reportedBy: "mayaquant", context: "Daily Strategy · #signals", createdAt: timeAgo(35 * MINUTE) },
  { id: "rep-2", status: "open", reason: "spam", target: "post", excerpt: "JOIN NOW JOIN NOW JOIN NOW t.me/xxxx", reportedUser: "pumpwatcher", reportedBy: "ninalevels", context: "Home feed", createdAt: timeAgo(2 * HOUR) },
  { id: "rep-3", status: "open", reason: "abuse", target: "message", excerpt: "Removed by reporter request.", reportedUser: "tomdesk", reportedBy: "khoadesk", context: "Direct message", createdAt: timeAgo(5 * HOUR) },
  { id: "rep-4", status: "resolved", reason: "offTopic", target: "post", excerpt: "Selling sneakers, size 42, cheap.", reportedUser: "shortsqueeze", reportedBy: "macrodesk", context: "Macro Crypto Desk · #news", createdAt: timeAgo(2 * DAY), resolvedAt: timeAgo(DAY), resolvedBy: "sarahlevels" },
  { id: "rep-5", status: "dismissed", reason: "spam", target: "message", excerpt: "Chart link, looked automated but is a regular member.", reportedUser: "lanhtrader", reportedBy: "pumpwatcher", context: "Pro Technical Analysts · #general", createdAt: timeAgo(3 * DAY), resolvedAt: timeAgo(2 * DAY), resolvedBy: "mayaquant" },
];

export const adminCommunities = [
  { id: "daily-strategy", name: "Daily Strategy", owner: "sarahlevels", memberCount: 48200, messages7d: 9120, status: "active", featured: true, createdAt: timeAgo(400 * DAY) },
  { id: "pro-technical-analysts", name: "Pro Technical Analysts", owner: "mayaquant", memberCount: 41800, messages7d: 6410, status: "active", featured: true, createdAt: timeAgo(360 * DAY) },
  { id: "macro-crypto-desk", name: "Macro Crypto Desk", owner: "macrodesk", memberCount: 31900, messages7d: 3180, status: "active", featured: false, createdAt: timeAgo(300 * DAY) },
  { id: "onchain-lab", name: "On-chain Lab", owner: "deskresearch", memberCount: 18600, messages7d: 1240, status: "active", featured: false, createdAt: timeAgo(180 * DAY) },
  { id: "meme-hunters-dao", name: "Meme Hunters DAO", owner: "pumpwatcher", memberCount: 23700, messages7d: 15980, status: "review", featured: false, createdAt: timeAgo(90 * DAY) },
  { id: "trading-school", name: "Trading School", owner: "ninalevels", memberCount: 52400, messages7d: 4460, status: "active", featured: true, createdAt: timeAgo(420 * DAY) },
  { id: "signal-vault", name: "Signal Vault", owner: "gridbot", memberCount: 1120, messages7d: 12, status: "archived", featured: false, createdAt: timeAgo(140 * DAY) },
];

export const adminAuditLog = [
  { id: "aud-1", actor: "sarahlevels", action: "user.suspended", target: "gridbot", note: "Repeated scam links", createdAt: timeAgo(50 * MINUTE) },
  { id: "aud-2", actor: "mayaquant", action: "report.dismissed", target: "rep-5", note: "False positive", createdAt: timeAgo(2 * DAY) },
  { id: "aud-3", actor: "sarahlevels", action: "community.featured", target: "trading-school", note: "", createdAt: timeAgo(3 * DAY) },
  { id: "aud-4", actor: "sarahlevels", action: "report.resolved", target: "rep-4", note: "Post removed", createdAt: timeAgo(DAY) },
  { id: "aud-5", actor: "macrodesk", action: "community.archived", target: "signal-vault", note: "Inactive for 60 days", createdAt: timeAgo(6 * DAY) },
  { id: "aud-6", actor: "sarahlevels", action: "user.reactivated", target: "khoadesk", note: "Appeal accepted", createdAt: timeAgo(8 * DAY) },
];
