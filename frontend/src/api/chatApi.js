import httpClient, { getApiErrorMessage, unwrap } from "./httpClient.js";

export { getApiErrorMessage };


export async function getMyProfile() {
  return unwrap(await httpClient.get("/api/v1/users/me"));
}

export async function updateMyProfile(request) {
  return unwrap(await httpClient.patch("/api/v1/users/me", request));
}

export async function searchUsers({ query, limit = 10 }) {
  return unwrap(
    await httpClient.get("/api/v1/users/search", {
      params: { q: query, limit },
    }),
  );
}

export async function getConversations({ limit = 20, cursor, snapshotAt } = {}) {
  return unwrap(
    await httpClient.get("/api/v1/conversations", {
      params: { limit, cursor, snapshotAt },
    }),
  );
}

export async function getConversation(conversationId) {
  return unwrap(await httpClient.get(`/api/v1/conversations/${conversationId}`));
}

export async function createDirectConversation(targetUserId) {
  return unwrap(await httpClient.post("/api/v1/conversations/direct", { targetUserId }));
}

export async function createGroupConversation(request) {
  return unwrap(await httpClient.post("/api/v1/conversations/group", request));
}

export async function inviteGroupMembers(conversationId, memberIds) {
  return unwrap(
    await httpClient.post(`/api/v1/conversations/${conversationId}/members`, { memberIds }),
  );
}

export async function acceptGroupInvitation(conversationId) {
  return unwrap(await httpClient.post(`/api/v1/conversations/${conversationId}/invitations/accept`));
}

export async function rejectGroupInvitation(conversationId) {
  return unwrap(await httpClient.post(`/api/v1/conversations/${conversationId}/invitations/reject`));
}

export async function removeGroupMember(conversationId, memberId) {
  return unwrap(await httpClient.delete(`/api/v1/conversations/${conversationId}/members/${memberId}`));
}

export async function leaveGroup(conversationId) {
  return unwrap(await httpClient.post(`/api/v1/conversations/${conversationId}/leave`));
}

export async function updateGroupProfile(conversationId, request) {
  return unwrap(await httpClient.patch(`/api/v1/conversations/${conversationId}/group-profile`, request));
}

export async function updateGroupMemberRole(conversationId, memberId, role) {
  return unwrap(
    await httpClient.patch(`/api/v1/conversations/${conversationId}/members/${memberId}/role`, { role }),
  );
}

export async function getMessageHistory({ conversationId, limit = 50, cursor }) {
  return unwrap(
    await httpClient.get("/api/v1/messages", {
      params: { conversationId, limit, cursor },
    }),
  );
}

export async function sendMessage(request) {
  return unwrap(await httpClient.post("/api/v1/messages", request));
}


export async function getPinnedMessages(conversationId) {
  return unwrap(await httpClient.get(`/api/v1/conversations/${conversationId}/pins`));
}

export async function pinMessage(messageId) {
  return unwrap(await httpClient.post(`/api/v1/messages/${messageId}/pin`));
}

export async function unpinMessage(messageId) {
  return unwrap(await httpClient.delete(`/api/v1/messages/${messageId}/pin`));
}

export async function addMessageReaction(messageId, emoji) {
  return unwrap(await httpClient.post(`/api/v1/messages/${messageId}/reactions`, { emoji }));
}

export async function removeMessageReaction(messageId, emoji) {
  return unwrap(await httpClient.delete(`/api/v1/messages/${messageId}/reactions/${encodeURIComponent(emoji)}`));
}

export async function getMessageReactions(messageId) {
  return unwrap(await httpClient.get(`/api/v1/messages/${messageId}/reactions`));
}

export async function editMessage(messageId, request) {
  return unwrap(await httpClient.patch(`/api/v1/messages/${messageId}`, request));
}

export async function deleteMessage(messageId) {
  return unwrap(await httpClient.delete(`/api/v1/messages/${messageId}`));
}

export async function getMessageReadReceipts(messageId) {
  return unwrap(await httpClient.get(`/api/v1/messages/${messageId}/reads`));
}

export async function createMultipartUpload(request, config) {
  return unwrap(await httpClient.post("/api/v1/uploads/multipart", request, config));
}

