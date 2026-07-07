export const USERNAME_PATTERN = /^[A-Za-z0-9._-]+$/;
export const NO_HTML_ANGLE_BRACKETS_PATTERN = /^[^<>]*$/;
export const OPTIONAL_HTTP_URL_PATTERN = /^(?:$|https?:\/\/\S+)$/;

export function hasNoHtmlAngleBrackets(value = "") {
  return NO_HTML_ANGLE_BRACKETS_PATTERN.test(value);
}

export function isOptionalHttpUrl(value = "") {
  return OPTIONAL_HTTP_URL_PATTERN.test(value);
}

export function isValidUsername(value = "") {
  return USERNAME_PATTERN.test(value);
}
