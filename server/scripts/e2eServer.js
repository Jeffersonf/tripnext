import { createApp } from "../src/app.js";
import { MemoryStore } from "../src/memoryStore.js";

createApp({
  store: new MemoryStore(),
  authSecret: "playwright-only-secret-with-thirty-two-characters",
  allowedOrigins: ["http://127.0.0.1:4173"],
  aiPlanner: async () => ({ overview: "Um primeiro dia leve e próximo, respeitando o perfil da viagem.", itinerary: [{ operation: "ADD", targetId: "", dayOffset: 0, time: "10:00", title: "Passeio pelo centro histórico", location: "Centro", type: "ACTIVITY", estimatedCostMinor: 5000, sourceUrl: "https://example.com/centro", reason: "Combina com o ritmo leve" }], checklist: [{ operation: "ADD", targetId: "", name: "Separar documento de identificação", category: "DOCUMENTS", reason: "Necessário para embarque" }], budgets: [{ category: "ACTIVITIES", percent: 20, reason: "Reserva para passeios" }], sources: [{ title: "Fonte E2E", url: "https://example.com/centro", checkedAt: "2026-08-13" }], generatedAt: "2026-08-13T00:00:00.000Z" }),
}).listen(8787, "127.0.0.1", () => console.log("TripNext E2E API on :8787"));
