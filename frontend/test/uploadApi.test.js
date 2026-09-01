import assert from "node:assert/strict";
import test from "node:test";
import httpClient from "../src/api/httpClient.js";
import { uploadMessageAttachment } from "../src/api/chatApi.js";

function apiResponse(config, data) {
  return {
    config,
    data: { data },
    headers: {},
    status: 200,
    statusText: "OK",
  };
}

test("multipart upload fails before complete-part when storage does not expose ETag", async () => {
  const originalAdapter = httpClient.defaults.adapter;
  const originalFetch = globalThis.fetch;
  let completedPart = false;

  httpClient.defaults.adapter = async (config) => {
    if (config.url === "/api/v1/uploads/multipart") {
      return apiResponse(config, {
        sessionId: 42,
        chunkSizeBytes: 3,
        totalParts: 1,
      });
    }
    if (config.url === "/api/v1/uploads/multipart/42/parts/1/presign") {
      return apiResponse(config, {
        uploadUrl: "https://storage.example.test/part",
        method: "PUT",
        requiredHeaders: {},
      });
    }
    if (config.url === "/api/v1/uploads/multipart/42/resume") {
      return apiResponse(config, {
        sessionId: 42,
        chunkSizeBytes: 3,
        totalParts: 1,
        uploadedParts: [],
        missingParts: [1],
      });
    }
    if (config.url === "/api/v1/uploads/multipart/42/parts/1/complete") {
      completedPart = true;
    }
    throw new Error(`Unexpected request: ${config.method} ${config.url}`);
  };
  globalThis.fetch = async () => ({
    headers: { get: () => null },
    ok: true,
    status: 200,
  });

  const file = new Blob(["abc"], { type: "text/plain" });
  Object.defineProperties(file, {
    lastModified: { value: 123 },
    name: { value: "notes.txt" },
  });

  try {
    await assert.rejects(
      uploadMessageAttachment(file),
      (error) => error.userMessageKey === "errors.uploadEtagMissing",
    );
    assert.equal(completedPart, false);
  } finally {
    httpClient.defaults.adapter = originalAdapter;
    globalThis.fetch = originalFetch;
  }
});
