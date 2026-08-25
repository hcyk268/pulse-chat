import { useState } from "react";
import { getApiErrorMessage } from "../../api/communityApi.js";
import Modal from "../../components/shared/Modal";
import { useTranslation } from "../../i18n/useTranslation.js";
import {
  buildChannelRequest,
  COMMUNITY_CHANNEL_TYPES,
  containsHtmlAngleBracket,
  createChannelForm,
} from "./communityManagement.js";

function validate(form, t) {
  if (!form.name.trim()) return t("community.validation.channelNameRequired");
  if (containsHtmlAngleBracket(form.name, form.description)) {
    return t("community.validation.noAngleBrackets");
  }
  return "";
}

export default function ChannelFormDialog({
  channel,
  mode = "create",
  onClose,
  onSaved,
  onSubmit,
}) {
  const { t } = useTranslation();
  const creating = mode === "create";
  const [form, setForm] = useState(() => createChannelForm(channel));
  const [status, setStatus] = useState({ saving: false, error: "" });

  function updateField(event) {
    const { checked, name, type, value } = event.target;
    setForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
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
      const saved = await onSubmit(buildChannelRequest(form));
      onSaved?.(saved);
      onClose?.();
    } catch (apiError) {
      setStatus({
        saving: false,
        error: getApiErrorMessage(
          apiError,
          t,
          creating ? "errors.communityChannelCreate" : "errors.communityChannelUpdate",
        ),
      });
    }
  }

  return (
    <Modal
      title={t(
        creating ? "community.channel.createTitle" : "community.channel.editTitle",
      )}
      description={t(
        creating
          ? "community.channel.createDescription"
          : "community.channel.editDescription",
      )}
      onClose={status.saving ? undefined : onClose}
    >
      <form className="dialog-form community-form" onSubmit={handleSubmit} noValidate>
        <label>
          {t("community.channel.name")}
          <input
            name="name"
            value={form.name}
            onChange={updateField}
            placeholder={t("community.channel.namePlaceholder")}
            maxLength={100}
            required
          />
        </label>

        <label>
          {t("community.channel.description")}
          <textarea
            name="description"
            rows="4"
            value={form.description}
            onChange={updateField}
            placeholder={t("community.channel.descriptionPlaceholder")}
            maxLength={500}
          />
          <small className="form-counter">{form.description.length}/500</small>
        </label>

        <label>
          {t("community.channel.type")}
          <select name="type" value={form.type} onChange={updateField}>
            {COMMUNITY_CHANNEL_TYPES.map((type) => (
              <option key={type} value={type}>
                {t(`community.channel.type.${type}`)}
              </option>
            ))}
          </select>
        </label>

        <label className="check-field">
          <input
            name="readOnly"
            type="checkbox"
            checked={form.readOnly}
            onChange={updateField}
          />
          <span>{t("community.channel.readOnly")}</span>
        </label>

        {status.error ? (
          <p className="form-error" role="alert">
            {status.error}
          </p>
        ) : null}

        <div className="dialog-form__actions">
          <button
            className="button button--ghost"
            type="button"
            onClick={onClose}
            disabled={status.saving}
          >
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
