import test from "node:test";
import assert from "node:assert/strict";
import {
  queueTripDeletion,
  queueTripSync,
  synchronizeStore,
} from "../src/syncClient.js";

test("deduplicates queued snapshots while keeping the newest trip", () => {
  const base = { trips: [], syncQueue: [], syncVersions: {} },
    first = queueTripSync(base, { id: "trip-old", name: "A" }),
    second = queueTripSync(first, { id: "trip-old", name: "B" });
  assert.equal(second.syncQueue.length, 1);
  assert.equal(second.syncQueue[0].payload.name, "B");
  assert.notEqual(
    second.syncQueue[0].mutationId,
    first.syncQueue[0].mutationId,
  );
});

test("does not send a tombstone for a trip that never reached the server", () => {
  const queued = queueTripSync(
    { trips: [], syncQueue: [], syncVersions: {} },
    { id: "draft", name: "Draft" },
  );
  assert.equal(queueTripDeletion(queued, "draft").syncQueue.length, 0);
});

test("creates a remote trip, pushes its document and consumes its own pull", async () => {
  const trip = {
      id: "legacy-readable-id",
      name: "Rio",
      updatedAt: "2026-01-01",
    },
    initial = queueTripSync(
      {
        trips: [trip],
        activeTripId: trip.id,
        syncQueue: [],
        syncVersions: {},
        syncCursor: 0,
      },
      trip,
    ),
    calls = [];
  const api = {
    trips: async () => ({ trips: [] }),
    createTrip: async (value) => {
      calls.push(["create", value.id]);
      return { trip: value };
    },
    push: async (operations) => {
      calls.push(["push", operations[0].tripId]);
      return {
        results: [
          {
            mutationId: operations[0].mutationId,
            status: "applied",
            version: 1,
          },
        ],
      };
    },
    pull: async () => ({
      cursor: 4,
      changes: [
        {
          sequence: 4,
          tripId: trip.id,
          entityType: "trip_document",
          entityId: trip.id,
          version: 1,
          deleted: false,
          payload: trip,
        },
      ],
    }),
  };
  const result = await synchronizeStore(initial, api);
  assert.deepEqual(calls, [
    ["create", trip.id],
    ["push", trip.id],
  ]);
  assert.equal(result.store.syncQueue.length, 0);
  assert.equal(result.store.syncVersions[trip.id], 1);
  assert.equal(result.store.syncCursor, 4);
});

test("keeps a conflicting local operation for explicit user resolution", async () => {
  const trip = { id: "trip", name: "Local" },
    initial = queueTripSync(
      {
        trips: [trip],
        activeTripId: "trip",
        syncQueue: [],
        syncVersions: { trip: 1 },
        syncCursor: 0,
      },
      trip,
    ),
    mutationId = initial.syncQueue[0].mutationId;
  const api = {
    trips: async () => ({ trips: [{ id: "trip", data: trip }] }),
    push: async () => ({
      results: [
        {
          mutationId,
          status: "conflict",
          current: { version: 2, payload: { id: "trip", name: "Remote" } },
        },
      ],
    }),
    pull: async () => ({ cursor: 2, changes: [] }),
  };
  const result = await synchronizeStore(initial, api);
  assert.equal(result.conflicts.length, 1);
  assert.equal(result.store.syncQueue.length, 1);
  assert.equal(result.store.trips[0].name, "Local");
});
