import { createSlice } from "@reduxjs/toolkit";
import { LOCALES, resolveInitialLocale } from "../../i18n/index.js";
import { THEMES, resolveInitialTheme } from "../../utils/theme.js";

const uiSlice = createSlice({
  name: "ui",
  initialState: {
    mobileMenuOpen: false,
    theme: resolveInitialTheme(),
    locale: resolveInitialLocale(),
  },
  reducers: {
    openMobileMenu(state) {
      state.mobileMenuOpen = true;
    },
    closeMobileMenu(state) {
      state.mobileMenuOpen = false;
    },
    setTheme(state, action) {
      if (!THEMES.includes(action.payload)) return;
      state.theme = action.payload;
    },
    setLocale(state, action) {
      if (!LOCALES.includes(action.payload)) return;
      state.locale = action.payload;
    },
  },
});

export const { openMobileMenu, closeMobileMenu, setTheme, setLocale } = uiSlice.actions;
export const selectMobileMenuOpen = (state) => state.ui.mobileMenuOpen;
export const selectTheme = (state) => state.ui.theme;
export const selectLocale = (state) => state.ui.locale;
export default uiSlice.reducer;
