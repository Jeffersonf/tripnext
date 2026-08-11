const uuid = () =>
  globalThis.crypto?.randomUUID?.() ||
  `mutation-${Date.now()}-${Math.random().toString(16).slice(2)}`;

export function queueTripSync(store, trip) {
  if (!trip?.id) return store;
  const current = store.syncQueue || [],
    prior = current.find((item) => item.tripId === trip.id);
  const operation = {
    mutationId: uuid(),
    tripId: trip.id,
    entityType: "trip_document",
    entityId: trip.id,
    baseVersion: Number(store.syncVersions?.[trip.id] || 0),
    payload: trip,
    queuedAt: new Date().toISOString(),
  };
  return {
    ...store,
    syncQueue: [
      ...current.filter((item) => item.tripId !== trip.id),
      {
        ...operation,
        mutationId:
          prior?.mutationId &&
          JSON.stringify(prior.payload) === JSON.stringify(trip)
            ? prior.mutationId
            : operation.mutationId,
      },
    ],
  };
}

export function queueTripDeletion(store, tripId) {
  const withoutPending = (store.syncQueue || []).filter(
    (item) => item.tripId !== tripId,
  );
  if (!store.syncVersions?.[tripId])
    return { ...store, syncQueue: withoutPending };
  return {
    ...store,
    syncQueue: [
      ...withoutPending,
      {
        mutationId: uuid(),
        tripId,
        entityType: "trip_document",
        entityId: tripId,
        baseVersion: Number(store.syncVersions[tripId]),
        deleted: true,
        queuedAt: new Date().toISOString(),
      },
    ],
  };
}

export async function synchronizeStore(store, api) {
  const remote = await api.trips(),
    remoteIds = new Set(remote.trips.map((trip) => trip.id));
  for (const trip of store.trips.filter(
    (item) => !item.archived && !remoteIds.has(item.id),
  ))
    await api.createTrip(trip);
  let queue = store.syncQueue || [],
    syncVersions = { ...(store.syncVersions || {}) },
    conflicts = [];
  if (queue.length) {
    const { results } = await api.push(
      queue.map(({ queuedAt, ...operation }) => operation),
    );
    const applied = new Set();
    results.forEach((result) => {
      if (result.status === "applied" || result.duplicate) {
        applied.add(result.mutationId);
        syncVersions[
          queue.find((item) => item.mutationId === result.mutationId)?.tripId
        ] = result.version;
      }
      if (result.status === "conflict") conflicts.push(result);
    });
    queue = queue.filter((item) => !applied.has(item.mutationId));
  }
  const pulled = await api.pull(store.syncCursor || 0),
    byId = new Map(store.trips.map((trip) => [trip.id, trip]));
  remote.trips.forEach((remoteTrip) => {
    if (!byId.has(remoteTrip.id)) byId.set(remoteTrip.id, remoteTrip.data);
  });
  pulled.changes
    .filter((change) => change.entityType === "trip_document")
    .forEach((change) => {
      syncVersions[change.tripId] = Math.max(
        Number(syncVersions[change.tripId] || 0),
        Number(change.version),
      );
      if (change.deleted) byId.delete(change.tripId);
      else if (!queue.some((item) => item.tripId === change.tripId))
        byId.set(change.tripId, change.payload);
    });
  const trips = [...byId.values()],
    activeTripId = trips.some(
      (trip) => trip.id === store.activeTripId && !trip.archived,
    )
      ? store.activeTripId
      : trips.find((trip) => !trip.archived)?.id || null;
  return {
    store: {
      ...store,
      trips,
      activeTripId,
      syncQueue: queue,
      syncVersions,
      syncCursor: pulled.cursor,
    },
    conflicts,
    pushed: (store.syncQueue || []).length - queue.length,
    pulled: pulled.changes.length,
  };
}