export async function presignMultipartUploadPart({ sessionId, partNumber }, config) {
  return unwrap(
    await httpClient.post(`/api/v1/uploads/multipart/${sessionId}/parts/${partNumber}/presign`, null, config),
  );
}

export async function completeMultipartUploadPart({ sessionId, partNumber, etag, sizeBytes }, config) {
  return unwrap(
    await httpClient.post(`/api/v1/uploads/multipart/${sessionId}/parts/${partNumber}/complete`, {
      etag,
      sizeBytes,
    }, config),
  );
}

export async function completeMultipartUpload(sessionId, config) {
  return unwrap(await httpClient.post(`/api/v1/uploads/multipart/${sessionId}/complete`, null, config));
}

export async function abortMultipartUpload(sessionId) {
  return unwrap(await httpClient.post(`/api/v1/uploads/multipart/${sessionId}/abort`));
}

export async function resumeMultipartUpload(sessionId, config) {
  return unwrap(await httpClient.get(`/api/v1/uploads/multipart/${sessionId}/resume`, config));
}

const RESUMABLE_UPLOADS_STORAGE_KEY = "chatapp:resumable-uploads";
function isUploadExpired(upload) {
  if (!upload?.expiresAt) return false;
  const expiresAt = Date.parse(upload.expiresAt);
  return Number.isFinite(expiresAt) && expiresAt <= Date.now();
}


function readResumableUploads() {
  if (typeof sessionStorage === "undefined") return [];

  try {
    const value = JSON.parse(sessionStorage.getItem(RESUMABLE_UPLOADS_STORAGE_KEY) ?? "[]");
    const uploads = Array.isArray(value) ? value : [];
    const activeUploads = uploads.filter((upload) => !isUploadExpired(upload));
    return activeUploads;
  } catch {
    return [];
  }
}

function writeResumableUploads(uploads) {
  if (typeof sessionStorage === "undefined") return;

  try {
    sessionStorage.setItem(RESUMABLE_UPLOADS_STORAGE_KEY, JSON.stringify(uploads));
  } catch {
    // Storage can be unavailable in private browsing; the active upload still works.
  }
}

function getResumableUpload(file, purpose) {
  return readResumableUploads().find(
    (upload) =>
      upload.fileName === file.name &&
      Number(upload.sizeBytes) === Number(file.size) &&
      Number(upload.lastModified) === Number(file.lastModified) &&
      upload.contentType === (file.type || "application/octet-stream") &&
      upload.purpose === purpose,
  );
}

function saveResumableUpload(file, purpose, session) {
  if (!session?.sessionId) return;

  const next = {
    sessionId: session.sessionId,
    fileName: file.name,
    sizeBytes: file.size,
    lastModified: file.lastModified,
    contentType: file.type || "application/octet-stream",
    purpose,
    chunkSizeBytes: session.chunkSizeBytes,
    totalParts: session.totalParts,
    uploadedParts: session.uploadedParts ?? [],
    missingParts: session.missingParts ?? [],
    expiresAt: session.expiresAt ?? null,
  };
  const uploads = readResumableUploads().filter((upload) => upload.sessionId !== next.sessionId);
  writeResumableUploads([...uploads, next]);
}

function clearResumableUpload(sessionId) {
  if (!sessionId) return;
  writeResumableUploads(readResumableUploads().filter((upload) => upload.sessionId !== sessionId));
}
function normalizeUploadHeaders(headers = {}) {
  return Object.fromEntries(
    Object.entries(headers).filter(([, value]) => value != null && value !== ""),
  );
}

async function uploadPresignedPart({
  uploadUrl,
  method = "PUT",
  requiredHeaders,
  chunk,
  signal,
}) {
  const response = await fetch(uploadUrl, {
    method,
    headers: normalizeUploadHeaders(requiredHeaders),
    body: chunk,
    signal,
  });

  if (!response.ok) {
    throw new Error(`Upload failed with ${response.status}`);
  }

  const etag = response.headers.get("ETag") ?? response.headers.get("etag");

  if (!etag?.trim()) {
    const error = new Error("The upload response did not expose an ETag header.");
    error.userMessageKey = "errors.uploadEtagMissing";
    throw error;
  }

  return etag;
}

