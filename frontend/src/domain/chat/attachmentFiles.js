export const MAX_ATTACHMENTS_PER_MESSAGE = 10;
export const MAX_ATTACHMENT_SIZE_BYTES = 25 * 1024 * 1024;

export const ALLOWED_ATTACHMENT_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "image/gif",
  "video/mp4",
  "video/webm",
  "audio/mpeg",
  "audio/ogg",
  "application/pdf",
  "text/plain",
  "application/zip",
]);

export const ATTACHMENT_ACCEPT = [...ALLOWED_ATTACHMENT_TYPES].join(",");

export function attachmentFileKey(file) {
  return `${file.name}:${file.size}:${file.lastModified}`;
}

function validateAttachmentFile(file) {
  if (!file || file.size <= 0) return { key: "chat.attachmentEmpty" };
  if (file.name.length > 255) return { key: "chat.attachmentNameTooLong" };
  if (file.size > MAX_ATTACHMENT_SIZE_BYTES) {
    return { key: "chat.attachmentTooLarge", params: { size: "25 MB" } };
  }
  if (!ALLOWED_ATTACHMENT_TYPES.has(file.type)) {
    return { key: "chat.attachmentTypeUnsupported", params: { type: file.type || "unknown" } };
  }
  return null;
}

export function mergeAttachmentFiles(currentFiles, incomingFiles) {
  const files = [];
  const seen = new Set();
  let error = null;

  [...currentFiles, ...incomingFiles].forEach((file) => {
    const validationError = validateAttachmentFile(file);
    if (validationError) {
      error ??= validationError;
      return;
    }

    const key = attachmentFileKey(file);
    if (seen.has(key)) return;
    seen.add(key);
    files.push(file);
  });

  if (files.length > MAX_ATTACHMENTS_PER_MESSAGE) {
    error ??= {
      key: "chat.attachmentCountExceeded",
      params: { count: MAX_ATTACHMENTS_PER_MESSAGE },
    };
  }

  return { files: files.slice(0, MAX_ATTACHMENTS_PER_MESSAGE), error };
}
