import { createSlice, nanoid } from "@reduxjs/toolkit";
import {
  adminAuditLog,
  adminCommunities,
  adminReports,
  adminUsers,
} from "../../data/adminMockData.js";

const CURRENT_ADMIN = "sarahlevels";

/** Every mutation writes an audit entry, the way a real console would. */
function pushAudit(state, action, target, note = "") {
  state.auditLog.unshift({
    id: `aud-${nanoid(6)}`,
    actor: CURRENT_ADMIN,
    action,
    target,
    note,
    createdAt: new Date().toISOString(),
  });
}

const adminSlice = createSlice({
  name: "admin",
  initialState: {
    userQuery: "",
    userStatusFilter: "all",
    reportStatusFilter: "open",
    communityQuery: "",
    users: adminUsers,
    reports: adminReports,
    communities: adminCommunities,
    auditLog: adminAuditLog,
  },
  reducers: {
    setUserQuery(state, action) {
      state.userQuery = action.payload;
    },
    setUserStatusFilter(state, action) {
      state.userStatusFilter = action.payload;
    },
    setReportStatusFilter(state, action) {
      state.reportStatusFilter = action.payload;
    },
    setCommunityQuery(state, action) {
      state.communityQuery = action.payload;
    },
    suspendUser(state, action) {
      const user = state.users.find((entry) => entry.id === action.payload);
      if (!user || user.status === "suspended") return;

      user.status = "suspended";
      pushAudit(state, "user.suspended", user.username);
    },
    reactivateUser(state, action) {
      const user = state.users.find((entry) => entry.id === action.payload);
      if (!user || user.status === "active") return;

      user.status = "active";
      pushAudit(state, "user.reactivated", user.username);
    },
    resolveReport(state, action) {
      const report = state.reports.find((entry) => entry.id === action.payload);
      if (!report || report.status !== "open") return;

      report.status = "resolved";
      report.resolvedAt = new Date().toISOString();
      report.resolvedBy = CURRENT_ADMIN;
      pushAudit(state, "report.resolved", report.id);
    },
    dismissReport(state, action) {
      const report = state.reports.find((entry) => entry.id === action.payload);
      if (!report || report.status !== "open") return;

      report.status = "dismissed";
      report.resolvedAt = new Date().toISOString();
      report.resolvedBy = CURRENT_ADMIN;
      pushAudit(state, "report.dismissed", report.id);
    },
    toggleCommunityFeatured(state, action) {
      const community = state.communities.find((entry) => entry.id === action.payload);
      if (!community) return;

      community.featured = !community.featured;
      pushAudit(state, community.featured ? "community.featured" : "community.unfeatured", community.id);
    },
    archiveCommunity(state, action) {
      const community = state.communities.find((entry) => entry.id === action.payload);
      if (!community || community.status === "archived") return;

      community.status = "archived";
      community.featured = false;
      pushAudit(state, "community.archived", community.id);
    },
    restoreCommunity(state, action) {
      const community = state.communities.find((entry) => entry.id === action.payload);
      if (!community || community.status === "active") return;

      community.status = "active";
      pushAudit(state, "community.restored", community.id);
    },
  },
});

export const {
  setUserQuery,
  setUserStatusFilter,
  setReportStatusFilter,
  setCommunityQuery,
  suspendUser,
  reactivateUser,
  resolveReport,
  dismissReport,
  toggleCommunityFeatured,
  archiveCommunity,
  restoreCommunity,
} = adminSlice.actions;

export const selectAdminUsers = (state) => state.admin.users;
export const selectAdminReports = (state) => state.admin.reports;
export const selectAdminCommunities = (state) => state.admin.communities;
export const selectAdminAuditLog = (state) => state.admin.auditLog;
export const selectUserQuery = (state) => state.admin.userQuery;
export const selectUserStatusFilter = (state) => state.admin.userStatusFilter;
export const selectReportStatusFilter = (state) => state.admin.reportStatusFilter;
export const selectAdminCommunityQuery = (state) => state.admin.communityQuery;
export const selectOpenReportCount = (state) =>
  state.admin.reports.reduce((total, report) => total + (report.status === "open" ? 1 : 0), 0);

/** Pure filters so the tables and their tests agree. */
export function filterAdminUsers(users, { query, status }) {
  const normalizedQuery = (query ?? "").trim().toLowerCase();

  return users.filter((user) => {
    if (status && status !== "all" && user.status !== status) return false;
    if (!normalizedQuery) return true;

    return [user.displayName, user.username, user.email]
      .join(" ")
      .toLowerCase()
      .includes(normalizedQuery);
  });
}

export function filterAdminReports(reports, status) {
  if (!status || status === "all") return reports;

  return reports.filter((report) => report.status === status);
}

export function filterAdminCommunities(communities, query) {
  const normalizedQuery = (query ?? "").trim().toLowerCase();
  if (!normalizedQuery) return communities;

  return communities.filter((community) =>
    [community.name, community.owner].join(" ").toLowerCase().includes(normalizedQuery),
  );
}

export default adminSlice.reducer;
