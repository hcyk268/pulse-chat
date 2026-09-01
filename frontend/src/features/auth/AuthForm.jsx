import { Eye, EyeOff, Lock, UserPlus } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { login, register } from "../../api/authApi.js";
import { getApiErrorCode, getApiErrorMessage } from "../../api/httpClient.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { resetRealtimeConnection } from "../../services/realtimeClient.js";
import { useAppDispatch, useAppSelector } from "../../store/hooks.js";
import {
  expiredNoticeDismissed,
  selectSessionExpiredNotice,
  signedIn,
} from "../../store/slices/authSlice.js";
import { saveAuthSession } from "../../utils/authStorage.js";
import {
  hasNoHtmlAngleBrackets,
  isValidEmail,
  isValidUsername,
} from "../../utils/validators.js";

const MIN_PASSWORD_LENGTH = 12;
const MAX_PASSWORD_LENGTH = 72;

const emptyForm = {
  usernameOrEmail: "",
  username: "",
  email: "",
  displayName: "",
  password: "",
  confirmPassword: "",
  remember: true,
};

function validate(form, isRegister, t) {
  if (!isRegister) {
    if (!form.usernameOrEmail.trim()) return t("auth.validation.usernameOrEmail");
    return form.password ? "" : t("auth.validation.passwordRequired");
  }

  if (!isValidUsername(form.username.trim())) return t("auth.validation.username");
  if (!isValidEmail(form.email)) return t("auth.validation.email");
  if (!form.displayName.trim()) return t("auth.validation.displayName");
  if (!hasNoHtmlAngleBrackets(form.displayName)) return t("auth.validation.noAngleBrackets");
  if (form.password.length < MIN_PASSWORD_LENGTH) {
    return t("auth.validation.password", { count: MIN_PASSWORD_LENGTH });
  }
  if (form.password.length > MAX_PASSWORD_LENGTH) {
    return t("auth.validation.passwordMax", { count: MAX_PASSWORD_LENGTH });
  }
  if (form.password !== form.confirmPassword) return t("auth.validation.passwordMatch");
  return "";
}

export default function AuthForm({
  mode = "login",
  onAuthenticated,
  onModeChange,
  onRegistrationComplete,
  showSessionExpired = false,
}) {
  const dispatch = useAppDispatch();
  const { t } = useTranslation();
  const sessionExpired = useAppSelector(selectSessionExpiredNotice);
  const isRegister = mode === "register";
  const [form, setForm] = useState(emptyForm);
  const [showPassword, setShowPassword] = useState(false);
  const [status, setStatus] = useState({ loading: false, error: "" });

  useEffect(() => {
    setForm(emptyForm);
    setShowPassword(false);
    setStatus({ loading: false, error: "" });
  }, [mode]);

  function updateField(event) {
    const { name, type, checked, value } = event.target;
    setForm((current) => ({
      ...current,
      [name]: type === "checkbox" ? checked : value,
    }));
    if (sessionExpired) dispatch(expiredNoticeDismissed());
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const validationError = validate(form, isRegister, t);
    if (validationError) {
      setStatus({ loading: false, error: validationError });
      return;
    }

    setStatus({ loading: true, error: "" });
    try {
      if (isRegister) {
        const registration = await register({
          username: form.username.trim(),
          email: form.email.trim(),
          displayName: form.displayName.trim(),
          password: form.password,
          confirmPassword: form.confirmPassword,
        });
        setStatus({ loading: false, error: "" });
        onRegistrationComplete?.(registration?.email ?? form.email.trim());
        return;
      }

      const authResponse = await login({
        usernameOrEmail: form.usernameOrEmail.trim(),
        password: form.password,
      });
      saveAuthSession(authResponse, form.remember);
      dispatch(signedIn(authResponse?.user));
      resetRealtimeConnection();
      setStatus({ loading: false, error: "" });
      onAuthenticated?.(authResponse?.user);
    } catch (error) {
      const fallbackKey = isRegister
        ? "errors.registration"
        : getApiErrorCode(error) === "EMAIL_VERIFICATION_REQUIRED"
          ? "auth.email.verification.required"
          : "errors.credentials";
      setStatus({ loading: false, error: getApiErrorMessage(error, t, fallbackKey) });
    }
  }

  function switchMode(event) {
    if (!onModeChange) return;
    event.preventDefault();
    onModeChange(isRegister ? "login" : "register");
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit} noValidate>
      {showSessionExpired && sessionExpired && !isRegister ? (
        <p className="form-notice">{t("auth.sessionExpired")}</p>
      ) : null}

      {isRegister ? (
        <>
          <label>
            {t("auth.username")}
            <input name="username" value={form.username} onChange={updateField} autoComplete="username" required />
          </label>
          <label>
            {t("auth.email")}
            <input name="email" type="email" value={form.email} onChange={updateField} autoComplete="email" required />
          </label>
          <label>
            {t("auth.displayName")}
            <input name="displayName" value={form.displayName} onChange={updateField} autoComplete="name" required />
          </label>
        </>
      ) : (
        <label>
          {t("auth.usernameOrEmail")}
          <input name="usernameOrEmail" value={form.usernameOrEmail} onChange={updateField} autoComplete="username" required />
        </label>
      )}

      <label>
        {t("auth.password")}
        <span className="input-with-action">
          <input
            name="password"
            type={showPassword ? "text" : "password"}
            value={form.password}
            onChange={updateField}
            autoComplete={isRegister ? "new-password" : "current-password"}
            minLength={isRegister ? MIN_PASSWORD_LENGTH : 8}
            maxLength={isRegister ? MAX_PASSWORD_LENGTH : 100}
            required
          />
          <button
            type="button"
            aria-label={showPassword ? t("auth.hidePassword") : t("auth.showPassword")}
            onClick={() => setShowPassword((current) => !current)}
          >
            {showPassword ? <EyeOff size={17} /> : <Eye size={17} />}
          </button>
        </span>
      </label>

      {isRegister ? (
        <label>
          {t("auth.confirmPassword")}
          <input
            name="confirmPassword"
            type={showPassword ? "text" : "password"}
            value={form.confirmPassword}
            onChange={updateField}
            autoComplete="new-password"
            minLength={MIN_PASSWORD_LENGTH}
            maxLength={MAX_PASSWORD_LENGTH}
            required
          />
        </label>
      ) : (
        <div className="auth-form-options">
          <label className="checkbox-row">
            <input name="remember" type="checkbox" checked={form.remember} onChange={updateField} />
            <span>{t("auth.remember")}</span>
          </label>
          <Link to="/forgot-password">{t("auth.forgotLink")}</Link>
        </div>
      )}

      {status.error ? <p className="form-error" role="alert">{status.error}</p> : null}

      <button className="button button--primary" type="submit" disabled={status.loading}>
        {isRegister ? <UserPlus size={17} /> : <Lock size={17} />}
        {status.loading
          ? t(isRegister ? "auth.pendingRegister" : "auth.pendingSignIn")
          : t(isRegister ? "auth.submitRegister" : "auth.submitSignIn")}
      </button>

      <Link className="auth-switch" to={isRegister ? "/login" : "/register"} onClick={switchMode}>
        {t(isRegister ? "auth.switchToSignIn" : "auth.switchToRegister")}
      </Link>
      {!isRegister ? (
        <Link className="auth-switch auth-switch--muted" to="/verify-email">
          {t("auth.verifyLink")}
        </Link>
      ) : null}
    </form>
  );
}
