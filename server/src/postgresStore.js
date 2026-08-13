import { randomUUID } from "node:crypto";

const tripShape = (row) => row && ({ id: row.id, ownerId: row.owner_id, version: Number(row.version), data: row.data, createdAt: row.created_at, updatedAt: row.updated_at, deletedAt: row.deleted_at });

export class PostgresStore {
  constructor(pool) { this.pool = pool; }
  async createUser({ email, name, passwordHash }) {
    try { const { rows } = await this.pool.query("INSERT INTO users(email,name,password_hash) VALUES($1,$2,$3) RETURNING *", [email, name, passwordHash]); return this.userShape(rows[0]); }
    catch (error) { if (error.code === "23505") throw Object.assign(new Error("email_exists"), { code: "email_exists" }); throw error; }
  }
  userShape(row) { return row && { id: row.id, email: row.email, name: row.name, passwordHash: row.password_hash, createdAt: row.created_at }; }
  async userByEmail(email) { const { rows } = await this.pool.query("SELECT * FROM users WHERE email=$1", [email]); return this.userShape(rows[0]); }
  async userById(id) { const { rows } = await this.pool.query("SELECT * FROM users WHERE id=$1", [id]); return this.userShape(rows[0]); }
  async listTrips(userId) { const { rows } = await this.pool.query("SELECT t.* FROM trips t JOIN trip_members m ON m.trip_id=t.id WHERE m.user_id=$1 AND t.deleted_at IS NULL ORDER BY t.updated_at DESC", [userId]); return rows.map(tripShape); }
  async tripRole(userId, tripId) { const { rows } = await this.pool.query("SELECT role FROM trip_members WHERE user_id=$1 AND trip_id=$2", [userId, tripId]); return rows[0]?.role || null; }
  async createTrip(userId, input) {
    const client = await this.pool.connect(), id = input.id || randomUUID(), data = input.data || input;
    try { await client.query("BEGIN"); const { rows } = await client.query("INSERT INTO trips(id,owner_id,data) VALUES($1,$2,$3) RETURNING *", [id, userId, data]); await client.query("INSERT INTO trip_members(trip_id,user_id,role) VALUES($1,$2,'ORGANIZER')", [id, userId]); await this.recordChange(client, userId, id, "trip", id, 1, false, data); await client.query("COMMIT"); return tripShape(rows[0]); }
    catch (error) { await client.query("ROLLBACK"); if (error.code === "23505") throw Object.assign(new Error("trip_exists"), { code: "trip_exists" }); throw error; } finally { client.release(); }
  }
  async updateTrip(userId, tripId, data, baseVersion) {
    const client = await this.pool.connect();
    try { await client.query("BEGIN"); const current = await client.query("SELECT * FROM trips WHERE id=$1 AND deleted_at IS NULL FOR UPDATE", [tripId]); if (!current.rows[0]) { await client.query("ROLLBACK"); return null; } const trip = tripShape(current.rows[0]); if (baseVersion != null && Number(baseVersion) !== trip.version) { await client.query("ROLLBACK"); throw Object.assign(new Error("version_conflict"), { code: "version_conflict", current: trip }); } const updated = await client.query("UPDATE trips SET data=data || $2::jsonb,version=version+1,updated_at=now() WHERE id=$1 RETURNING *", [tripId, data]); const result = tripShape(updated.rows[0]); await this.recordChange(client, userId, tripId, "trip", tripId, result.version, false, result.data); await client.query("COMMIT"); return result; }
    catch (error) { try { await client.query("ROLLBACK"); } catch {} throw error; } finally { client.release(); }
  }
  async push(userId, operations) {
    const client = await this.pool.connect(), results = [];
    try { await client.query("BEGIN"); for (const operation of operations) {
      const previous = await client.query("SELECT result FROM sync_mutations WHERE user_id=$1 AND mutation_id=$2", [userId, operation.mutationId]); if (previous.rows[0]) { results.push({ ...previous.rows[0].result, duplicate: true }); continue; }
      const member = await client.query("SELECT role FROM trip_members WHERE user_id=$1 AND trip_id=$2", [userId, operation.tripId]); if (!member.rows[0] || ["VIEWER", "GUEST"].includes(member.rows[0].role)) { results.push({ mutationId: operation.mutationId, status: "forbidden" }); continue; }
      const currentQuery = await client.query("SELECT * FROM sync_entities WHERE trip_id=$1 AND entity_type=$2 AND entity_id=$3 FOR UPDATE", [operation.tripId, operation.entityType, operation.entityId]); const current = currentQuery.rows[0];
      if (current && operation.baseVersion != null && Number(operation.baseVersion) !== Number(current.version)) { results.push({ mutationId: operation.mutationId, status: "conflict", current: { ...current, version: Number(current.version) } }); continue; }
      const version = Number(current?.version || 0) + 1, deleted = Boolean(operation.deleted), payload = deleted ? null : operation.payload;
      await client.query("INSERT INTO sync_entities(trip_id,entity_type,entity_id,version,deleted,payload,updated_by) VALUES($1,$2,$3,$4,$5,$6,$7) ON CONFLICT(trip_id,entity_type,entity_id) DO UPDATE SET version=EXCLUDED.version,deleted=EXCLUDED.deleted,payload=EXCLUDED.payload,updated_at=now(),updated_by=EXCLUDED.updated_by", [operation.tripId, operation.entityType, operation.entityId, version, deleted, payload, userId]); await this.recordChange(client, userId, operation.tripId, operation.entityType, operation.entityId, version, deleted, payload);
      const result = { mutationId: operation.mutationId, status: "applied", version }; await client.query("INSERT INTO sync_mutations(user_id,mutation_id,result) VALUES($1,$2,$3)", [userId, operation.mutationId, result]); results.push(result);
    } await client.query("COMMIT"); return results; } catch (error) { await client.query("ROLLBACK"); throw error; } finally { client.release(); }
  }
  async pull(userId, cursor = 0, tripId = null) { const { rows } = await this.pool.query("SELECT c.* FROM sync_changes c JOIN trip_members m ON m.trip_id=c.trip_id WHERE m.user_id=$1 AND c.sequence>$2 AND ($3::text IS NULL OR c.trip_id=$3) ORDER BY c.sequence LIMIT 1000", [userId, Number(cursor), tripId]); const changes = rows.map(row => ({ sequence: Number(row.sequence), tripId: row.trip_id, entityType: row.entity_type, entityId: row.entity_id, version: Number(row.version), deleted: row.deleted, payload: row.payload, changedBy: row.changed_by, changedAt: row.changed_at })); return { cursor: changes.at(-1)?.sequence || Number(cursor), changes }; }
  proposalShape(row) { return row && { id: row.id, tripId: row.trip_id, createdBy: row.created_by, status: row.status, proposal: row.proposal, selectedItemIds: row.selected_item_ids, createdAt: row.created_at, expiresAt: row.expires_at, appliedAt: row.applied_at }; }
  async createAiProposal(userId, tripId, proposal) { const { rows } = await this.pool.query("INSERT INTO ai_proposals(trip_id,created_by,proposal) VALUES($1,$2,$3) RETURNING *", [tripId, userId, proposal]); return this.proposalShape(rows[0]); }
  async listAiProposals(userId, tripId) { const { rows } = await this.pool.query("SELECT p.* FROM ai_proposals p JOIN trip_members m ON m.trip_id=p.trip_id WHERE m.user_id=$1 AND p.trip_id=$2 ORDER BY p.created_at DESC LIMIT 20", [userId, tripId]); return rows.map(row => this.proposalShape(row)); }
  async aiProposal(userId, proposalId) { const { rows } = await this.pool.query("SELECT p.* FROM ai_proposals p JOIN trip_members m ON m.trip_id=p.trip_id WHERE m.user_id=$1 AND p.id=$2", [userId, proposalId]); return this.proposalShape(rows[0]); }
  async applyAiProposal(userId, proposalId, selectedItemIds) { const { rows } = await this.pool.query("UPDATE ai_proposals p SET status='APPLIED',selected_item_ids=$3::jsonb,applied_at=now() FROM trip_members m WHERE p.id=$1 AND m.trip_id=p.trip_id AND m.user_id=$2 AND m.role IN ('ORGANIZER','EDITOR') AND p.status='DRAFT' AND p.expires_at>now() RETURNING p.*", [proposalId, userId, JSON.stringify(selectedItemIds)]); return this.proposalShape(rows[0]); }
  async dismissAiProposal(userId, proposalId) { const { rows } = await this.pool.query("UPDATE ai_proposals p SET status='DISMISSED' FROM trip_members m WHERE p.id=$1 AND m.trip_id=p.trip_id AND m.user_id=$2 AND m.role IN ('ORGANIZER','EDITOR') AND p.status='DRAFT' RETURNING p.*", [proposalId, userId]); return this.proposalShape(rows[0]); }
  async recordChange(client, userId, tripId, entityType, entityId, version, deleted, payload) { await client.query("INSERT INTO sync_changes(trip_id,entity_type,entity_id,version,deleted,payload,changed_by) VALUES($1,$2,$3,$4,$5,$6,$7)", [tripId, entityType, entityId, version, deleted, payload, userId]); }
}
