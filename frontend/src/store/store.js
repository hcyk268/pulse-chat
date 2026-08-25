import { configureStore } from "@reduxjs/toolkit";
import adminReducer from "./slices/adminSlice";
import authReducer from "./slices/authSlice";
import communityReducer from "./slices/communitySlice";
import marketReducer from "./slices/marketSlice";
import notificationsReducer from "./slices/notificationsSlice";
import uiReducer from "./slices/uiSlice";
import workspaceReducer from "./slices/workspaceSlice";

export const store = configureStore({
  reducer: {
    admin: adminReducer,
    auth: authReducer,
    community: communityReducer,
    market: marketReducer,
    notifications: notificationsReducer,
    ui: uiReducer,
    workspace: workspaceReducer,
  },
});

export default store;
