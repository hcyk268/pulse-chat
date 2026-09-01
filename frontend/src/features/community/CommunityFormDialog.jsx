import { useState } from "react";
import { getApiErrorMessage } from "../../api/communityApi.js";
import Modal from "../../components/shared/Modal";
import { classNames } from "../../components/shared/utils";
import { useTranslation } from "../../i18n/useTranslation.js";
import {
  buildCommunityRequest,
  COMMUNITY_CHANNEL_TYPES,
  COMMUNITY_VISIBILITIES,
  containsHtmlAngleBracket,
  createCommunityForm,
  toggleTag,
} from "./communityManagement.js";

function validate(form, creating, t) {
  if (!form.name.trim()) return t("community.validation.nameRequired");
  if (creating && !form.channelName.trim()) {
    return t("community.validation.channelNameRequired");
  }
  if (
    containsHtmlAngleBracket(
      form.name,
      form.description,
      form.channelName,
      form.channelDescription,
    )
  ) {
    return t("community.validation.noAngleBrackets");
  }
  return "";
}

export default function CommunityFormDialog({
  mode = "create",
  community,
  categories = [],
  tags = [],
  onClose,
  onSaved,
  onSubmit,
}) {
  const { t } = useTranslation();
  const creating = mode === "create";
  const [form, setForm] = useState(() => createCommunityForm(community));
  const [status, setStatus] = useState({ saving: false, error: "" });

  function updateField(event) {
    const { checked, name, type, value } = event.target;
    setForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
  }

  function handleTagToggle(slug) {
    setForm((current) => ({ ...current, tagSlugs: toggleTag(current.tagSlugs, slug) }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const validationError = validate(form, creating, t);
    if (validationError) {
      setStatus({ saving: false, error: validationError });
      return;
    }

    setStatus({ saving: true, error: "" });
    try {
      const saved = await onSubmit(buildCommunityRequest(form, { creating }));
      onSaved?.(saved);
      onClose?.();
    } catch (apiError) {
      setStatus({
        saving: false,
        error: getApiErrorMessage(
          apiError,
          t,
          creating ? "errors.communityCreate" : "errors.communityUpdate",
        ),
      });
    }
  }

  return (
    <Modal
      title={t(creating ? "community.createTitle" : "community.editTitle")}
      description={t(
        creating ? "community.createDescription" : "community.editDescription",
      )}
      onClose={status.saving ? undefined : onClose}
    >
      <form className="dialog-form community-form" onSubmit={handleSubmit} noValidate>
        <label>
          {t("community.form.name")}
          <input
            name="name"
            value={form.name}
            onChange={updateField}
            placeholder={t("community.form.namePlaceholder")}
            maxLength={100}
            required
          />
        </label>

        <label>
          {t("community.form.description")}
          <textarea
            name="description"
            rows="4"
            value={form.description}
            onChange={updateField}
            placeholder={t("community.form.descriptionPlaceholder")}
            maxLength={1000}
          />
          <small className="form-counter">{form.description.length}/1000</small>
        </label>

        <div className="community-form__grid">
          <label>
            {t("community.form.category")}
            <select name="categorySlug" value={form.categorySlug} onChange={updateField}>
              <option value="">{t("community.form.noCategory")}</option>
              {categories
                .filter((category) => category.slug !== "all")
                .map((category) => (
                  <option key={category.slug} value={category.slug}>
                    {category.name}
                  </option>
                ))}
            </select>
          </label>

          <label>
            {t("community.form.visibility")}
            <select name="visibility" value={form.visibility} onChange={updateField}>
              {COMMUNITY_VISIBILITIES.map((visibility) => (
                <option key={visibility} value={visibility}>
                  {t(`community.visibility.${visibility}`)}
                </option>
              ))}
            </select>
            <small>{t(`community.visibilityHint.${form.visibility}`)}</small>
          </label>
        </div>

        <fieldset className="community-form__section">
          <legend>{t("community.form.tags")}</legend>
          {tags.length ? (
            <div className="tag-picker">
              {tags.map((tag) => {
                const selected = form.tagSlugs.includes(tag.slug);
                return (
                  <button
                    key={tag.slug}
                    className={classNames(selected && "is-selected")}
                    type="button"
                    aria-pressed={selected}
                    onClick={() => handleTagToggle(tag.slug)}
                  >
                    {tag.name}
                  </button>
                );
              })}
            </div>
          ) : (
            <p className="form-hint">{t("community.form.noTags")}</p>
          )}
        </fieldset>

        {creating ? (
          <fieldset className="community-form__section">
            <legend>{t("community.form.initialChannel")}</legend>
            <p className="form-hint">{t("community.form.initialChannelHint")}</p>
            <div className="community-form__grid">
              <label>
                {t("community.channel.name")}
                <input
                  name="channelName"
                  value={form.channelName}
                  onChange={updateField}
                  placeholder={t("community.channel.namePlaceholder")}
                  maxLength={100}
                  required
                />
              </label>
              <label>
                {t("community.channel.type")}
                <select name="channelType" value={form.channelType} onChange={updateField}>
                  {COMMUNITY_CHANNEL_TYPES.map((type) => (
                    <option key={type} value={type}>
                      {t(`community.channel.type.${type}`)}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <label>
              {t("community.channel.description")}
              <textarea
                name="channelDescription"
                rows="3"
                value={form.channelDescription}
                onChange={updateField}
                placeholder={t("community.channel.descriptionPlaceholder")}
                maxLength={500}
              />
            </label>
            <label className="check-field">
              <input
                name="channelReadOnly"
                type="checkbox"
                checked={form.channelReadOnly}
                onChange={updateField}
              />
              <span>{t("community.channel.readOnly")}</span>
            </label>
          </fieldset>
        ) : null}

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
