CREATE TABLE IF NOT EXISTS ai_proposals (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  trip_id TEXT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
  created_by UUID NOT NULL REFERENCES users(id),
  status TEXT NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','APPLIED','DISMISSED')),
  proposal JSONB NOT NULL,
  selected_item_ids JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL DEFAULT (now() + interval '7 days'),
  applied_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS ai_proposals_trip_created ON ai_proposals(trip_id, created_at DESC);