function uploadedBytesForParts(parts, chunkSize, fileSize) {
  return (parts ?? []).reduce((total, partNumber) => {
    const start = (partNumber - 1) * chunkSize;
    const end = Math.min(start + chunkSize, fileSize);
    return total + Math.max(0, end - start);
  }, 0);
}

async function uploadMultipartMessageAttachment(
  file,
  { purpose = "MESSAGE_ATTACHMENT", signal, onProgress } = {},
) {
  const storedUpload = getResumableUpload(file, purpose);
  let session = null;
  signal?.throwIfAborted?.();

  if (storedUpload?.sessionId) {
    try {
      session = {
        ...storedUpload,
        ...(await resumeMultipartUpload(storedUpload.sessionId, { signal })),
      };
    } catch (error) {
      if (signal?.aborted) throw error;
      clearResumableUpload(storedUpload.sessionId);
    }
  }

  session ??= await createMultipartUpload({
    fileName: file.name,
    contentType: file.type || "application/octet-stream",
    sizeBytes: file.size,
    purpose,
  }, { signal });

  try {
    const chunkSize = session?.chunkSizeBytes ?? file.size;
    const totalParts = session?.totalParts ?? Math.ceil(file.size / chunkSize);
    const missingParts = session?.missingParts?.length
      ? session.missingParts
      : Array.from({ length: totalParts }, (_, index) => index + 1);
    let uploadedBytes = uploadedBytesForParts(session?.uploadedParts, chunkSize, file.size);
    onProgress?.({ uploadedBytes, totalBytes: file.size });



    saveResumableUpload(file, purpose, session);

    for (const partNumber of missingParts) {
      const start = (partNumber - 1) * chunkSize;
      const chunk = file.slice(start, Math.min(start + chunkSize, file.size));
      const presignedPart = await presignMultipartUploadPart({
        sessionId: session.sessionId,
        partNumber,
      }, { signal });
      const etag = await uploadPresignedPart({
        uploadUrl: presignedPart.uploadUrl,
        method: presignedPart.method,
        requiredHeaders: presignedPart.requiredHeaders,
        chunk,
        signal,
      });

      session = {
        ...session,
        ...(await completeMultipartUploadPart({
          sessionId: session.sessionId,
          partNumber,
          etag,
          sizeBytes: chunk.size,
        }, { signal })),
      };
      saveResumableUpload(file, purpose, session);
      uploadedBytes = Math.min(file.size, uploadedBytes + chunk.size);
      onProgress?.({ uploadedBytes, totalBytes: file.size });
    }

    const uploadedAsset = await completeMultipartUpload(session.sessionId, { signal });
    clearResumableUpload(session.sessionId);
    return uploadedAsset;
  } catch (error) {
    try {
      saveResumableUpload(file, purpose, await resumeMultipartUpload(session.sessionId));
    } catch {
      await abortMultipartUpload(session.sessionId).catch(() => {});
      clearResumableUpload(session.sessionId);
    }
    throw error;
  }
}

export async function uploadMessageAttachment(file, options = {}) {
  return uploadMultipartMessageAttachment(file, options);
}

export async function uploadMessageAttachments(files, options) {
  const uploadFiles = Array.from(files);
  const uploaded = [];
  const totalBytes = uploadFiles.reduce((total, file) => total + file.size, 0);
  let completedBytes = 0;

  for (const [fileIndex, file] of uploadFiles.entries()) {
    const asset = await uploadMessageAttachment(file, {
      ...options,
      onProgress: ({ uploadedBytes }) => {
        const loadedBytes = Math.min(totalBytes, completedBytes + uploadedBytes);
        options?.onProgress?.({
          file,
          fileIndex,
          loadedBytes,
          totalBytes,
          progress: totalBytes > 0 ? loadedBytes / totalBytes : 1,
        });
      },
    });

    uploaded.push(asset);
    completedBytes += file.size;
  }

  return uploaded;
}

export async function markConversationRead({ conversationId, lastReadMessageId }) {
  return unwrap(
    await httpClient.post("/api/v1/messages/read", { conversationId, lastReadMessageId }),
  );
}
