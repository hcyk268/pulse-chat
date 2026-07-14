import { apiRequest } from "./apiClient";

const DEFAULT_CHUNK_SIZE_BYTES = 5 * 1024 * 1024;

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
  const session = await createMultipartUpload({
    fileName: file.name || "attachment",
    contentType: file.type || "application/octet-stream",
    sizeBytes: file.size,
    purpose,  });
  const chunkSize = session.chunkSizeBytes || DEFAULT_CHUNK_SIZE_BYTES;
  const totalParts = session.totalParts || Math.ceil(file.size / chunkSize) || 1;
  let uploadedBytes = 0;

  try {
    for (let partNumber = 1; partNumber <= totalParts; partNumber += 1) {
      const start = (partNumber - 1) * chunkSize;
      const end = Math.min(start + chunkSize, file.size);
      const blob = file.slice(start, end);
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
      uploadedBytes += blob.size;
      onProgress?.(Math.round((uploadedBytes / file.size) * 100));
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