export class ApiError extends Error {
  constructor(status, body) {
    super(body?.error || `http_${status}`);
    this.status = status;
    this.body = body;
  }
}

export function createApiClient(baseUrl, token = "") {
  const base = String(baseUrl || "").replace(/\/$/, "");
  const request = async (path, options = {}) => {
    const response = await fetch(`${base}${path}`, {
      ...options,
      headers: {
        ...(options.body ? { "content-type": "application/json" } : {}),
        ...(token ? { authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
      body: options.body ? JSON.stringify(options.body) : undefined,
    });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new ApiError(response.status, body);
    return body;
  };
  return {
    register: (input) =>
      request("/api/auth/register", { method: "POST", body: input }),
    login: (input) =>
      request("/api/auth/login", { method: "POST", body: input }),
    me: () => request("/api/me"),
    trips: () => request("/api/trips"),
    createTrip: (trip) =>
      request("/api/trips", {
        method: "POST",
        body: { id: trip.id, data: trip },
      }),
    push: (operations) =>
      request("/api/sync/push", { method: "POST", body: { operations } }),
    pull: (cursor = 0) =>
      request(`/api/sync/pull?cursor=${encodeURIComponent(cursor)}`),
    planTrip: (trip) => request("/api/ai/plan", { method: "POST", body: { tripId: trip.id, context: trip } }),
    proposals: (tripId) => request(`/api/trips/${encodeURIComponent(tripId)}/ai/proposals`),
    applyProposal: (proposalId, selectedItemIds) => request(`/api/ai/proposals/${encodeURIComponent(proposalId)}/apply`, { method: "POST", body: { selectedItemIds } }),
  };
}
