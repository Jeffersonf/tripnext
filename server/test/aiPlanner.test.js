import test from "node:test";
import assert from "node:assert/strict";
import { createGeminiPlanner, normalizeProposal } from "../src/aiPlanner.js";

test("normalizes untrusted model output into bounded planning data", () => {
  const proposal = normalizeProposal({ overview: "  roteiro  ", itinerary: [{ dayOffset: -4, time: "99:00", title: "Museu", type: "INVALID", estimatedCostMinor: -2, sourceUrl: "javascript:alert(1)" }], checklist: [{ name: "Passaporte", category: "INVALID" }], budgets: [{ category: "FOOD", percent: 180 }], sources: [{ title: "Prefeitura", url: "https://example.com/x", checkedAt: "hoje" }] });
  assert.equal(proposal.itinerary[0].dayOffset, 0); assert.equal(proposal.itinerary[0].time, "09:00"); assert.equal(proposal.itinerary[0].type, "OTHER"); assert.equal(proposal.itinerary[0].estimatedCostMinor, 0); assert.equal(proposal.itinerary[0].sourceUrl, "");
  assert.equal(proposal.checklist[0].category, "OTHER"); assert.equal(proposal.budgets[0].percent, 100); assert.equal(proposal.sources[0].url, "https://example.com/x");
});

test("keeps the Gemini key in the server request header", async () => {
  let captured;
  const planner = createGeminiPlanner({ apiKey: "server-only-secret", fetchImpl: async (url, options) => { captured = { url, options }; return { ok: true, json: async () => ({ candidates: [{ content: { parts: [{ text: JSON.stringify({ overview: "Plano", itinerary: [], checklist: [], budgets: [], sources: [] }) }] } }] }) }; } });
  const result = await planner({ id: "trip-1", name: "Recife" });
  assert.equal(result.overview, "Plano"); assert.equal(captured.options.headers["x-goog-api-key"], "server-only-secret"); assert.equal(captured.options.body.includes("server-only-secret"), false); assert.match(captured.url, /generateContent$/);
});
