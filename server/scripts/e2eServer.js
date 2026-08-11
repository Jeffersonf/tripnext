import { createApp } from "../src/app.js";
import { MemoryStore } from "../src/memoryStore.js";

createApp({
  store: new MemoryStore(),
  authSecret: "playwright-only-secret-with-thirty-two-characters",
  allowedOrigins: ["http://127.0.0.1:4173"],
}).listen(8787, "127.0.0.1", () => console.log("TripNext E2E API on :8787"));
