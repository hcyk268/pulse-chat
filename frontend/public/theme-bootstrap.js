// Runs before the app bundle so theme and language are correct on first paint.
(function () {
  try {
    var stored = localStorage.getItem("chatapp.theme");
    var prefersDark =
      window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
    var theme = stored === "light" || stored === "dark" ? stored : prefersDark ? "dark" : "light";
    document.documentElement.dataset.theme = theme;

    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute("content", theme === "dark" ? "#0a0f1a" : "#f8fafc");

    var storedLocale = localStorage.getItem("chatapp.locale");
    var browserLocale = (navigator.language || "en").toLowerCase().split("-")[0];
    document.documentElement.lang =
      storedLocale === "vi" || storedLocale === "en"
        ? storedLocale
        : browserLocale === "vi"
          ? "vi"
          : "en";
  } catch {
    // Storage can be blocked; CSS and the app still provide safe defaults.
  }
})();
