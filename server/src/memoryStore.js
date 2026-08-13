import { randomUUID } from "node:crypto";

export class MemoryStore {
  constructor() {
    this.users = new Map();
    this.trips = new Map();
    this.members = new Map();
    this.entities = new Map();
    this.mutations = new Map();
    this.changes = [];
    this.aiProposals = new Map();
    this.sequence = 0;
  }

  async createUser({ email, name, passwordHash }) {
    if ([...this.users.values()].some((user) => user.email === email)) throw Object.assign(new Error("email_exists"), { code: "email_exists" });
    const user = { id: randomUUID(), email, name, passwordHash, createdAt: new Date().toISOString() };
    this.users.set(user.id, user);
    return user;
  }
  async userByEmail(email) { return [...this.users.values()].find((user) => user.email === email) || null; }
  async userById(id) { return this.users.get(id) || null; }

  async listTrips(userId) {
    const ids = [...this.members.entries()].filter(([, members]) => members.has(userId)).map(([id]) => id);
    return ids.map((id) => this.trips.get(id)).filter((trip) => trip && !trip.deletedAt);
  }
  async tripRole(userId, tripId) { return this.members.get(tripId)?.get(userId) || null; }
  async createTrip(userId, input) {
    const now = new Date().toISOString(), id = input.id || randomUUID();
    if (this.trips.has(id)) throw Object.assign(new Error("trip_exists"), { code: "trip_exists" });
    const trip = { id, ownerId: userId, version: 1, data: input.data || input, createdAt: now, updatedAt: now, deletedAt: null };
    this.trips.set(id, trip); this.members.set(id, new Map([[userId, "ORGANIZER"]])); this.recordChange(userId, id, "trip", id, 1, false, trip.data);
    return trip;
  }
  async updateTrip(userId, tripId, data, baseVersion) {
    const trip = this.trips.get(tripId);
    if (!trip || trip.deletedAt) return null;
    if (baseVersion != null && Number(baseVersion) !== trip.version) throw Object.assign(new Error("version_conflict"), { code: "version_conflict", current: trip });
    trip.data = { ...trip.data, ...data }; trip.version += 1; trip.updatedAt = new Date().toISOString();
    this.recordChange(userId, tripId, "trip", tripId, trip.version, false, trip.data); return trip;
  }

  async push(userId, operations) {
    const results = [];
    for (const operation of operations) {
      const prior = this.mutations.get(`${userId}:${operation.mutationId}`);
      if (prior) { results.push({ ...prior, duplicate: true }); continue; }
      const role = await this.tripRole(userId, operation.tripId);
      if (!role || role === "VIEWER" || role === "GUEST") { results.push({ mutationId: operation.mutationId, status: "forbidden" }); continue; }
      const key = `${operation.tripId}:${operation.entityType}:${operation.entityId}`, current = this.entities.get(key);
      if (current && operation.baseVersion != null && Number(operation.baseVersion) !== current.version) {
        results.push({ mutationId: operation.mutationId, status: "conflict", current }); continue;
      }
      const version = (current?.version || 0) + 1;
      const entity = { tripId: operation.tripId, entityType: operation.entityType, entityId: operation.entityId, version, deleted: Boolean(operation.deleted), payload: operation.deleted ? null : operation.payload, updatedAt: new Date().toISOString(), updatedBy: userId };
      this.entities.set(key, entity); this.recordChange(userId, operation.tripId, operation.entityType, operation.entityId, version, entity.deleted, entity.payload);
      const result = { mutationId: operation.mutationId, status: "applied", version };
      this.mutations.set(`${userId}:${operation.mutationId}`, result); results.push(result);
    }
    return results;
  }
  async pull(userId, cursor = 0, tripId = null) {
    const allowed = new Set([...this.members.entries()].filter(([, members]) => members.has(userId)).map(([id]) => id));
    const changes = this.changes.filter((change) => change.sequence > cursor && allowed.has(change.tripId) && (!tripId || change.tripId === tripId));
    return { cursor: changes.at(-1)?.sequence || Number(cursor), changes };
  }
  async createAiProposal(userId, tripId, proposal) { const now = new Date(), value = { id: randomUUID(), tripId, createdBy: userId, status: "DRAFT", proposal, selectedItemIds: null, createdAt: now.toISOString(), expiresAt: new Date(now.getTime() + 7 * 86400000).toISOString(), appliedAt: null }; this.aiProposals.set(value.id, value); return value; }
  async listAiProposals(userId, tripId) { if (!await this.tripRole(userId, tripId)) return []; return [...this.aiProposals.values()].filter(value => value.tripId === tripId).sort((a, b) => b.createdAt.localeCompare(a.createdAt)); }
  async aiProposal(userId, proposalId) { const value = this.aiProposals.get(proposalId); return value && await this.tripRole(userId, value.tripId) ? value : null; }
  async applyAiProposal(userId, proposalId, selectedItemIds) { const value = this.aiProposals.get(proposalId), role = value && await this.tripRole(userId, value.tripId); if (!value || !["ORGANIZER", "EDITOR"].includes(role)) return null; if (value.status !== "DRAFT" || new Date(value.expiresAt) <= new Date()) throw Object.assign(new Error("proposal_not_applicable"), { code: "proposal_not_applicable" }); value.status = "APPLIED"; value.selectedItemIds = selectedItemIds; value.appliedAt = new Date().toISOString(); return value; }
  async dismissAiProposal(userId, proposalId) { const value = this.aiProposals.get(proposalId), role = value && await this.tripRole(userId, value.tripId); if (!value || !["ORGANIZER", "EDITOR"].includes(role)) return null; if (value.status !== "DRAFT") throw Object.assign(new Error("proposal_not_applicable"), { code: "proposal_not_applicable" }); value.status = "DISMISSED"; return value; }
  recordChange(userId, tripId, entityType, entityId, version, deleted, payload) {
    this.changes.push({ sequence: ++this.sequence, tripId, entityType, entityId, version, deleted, payload, changedBy: userId, changedAt: new Date().toISOString() });
  }
}
