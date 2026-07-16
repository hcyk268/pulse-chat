import { apiRequest } from "./apiClient";

const DEFAULT_CHUNK_SIZE_BYTES = 5 * 1024 * 1024;
const MAX_PART_ATTEMPTS = 3;

export const MAX_UPLOAD_SIZE_BYTES = 25 * 1024 * 1024;
export const ALLOWED_UPLOAD_CONTENT_TYPES = new Set([
  "image/jpeg", "image/png", "image/webp", "image/gif",
  "video/mp4", "video/webm", "audio/mpeg", "audio/ogg",
  "application/pdf", "text/plain", "application/zip",
]);

export function validateUploadFile(file, { imagesOnly = false } = {}) {
  if (!file || file.size <= 0) return "Choose a non-empty file.";
  if (file.size > MAX_UPLOAD_SIZE_BYTES) return "Each file must be 25 MB or smaller.";
  if (!ALLOWED_UPLOAD_CONTENT_TYPES.has(file.type)) return "This file type is not supported.";
  if (imagesOnly && !file.type.startsWith("image/")) return "Choose an image file.";
  return "";
}

function getResponseHeader(headers, name) {
  return headers.get(name) || headers.get(name.toLowerCase()) || headers.get(name.toUpperCase());
}

export function createMultipartUpload({
  fileName,
  contentType,
  sizeBytes,
  chunkSizeBytes = DEFAULT_CHUNK_SIZE_BYTES,
  fileChecksum = null,
  purpose = "MESSAGE_ATTACHMENT",
}) {
  return apiRequest("/api/v1/uploads/multipart", {
    method: "POST",
    body: JSON.stringify({
      fileName,
      contentType,
      sizeBytes,
      chunkSizeBytes,
      fileChecksum,
      purpose,
    }),
  });
}

export function presignMultipartUploadPart(sessionId, partNumber) {
  return apiRequest(`/api/v1/uploads/multipart/${sessionId}/parts/${partNumber}/presign`, {
    method: "POST",
  });
}

export function completeMultipartUploadPart(sessionId, partNumber, { etag, sizeBytes }) {
  return apiRequest(`/api/v1/uploads/multipart/${sessionId}/parts/${partNumber}/complete`, {
    method: "POST",
    body: JSON.stringify({
      etag,
      sizeBytes,
    }),
  });
}

export function resumeMultipartUpload(sessionId) {
  return apiRequest(`/api/v1/uploads/multipart/${sessionId}/resume`);
}

export function completeMultipartUpload(sessionId) {
  return apiRequest(`/api/v1/uploads/multipart/${sessionId}/complete`, {
    method: "POST",
  });
}

export function abortMultipartUpload(sessionId) {
  return apiRequest(`/api/v1/uploads/multipart/${sessionId}/abort`, {
    method: "POST",
  });
}

export async function uploadFilePart({ uploadUrl, method = "PUT", requiredHeaders = {}, blob }) {
  const response = await fetch(uploadUrl, {
    method,
    headers: requiredHeaders,
    body: blob,
  });

  if (!response.ok) {
    throw new Error("Could not upload file part.");
  }

  const etag = getResponseHeader(response.headers, "etag");
  if (!etag) {
    throw new Error("Upload part response did not include an ETag.");
  }

  return etag.replace(/^"|"$/g, "");
}

async function uploadFile(file, { onProgress, purpose = "MESSAGE_ATTACHMENT" } = {}) {
  const validationError = validateUploadFile(file, { imagesOnly: purpose === "AVATAR" });
  if (validationError) throw new Error(validationError);

  const session = await createMultipartUpload({
    fileName: file.name || "attachment",
    contentType: file.type || "application/octet-stream",
    sizeBytes: file.size,
    purpose,
  });
  const chunkSize = session.chunkSizeBytes || DEFAULT_CHUNK_SIZE_BYTES;
  const totalParts = session.totalParts || Math.ceil(file.size / chunkSize) || 1;
  const uploadedParts = new Set();

  function getPartSize(partNumber) {
    const start = (partNumber - 1) * chunkSize;
    return Math.min(start + chunkSize, file.size) - start;
  }

  function reportProgress() {
    const uploadedBytes = Array.from(uploadedParts).reduce(
      (total, partNumber) => total + getPartSize(partNumber),
      0,
    );
    onProgress?.(Math.round((uploadedBytes / file.size) * 100));
  }

  try {
    reportProgress();
    for (let partNumber = 1; partNumber <= totalParts; partNumber += 1) {
      const start = (partNumber - 1) * chunkSize;
      const end = Math.min(start + chunkSize, file.size);
      const blob = file.slice(start, end);
      let lastError = null;

      for (let attempt = 1; attempt <= MAX_PART_ATTEMPTS; attempt += 1) {
        try {
          const presigned = await presignMultipartUploadPart(session.sessionId, partNumber);
          const etag = await uploadFilePart({
            uploadUrl: presigned.uploadUrl,
            method: presigned.method || "PUT",
            requiredHeaders: presigned.requiredHeaders || {},
            blob,
          });
          await completeMultipartUploadPart(session.sessionId, partNumber, {
            etag,
            sizeBytes: blob.size,
          });
          uploadedParts.add(partNumber);
          reportProgress();
          lastError = null;
          break;
        } catch (error) {
          lastError = error;
          const resumed = await resumeMultipartUpload(session.sessionId).catch(() => null);
          (resumed?.uploadedParts ?? []).forEach((uploadedPart) => uploadedParts.add(uploadedPart));
          reportProgress();
          if (uploadedParts.has(partNumber)) {
            lastError = null;
            break;
          }
        }
      }

      if (lastError) throw lastError;
    }

    const asset = await completeMultipartUpload(session.sessionId);
    return {
      assetId: asset.id,
      objectKey: asset.objectKey,
      url: asset.publicUrl,
      fileName: asset.fileName,
      contentType: asset.contentType,
      sizeBytes: asset.sizeBytes,
    };
  } catch (error) {
    abortMultipartUpload(session.sessionId).catch(() => {});
    throw error;
  }
}

export function uploadMessageAttachment(file, options = {}) {
  return uploadFile(file, options);
}

export function uploadAvatar(file, options = {}) {
  return uploadFile(file, { ...options, purpose: "AVATAR" });
}
