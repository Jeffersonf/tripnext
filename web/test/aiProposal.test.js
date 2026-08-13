import test from "node:test";
import assert from "node:assert/strict";
import { applySelectedProposal, selectableProposalIds } from "../src/aiProposal.js";

test("applies only selected AI additions, moves, removals and budgets", () => {
  const trip = { id: "t", start: "2027-01-10", budget: 1000, itinerary: [{ id: "old", title: "Museu", date: "2027-01-10", time: "09:00", sortOrder: 0 }, { id: "remove", title: "Cancelar" }], checklist: [{ id: "passport", name: "Passaporte", done: true }], categoryBudgets: [] };
  const proposal = { generatedAt: "2026-08-13T00:00:00Z", itinerary: [{ id: "move", targetId: "old", action: "MOVE", title: "Museu", type: "ACTIVITY", dayOffset: 1, time: "14:00", location: "Centro", estimatedCostMinor: 3000 }, { id: "remove-op", targetId: "remove", action: "REMOVE", title: "Cancelar" }, { id: "ignored", action: "ADD", title: "Praia" }], checklist: [{ id: "task", action: "ADD", name: "Protetor", category: "OTHER" }], budgets: [{ id: "food", action: "ADD", category: "FOOD", percent: 25 }] };
  const result = applySelectedProposal(trip, proposal, ["move", "remove-op", "task", "food"]);
  assert.equal(result.itinerary.length, 1); assert.equal(result.itinerary[0].id, "old"); assert.equal(result.itinerary[0].date, "2027-01-11"); assert.equal(result.itinerary[0].cost, 30); assert.equal(result.checklist.length, 2); assert.equal(result.checklist[0].done, true); assert.equal(result.categoryBudgets[0].limitMinor, 25000);
});

test("excludes duplicate and unchanged operations from selection", () => {
  assert.deepEqual(selectableProposalIds({ itinerary: [{ id: "a", action: "ADD" }, { id: "b", action: "SKIP_DUPLICATE" }], checklist: [{ id: "c", action: "SKIP_UNCHANGED" }], budgets: [{ id: "d", action: "UPDATE" }] }), ["a", "d"]);
});
