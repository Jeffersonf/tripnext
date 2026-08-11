const idFallback = () =>
  `item-${Date.now()}-${Math.random().toString(36).slice(2)}`;

export function normalizeTrip(
  trip,
  idFactory = idFallback,
  now = new Date().toISOString(),
) {
  const currency = trip?.currency || "BRL";
  const travelerCount = Math.max(1, Number(trip?.travelers) || 1);
  const participants = (
    trip?.participants?.length
      ? trip.participants
      : Array.from({ length: travelerCount }, (_, index) => ({
          id: `${trip?.id || "trip"}-traveler-${index + 1}`,
          name: `Viajante ${index + 1}`,
        }))
  ).map((participant, index) => ({
    id: participant.id || idFactory(),
    name: participant.name?.trim() || `Viajante ${index + 1}`,
  }));
  return {
    name: "",
    destination: "",
    start: "",
    end: "",
    travelers: 1,
    participants: [],
    budget: 0,
    currency: "BRL",
    contingencyPercent: 0,
    checklist: [],
    ideas: [],
    options: [],
    itinerary: [],
    ...trip,
    id: trip?.id || idFactory(),
    createdAt: trip?.createdAt || now,
    updatedAt: trip?.updatedAt || now,
    currency,
    travelers: participants.length,
    participants,
    itinerary: (trip?.itinerary || []).map((item, index) => ({
      ...normalizeCostItem(item, currency),
      id: item.id || idFactory(),
      sortOrder: Number.isFinite(item.sortOrder) ? item.sortOrder : index,
    })),
    ideas: (trip?.ideas || []).map((item) => ({
      ...item,
      id: item.id || idFactory(),
    })),
    options: (trip?.options || []).map((item) => {
      const normalized = normalizeCostItem(item, currency, "price");
      return {
        ...normalized,
        id: item.id || idFactory(),
        bookingDeadline: item.bookingDeadline || "",
        cancellationDeadline: item.cancellationDeadline || "",
        priceHistory: item.priceHistory?.length
          ? item.priceHistory
          : [
              {
                price: normalized.price,
                currency: normalized.costCurrency,
                exchangeRate: normalized.exchangeRate,
                observedAt: item.observedAt || now,
              },
            ],
      };
    }),
    checklist: trip?.checklist || [],
  };
}

export function normalizeCostItem(
  item = {},
  tripCurrency = "BRL",
  valueKey = "cost",
) {
  const expected = Number(item[valueKey] || 0);
  return {
    ...item,
    [valueKey]: expected,
    costMin: Number(item.costMin ?? expected),
    costMax: Number(item.costMax ?? expected),
    costCurrency: item.costCurrency || item.currency || tripCurrency,
    exchangeRate: Number(item.exchangeRate || 1),
    quoteDate: item.quoteDate || "",
    costScope: item.costScope || "group",
    costClass: item.costClass || "daily",
    participantIds: Array.isArray(item.participantIds)
      ? item.participantIds
      : [],
  };
}

export function costRange(item, travelers = 1, valueKey = "cost") {
  const multiplier =
    (item.costScope === "person" ? Math.max(1, Number(travelers) || 1) : 1) *
    Math.max(0, Number(item.exchangeRate ?? 1));
  const expected = Number(item[valueKey] || 0);
  return {
    min: Number(item.costMin ?? expected) * multiplier,
    expected: expected * multiplier,
    max: Number(item.costMax ?? expected) * multiplier,
  };
}

export function summarizeCosts(items, participants = 1) {
  const people = Array.isArray(participants)
    ? participants
    : Array.from(
        { length: Math.max(1, Number(participants) || 1) },
        (_, index) => ({
          id: `traveler-${index + 1}`,
          name: `Viajante ${index + 1}`,
        }),
      );
  const rows = items.map((item) => ({
    ...item,
    range: costRange(
      item,
      item.costScope === "person" && item.participantIds?.length
        ? item.participantIds.length
        : people.length,
    ),
  }));
  const sum = (key) => rows.reduce((total, row) => total + row.range[key], 0);
  const dimensions = (key) =>
    Object.fromEntries(
      [...new Set(rows.map((row) => row[key] || "Não informado"))].map(
        (value) => [
          value,
          rows
            .filter((row) => (row[key] || "Não informado") === value)
            .reduce((total, row) => total + row.range.expected, 0),
        ],
      ),
    );
  const byTraveler = Object.fromEntries(people.map((person) => [person.id, 0]));
  rows.forEach((row) => {
    const selected = row.participantIds?.length
      ? people.filter((person) => row.participantIds.includes(person.id))
      : people;
    if (!selected.length) return;
    const share = row.range.expected / selected.length;
    selected.forEach((person) => {
      byTraveler[person.id] += share;
    });
  });
  return {
    rows,
    total: { min: sum("min"), expected: sum("expected"), max: sum("max") },
    byDay: dimensions("date"),
    byCity: dimensions("city"),
    byClass: dimensions("costClass"),
    byTraveler,
  };
}

