import React from "react";
import ReactDOM from "react-dom/client";
import { Provider } from "react-redux";
import App from "./App";
import { setApiLocale } from "./api/httpClient";
import { resolveInitialLocale } from "./i18n";
import { store } from "./store/store";
import "./styles/index.css";
import "./styles/community-management.css";
import { setFormatterLocale } from "./utils/formatters";

// Set before the first render so the initial paint already formats correctly.
const initialLocale = resolveInitialLocale();
setFormatterLocale(initialLocale);
setApiLocale(initialLocale);

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </React.StrictMode>,
);
