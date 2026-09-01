import { useState } from "react";
import { getApiErrorMessage, updateMyProfile } from "../../api/chatApi.js";
import Modal from "../../components/shared/Modal";
import { useTranslation } from "../../i18n/useTranslation.js";
import { hasNoHtmlAngleBrackets, isOptionalHttpUrl } from "../../utils/validators.js";

const MAX_BIO_LENGTH = 500;

function validate({ displayName, avatarUrl, bio }, t) {
  if (!displayName.trim()) return t("profile.dialog.displayNameEmpty");
  if (!hasNoHtmlAngleBrackets(displayName) || !hasNoHtmlAngleBrackets(bio)) {
    return t("auth.validation.noAngleBrackets");
  }
  if (!isOptionalHttpUrl(avatarUrl.trim())) return t("profile.dialog.avatarInvalid");

  return "";
}

export default function EditProfileDialog({ profile, onClose, onSaved }) {
  const { t } = useTranslation();
  const [form, setForm] = useState({
    displayName: profile?.displayName ?? "",
    avatarUrl: profile?.avatarUrl ?? "",
    bio: profile?.bio ?? "",
  });
  const [status, setStatus] = useState({ saving: false, error: "" });

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function handleSubmit(event) {
    event.preventDefault();

    const validationError = validate(form, t);
    if (validationError) {
      setStatus({ saving: false, error: validationError });
      return;
    }

    setStatus({ saving: true, error: "" });

    try {
      const updated = await updateMyProfile({
        displayName: form.displayName.trim(),
        avatarUrl: form.avatarUrl.trim(),
        bio: form.bio.trim(),
      });

      onSaved(updated);
      onClose();
    } catch (apiError) {
      setStatus({
        saving: false,
        error: getApiErrorMessage(apiError, t, "errors.profileSave"),
      });
    }
  }

  return (
    <Modal
      title={t("profile.dialog.title")}
      description={t("profile.dialog.description")}
      onClose={onClose}
    >
      <form className="dialog-form" onSubmit={handleSubmit} noValidate>
        <label>
          {t("profile.dialog.displayName")}
          <input name="displayName" value={form.displayName} onChange={updateField} maxLength={100} />
        </label>
        <label>
          {t("profile.dialog.avatarUrl")}
          <input
            name="avatarUrl"
            value={form.avatarUrl}
            onChange={updateField}
            placeholder={t("profile.dialog.avatarPlaceholder")}
            maxLength={500}
          />
        </label>
        <label>
          {t("profile.dialog.bio")}
          <textarea
            name="bio"
            rows="4"
            value={form.bio}
            onChange={updateField}
            maxLength={MAX_BIO_LENGTH}
            placeholder={t("profile.dialog.bioPlaceholder")}
          />
          <small className="composer-card__counter">
            {t("home.composer.counter", { count: form.bio.length, max: MAX_BIO_LENGTH })}
          </small>
        </label>

        {status.error ? (
          <p className="form-error" role="alert">
            {status.error}
          </p>
        ) : null}

        <div className="dialog-form__actions">
          <button className="button button--ghost" type="button" onClick={onClose}>
            {t("common.cancel")}
          </button>
          <button className="button button--primary" type="submit" disabled={status.saving}>
            {status.saving ? t("common.saving") : t("common.save")}
          </button>
        </div>
      </form>
    </Modal>
  );
}
