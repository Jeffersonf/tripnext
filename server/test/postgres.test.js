import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import pg from "pg";
import { migrate } from "../src/migrate.js";
import { PostgresStore } from "../src/postgresStore.js";

test("PostgreSQL migration is repeatable and sync persists idempotently", { skip: !process.env.TEST_DATABASE_URL }, async () => {
  const pool = new pg.Pool({ connectionString: process.env.TEST_DATABASE_URL });
  try {
    await migrate(pool);
    await migrate(pool);
    const store = new PostgresStore(pool), suffix = randomUUID(), user = await store.createUser({ email: `${suffix}@example.com`, name: "Integration", passwordHash: "not-used-in-store-test" }), trip = await store.createTrip(user.id, { id: randomUUID(), data: { name: "Integration trip" } });
    const operation = { mutationId: randomUUID(), tripId: trip.id, entityType: "idea", entityId: randomUUID(), baseVersion: 0, payload: { title: "Museu" } };
    const first = await store.push(user.id, [operation]), repeated = await store.push(user.id, [operation]), pulled = await store.pull(user.id, 0, trip.id);
    assert.equal(first[0].status, "applied");
    assert.equal(repeated[0].duplicate, true);
    assert.equal(pulled.changes.at(-1).payload.title, "Museu");
    const proposal = await store.createAiProposal(user.id, trip.id, { itinerary: [{ id: randomUUID(), action: "ADD" }], checklist: [], budgets: [] });
    assert.equal((await store.listAiProposals(user.id, trip.id))[0].id, proposal.id);
    const selectedId = proposal.proposal.itinerary[0].id, applied = await store.applyAiProposal(user.id, proposal.id, [selectedId]);
    assert.equal(applied.status, "APPLIED"); assert.deepEqual(applied.selectedItemIds, [selectedId]);
  } finally {
    await pool.end();
  }
});
