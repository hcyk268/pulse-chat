import { useTranslation } from "../../i18n/useTranslation.js";

function describeTyping(users, t) {
  const names = users.map((user) => user.username ?? "");

  if (names.length === 1) return t("chat.typing.one", { name: names[0] });
  if (names.length === 2) return t("chat.typing.two", { first: names[0], second: names[1] });

  return t("chat.typing.many", { name: names[0], count: names.length - 1 });
}

export default function TypingIndicator({ users = [] }) {
  const { t } = useTranslation();

  if (users.length === 0) return null;

  return (
    <p className="typing-indicator" aria-live="polite">
      <span className="typing-indicator__dots" aria-hidden="true">
        <i />
        <i />
        <i />
      </span>
      {describeTyping(users, t)}
    </p>
  );
}
