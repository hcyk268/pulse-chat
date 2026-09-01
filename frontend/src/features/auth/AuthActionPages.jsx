import { CheckCircle2, KeyRound, MailCheck, Send } from "lucide-react";
import { useState } from "react";
import { Link, Navigate, useLocation, useSearchParams } from "react-router-dom";
import {
  changePassword,
  forgotPassword,
  resendVerification,
  resetPassword,
  verifyEmail,
} from "../../api/authApi.js";
import { getApiErrorMessage } from "../../api/httpClient.js";
import { useTranslation } from "../../i18n/useTranslation.js";
import { resetRealtimeConnection } from "../../services/realtimeClient.js";
import { useAppDispatch, useAppSelector } from "../../store/hooks";
import { selectIsAuthenticated, signedIn } from "../../store/slices/authSlice";
import { isPersistentSession, saveAuthSession } from "../../utils/authStorage.js";
import { isValidEmail } from "../../utils/validators.js";
import AuthCardLayout from "./AuthCardLayout.jsx";
import PasswordInput from "./PasswordInput.jsx";

const MIN_PASSWORD_LENGTH = 12;
const MAX_PASSWORD_LENGTH = 72;

function validateNewPasswords(password, confirmation, t) {
  if (password.length < MIN_PASSWORD_LENGTH) {
    return t("auth.validation.password", { count: MIN_PASSWORD_LENGTH });
  }
  if (password.length > MAX_PASSWORD_LENGTH) {
    return t("auth.validation.passwordMax", { count: MAX_PASSWORD_LENGTH });
  }
  if (password !== confirmation) return t("auth.validation.passwordMatch");
  return "";
}

function StatusMessage({ status }) {
  if (!status.message) return null;
  return (
    <p className={status.success ? "form-success" : "form-error"} role={status.success ? "status" : "alert"}>
      {status.message}
    </p>
  );
}

export function VerifyEmailPage() {
  const { t } = useTranslation();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const token = searchParams.get("token")?.trim() ?? "";
  const [email, setEmail] = useState(location.state?.email ?? "");
  const [verifyStatus, setVerifyStatus] = useState({ loading: false, message: "", success: false });
  const [resendStatus, setResendStatus] = useState({ loading: false, message: "", success: false });

  if (isAuthenticated) return <Navigate to="/chat" replace />;

  async function handleVerify() {
    if (!token) {
      setVerifyStatus({ loading: false, message: t("auth.validation.tokenMissing"), success: false });
      return;
    }

    setVerifyStatus({ loading: true, message: "", success: false });
    try {
      await verifyEmail(token);
      setVerifyStatus({ loading: false, message: t("auth.verifiedBody"), success: true });
    } catch (error) {
      setVerifyStatus({
        loading: false,
        message: getApiErrorMessage(error, t, "errors.emailVerification"),
        success: false,
      });
    }
  }

  async function handleResend(event) {
    event.preventDefault();
    if (!isValidEmail(email)) {
      setResendStatus({ loading: false, message: t("auth.validation.email"), success: false });
      return;
    }

    setResendStatus({ loading: true, message: "", success: false });
    try {
      await resendVerification(email.trim());
      setResendStatus({ loading: false, message: t("auth.resendSuccess"), success: true });
    } catch (error) {
      setResendStatus({
        loading: false,
        message: getApiErrorMessage(error, t, "errors.emailVerification"),
        success: false,
      });
    }
  }

  return (
    <AuthCardLayout title={t("auth.verifyTitle")} subtitle={t("auth.verifySubtitle")} backToLogin>
      {location.state?.registrationComplete ? (
        <p className="form-notice">{t("auth.registrationSent", { email: location.state.email })}</p>
      ) : null}

      {token ? (
        <div className="auth-action-block">
          <MailCheck className="auth-action-icon" size={30} aria-hidden="true" />
          <button className="button button--primary" type="button" onClick={handleVerify} disabled={verifyStatus.loading || verifyStatus.success}>
            <CheckCircle2 size={17} />
            {verifyStatus.loading ? t("auth.verifying") : t("auth.verifyTokenAction")}
          </button>
          <StatusMessage status={verifyStatus} />
        </div>
      ) : null}

      {!verifyStatus.success ? (
        <form onSubmit={handleResend} noValidate>
          <label>
            {t("auth.email")}
            <input name="email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" required />
          </label>
          <StatusMessage status={resendStatus} />
          <button className="button button--ghost" type="submit" disabled={resendStatus.loading}>
            <Send size={17} />
            {resendStatus.loading ? t("auth.resending") : t("auth.resendAction")}
          </button>
        </form>
      ) : null}
    </AuthCardLayout>
  );
}

