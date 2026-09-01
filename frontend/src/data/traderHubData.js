/**
 * Mock content for the product surfaces the backend does not expose yet
 * (social feed, news, communities, notifications, profile stats).
 *
 * Market and chat screens use the real API and only fall back to the mock
 * assets below when the backend is unreachable.
 */

const MINUTE = 60 * 1000;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;
const NOW = Date.now();

function timeAgo(offsetMs) {
  return new Date(NOW - offsetMs).toISOString();
}

export const mockAssets = [
  {
    rank: 1,
    name: "Bitcoin",
    symbol: "BTC",
    price: 64281.44,
    change24h: 2.45,
    marketCap: 1_260_000_000_000,
    volume: 32_180_000_000,
    high24h: 64980.12,
    low24h: 62410.5,
    color: "#f59e0b",
    chart: [40, 58, 54, 80, 76, 91],
  },
  {
    rank: 2,
    name: "Ethereum",
    symbol: "ETH",
    price: 3491.12,
    change24h: -1.12,
    marketCap: 421_980_000_000,
    volume: 18_440_000_000,
    high24h: 3562.4,
    low24h: 3448.9,
    color: "#6366f1",
    chart: [80, 70, 75, 50, 45, 30],
  },
  {
    rank: 3,
    name: "Solana",
    symbol: "SOL",
    price: 145.88,
    change24h: 5.67,
    marketCap: 65_110_000_000,
    volume: 4_550_000_000,
    high24h: 148.2,
    low24h: 136.05,
    color: "#14b8a6",
    chart: [20, 35, 60, 55, 85, 95],
  },
  {
    rank: 4,
    name: "Tether",
    symbol: "USDT",
    price: 1,
    change24h: 0,
    marketCap: 110_440_000_000,
    volume: 45_180_000_000,
    high24h: 1.001,
    low24h: 0.999,
    color: "#10b981",
    chart: [50, 50, 50, 50, 50, 50],
  },
  {
    rank: 5,
    name: "Chainlink",
    symbol: "LINK",
    price: 18.52,
    change24h: 8.1,
    marketCap: 11_360_000_000,
    volume: 892_400_000,
    high24h: 18.94,
    low24h: 16.98,
    color: "#2563eb",
    chart: [35, 42, 48, 62, 80, 88],
  },
];

export const feedPosts = [
  {
    id: "post-1",
    visual: true,
    author: "Alex Crypto",
    handle: "@alextrades",
    createdAt: timeAgo(2 * HOUR),
    body: "Just analyzed the 4H chart for BTC. Support is holding around $64k and the next resistance band looks thin. A break above it could invite momentum traders back into the move.",
    likeCount: 1240,
    replyCount: 245,
    shareCount: 89,
    tone: "chart",
    topics: ["BTC", "Technical"],
  },
  {
    id: "post-2",
    visual: false,
    author: "Maya Quant",
    handle: "@mayaquant",
    createdAt: timeAgo(4 * HOUR),
    body: "Funding rates cooled while spot bids stayed firm. That mix usually gives cleaner entries than late leverage spikes. Watching ETH and SOL into the US session.",
    likeCount: 842,
    replyCount: 96,
    shareCount: 41,
    tone: "signal",
    topics: ["ETH", "SOL"],
  },
  {
    id: "post-3",
    visual: true,
    author: "Desk Research",
    handle: "@deskresearch",
    createdAt: timeAgo(9 * HOUR),
    body: "Weekly liquidity map is out. Stablecoin netflows turned positive for the first time in three weeks, and perp open interest is rebuilding without a funding blow-off.",
    likeCount: 356,
    replyCount: 312,
    shareCount: 64,
    tone: "chart",
    topics: ["Macro", "On-chain"],
  },
  {
    id: "post-4",
    visual: false,
    author: "Nina Levels",
    handle: "@ninalevels",
    createdAt: timeAgo(26 * HOUR),
    body: "Reminder for the new traders in here: position size before conviction. A clean invalidation level is worth more than a perfect entry.",
    likeCount: 2810,
    replyCount: 428,
    shareCount: 233,
    tone: "signal",
    topics: ["Education", "Risk"],
  },
];

export const newsCards = [
  {
    id: "news-1",
    tag: "Breaking News",
    title: "Major Bank Integrates Blockchain for Real-Time Settlements",
    summary: "Global Reserve announced a successful private ledger pilot for institutional settlement flows.",
    createdAt: timeAgo(45 * MINUTE),
  },
  {
    id: "news-2",
    tag: "Market Desk",
    title: "Ethereum Layer-2 Adoption Hits All-Time High",
    summary: "Network activity is spreading across scaling ecosystems while mainnet fees remain calmer.",
    createdAt: timeAgo(3 * HOUR),
  },
  {
    id: "news-3",
    tag: "Research",
    title: "Institutional Investors Accumulate ETH Amid Volatility",
    summary: "Desk flows show steady accumulation during intraday weakness across large-cap assets.",
    createdAt: timeAgo(7 * HOUR),
  },
];

