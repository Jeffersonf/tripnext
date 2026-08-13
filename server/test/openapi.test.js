import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import YAML from "yaml";

test("OpenAPI is valid YAML and documents every public route", async () => {
  const contract = YAML.parse(await readFile(new URL("../openapi.yaml", import.meta.url), "utf8"));
  assert.equal(contract.openapi, "3.1.0");
  for (const route of ["/health", "/api/auth/register", "/api/auth/login", "/api/me", "/api/trips", "/api/trips/{tripId}", "/api/sync/push", "/api/sync/pull", "/api/ai/plan"]) assert.ok(contract.paths[route], `${route} is missing`);
  assert.equal(contract.components.securitySchemes.bearerAuth.scheme, "bearer");
});

test("allows CORS only for configured origins", async () => {
  const { createApp } = await import("../src/app.js");
  const { MemoryStore } = await import("../src/memoryStore.js");
  const server = createApp({ store: new MemoryStore(), authSecret: "cors-secret-with-more-than-thirty-two-characters", allowedOrigins: ["https://allowed.example"] }).listen(0);
  await new Promise(resolve => server.once("listening", resolve));
  const url = `http://127.0.0.1:${server.address().port}/health`;
  const allowed = await fetch(url, { headers: { origin: "https://allowed.example" } }), denied = await fetch(url, { headers: { origin: "https://denied.example" } });
  assert.equal(allowed.headers.get("access-control-allow-origin"), "https://allowed.example");
  assert.equal(denied.headers.get("access-control-allow-origin"), null);
  await new Promise(resolve => server.close(resolve));
});
