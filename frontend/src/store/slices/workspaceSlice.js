import { createSlice } from "@reduxjs/toolkit";

const workspaceSlice = createSlice({
  name: "workspace",
  initialState: {
    activeConversation: null,
  },
  reducers: {
    setActiveConversation(state, action) {
      state.activeConversation = action.payload;
    },
    clearActiveConversation(state) {
      state.activeConversation = null;
    },
  },
});

export const { setActiveConversation, clearActiveConversation } = workspaceSlice.actions;
export const selectActiveConversation = (state) => state.workspace.activeConversation;
export default workspaceSlice.reducer;