export const upcomingEvents = [
  { id: "event-1", month: "Jun", day: "12", title: "Ethereum Dencun Fork", place: "Mainnet Launch" },
  { id: "event-2", month: "Jun", day: "18", title: "Crypto Global Expo", place: "Dubai, UAE" },
  { id: "event-3", month: "Jun", day: "24", title: "FOMC Minutes", place: "Macro calendar" },
];

export const communityCategories = [
  "all",
  "technical",
  "signals",
  "news",
  "education",
  "onchain",
  "memes",
];

export const communities = [
  {
    id: "daily-strategy",
    slug: "daily-strategy",
    name: "Daily Strategy",
    category: "signals",
    memberCount: 48200,
    onlineCount: 1400,
    description: "Pre-market planning, intraday levels, and a shared journal for every session.",
    tags: ["Signals", "Intraday", "Journal"],
  },
  {
    id: "pro-technical-analysts",
    slug: "pro-technical-analysts",
    name: "Pro Technical Analysts",
    category: "technical",
    memberCount: 41800,
    onlineCount: 1120,
    description: "Structured chart reviews, weekly thesis notes, and high-signal trade planning.",
    tags: ["Technical", "Signals", "Education"],
  },
  {
    id: "macro-crypto-desk",
    slug: "macro-crypto-desk",
    name: "Macro Crypto Desk",
    category: "news",
    memberCount: 31900,
    onlineCount: 643,
    description: "Macro calendars, liquidity reads, policy headlines, and risk dashboards.",
    tags: ["Macro", "News", "Risk"],
  },
  {
    id: "onchain-lab",
    slug: "onchain-lab",
    name: "On-chain Lab",
    category: "onchain",
    memberCount: 18600,
    onlineCount: 388,
    description: "Wallet clustering, exchange netflows, and dashboards you can copy into your own workflow.",
    tags: ["On-chain", "Data", "Research"],
  },
  {
    id: "meme-hunters-dao",
    slug: "meme-hunters-dao",
    name: "Meme Hunters DAO",
    category: "memes",
    memberCount: 23700,
    onlineCount: 822,
    description: "Early narrative tracking, social momentum boards, and community discovery.",
    tags: ["Memes", "On-chain", "Social"],
  },
  {
    id: "trading-school",
    slug: "trading-school",
    name: "Trading School",
    category: "education",
    memberCount: 52400,
    onlineCount: 917,
    description: "Structured curriculum from risk basics to execution reviews, with weekly office hours.",
    tags: ["Education", "Risk", "Mentoring"],
  },
];

export const communityChannels = [
  { id: "general", label: "General", unread: 12 },
  { id: "technical-analysis", label: "Technical Analysis", unread: 4 },
  { id: "signals", label: "Signals", unread: 7 },
  { id: "announcements", label: "Announcements", unread: 0 },
  { id: "trading-school", label: "Trading School", unread: 2 },
];

export const communityChannelTopics = {
  general: "Market context, chart reads, and desk notes.",
  "technical-analysis": "Structure, levels, and chart reviews from the desk.",
  signals: "Actionable setups with invalidation levels attached.",
  announcements: "Read-only updates from the moderation team.",
  "trading-school": "Lessons, homework reviews, and office hours.",
};

export const communityMembers = [
  { id: "member-1", displayName: "Sarah Connor", role: "Options trader", online: true },
  { id: "member-2", displayName: "Maya Quant", role: "Quant research", online: true },
  { id: "member-3", displayName: "Alex Rivera", role: "Community lead", online: true },
  { id: "member-4", displayName: "Nina Levels", role: "Swing trader", online: false },
  { id: "member-5", displayName: "Tom Osaka", role: "Macro desk", online: false },
];

function mockMessage(id, author, content, offsetMs) {
  return {
    id,
    senderId: author,
    sender: { displayName: author },
    content,
    createdAt: timeAgo(offsetMs),
  };
}

