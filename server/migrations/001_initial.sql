CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS schema_migrations (
  version TEXT PRIMARY KEY,
  applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT NOT NULL UNIQUE,
  name TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS trips (
  id TEXT PRIMARY KEY,
  owner_id UUID NOT NULL REFERENCES users(id),
  version BIGINT NOT NULL DEFAULT 1,
  data JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS trip_members (
  trip_id TEXT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role TEXT NOT NULL CHECK (role IN ('ORGANIZER','EDITOR','VIEWER','GUEST')),
  PRIMARY KEY (trip_id, user_id)
);

CREATE TABLE IF NOT EXISTS sync_entities (
  trip_id TEXT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  version BIGINT NOT NULL,
  deleted BOOLEAN NOT NULL DEFAULT false,
  payload JSONB,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_by UUID NOT NULL REFERENCES users(id),
  PRIMARY KEY (trip_id, entity_type, entity_id)
);

CREATE TABLE IF NOT EXISTS sync_mutations (
  user_id UUID NOT NULL REFERENCES users(id),
  mutation_id UUID NOT NULL,
  result JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, mutation_id)
);

CREATE TABLE IF NOT EXISTS sync_changes (
  sequence BIGSERIAL PRIMARY KEY,
  trip_id TEXT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  version BIGINT NOT NULL,
  deleted BOOLEAN NOT NULL,
  payload JSONB,
  changed_by UUID NOT NULL REFERENCES users(id),
  changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS sync_changes_trip_sequence ON sync_changes(trip_id, sequence);
CREATE INDEX IF NOT EXISTS trip_members_user ON trip_members(user_id);
