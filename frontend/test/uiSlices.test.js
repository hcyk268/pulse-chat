import test from "node:test";
import assert from "node:assert/strict";
import notificationsReducer, {
  countNotifications,
  markAllNotificationsRead,
  notificationMatchesFilter,
  selectUnreadNotificationCount,
  setNotificationPage,
  toggleNotificationRead,
} from "../src/store/slices/notificationsSlice.js";
import communityReducer, {
  filterCommunities,
  postChannelMessage,
  setActiveChannel,
  toggleJoinCommunity,
} from "../src/store/slices/communitySlice.js";
import marketReducer, {
  setActiveMarketFilter,
  setFavorites,
  toggleFavorite,
} from "../src/store/slices/marketSlice.js";
import uiReducer, { closeMobileMenu, selectTheme, setTheme } from "../src/store/slices/uiSlice.js";
import { identityHue } from "../src/components/shared/utils.js";
import { readStoredTheme, resolveInitialTheme } from "../src/utils/theme.js";

const communityFixtures = [
  { id: "a", name: "Alpha Desk", category: "signals", description: "Intraday levels", tags: ["Signals"] },
  { id: "b", name: "Beta Lab", category: "onchain", description: "Wallet clustering", tags: ["Data"] },
];

test("notification filters and unread counters stay in sync", () => {
  const items = [
    { id: 3, type: "PRICE_ALERT", read: false },
    { id: 2, type: "MENTION", read: true },
    { id: 1, type: "COMMUNITY", read: false },
  ];

  assert.equal(countNotifications(items, "all"), 3);
  assert.equal(countNotifications(items, "unread"), 2);
  assert.equal(countNotifications(items, "community"), 1);
  assert.equal(notificationMatchesFilter(items[0], "alerts"), true);
  assert.equal(notificationMatchesFilter(items[0], "unknown filter"), true);

  let state = notificationsReducer(
    undefined,
    setNotificationPage({ items, unreadCount: 2, hasMore: false }),
  );
  state = notificationsReducer(state, markAllNotificationsRead());
  assert.equal(selectUnreadNotificationCount({ notifications: state }), 0);

  state = notificationsReducer(state, toggleNotificationRead(state.items[0].id));
  assert.equal(selectUnreadNotificationCount({ notifications: state }), 1);
});

test("community discovery filters by category and free text", () => {
  assert.equal(filterCommunities(communityFixtures, { category: "all", query: "" }).length, 2);
  assert.equal(filterCommunities(communityFixtures, { category: "signals" })[0].id, "a");
  assert.equal(filterCommunities(communityFixtures, { query: "clustering" })[0].id, "b");
  assert.equal(filterCommunities(communityFixtures, { query: "nothing" }).length, 0);
});

test("community membership and channel state react to user actions", () => {
  let state = communityReducer(undefined, toggleJoinCommunity("pro-technical-analysts"));
  assert.ok(state.joinedIds.includes("pro-technical-analysts"));

  state = communityReducer(state, toggleJoinCommunity("pro-technical-analysts"));
  assert.equal(state.joinedIds.includes("pro-technical-analysts"), false);

  state = communityReducer(state, toggleJoinCommunity("new-community"));
  assert.equal(state.joinedIds.includes("new-community"), true);

  state = communityReducer(state, setActiveChannel("signals"));
  assert.equal(state.activeChannelId, "signals");
  assert.equal(state.unreadByChannel.signals, 0);

  const before = state.messagesByChannel.signals.length;
  state = communityReducer(state, postChannelMessage({ channelId: "signals", author: "Me", content: "Hi" }));
  assert.equal(state.messagesByChannel.signals.length, before + 1);
  assert.equal(state.messagesByChannel.signals.at(-1).content, "Hi");
});

test("theme state only accepts known themes", () => {
  let state = uiReducer(undefined, closeMobileMenu());
  assert.ok(["light", "dark"].includes(selectTheme({ ui: state })));

  state = uiReducer(state, setTheme("dark"));
  assert.equal(state.theme, "dark");

  state = uiReducer(state, setTheme("solarized"));
  assert.equal(state.theme, "dark", "unknown themes are ignored");

  state = uiReducer(state, setTheme("light"));
  assert.equal(state.theme, "light");
});

test("theme resolution is safe without a browser", () => {
  assert.equal(readStoredTheme(), null);
  assert.equal(resolveInitialTheme(), "light");
});

test("identity hues are deterministic and inside the curated ring", () => {
  const hues = ["alex", "maya", "daily-strategy", "", null, 42].map(identityHue);

  hues.forEach((hue) => {
    assert.equal(Number.isInteger(hue), true);
    assert.ok(hue >= 0 && hue < 360);
  });

  assert.equal(identityHue("alex"), identityHue("alex"));
  assert.notEqual(identityHue("alex"), identityHue("alexa"));
});

test("market favorites start empty and toggle without duplicates", () => {
  let state = marketReducer(undefined, setFavorites(["BTC", "ETH"]));
  assert.deepEqual(state.favoriteSymbols, ["BTC", "ETH"]);

  state = marketReducer(state, toggleFavorite("BTC"));
  assert.deepEqual(state.favoriteSymbols, ["ETH"]);

  state = marketReducer(state, toggleFavorite("BTC"));
  assert.deepEqual(state.favoriteSymbols, ["ETH", "BTC"]);

  state = marketReducer(state, setActiveMarketFilter("nope"));
  assert.equal(state.activeFilter, "all");
});