export function migrateStoredData(
  storeText,
  legacyText,
  idFactory = idFallback,
  now = new Date().toISOString(),
) {
  try {
    const saved = storeText ? JSON.parse(storeText) : null;
    if (Array.isArray(saved?.trips)) {
      const trips = saved.trips.map((t) => normalizeTrip(t, idFactory, now));
      const active = trips.some(
        (t) => t.id === saved.activeTripId && !t.archived,
      )
        ? saved.activeTripId
        : trips.find((t) => !t.archived)?.id || null;
      return { version: 5, trips, activeTripId: active };
    }
    const legacy = legacyText ? JSON.parse(legacyText) : null;
    if (legacy) {
      const trip = normalizeTrip(legacy, idFactory, now);
      return { version: 5, trips: [trip], activeTripId: trip.id };
    }
  } catch {}
  return { version: 5, trips: [], activeTripId: null };
}

export function sortPlanItems(items) {
  return [...items].sort(
    (a, b) =>
      a.date.localeCompare(b.date) ||
      (Number(a.sortOrder) || 0) - (Number(b.sortOrder) || 0) ||
      (a.time || "").localeCompare(b.time || ""),
  );
}

export function dayPart(time) {
  const hour = Number((time || "12:00").split(":")[0]);
  return hour < 12 ? "Manhã" : hour < 18 ? "Tarde" : "Noite";
}

export function findConflictIds(items) {
  const conflicts = new Set();
  const timed = items.filter((x) => x.date && x.time && Number(x.duration) > 0);
  for (let i = 0; i < timed.length; i++)
    for (let j = i + 1; j < timed.length; j++) {
      if (timed[i].date !== timed[j].date) continue;
      const startA = minutes(timed[i].time),
        endA = startA + Number(timed[i].duration),
        startB = minutes(timed[j].time),
        endB = startB + Number(timed[j].duration);
      if (startA < endB && startB < endA) {
        conflicts.add(timed[i].id);
        conflicts.add(timed[j].id);
      }
    }
  return conflicts;
}

export function movePlanItem(items, itemId, targetDate, targetIndex) {
  const moving = items.find((x) => x.id === itemId);
  if (!moving) return items;
  const others = items.filter((x) => x.id !== itemId),
    target = sortPlanItems(others.filter((x) => x.date === targetDate));
  target.splice(Math.max(0, Math.min(targetIndex, target.length)), 0, {
    ...moving,
    date: targetDate,
  });
  const order = new Map(target.map((x, index) => [x.id, index]));
  return others
    .filter((x) => x.date !== targetDate)
    .concat(target)
    .map((x) => (order.has(x.id) ? { ...x, sortOrder: order.get(x.id) } : x));
}

export function buildRouteLegs(items, mode = "walking") {
  const speed =
    { walking: 4.8, cycling: 15, driving: 35, transit: 22 }[mode] || 4.8;
  const located = sortPlanItems(items).filter(
    (x) => Number.isFinite(x.latitude) && Number.isFinite(x.longitude),
  );
  return located.slice(1).map((to, index) => {
    const from = located[index],
      distanceKm = haversine(
        from.latitude,
        from.longitude,
        to.latitude,
        to.longitude,
      ),
      durationMinutes = Math.max(1, Math.round((distanceKm / speed) * 60));
    return {
      fromId: from.id,
      toId: to.id,
      fromTitle: from.title,
      toTitle: to.title,
      fromLatitude: from.latitude,
      fromLongitude: from.longitude,
      toLatitude: to.latitude,
      toLongitude: to.longitude,
      distanceKm,
      durationMinutes,
      mode,
    };
  });
}

export function findTightRouteLegs(items, legs) {
  const byId = new Map(items.map((item) => [item.id, item]));
  return legs
    .map((leg) => {
      const from = byId.get(leg.fromId),
        to = byId.get(leg.toId);
      if (!from || !to || from.date !== to.date || !from.time || !to.time)
        return null;
      const availableMinutes =
          minutes(to.time) - minutes(from.time) - Number(from.duration || 0),
        deficitMinutes = Math.ceil(leg.durationMinutes - availableMinutes);
      return deficitMinutes > 0
        ? { ...leg, availableMinutes, deficitMinutes }
        : null;
    })
    .filter(Boolean);
}

function haversine(lat1, lon1, lat2, lon2) {
  const rad = (value) => (value * Math.PI) / 180,
    dLat = rad(lat2 - lat1),
    dLon = rad(lon2 - lon1),
    a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLon / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function minutes(time) {
  const [h, m] = time.split(":").map(Number);
  return h * 60 + m;
}
