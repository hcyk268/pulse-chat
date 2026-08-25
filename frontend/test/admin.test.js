import test from "node:test";
import assert from "node:assert/strict";
import adminReducer, {
  archiveCommunity,
  dismissReport,
  filterAdminCommunities,
  filterAdminReports,
  filterAdminUsers,
  reactivateUser,
  resolveReport,
  restoreCommunity,
  selectOpenReportCount,
  suspendUser,
  toggleCommunityFeatured,
} from "../src/store/slices/adminSlice.js";

const users = [
  { id: 1, displayName: "Alex Crypto", username: "alextrades", email: "alex@example.com", status: "active" },
  { id: 2, displayName: "Grid Bot", username: "gridbot", email: "grid@example.com", status: "suspended" },
  { id: 3, displayName: "Pump Watcher", username: "pumpwatcher", email: "pump@example.com", status: "pending" },
];

function initialState() {
  return adminReducer(undefined, { type: "@@INIT" });
}

test("user table filters combine free text and status", () => {
  assert.equal(filterAdminUsers(users, { query: "", status: "all" }).length, 3);
  assert.equal(filterAdminUsers(users, { status: "suspended" })[0].username, "gridbot");
  assert.equal(filterAdminUsers(users, { query: "example.com" }).length, 3, "matches on email");
  assert.equal(filterAdminUsers(users, { query: "pump" })[0].id, 3);
  assert.equal(filterAdminUsers(users, { query: "alex", status: "pending" }).length, 0);
});

test("report and community filters", () => {
  const reports = [
    { id: "a", status: "open" },
    { id: "b", status: "resolved" },
  ];

  assert.equal(filterAdminReports(reports, "all").length, 2);
  assert.equal(filterAdminReports(reports, "open")[0].id, "a");
  assert.equal(filterAdminReports(reports).length, 2);

  const communities = [
    { id: "x", name: "Daily Strategy", owner: "sarah" },
    { id: "y", name: "Meme Hunters", owner: "pump" },
  ];

  assert.equal(filterAdminCommunities(communities, "").length, 2);
  assert.equal(filterAdminCommunities(communities, "meme")[0].id, "y");
  assert.equal(filterAdminCommunities(communities, "sarah")[0].id, "x");
});

test("suspending and reactivating a user records an audit entry each time", () => {
  let state = initialState();
  const auditBefore = state.auditLog.length;
  const target = state.users.find((user) => user.status === "active");

  state = adminReducer(state, suspendUser(target.id));
  assert.equal(state.users.find((user) => user.id === target.id).status, "suspended");
  assert.equal(state.auditLog.length, auditBefore + 1);
  assert.equal(state.auditLog[0].action, "user.suspended");
  assert.equal(state.auditLog[0].target, target.username);

  // Suspending twice must not double-log.
  state = adminReducer(state, suspendUser(target.id));
  assert.equal(state.auditLog.length, auditBefore + 1);

  state = adminReducer(state, reactivateUser(target.id));
  assert.equal(state.users.find((user) => user.id === target.id).status, "active");
  assert.equal(state.auditLog[0].action, "user.reactivated");
});

test("only open reports can be resolved or dismissed", () => {
  let state = initialState();
  const openCount = selectOpenReportCount({ admin: state });
  assert.ok(openCount > 0);

  const open = state.reports.find((report) => report.status === "open");
  state = adminReducer(state, resolveReport(open.id));
  assert.equal(state.reports.find((report) => report.id === open.id).status, "resolved");
  assert.equal(selectOpenReportCount({ admin: state }), openCount - 1);

  const auditLength = state.auditLog.length;
  state = adminReducer(state, dismissReport(open.id));
  assert.equal(state.reports.find((report) => report.id === open.id).status, "resolved");
  assert.equal(state.auditLog.length, auditLength, "a closed report cannot be reopened");
});

test("archiving a community drops its featured flag and can be restored", () => {
  let state = initialState();
  const featured = state.communities.find((community) => community.featured);

  state = adminReducer(state, archiveCommunity(featured.id));
  const archived = state.communities.find((community) => community.id === featured.id);
  assert.equal(archived.status, "archived");
  assert.equal(archived.featured, false);

  state = adminReducer(state, restoreCommunity(featured.id));
  assert.equal(state.communities.find((community) => community.id === featured.id).status, "active");

  state = adminReducer(state, toggleCommunityFeatured(featured.id));
  assert.equal(state.communities.find((community) => community.id === featured.id).featured, true);
  assert.equal(state.auditLog[0].action, "community.featured");
});
