import test from "node:test";
import assert from "node:assert/strict";
import {
  migrateStoredData,
  findConflictIds,
  movePlanItem,
  sortPlanItems,
  dayPart,
  buildRouteLegs,
  findTightRouteLegs,
  costRange,
  summarizeCosts,
} from "../src/planner.js";

test("migra uma viagem legada sem perder roteiro", () => {
  let n = 0;
  const data = migrateStoredData(
    null,
    JSON.stringify({
      name: "Chile",
      itinerary: [
        { title: "Voo", date: "2027-01-10", time: "08:00", cost: 800 },
      ],
    }),
    () => `id-${++n}`,
    "2026-08-11T00:00:00Z",
  );
  assert.equal(data.version, 6);
  assert.equal(data.trips[0].name, "Chile");
  assert.equal(data.trips[0].itinerary[0].title, "Voo");
  assert.equal(data.trips[0].itinerary[0].costMin, 800);
  assert.equal(data.trips[0].itinerary[0].costScope, "group");
  assert.ok(data.trips[0].itinerary[0].id);
});
test("migra coleção v2 e preserva viagem ativa", () => {
  const raw = {
    version: 2,
    activeTripId: "b",
    trips: [
      { id: "a", name: "A" },
      { id: "b", name: "B" },
    ],
  };
  const data = migrateStoredData(JSON.stringify(raw), null);
  assert.equal(data.version, 6);
  assert.equal(data.activeTripId, "b");
  assert.equal(data.trips.length, 2);
});
test("normaliza alternativas antigas com ids sem perder a escolha", () => {
  let n = 0;
  const raw = {
    version: 3,
    activeTripId: "trip",
    trips: [
      { id: "trip", name: "A", options: [{ title: "Hotel A", chosen: true }] },
    ],
  };
  const data = migrateStoredData(JSON.stringify(raw), null, () => `new-${++n}`);
  assert.equal(data.trips[0].options[0].title, "Hotel A");
  assert.equal(data.trips[0].options[0].chosen, true);
  assert.ok(data.trips[0].options[0].id);
});
test("detecta sobreposição real e ignora eventos encostados", () => {
  const ids = findConflictIds([
    { id: "a", date: "2027-01-01", time: "09:00", duration: 90 },
    { id: "b", date: "2027-01-01", time: "10:00", duration: 60 },
    { id: "c", date: "2027-01-01", time: "11:00", duration: 30 },
  ]);
  assert.deepEqual([...ids].sort(), ["a", "b"]);
});
test("move item para outro dia na posição solicitada", () => {
  const items = [
    { id: "a", date: "2027-01-01", sortOrder: 0 },
    { id: "b", date: "2027-01-02", sortOrder: 0 },
  ];
  const moved = movePlanItem(items, "a", "2027-01-02", 1);
  assert.deepEqual(
    sortPlanItems(moved).map((x) => x.id),
    ["b", "a"],
  );
  assert.equal(moved.find((x) => x.id === "a").date, "2027-01-02");
});
test("classifica blocos do dia", () => {
  assert.equal(dayPart("08:00"), "Manhã");
  assert.equal(dayPart("14:00"), "Tarde");
  assert.equal(dayPart("20:00"), "Noite");
});
test("calcula trechos aproximados apenas entre lugares confirmados", () => {
  const legs = buildRouteLegs(
    [
      {
        id: "a",
        title: "A",
        date: "2027-01-01",
        sortOrder: 0,
        latitude: -23.5505,
        longitude: -46.6333,
      },
      { id: "x", title: "Sem local", date: "2027-01-01", sortOrder: 1 },
      {
        id: "b",
        title: "B",
        date: "2027-01-01",
        sortOrder: 2,
        latitude: -23.5614,
        longitude: -46.6559,
      },
    ],
    "walking",
  );
  assert.equal(legs.length, 1);
  assert.equal(legs[0].fromId, "a");
  assert.equal(legs[0].toId, "b");
  assert.ok(legs[0].distanceKm > 2 && legs[0].distanceKm < 3);
  assert.ok(legs[0].durationMinutes > 20);
});
test("alerta quando não há tempo suficiente para o deslocamento", () => {
  const items = [
      { id: "a", date: "2027-01-01", time: "09:00", duration: 60 },
      { id: "b", date: "2027-01-01", time: "10:10", duration: 30 },
    ],
    legs = [{ fromId: "a", toId: "b", durationMinutes: 25 }];
  const tight = findTightRouteLegs(items, legs);
  assert.equal(tight.length, 1);
  assert.equal(tight[0].availableMinutes, 10);
  assert.equal(tight[0].deficitMinutes, 15);
});
test("converte faixa por cotação e multiplica custo individual pelos viajantes", () => {
  assert.deepEqual(
    costRange(
      {
        cost: 100,
        costMin: 80,
        costMax: 130,
        exchangeRate: 5,
        costScope: "person",
      },
      2,
    ),
    { min: 800, expected: 1000, max: 1300 },
  );
});
test("resume custos por dia, cidade e classe sem perder a faixa", () => {
  const summary = summarizeCosts(
    [
      {
        cost: 100,
        costMin: 90,
        costMax: 120,
        date: "2027-01-01",
        city: "Roma",
        costClass: "daily",
      },
      { cost: 500, date: "2027-01-02", city: "Roma", costClass: "fixed" },
    ],
    1,
  );
  assert.deepEqual(summary.total, { min: 590, expected: 600, max: 620 });
  assert.equal(summary.byCity.Roma, 600);
  assert.equal(summary.byClass.fixed, 500);
});
test("atribui custo individual e rateio do grupo aos viajantes corretos", () => {
  const people = [
    { id: "ana", name: "Ana" },
    { id: "bia", name: "Bia" },
  ];
  const summary = summarizeCosts(
    [
      { cost: 100, costScope: "group" },
      { cost: 40, costScope: "person", participantIds: ["ana"] },
    ],
    people,
  );
  assert.equal(summary.total.expected, 140);
  assert.equal(summary.byTraveler.ana, 90);
  assert.equal(summary.byTraveler.bia, 50);
});
