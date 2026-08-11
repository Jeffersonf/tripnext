import pg from "pg";
import { createApp } from "./app.js";
import { migrate } from "./migrate.js";
import { PostgresStore } from "./postgresStore.js";

const port = Number(process.env.PORT || 8787), databaseUrl = process.env.DATABASE_URL, authSecret = process.env.AUTH_SECRET;
if (!databaseUrl) throw new Error("DATABASE_URL is required");
const pool = new pg.Pool({ connectionString: databaseUrl, ssl: process.env.NODE_ENV === "production" ? { rejectUnauthorized: false } : undefined });
await migrate(pool);
const server = createApp({ store: new PostgresStore(pool), authSecret, tokenTtlSeconds: Number(process.env.TOKEN_TTL_SECONDS || 604800), allowedOrigins: String(process.env.APP_ORIGINS || "http://localhost:5173").split(",").map(value => value.trim()).filter(Boolean) }).listen(port, () => console.log(`TripNext API listening on :${port}`));
const close = () => server.close(async () => { await pool.end(); process.exit(0); });
process.on("SIGTERM", close); process.on("SIGINT", close);