export function ForgotPasswordPage() {
  const { t } = useTranslation();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const [email, setEmail] = useState("");
  const [status, setStatus] = useState({ loading: false, message: "", success: false });

  if (isAuthenticated) return <Navigate to="/chat" replace />;

  async function handleSubmit(event) {
    event.preventDefault();
    if (!isValidEmail(email)) {
      setStatus({ loading: false, message: t("auth.validation.email"), success: false });
      return;
    }

    setStatus({ loading: true, message: "", success: false });
    try {
      await forgotPassword(email.trim());
      setStatus({ loading: false, message: t("auth.forgotSuccessBody"), success: true });
    } catch (error) {
      setStatus({
        loading: false,
        message: getApiErrorMessage(error, t, "errors.passwordReset"),
        success: false,
      });
    }
  }

  return (
    <AuthCardLayout title={t("auth.forgotTitle")} subtitle={t("auth.forgotSubtitle")} backToLogin>
      <form onSubmit={handleSubmit} noValidate>
        <label>
          {t("auth.email")}
          <input name="email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" required disabled={status.success} />
        </label>
        <StatusMessage status={status} />
        {!status.success ? (
          <button className="button button--primary" type="submit" disabled={status.loading}>
            <Send size={17} />
            {status.loading ? t("auth.forgotPending") : t("auth.forgotSubmit")}
          </button>
        ) : null}
      </form>
    </AuthCardLayout>
  );
}

export function ResetPasswordPage() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const isAuthenticated = useAppSelector(selectIsAuthenticated);
  const token = searchParams.get("token")?.trim() ?? "";
  const [form, setForm] = useState({ newPassword: "", confirmPassword: "" });
  const [status, setStatus] = useState({
    loading: false,
    message: token ? "" : t("auth.validation.tokenMissing"),
    success: false,
  });

  if (isAuthenticated) return <Navigate to="/chat" replace />;

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const validationError = token
      ? validateNewPasswords(form.newPassword, form.confirmPassword, t)
      : t("auth.validation.tokenMissing");
    if (validationError) {
      setStatus({ loading: false, message: validationError, success: false });
      return;
    }

    setStatus({ loading: true, message: "", success: false });
    try {
      await resetPassword({ token, ...form });
      setStatus({ loading: false, message: t("auth.resetSuccessBody"), success: true });
      setForm({ newPassword: "", confirmPassword: "" });
    } catch (error) {
      setStatus({
        loading: false,
        message: getApiErrorMessage(error, t, "errors.passwordReset"),
        success: false,
      });
    }
  }

  return (
    <AuthCardLayout title={t("auth.resetTitle")} subtitle={t("auth.resetSubtitle")} backToLogin>
      <form onSubmit={handleSubmit} noValidate>
        <PasswordInput label={t("auth.newPassword")} name="newPassword" value={form.newPassword} onChange={updateField} autoComplete="new-password" minLength={MIN_PASSWORD_LENGTH} maxLength={MAX_PASSWORD_LENGTH} required disabled={status.success} />
        <PasswordInput label={t("auth.confirmPassword")} name="confirmPassword" value={form.confirmPassword} onChange={updateField} autoComplete="new-password" minLength={MIN_PASSWORD_LENGTH} maxLength={MAX_PASSWORD_LENGTH} required disabled={status.success} />
        <StatusMessage status={status} />
        {!status.success ? (
          <button className="button button--primary" type="submit" disabled={status.loading || !token}>
            <KeyRound size={17} />
            {status.loading ? t("auth.resetPending") : t("auth.resetSubmit")}
          </button>
        ) : null}
      </form>
    </AuthCardLayout>
  );
}

export function ChangePasswordPage() {
  const { t } = useTranslation();
  const dispatch = useAppDispatch();
  const [form, setForm] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [status, setStatus] = useState({ loading: false, message: "", success: false });

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const validationError = form.currentPassword.length < 8
      ? t("auth.validation.currentPassword")
      : validateNewPasswords(form.newPassword, form.confirmPassword, t);
    if (validationError) {
      setStatus({ loading: false, message: validationError, success: false });
      return;
    }

    setStatus({ loading: true, message: "", success: false });
    try {
      const authResponse = await changePassword(form);
      saveAuthSession(authResponse, isPersistentSession());
      dispatch(signedIn(authResponse?.user));
      resetRealtimeConnection();
      setForm({ currentPassword: "", newPassword: "", confirmPassword: "" });
      setStatus({ loading: false, message: t("auth.changeSuccess"), success: true });
    } catch (error) {
      setStatus({
        loading: false,
        message: getApiErrorMessage(error, t, "errors.passwordChange"),
        success: false,
      });
    }
  }

  return (
    <AuthCardLayout title={t("auth.changeTitle")} subtitle={t("auth.changeSubtitle")}>
      <form onSubmit={handleSubmit} noValidate>
        <PasswordInput label={t("auth.currentPassword")} name="currentPassword" value={form.currentPassword} onChange={updateField} autoComplete="current-password" minLength={8} maxLength={100} required />
        <PasswordInput label={t("auth.newPassword")} name="newPassword" value={form.newPassword} onChange={updateField} autoComplete="new-password" minLength={MIN_PASSWORD_LENGTH} maxLength={MAX_PASSWORD_LENGTH} required />
        <PasswordInput label={t("auth.confirmPassword")} name="confirmPassword" value={form.confirmPassword} onChange={updateField} autoComplete="new-password" minLength={MIN_PASSWORD_LENGTH} maxLength={MAX_PASSWORD_LENGTH} required />
        <StatusMessage status={status} />
        <button className="button button--primary" type="submit" disabled={status.loading}>
          <KeyRound size={17} />
          {status.loading ? t("auth.changePending") : t("auth.changeSubmit")}
        </button>
        <Link className="auth-switch" to="/profile">{t("auth.backToProfile")}</Link>
      </form>
    </AuthCardLayout>
  );
}
