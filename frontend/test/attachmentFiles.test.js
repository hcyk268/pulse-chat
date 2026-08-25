import assert from "node:assert/strict";
import test from "node:test";
import {
  MAX_ATTACHMENT_SIZE_BYTES,
  MAX_ATTACHMENTS_PER_MESSAGE,
  mergeAttachmentFiles,
} from "../src/domain/chat/attachmentFiles.js";

function makeFile({
  name = "photo.png",
  size = 128,
  type = "image/png",
  lastModified = 1,
} = {}) {
  return { name, size, type, lastModified };
}

test("accepts backend-supported attachment types and removes duplicates", () => {
  const image = makeFile();
  const document = makeFile({
    name: "notes.txt",
    type: "text/plain",
    lastModified: 2,
  });

  const result = mergeAttachmentFiles([image], [image, document]);

  assert.deepEqual(result.files, [image, document]);
  assert.equal(result.error, null);
});

test("rejects empty, oversized, and unsupported attachments", () => {
  assert.equal(
    mergeAttachmentFiles([], [makeFile({ size: 0 })]).error?.key,
    "chat.attachmentEmpty",
  );
  assert.equal(
    mergeAttachmentFiles(
      [],
      [makeFile({ size: MAX_ATTACHMENT_SIZE_BYTES + 1 })],
    ).error?.key,
    "chat.attachmentTooLarge",
  );
  assert.equal(
    mergeAttachmentFiles(
      [],
      [makeFile({ type: "application/x-msdownload" })],
    ).error?.key,
    "chat.attachmentTypeUnsupported",
  );
});

test("caps a message at the backend attachment limit", () => {
  const files = Array.from(
    { length: MAX_ATTACHMENTS_PER_MESSAGE + 1 },
    (_, index) =>
      makeFile({
        name: `photo-${index}.png`,
        lastModified: index,
      }),
  );

  const result = mergeAttachmentFiles([], files);

  assert.equal(result.files.length, MAX_ATTACHMENTS_PER_MESSAGE);
  assert.equal(result.error?.key, "chat.attachmentCountExceeded");
});
