import test from "node:test";
import assert from "node:assert/strict";
import { buildProposalDiff, createGeminiPlanner, normalizeProposal } from "../src/aiPlanner.js";

test("normalizes untrusted model output into bounded planning data", () => {
  const proposal = normalizeProposal({ overview: "  roteiro  ", itinerary: [{ dayOffset: -4, time: "99:00", title: "Museu", type: "INVALID", estimatedCostMinor: -2, sourceUrl: "javascript:alert(1)" }], checklist: [{ name: "Passaporte", category: "INVALID" }], budgets: [{ category: "FOOD", percent: 180 }], sources: [{ title: "Prefeitura", url: "https://example.com/x", checkedAt: "hoje" }] });
  assert.equal(proposal.itinerary[0].dayOffset, 0); assert.equal(proposal.itinerary[0].time, "09:00"); assert.equal(proposal.itinerary[0].type, "OTHER"); assert.equal(proposal.itinerary[0].estimatedCostMinor, 0); assert.equal(proposal.itinerary[0].sourceUrl, "");
  assert.equal(proposal.checklist[0].category, "OTHER"); assert.equal(proposal.budgets[0].percent, 100); assert.equal(proposal.sources[0].url, "https://example.com/x");
});

test("keeps the Gemini key in the server request header", async () => {
  let captured;
  const planner = createGeminiPlanner({ apiKey: "server-only-secret", fetchImpl: async (url, options) => { captured = { url, options }; return { ok: true, json: async () => ({ candidates: [{ content: { parts: [{ text: JSON.stringify({ overview: "Plano", itinerary: [], checklist: [], budgets: [], sources: [] }) }] } }] }) }; } });
  const result = await planner({ id: "trip-1", name: "Recife", planningProfile: { origin: "São Paulo", pace: "LIGHT", dietaryRestrictions: "sem lactose" } });
  assert.equal(result.overview, "Plano"); assert.equal(captured.options.headers["x-goog-api-key"], "server-only-secret"); assert.equal(captured.options.body.includes("server-only-secret"), false); assert.match(captured.url, /generateContent$/);
  assert.equal(captured.options.body.includes("sem lactose"), true);
});

test("diff marks existing itinerary and checklist items as non-selectable duplicates", () => {
  const proposal = normalizeProposal({ overview: "Plano", itinerary: [{ title: "Museu", location: "Centro", type: "ACTIVITY" }, { title: "Praia", location: "Orla", type: "ACTIVITY" }], checklist: [{ name: "Passaporte", category: "DOCUMENTS" }], budgets: [{ category: "FOOD", percent: 20 }], sources: [] });
  const diff = buildProposalDiff(proposal, { itinerary: [{ id: "event-1", title: "museu", location: "CENTRO" }], checklist: [{ id: "task-1", name: "passaporte" }] });
  assert.equal(diff.itinerary[0].action, "SKIP_DUPLICATE"); assert.equal(diff.itinerary[0].targetId, "event-1"); assert.equal(diff.itinerary[1].action, "ADD"); assert.equal(diff.checklist[0].action, "SKIP_DUPLICATE"); assert.equal(diff.budgets[0].action, "ADD");
});

test("diff derives move, update and remove operations only for valid existing ids", () => {
  const proposal = normalizeProposal({ overview: "Replanejar", itinerary: [{ operation: "MOVE", targetId: "event-1", dayOffset: 2, time: "14:00", title: "Museu", location: "Centro", type: "ACTIVITY", estimatedCostMinor: 3000 }, { operation: "REMOVE", targetId: "event-2", title: "Passeio cancelado", type: "ACTIVITY" }], checklist: [{ operation: "UPDATE", targetId: "task-1", name: "Passaporte válido", category: "DOCUMENTS" }, { operation: "REMOVE", targetId: "missing", name: "Não existe", category: "OTHER" }], budgets: [], sources: [] });
  const context = { start: "2026-10-10", itinerary: [{ id: "event-1", title: "Museu", location: "Centro", date: "2026-10-10", time: "09:00", cost: 20 }, { id: "event-2", title: "Passeio cancelado", location: "", date: "2026-10-11", time: "09:00", cost: 0 }], checklist: [{ id: "task-1", name: "Passaporte", category: "DOCUMENTS" }] };
  const diff = buildProposalDiff(proposal, context);
  assert.equal(diff.itinerary[0].action, "MOVE"); assert.deepEqual(diff.itinerary[0].changes.map(value => value.field), ["date", "time", "cost"]); assert.equal(diff.itinerary[1].action, "REMOVE"); assert.equal(diff.checklist[0].action, "UPDATE"); assert.equal(diff.checklist[1].action, "ADD");
});

test("diff compares category budget percentages against the current trip", () => {
  const proposal = normalizeProposal({ overview: "Custos", itinerary: [], checklist: [], budgets: [{ category: "FOOD", percent: 25 }, { category: "TRANSPORT", percent: 30 }], sources: [] });
  const diff = buildProposalDiff(proposal, { budget: 1000, categoryBudgets: [{ category: "FOOD", limitMinor: 20000 }] });
  assert.equal(diff.budgets[0].action, "UPDATE"); assert.deepEqual(diff.budgets[0].changes[0], { field: "percent", before: 20, after: 25 }); assert.equal(diff.budgets[1].action, "ADD");
});
