import { Eye, EyeOff } from "lucide-react";
import { useState } from "react";
import { useTranslation } from "../../i18n/useTranslation.js";

export default function PasswordInput({ label, ...inputProps }) {
  const { t } = useTranslation();
  const [visible, setVisible] = useState(false);

  return (
    <label>
      {label}
      <span className="input-with-action">
        <input {...inputProps} type={visible ? "text" : "password"} />
        <button
          type="button"
          aria-label={visible ? t("auth.hidePassword") : t("auth.showPassword")}
          onClick={() => setVisible((current) => !current)}
        >
          {visible ? <EyeOff size={17} /> : <Eye size={17} />}
        </button>
      </span>
    </label>
  );
}
