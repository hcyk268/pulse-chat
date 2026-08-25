import assert from "node:assert/strict";
import test from "node:test";
import { isValidEmail } from "../src/utils/validators.js";

test("email validation accepts normal addresses and trims whitespace", () => {
  assert.equal(isValidEmail("trader@example.com"), true);
  assert.equal(isValidEmail("  trader+alerts@example.co.uk  "), true);
});

test("email validation rejects incomplete and whitespace-containing addresses", () => {
  assert.equal(isValidEmail("trader"), false);
  assert.equal(isValidEmail("trader@"), false);
  assert.equal(isValidEmail("trader @example.com"), false);
});
