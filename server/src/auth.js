import { createHmac, randomBytes, scrypt as scryptCallback, timingSafeEqual } from "node:crypto";
import { promisify } from "node:util";

const scrypt = promisify(scryptCallback);
const encode = (value) => Buffer.from(value).toString("base64url");
const decodeJson = (value) => JSON.parse(Buffer.from(value, "base64url").toString("utf8"));

export async function hashPassword(password) {
  const salt = randomBytes(16).toString("hex");
  const derived = await scrypt(password, salt, 64);
  return `scrypt:${salt}:${Buffer.from(derived).toString("hex")}`;
}

export async function verifyPassword(password, stored) {
  const [algorithm, salt, expectedHex] = String(stored).split(":");
  if (algorithm !== "scrypt" || !salt || !expectedHex) return false;
  const actual = Buffer.from(await scrypt(password, salt, 64));
  const expected = Buffer.from(expectedHex, "hex");
  return actual.length === expected.length && timingSafeEqual(actual, expected);
}

export function signToken(user, secret, ttlSeconds = 604800) {
  const header = encode(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const payload = encode(JSON.stringify({ sub: user.id, email: user.email, exp: Math.floor(Date.now() / 1000) + ttlSeconds }));
  const signature = createHmac("sha256", secret).update(`${header}.${payload}`).digest("base64url");
  return `${header}.${payload}.${signature}`;
}

export function verifyToken(token, secret) {
  const [header, payload, signature] = String(token || "").split(".");
  if (!header || !payload || !signature) throw new Error("invalid_token");
  const expected = createHmac("sha256", secret).update(`${header}.${payload}`).digest();
  const actual = Buffer.from(signature, "base64url");
  if (actual.length !== expected.length || !timingSafeEqual(actual, expected)) throw new Error("invalid_token");
  const claims = decodeJson(payload);
  if (!claims.sub || claims.exp <= Math.floor(Date.now() / 1000)) throw new Error("expired_token");
  return claims;
}

export function authMiddleware(secret) {
  return (request, response, next) => {
    try {
      const [scheme, token] = String(request.headers.authorization || "").split(" ");
      if (scheme !== "Bearer") throw new Error("missing_token");
      request.auth = verifyToken(token, secret);
      next();
    } catch {
      response.status(401).json({ error: "unauthorized" });
    }
  };
}
