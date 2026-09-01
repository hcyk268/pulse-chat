import { createSlice } from "@reduxjs/toolkit";
import { toCurrentUser } from "../../domain/chat/normalizers.js";
import { getAuthSession } from "../../utils/authStorage.js";

function readStoredState() {
  const session = getAuthSession();

  return {
    authenticated: Boolean(session),
    user: session?.user ? toCurrentUser(session.user) : null,
    expiredNotice: false,
  };
}

const authSlice = createSlice({
  name: "auth",
  initialState: readStoredState(),
  reducers: {
    signedIn(state, action) {
      state.authenticated = true;
      state.user = toCurrentUser(action.payload);
      state.expiredNotice = false;
    },
    profileLoaded(state, action) {
      if (!action.payload) return;
      state.user = toCurrentUser(action.payload);
    },
    signedOut(state, action) {
      state.authenticated = false;
      state.user = null;
      state.expiredNotice = Boolean(action.payload?.expired);
    },
    expiredNoticeDismissed(state) {
      state.expiredNotice = false;
    },
  },
});

export const { signedIn, profileLoaded, signedOut, expiredNoticeDismissed } = authSlice.actions;
export const selectIsAuthenticated = (state) => state.auth.authenticated;
export const selectCurrentUser = (state) => state.auth.user;
export const selectSessionExpiredNotice = (state) => state.auth.expiredNotice;
export default authSlice.reducer;