export const communityMessagesByChannel = {
  general: [
    mockMessage("c-general-1", "Sarah Connor", "Morning. BTC retested the breakout zone without heavy selling.", 96 * MINUTE),
    mockMessage("c-general-2", "Alex Rivera", "Same read here. Volume profile still favours the buyers above the value area high.", 88 * MINUTE),
    mockMessage("c-general-3", "Maya Quant", "Funding is flat, so the move is spot-led for now. That is usually the healthier version.", 61 * MINUTE),
    mockMessage("c-general-4", "Nina Levels", "Watching the daily close. Below it I stand down and wait for the retest.", 24 * MINUTE),
  ],
  "technical-analysis": [
    mockMessage("c-ta-1", "Nina Levels", "ETH is compressing into the 4H trendline. Third touch usually resolves fast.", 4 * HOUR),
    mockMessage("c-ta-2", "Sarah Connor", "Agreed, and relative strength against BTC just turned up on the 1H.", 3 * HOUR),
    mockMessage("c-ta-3", "Tom Osaka", "I marked 3,410 as the invalidation. Clean level, no need to overthink it.", 108 * MINUTE),
  ],
  signals: [
    mockMessage("c-signals-1", "Maya Quant", "SOL long from the reclaim, stop below the session low, first target at the prior high.", 5 * HOUR),
    mockMessage("c-signals-2", "Alex Rivera", "Filled and moved to break-even. Partial taken into the range high.", 2 * HOUR),
  ],
  announcements: [
    mockMessage("c-ann-1", "Alex Rivera", "Weekly review moves to Friday 15:00 UTC. Recording will be posted in Trading School.", 22 * HOUR),
  ],
  "trading-school": [
    mockMessage("c-school-1", "Tom Osaka", "Homework for this week: journal three trades with the invalidation written before entry.", 30 * HOUR),
    mockMessage("c-school-2", "Nina Levels", "Office hours tomorrow. Bring one chart and one question.", 6 * HOUR),
  ],
};

export const notifications = [
  {
    id: "notif-1",
    type: "Price Alert",
    title: "Bitcoin Price Alert",
    text: "BTC crossed your $64,000 alert and is up 2.45% today.",
    createdAt: timeAgo(5 * MINUTE),
    unread: true,
    link: "/coins/btc",
  },
  {
    id: "notif-2",
    type: "Mention",
    title: "Alex Rivera mentioned you",
    text: "Daily Strategy needs your view on the SOL pullback.",
    createdAt: timeAgo(22 * MINUTE),
    unread: true,
    link: "/community/daily-strategy",
  },
  {
    id: "notif-3",
    type: "News",
    title: "Regulatory Update: SEC Guidelines",
    text: "A policy brief was added to the macro news board.",
    createdAt: timeAgo(HOUR),
    unread: true,
    link: "/community/macro-crypto-desk",
  },
  {
    id: "notif-4",
    type: "Community",
    title: "Community Goal Reached",
    text: "Pro Technical Analysts crossed 48k members.",
    createdAt: timeAgo(3 * HOUR),
    unread: false,
    link: "/community/pro-technical-analysts",
  },
  {
    id: "notif-5",
    type: "Price Alert",
    title: "Ethereum Volatility Alert",
    text: "ETH moved more than 3% within the last hour of the session.",
    createdAt: timeAgo(2 * DAY),
    unread: false,
    link: "/coins/eth",
  },
];

// Labels come from `profile.stat.{key}`; the values are sample data.
export const profileStats = [
  { key: "followers", value: "18.4k" },
  { key: "winRate", value: "62%" },
  { key: "signals", value: "318" },
  { key: "reputation", value: "9.4" },
];

export const profileFocusSummary =
  "BTC structure, ETH relative strength, Solana beta, macro calendar risks, and on-chain liquidity flows.";

export const profileFocusTags = ["Risk-first", "Swing", "Macro", "On-chain"];

export const profileBadges = ["Verified Analyst", "Top Contributor", "Signal Mentor"];

export const insightCards = [
  {
    id: "insight-1",
    title: "Bullish Momentum Continues",
    body: "BTC, SOL, and LINK are trading above short-term trend support.",
    action: "Read full analysis",
  },
  {
    id: "insight-2",
    title: "Risk Desk",
    body: "Stablecoin flows are neutral while alt beta is heating up.",
    action: "Open market view",
  },
];

export const coinNews = [
  {
    id: "coin-news-1",
    tag: "Market Desk",
    title: "Spot desks report steady demand into the weekly close",
    createdAt: timeAgo(52 * MINUTE),
  },
  {
    id: "coin-news-2",
    tag: "On-chain",
    title: "Exchange balances keep drifting lower across major venues",
    createdAt: timeAgo(5 * HOUR),
  },
  {
    id: "coin-news-3",
    tag: "Research",
    title: "Derivatives positioning resets after the midweek flush",
    createdAt: timeAgo(20 * HOUR),
  },
];

export const coinDiscussions = [
  {
    id: "coin-post-1",
    author: "Sarah Connor",
    handle: "@sarahlevels",
    createdAt: timeAgo(70 * MINUTE),
    body: "Range high is the only level that matters today. Above it I trail, below it I wait for the retest and let someone else take the first entry.",
  },
  {
    id: "coin-post-2",
    author: "Tom Osaka",
    handle: "@tomdesk",
    createdAt: timeAgo(6 * HOUR),
    body: "Volume is thinner than last week, so I am sizing down and keeping the invalidation tight. Nothing wrong with a smaller position in a slow tape.",
  },
];
