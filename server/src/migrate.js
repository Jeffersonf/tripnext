import { readdir, readFile } from "node:fs/promises";
import { resolve } from "node:path";
import pg from "pg";

export async function migrate(pool, directory = resolve("migrations")) {
  await pool.query("CREATE TABLE IF NOT EXISTS schema_migrations(version TEXT PRIMARY KEY, applied_at TIMESTAMPTZ NOT NULL DEFAULT now())");
  const files = (await readdir(directory)).filter((name) => name.endsWith(".sql")).sort();
  for (const file of files) {
    const version = file.split("_")[0], existing = await pool.query("SELECT 1 FROM schema_migrations WHERE version=$1", [version]);
    if (existing.rowCount) continue;
    const client = await pool.connect();
    try { await client.query("BEGIN"); await client.query(await readFile(resolve(directory, file), "utf8")); await client.query("INSERT INTO schema_migrations(version) VALUES($1)", [version]); await client.query("COMMIT"); }
    catch (error) { await client.query("ROLLBACK"); throw error; } finally { client.release(); }
  }
}

if (process.argv[1]?.endsWith("migrate.js")) {
  if (!process.env.DATABASE_URL) throw new Error("DATABASE_URL is required");
  const pool = new pg.Pool({ connectionString: process.env.DATABASE_URL });
  await migrate(pool); await pool.end();
}
