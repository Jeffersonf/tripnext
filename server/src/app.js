import express from "express";
import helmet from "helmet";
import { authMiddleware, hashPassword, signToken, verifyPassword } from "./auth.js";
import { buildProposalDiff } from "./aiPlanner.js";

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const publicUser = (user) => ({ id: user.id, email: user.email, name: user.name, createdAt: user.createdAt });
const presentProposal = (record) => record && ({ ...record, status: record.status === "DRAFT" && new Date(record.expiresAt) <= new Date() ? "EXPIRED" : record.status });
const badRequest = (response, details) => response.status(400).json({ error: "validation_error", details });

export function createApp({ store, authSecret, tokenTtlSeconds = 604800, allowedOrigins = [], aiPlanner = null }) {
  if (!authSecret || authSecret.length < 32) throw new Error("AUTH_SECRET must contain at least 32 characters");
  const app = express();
  app.disable("x-powered-by");
  app.use(helmet());
  app.use((request, response, next) => {
    const origin = request.headers.origin;
    if (origin && allowedOrigins.includes(origin)) {
      response.setHeader("Access-Control-Allow-Origin", origin);
      response.setHeader("Vary", "Origin");
      response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Idempotency-Key");
      response.setHeader("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS");
    }
    if (request.method === "OPTIONS") return allowedOrigins.includes(origin) ? response.sendStatus(204) : response.sendStatus(403);
    next();
  });
  app.use(express.json({ limit: "1mb" }));

  app.get("/health", (_request, response) => response.json({ status: "ok" }));
  app.post("/api/auth/register", async (request, response, next) => {
    try {
      const email = String(request.body?.email || "").trim().toLowerCase(), name = String(request.body?.name || "").trim(), password = String(request.body?.password || "");
      if (!emailPattern.test(email) || name.length < 2 || password.length < 10) return badRequest(response, "email, name and password (10+ characters) are required");
      const user = await store.createUser({ email, name, passwordHash: await hashPassword(password) });
      response.status(201).json({ user: publicUser(user), token: signToken(user, authSecret, tokenTtlSeconds) });
    } catch (error) { if (error.code === "email_exists") return response.status(409).json({ error: "email_exists" }); next(error); }
  });
  app.post("/api/auth/login", async (request, response, next) => {
    try {
      const email = String(request.body?.email || "").trim().toLowerCase(), password = String(request.body?.password || ""), user = await store.userByEmail(email);
      if (!user || !(await verifyPassword(password, user.passwordHash))) return response.status(401).json({ error: "invalid_credentials" });
      response.json({ user: publicUser(user), token: signToken(user, authSecret, tokenTtlSeconds) });
    } catch (error) { next(error); }
  });

  const authenticate = authMiddleware(authSecret);
  app.get("/api/me", authenticate, async (request, response) => { const user = await store.userById(request.auth.sub); response.json({ user: publicUser(user) }); });
  app.get("/api/trips", authenticate, async (request, response) => response.json({ trips: await store.listTrips(request.auth.sub) }));
  app.post("/api/trips", authenticate, async (request, response, next) => {
    try {
      if (request.body?.id && String(request.body.id).length > 200) return badRequest(response, "id is too long");
      const data = request.body?.data;
      if (!data || typeof data !== "object" || !String(data.name || "").trim()) return badRequest(response, "data.name is required");
      response.status(201).json({ trip: await store.createTrip(request.auth.sub, { id: request.body.id, data }) });
    } catch (error) { if (error.code === "trip_exists") return response.status(409).json({ error: "trip_exists" }); next(error); }
  });
  app.patch("/api/trips/:tripId", authenticate, async (request, response, next) => {
    try {
      const role = await store.tripRole(request.auth.sub, request.params.tripId);
      if (!role) return response.status(404).json({ error: "trip_not_found" });
      if (["VIEWER", "GUEST"].includes(role)) return response.status(403).json({ error: "forbidden" });
      const trip = await store.updateTrip(request.auth.sub, request.params.tripId, request.body?.data || {}, request.body?.baseVersion);
      response.json({ trip });
    } catch (error) { if (error.code === "version_conflict") return response.status(409).json({ error: "version_conflict", current: error.current }); next(error); }
  });
  app.post("/api/sync/push", authenticate, async (request, response, next) => {
    try {
      const operations = request.body?.operations;
      if (!Array.isArray(operations) || operations.length > 500) return badRequest(response, "operations must be an array with at most 500 items");
      for (const operation of operations) if (!uuidPattern.test(String(operation.mutationId || "")) || !String(operation.tripId || "") || !operation.entityType || !operation.entityId || (!operation.deleted && (operation.payload == null || typeof operation.payload !== "object"))) return badRequest(response, "each operation requires UUID mutationId, tripId, entityType, entityId and payload or deleted=true");
      response.json({ results: await store.push(request.auth.sub, operations) });
    } catch (error) { next(error); }
  });
  app.get("/api/sync/pull", authenticate, async (request, response, next) => {
    try {
      const cursor = Math.max(0, Number(request.query.cursor) || 0), tripId = request.query.tripId ? String(request.query.tripId) : null;
      if (tripId && tripId.length > 200) return badRequest(response, "tripId is too long");
      response.json(await store.pull(request.auth.sub, cursor, tripId));
    } catch (error) { next(error); }
  });
  app.post("/api/ai/plan", authenticate, async (request, response, next) => {
    try {
      if (!aiPlanner) return response.status(503).json({ error: "ai_unavailable" });
      const tripId = String(request.body?.tripId || ""), context = request.body?.context;
      if (!tripId || tripId.length > 200 || !context || typeof context !== "object" || context.id !== tripId || JSON.stringify(context).length > 100000) return badRequest(response, "tripId and matching context (up to 100 KB) are required");
      if (!await store.tripRole(request.auth.sub, tripId)) return response.status(404).json({ error: "trip_not_found" });
      const proposal = buildProposalDiff(await aiPlanner(context), context);
      response.status(201).json({ record: await store.createAiProposal(request.auth.sub, tripId, proposal) });
    } catch (error) { if (error.code === "ai_provider_error" || error.name === "AbortError") return response.status(502).json({ error: "ai_provider_error" }); next(error); }
  });
  app.get("/api/trips/:tripId/ai/proposals", authenticate, async (request, response) => {
    if (!await store.tripRole(request.auth.sub, request.params.tripId)) return response.status(404).json({ error: "trip_not_found" });
    response.json({ proposals: (await store.listAiProposals(request.auth.sub, request.params.tripId)).map(presentProposal) });
  });
  app.post("/api/ai/proposals/:proposalId/apply", authenticate, async (request, response, next) => {
    try {
      const selectedItemIds = request.body?.selectedItemIds;
      if (!Array.isArray(selectedItemIds) || selectedItemIds.length > 50 || selectedItemIds.some(id => !uuidPattern.test(String(id)))) return badRequest(response, "selectedItemIds must contain at most 50 UUIDs");
      const existing = await store.aiProposal(request.auth.sub, request.params.proposalId);
      if (!existing) return response.status(404).json({ error: "proposal_not_found" });
      const available = new Set([...(existing.proposal.itinerary || []), ...(existing.proposal.checklist || []), ...(existing.proposal.budgets || [])].filter(item => !["SKIP_DUPLICATE", "SKIP_UNCHANGED"].includes(item.action)).map(item => item.id));
      const uniqueIds = [...new Set(selectedItemIds)];
      if (uniqueIds.some(id => !available.has(id))) return response.status(409).json({ error: "invalid_proposal_selection" });
      if (existing.status === "APPLIED") return JSON.stringify(existing.selectedItemIds || []) === JSON.stringify(uniqueIds) ? response.json({ record: existing, duplicate: true }) : response.status(409).json({ error: "proposal_not_applicable" });
      if (existing.status !== "DRAFT" || new Date(existing.expiresAt) <= new Date()) return response.status(409).json({ error: "proposal_not_applicable" });
      const record = await store.applyAiProposal(request.auth.sub, request.params.proposalId, uniqueIds);
      if (!record) return response.status(404).json({ error: "proposal_not_found" });
      response.json({ record });
    } catch (error) { if (error.code === "proposal_not_applicable") return response.status(409).json({ error: "proposal_not_applicable" }); next(error); }
  });
  app.post("/api/ai/proposals/:proposalId/dismiss", authenticate, async (request, response, next) => {
    try { const existing = await store.aiProposal(request.auth.sub, request.params.proposalId); if (!existing) return response.status(404).json({ error: "proposal_not_found" }); if (existing.status !== "DRAFT") return response.status(409).json({ error: "proposal_not_applicable" }); const record = await store.dismissAiProposal(request.auth.sub, request.params.proposalId); if (!record) return response.status(404).json({ error: "proposal_not_found" }); response.json({ record: presentProposal(record) }); }
    catch (error) { if (error.code === "proposal_not_applicable") return response.status(409).json({ error: "proposal_not_applicable" }); next(error); }
  });

  app.use((_request, response) => response.status(404).json({ error: "not_found" }));
  app.use((error, _request, response, _next) => { console.error(error); response.status(500).json({ error: "internal_error" }); });
  return app;
}
