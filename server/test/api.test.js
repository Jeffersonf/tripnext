import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { createApp } from "../src/app.js";
import { MemoryStore } from "../src/memoryStore.js";

const secret = "test-secret-with-more-than-thirty-two-characters";
let server, baseUrl, store;

test.before(async () => {
  store = new MemoryStore();
  server = createApp({ store, authSecret: secret }).listen(0);
  await new Promise((resolve) => server.once("listening", resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});
test.after(() => new Promise((resolve) => server.close(resolve)));

async function request(path, { token, method = "GET", body } = {}) {
  const response = await fetch(`${baseUrl}${path}`, { method, headers: { ...(body ? { "content-type": "application/json" } : {}), ...(token ? { authorization: `Bearer ${token}` } : {}) }, body: body ? JSON.stringify(body) : undefined });
  return { status: response.status, body: await response.json() };
}
async function register(email) { return request("/api/auth/register", { method: "POST", body: { name: email.split("@")[0], email, password: "very-secure-password" } }); }
async function createTrip(token) { const id = randomUUID(); const result = await request("/api/trips", { token, method: "POST", body: { id, data: { name: "Rio", destination: "Rio de Janeiro" } } }); return { id, result }; }

test("registers, authenticates and returns the current user", async () => {
  const registered = await register("ana@example.com");
  assert.equal(registered.status, 201);
  const me = await request("/api/me", { token: registered.body.token });
  assert.equal(me.status, 200); assert.equal(me.body.user.email, "ana@example.com"); assert.equal(me.body.user.passwordHash, undefined);
});

test("isolates trips between users", async () => {
  const ana = await register("owner@example.com"), bia = await register("other@example.com");
  await createTrip(ana.body.token);
  assert.equal((await request("/api/trips", { token: bia.body.token })).body.trips.length, 0);
});

test("deduplicates an offline mutation by mutationId", async () => {
  const user = await register("sync@example.com"), { id: tripId } = await createTrip(user.body.token), operation = { mutationId: randomUUID(), tripId, entityType: "itinerary", entityId: randomUUID(), baseVersion: 0, payload: { title: "Cristo Redentor" } };
  const first = await request("/api/sync/push", { token: user.body.token, method: "POST", body: { operations: [operation] } });
  const retry = await request("/api/sync/push", { token: user.body.token, method: "POST", body: { operations: [operation] } });
  assert.equal(first.body.results[0].status, "applied"); assert.equal(retry.body.results[0].duplicate, true); assert.equal(retry.body.results[0].version, 1);
});

test("returns the server version instead of overwriting a conflict", async () => {
  const user = await register("conflict@example.com"), { id: tripId } = await createTrip(user.body.token), entityId = randomUUID();
  const push = (baseVersion, title) => request("/api/sync/push", { token: user.body.token, method: "POST", body: { operations: [{ mutationId: randomUUID(), tripId, entityType: "idea", entityId, baseVersion, payload: { title } }] } });
  await push(0, "Praia"); const conflict = await push(0, "Museu");
  assert.equal(conflict.body.results[0].status, "conflict"); assert.equal(conflict.body.results[0].current.payload.title, "Praia");
});

test("pull returns ordered changes and preserves tombstones", async () => {
  const user = await register("pull@example.com"), { id: tripId } = await createTrip(user.body.token), entityId = randomUUID();
  const operations = [
    { mutationId: randomUUID(), tripId, entityType: "checklist", entityId, baseVersion: 0, payload: { name: "Passaporte" } },
    { mutationId: randomUUID(), tripId, entityType: "checklist", entityId, baseVersion: 1, deleted: true },
  ];
  await request("/api/sync/push", { token: user.body.token, method: "POST", body: { operations } });
  const pulled = await request(`/api/sync/pull?cursor=0&tripId=${tripId}`, { token: user.body.token });
  assert.deepEqual(pulled.body.changes.slice(-2).map((change) => change.deleted), [false, true]); assert.ok(pulled.body.cursor > 0);
});

test("AI planning requires authentication, trip access and returns a proposal", async () => {
  const aiCalls = [];
  const aiServer = createApp({ store, authSecret: secret, aiPlanner: async context => { aiCalls.push(context); return { overview: "Plano revisável", itinerary: [{ dayOffset: 0, time: "09:00", title: "Museu", location: "Centro", type: "ACTIVITY", estimatedCostMinor: 2000, sourceUrl: "", reason: "Perto" }], checklist: [], budgets: [], sources: [], generatedAt: "2026-08-13T00:00:00.000Z" }; } }).listen(0);
  await new Promise(resolve => aiServer.once("listening", resolve));
  const aiUrl = `http://127.0.0.1:${aiServer.address().port}`;
  const user = await register("planner@example.com"), { id: tripId } = await createTrip(user.body.token);
  const call = async (token, body) => { const response = await fetch(`${aiUrl}/api/ai/plan`, { method: "POST", headers: { "content-type": "application/json", ...(token ? { authorization: `Bearer ${token}` } : {}) }, body: JSON.stringify(body) }); return { status: response.status, body: await response.json() }; };
  assert.equal((await call(null, { tripId, context: { id: tripId } })).status, 401);
  assert.equal((await call(user.body.token, { tripId: "missing", context: { id: "missing" } })).status, 404);
  const result = await call(user.body.token, { tripId, context: { id: tripId, name: "Rio" } });
  assert.equal(result.status, 201); assert.equal(result.body.record.proposal.overview, "Plano revisável"); assert.equal(aiCalls.length, 1);
  const proposalId = result.body.record.id, itemId = result.body.record.proposal.itinerary[0].id;
  const listed = await fetch(`${aiUrl}/api/trips/${tripId}/ai/proposals`, { headers: { authorization: `Bearer ${user.body.token}` } }).then(response => response.json());
  assert.equal(listed.proposals[0].id, proposalId);
  const invalidSelection = await fetch(`${aiUrl}/api/ai/proposals/${proposalId}/apply`, { method: "POST", headers: { "content-type": "application/json", authorization: `Bearer ${user.body.token}` }, body: JSON.stringify({ selectedItemIds: [randomUUID()] }) });
  assert.equal(invalidSelection.status, 409);
  const appliedResponse = await fetch(`${aiUrl}/api/ai/proposals/${proposalId}/apply`, { method: "POST", headers: { "content-type": "application/json", authorization: `Bearer ${user.body.token}` }, body: JSON.stringify({ selectedItemIds: [itemId] }) });
  const applied = await appliedResponse.json(); assert.equal(appliedResponse.status, 200); assert.deepEqual(applied.record.selectedItemIds, [itemId]); assert.equal(applied.record.status, "APPLIED");
  const repeated = await fetch(`${aiUrl}/api/ai/proposals/${proposalId}/apply`, { method: "POST", headers: { "content-type": "application/json", authorization: `Bearer ${user.body.token}` }, body: JSON.stringify({ selectedItemIds: [itemId] }) }).then(response => response.json());
  assert.equal(repeated.duplicate, true);
  await new Promise(resolve => aiServer.close(resolve));
});
