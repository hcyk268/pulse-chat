import { createSlice } from "@reduxjs/toolkit";

/** Stable ids: the visible label comes from `market.filter.{id}`. */
export const MARKET_FILTERS = ["all", "top100", "favorites", "trending"];

const marketSlice = createSlice({
  name: "market",
  initialState: {
    // Seeded from the watchlist endpoint once the user is authenticated.
    favoriteSymbols: [],
    activeFilter: MARKET_FILTERS[0],
  },
  reducers: {
    toggleFavorite(state, action) {
      const symbol = action.payload;
      if (state.favoriteSymbols.includes(symbol)) {
        state.favoriteSymbols = state.favoriteSymbols.filter((item) => item !== symbol);
        return;
      }

      state.favoriteSymbols.push(symbol);
    },
    setFavorites(state, action) {
      state.favoriteSymbols = action.payload ?? [];
    },
    clearFavorites(state) {
      state.favoriteSymbols = [];
    },
    setActiveMarketFilter(state, action) {
      state.activeFilter = MARKET_FILTERS.includes(action.payload)
        ? action.payload
        : MARKET_FILTERS[0];
    },
  },
});

export const { toggleFavorite, setFavorites, clearFavorites, setActiveMarketFilter } =
  marketSlice.actions;
export const selectFavoriteSymbols = (state) => state.market.favoriteSymbols;
export const selectMarketFilter = (state) => state.market.activeFilter;
export default marketSlice.reducer;
