import React, { useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Plane,
  Map,
  CalendarDays,
  ListChecks,
  Plus,
  Wallet,
  Hotel,
  Train,
  Route,
  CheckCircle2,
  Circle,
  ChevronRight,
  MapPin,
  Trash2,
  Pencil,
  Utensils,
  Ticket,
  Car,
  BedDouble,
  ExternalLink,
  Compass,
  X,
  Download,
  AlertTriangle,
  Lightbulb,
  Clock,
  Copy,
  GitCompareArrows,
  Trophy,
} from "lucide-react";
import "./style.css";
import "./improvements.css";
import "./trips.css";
import "./ideas.css";
import "./schedule.css";
import "./archive.css";
import "./e2e-fixes.css";
import {
  migrateStoredData,
  sortPlanItems,
  findConflictIds,
  movePlanItem,
  dayPart,
  buildRouteLegs,
  findTightRouteLegs,
  costRange,
  summarizeCosts,
} from "./planner.js";
import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Popup,
  Polyline,
} from "react-leaflet";
import "leaflet/dist/leaflet.css";
import "./map.css";
import "./routes.css";
import "./route-provider.css";
import "./logistics.css";
import "./comparisons.css";
import { fetchDrivingRoute } from "./routeProvider.js";

const TYPES = {
  transporte: { label: "Transporte", icon: Train, color: "#2563eb" },
  hospedagem: { label: "Hospedagem", icon: BedDouble, color: "#7c3aed" },
  passeio: { label: "Passeio", icon: Ticket, color: "#ea580c" },
  alimentacao: { label: "Alimentação", icon: Utensils, color: "#db2777" },
  deslocamento: { label: "Deslocamento", icon: Car, color: "#0891b2" },
  livre: { label: "Tempo livre", icon: Clock, color: "#64748b" },
  outro: { label: "Outro", icon: MapPin, color: "#4d7c0f" },
};
const emptyTrip = {
  name: "",
  destination: "",
  start: "",
  end: "",
  travelers: 1,
  participants: [],
  budget: 0,
  currency: "BRL",
  contingencyPercent: 0,
  itinerary: [],
  ideas: [],
  options: [],
  checklist: [],
};
const money = (v, currency = "BRL") =>
  new Intl.NumberFormat("pt-BR", { style: "currency", currency }).format(
    Number(v) || 0,
  );
const pretty = (d) =>
  d
    ? new Date(`${d}T12:00:00`)
        .toLocaleDateString("pt-BR", {
          weekday: "short",
          day: "2-digit",
          month: "short",
        })
        .replaceAll(".", "")
    : "Data a definir";
const daysUntil = (date) =>
  date
    ? Math.ceil((new Date(`${date}T23:59:59`) - new Date()) / 86400000)
    : null;
const iso = (d) => {
  const x = new Date(d);
  return `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, "0")}-${String(x.getDate()).padStart(2, "0")}`;
};
const daysBetween = (start, end) => {
  if (!start || !end) return [];
  const a = [];
  for (
    let d = new Date(`${start}T12:00:00`), z = new Date(`${end}T12:00:00`);
    d <= z;
    d.setDate(d.getDate() + 1)
  )
    a.push(iso(d));
  return a;
};

function App() {
  const [store, setStore] = useState(loadStore);
  const [tab, setTab] = useState("inicio");
  const [modal, setModal] = useState(null);
  const [editing, setEditing] = useState(null);
  const [draftEvent, setDraftEvent] = useState(null);
  const trip =
    store.trips.find((t) => t.id === store.activeTripId && !t.archived) ||
    store.trips.find((t) => !t.archived) ||
    null;
  useEffect(() => {
    localStorage.setItem("tripnext-store", JSON.stringify(store));
    localStorage.removeItem("tripnext-trip");
  }, [store]);
  const setTrip = (next) =>
    setStore((s) => ({
      ...s,
      trips: s.trips.map((t) =>
        t.id === trip?.id ? (typeof next === "function" ? next(t) : next) : t,
      ),
    }));
  const update = (patch) =>
    setTrip((t) => ({ ...t, ...patch, updatedAt: new Date().toISOString() }));
  const createTrip = (data) => {
    const item = {
      ...emptyTrip,
      ...data,
      id: newId(),
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    setStore((s) => ({
      ...s,
      trips: [...s.trips, item],
      activeTripId: item.id,
    }));
    setModal(null);
    setTab("inicio");
  };
  const duplicateTrip = () => {
    const copy = {
      ...trip,
      id: newId(),
      name: `${trip.name} — cópia`,
      itinerary: (trip.itinerary || []).map((x) => ({ ...x, id: newId() })),
      ideas: (trip.ideas || []).map((x) => ({ ...x, id: newId() })),
      options: (trip.options || []).map((x) => ({
        ...x,
        id: newId(),
        chosen: false,
      })),
      checklist: (trip.checklist || []).map((x) => ({
        ...x,
        id: newId(),
        done: false,
      })),
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    setStore((s) => ({
      ...s,
      trips: [...s.trips, copy],
      activeTripId: copy.id,
    }));
    setTab("inicio");
  };
  const archiveTrip = () => {
    if (!confirm(`Arquivar ${trip.name}?`)) return;
    const remaining = store.trips.filter(
      (t) => t.id !== trip.id && !t.archived,
    );
    setStore((s) => ({
      ...s,
      trips: s.trips.map((t) =>
        t.id === trip.id ? { ...t, archived: true } : t,
      ),
      activeTripId: remaining[0]?.id || null,
    }));
  };
  const saveEvent = (e) => {
    const list = [...(trip.itinerary || [])];
    if (editing !== null)
      list[editing] = { ...e, id: list[editing].id || newId() };
    else
      list.push({
        ...e,
        id: newId(),
        sortOrder: list.filter((x) => x.date === e.date).length,
      });
    const patch = { itinerary: list };
    if (draftEvent?.ideaId)
      patch.ideas = (trip.ideas || []).filter(
        (x) => x.id !== draftEvent.ideaId,
      );
    update(patch);
    setModal(null);
    setEditing(null);
    setDraftEvent(null);
  };
  const openEdit = (i) => {
    setEditing(i);
    setModal("event");
  };
  const dialogs = (
    <>
      {modal === "newTrip" && (
        <TripModal onClose={() => setModal(null)} onSave={createTrip} />
      )}{" "}
      {modal === "trip" && (
        <TripModal
          initial={trip}
          onClose={() => setModal(null)}
          onSave={(t) => {
            update(t);
            setModal(null);
          }}
        />
      )}
      {modal === "event" && trip && (
        <EventModal
          trip={trip}
          initial={editing !== null ? trip.itinerary[editing] : draftEvent}
          onClose={() => {
            setModal(null);
            setEditing(null);
            setDraftEvent(null);
          }}
          onSave={saveEvent}
        />
      )}{" "}
      {modal === "idea" && (
        <IdeaModal
          onClose={() => setModal(null)}
          onSave={(x) => {
            update({ ideas: [...(trip.ideas || []), { ...x, id: newId() }] });
            setModal(null);
          }}
        />
      )}
      {modal === "option" && (
        <OptionModal
          trip={trip}
          onClose={() => setModal(null)}
          onSave={(option) => {
            update({
              options: [
                ...(trip.options || []),
                {
                  ...option,
                  id: newId(),
                  chosen: false,
                  observedAt: new Date().toISOString(),
                  priceHistory: [
                    {
                      price: option.price,
                      currency: option.costCurrency,
                      exchangeRate: option.exchangeRate,
                      observedAt: new Date().toISOString(),
                    },
                  ],
                },
              ],
            });
            setModal(null);
          }}
        />
      )}
      {modal === "task" && (
        <TaskModal
          onClose={() => setModal(null)}
          onSave={(x) => {
            update({ checklist: [...(trip.checklist || []), x] });
            setModal(null);
          }}
        />
      )}
    </>
  );
  const restore = (id) =>
    setStore((s) => ({
      ...s,
      trips: s.trips.map((t) => (t.id === id ? { ...t, archived: false } : t)),
      activeTripId: id,
    }));
  if (!trip)
    return (
      <>
        <Empty
          onCreate={() => setModal("newTrip")}
          archived={store.trips.filter((t) => t.archived)}
          restore={restore}
        />
        {dialogs}
      </>
    );
  return (
    <>
      <div className="shell">
        <Sidebar
          tab={tab}
          setTab={setTab}
          trip={trip}
          trips={store.trips.filter((t) => !t.archived)}
          select={(id) => setStore((s) => ({ ...s, activeTripId: id }))}
          create={() => setModal("newTrip")}
        />
        <main>
          {tab === "inicio" && <Home trip={trip} go={setTab} open={setModal} />}
          {tab === "itinerario" && (
            <Itinerary
              trip={trip}
              update={update}
              open={() => {
                setEditing(null);
                setModal("event");
              }}
              edit={openEdit}
            />
          )}
          {tab === "ideias" && (
            <Ideas
              trip={trip}
              update={update}
              open={() => setModal("idea")}
              schedule={(idea) => {
                setDraftEvent({
                  ...idea,
                  ideaId: idea.id,
                  date: trip.start,
                  time: "09:00",
                  status: "pesquisar",
                });
                setModal("event");
              }}
            />
          )}
          {tab === "comparar" && (
            <Comparisons
              trip={trip}
              update={update}
              open={() => setModal("option")}
              schedule={(option) => {
                setDraftEvent({
                  type: option.kind,
                  title: option.title,
                  location: option.location || option.destination || "",
                  date: option.departAt?.slice(0, 10) || trip.start,
                  time: option.departAt?.slice(11, 16) || "09:00",
                  duration:
                    option.duration ||
                    (option.departAt && option.arriveAt
                      ? Math.max(
                          0,
                          Math.round(
                            (new Date(option.arriveAt) -
                              new Date(option.departAt)) /
                              60000,
                          ),
                        )
                      : 0),
                  status: option.chosen ? "reservar" : "pesquisar",
                  cost: option.price,
                  costMin: option.costMin,
                  costMax: option.costMax,
                  costCurrency: option.costCurrency,
                  exchangeRate: option.exchangeRate,
                  quoteDate: option.quoteDate,
                  costScope: option.costScope,
                  costClass: option.costClass,
                  participantIds: option.participantIds,
                  link: option.url || "",
                  notes: [
                    option.provider && `Fornecedor: ${option.provider}`,
                    option.cancellation &&
                      `Cancelamento: ${option.cancellation}`,
                    option.baggage && `Bagagem: ${option.baggage}`,
                    option.origin &&
                      option.destination &&
                      `${option.origin} → ${option.destination}`,
                    option.roomType && `Quarto: ${option.roomType}`,
                    option.bookingDeadline &&
                      `Reservar até: ${option.bookingDeadline}`,
                    option.cancellationDeadline &&
                      `Cancelamento grátis até: ${option.cancellationDeadline}`,
                  ]
                    .filter(Boolean)
                    .join(" · "),
                });
                setModal("event");
              }}
            />
          )}
          {tab === "custos" && <Costs trip={trip} edit={openEdit} />}
          {tab === "checklist" && (
            <Checklist
              trip={trip}
              update={update}
              open={() => setModal("task")}
            />
          )}
          {tab === "ajustes" && (
            <SettingsPage
              trip={trip}
              archived={store.trips.filter((t) => t.archived)}
              restore={restore}
              edit={() => setModal("trip")}
              duplicate={duplicateTrip}
              archive={archiveTrip}
              remove={() => {
                if (confirm("Excluir esta viagem e todo o planejamento?"))
                  setStore((s) => {
                    const trips = s.trips.filter((t) => t.id !== trip.id),
                      next = trips.find((t) => !t.archived);
                    return { ...s, trips, activeTripId: next?.id || null };
                  });
              }}
            />
          )}
        </main>
      </div>
      {dialogs}
    </>
  );
}
function Sidebar({ tab, setTab, trip, trips, select, create }) {
  const items = [
    ["inicio", "Visão geral", Map],
    ["itinerario", "Roteiro", CalendarDays],
    ["ideias", "Ideias", Lightbulb],
    ["comparar", "Comparar", GitCompareArrows],
    ["custos", "Custos previstos", Wallet],
    ["checklist", "Checklist", ListChecks],
    ["ajustes", "Ajustes", Compass],
  ];
  return (
    <aside>
      <div className="brand">
        <span>
          <Plane />
        </span>
        <b>TripNext</b>
      </div>
      <div className="trip-switcher">
        <small>VIAGEM ATIVA</small>
        <select value={trip.id} onChange={(e) => select(e.target.value)}>
          {trips.map((t) => (
            <option value={t.id} key={t.id}>
              {t.name}
            </option>
          ))}
        </select>
        <button onClick={create}>
          <Plus /> Nova viagem
        </button>
      </div>
      <nav>
        {items.map(([id, l, I]) => (
          <button
            className={tab === id ? "active" : ""}
            onClick={() => setTab(id)}
            key={id}
          >
            <I />
            {l}
          </button>
        ))}
      </nav>
      <div className="profile">
        <i>J</i>
        <div>
          <b>Jefferson</b>
          <small>{trips.length} viagem(ns)</small>
        </div>
      </div>
    </aside>
  );
}
function Empty({ onCreate, archived = [], restore }) {
  return (
    <div className="empty">
      <div className="empty-card">
        <span className="big-icon">
          <Map />
        </span>
        <p className="eyebrow">SEU PLANEJADOR DE VIAGEM</p>
        <h1>Da ideia ao embarque.</h1>
        <p>
          Defina as datas e monte cada dia com transportes, hospedagens,
          passeios, reservas e custos previstos.
        </p>
        <button className="primary" onClick={onCreate}>
          <Plus /> Começar a planejar
        </button>
        {archived.length > 0 && (
          <div className="archived-empty">
            <small>VIAGENS ARQUIVADAS</small>
            {archived.map((t) => (
              <button key={t.id} onClick={() => restore(t.id)}>
                {t.name}
                <span>Restaurar</span>
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
function Header({ title, subtitle, action }) {
  return (
    <header>
      <div>
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>
      {action}
    </header>
  );
}
function Card({ children, className = "", onClick }) {
  return (
    <section className={`card ${className}`} onClick={onClick}>
      {children}
    </section>
  );
}
function Home({ trip, go, open }) {
  const events = trip.itinerary || [],
    total = events.reduce((s, e) => s + Number(e.cost || 0), 0),
    days = daysBetween(trip.start, trip.end),
    planned = new Set(events.map((e) => e.date)).size,
    next = [...events].sort(sortEvents)[0],
    emptyDays = days.filter((d) => !events.some((e) => e.date === d)).length,
    pending = events.filter(
      (e) =>
        e.status !== "reservado" &&
        ["transporte", "hospedagem", "passeio"].includes(e.type),
    ).length;
  const insights = [
    !events.some((e) => e.type === "transporte") &&
      "Defina como chegar ao destino",
    !events.some((e) => e.type === "hospedagem") &&
      "Inclua sua hospedagem no roteiro",
    emptyDays > 0 && `${emptyDays} dia(s) ainda sem programação`,
    pending > 0 && `${pending} reserva(s) ainda precisam de atenção`,
  ].filter(Boolean);
  return (
    <div className="page">
      <Header
        title="Sua próxima viagem"
        subtitle="Planeje primeiro. Embarque tranquilo."
        action={
          <button className="outline" onClick={() => open("trip")}>
            <Pencil /> Editar viagem
          </button>
        }
      />
      <section className="boarding">
        <div>
          <small>PLANO DE VIAGEM</small>
          <h2>{trip.name}</h2>
          <p>
            <MapPin /> {trip.destination}
          </p>
        </div>
        <Plane />
        <div className="dates">
          {pretty(trip.start)} — {pretty(trip.end)} · {days.length} dias
        </div>
        <div className="barcode" />
      </section>
      <div className="stats">
        <Card>
          <small>DIAS PLANEJADOS</small>
          <strong>
            {planned} <i>/ {days.length}</i>
          </strong>
          <span>{events.length} itens no roteiro</span>
        </Card>
        <Card>
          <small>CUSTO PREVISTO</small>
          <strong>{money(total)}</strong>
          <span>
            {trip.budget
              ? `${money(Math.max(0, trip.budget - total))} livres no teto`
              : "Defina um teto se quiser"}
          </span>
        </Card>
        <Card>
          <small>RESERVAS PENDENTES</small>
          <strong>{pending}</strong>
          <span>
            {pending ? "Ainda precisam de decisão" : "Tudo encaminhado"}
          </span>
        </Card>
      </div>
      <div className="home-grid">
        <div>
          <div className="section-head">
            <h3>Próximo passo</h3>
            <button onClick={() => go("itinerario")}>
              Ver roteiro <ChevronRight />
            </button>
          </div>
          <Card className="next-step" onClick={() => go("itinerario")}>
            {next ? (
              <>
                <TypeIcon type={next.type} />
                <div>
                  <small>
                    {pretty(next.date)} · {next.time}
                  </small>
                  <h3>{next.title}</h3>
                  <p>{next.location || "Local ainda não definido"}</p>
                </div>
              </>
            ) : (
              <>
                <TypeIcon type="passeio" />
                <div>
                  <small>COMECE POR AQUI</small>
                  <h3>Monte o primeiro dia</h3>
                  <p>
                    Adicione como você vai chegar, onde ficará e o primeiro
                    passeio.
                  </p>
                </div>
              </>
            )}
          </Card>
          {insights.length > 0 && (
            <Card className="diagnosis">
              <h3>
                <AlertTriangle /> Para fechar o plano
              </h3>
              {insights.map((x) => (
                <button key={x} onClick={() => go("itinerario")}>
                  <Circle />
                  {x}
                  <ChevronRight />
                </button>
              ))}
            </Card>
          )}
        </div>
        <div>
          <h3 className="section-title">Adicionar ao plano</h3>
          <div className="quick-actions">
            <button onClick={() => open("event")}>
              <Train /> Transporte
            </button>
            <button onClick={() => open("event")}>
              <Hotel /> Hospedagem
            </button>
            <button onClick={() => open("event")}>
              <Ticket /> Passeio
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
function TypeIcon({ type }) {
  const t = TYPES[type] || TYPES.outro,
    I = t.icon;
  return (
    <span
      className="type-icon"
      style={{ background: `${t.color}18`, color: t.color }}
    >
      <I />
    </span>
  );
}
function TripMap({ events }) {
  const [mode, setMode] = useState("walking");
  const [roadRoute, setRoadRoute] = useState(null),
    [routeLoading, setRouteLoading] = useState(false),
    [routeError, setRouteError] = useState("");
  const located = events.filter(
      (e) => Number.isFinite(e.latitude) && Number.isFinite(e.longitude),
    ),
    estimatedLegs = buildRouteLegs(events, mode),
    legs =
      mode === "driving" && roadRoute
        ? estimatedLegs.map((leg, index) => ({
            ...leg,
            ...roadRoute.legs[index],
            real: true,
          }))
        : estimatedLegs;
  const tightLegs = findTightRouteLegs(events, legs),
    tightLegIds = new Set(tightLegs.map((leg) => `${leg.fromId}-${leg.toId}`));
  const coordinateSignature = located
    .map((e) => `${e.id}:${e.latitude},${e.longitude}`)
    .join(";");
  useEffect(() => {
    setRoadRoute(null);
    setRouteError("");
  }, [coordinateSignature]);
  if (!located.length)
    return (
      <div className="map-empty">
        <Map />
        <div>
          <b>Confirme os lugares para vê-los no mapa</b>
          <span>
            Edite um item, busque o endereço e selecione um resultado.
          </span>
        </div>
      </div>
    );
  const positions = located.map((e) => [e.latitude, e.longitude]);
  const calculateRoadRoute = async () => {
    setRouteLoading(true);
    setRouteError("");
    try {
      setRoadRoute(await fetchDrivingRoute(located));
    } catch (error) {
      setRouteError(error.message);
    } finally {
      setRouteLoading(false);
    }
  };
  return (
    <section className="map-section">
      <div className="map-toolbar">
        <b>Mapa do dia</b>
        <div>
          {[
            ["walking", "A pé"],
            ["cycling", "Bicicleta"],
            ["driving", "Carro"],
            ["transit", "Transporte"],
          ].map(([id, label]) => (
            <button
              className={mode === id ? "active" : ""}
              onClick={() => setMode(id)}
              key={id}
            >
              {label}
            </button>
          ))}
          {mode === "driving" && located.length > 1 && (
            <button
              className="route-action"
              disabled={routeLoading}
              onClick={calculateRoadRoute}
            >
              {routeLoading
                ? "Calculando…"
                : roadRoute
                  ? "Atualizar rota"
                  : "Calcular rota real"}
            </button>
          )}
        </div>
      </div>
      {routeError && (
        <div className="route-error">
          <AlertTriangle />
          {routeError} Usando estimativa local.
        </div>
      )}
      {tightLegs.length > 0 && (
        <div className="route-warning">
          <AlertTriangle />
          <div>
            <b>O horário pode não fechar</b>
            <span>
              {tightLegs
                .map(
                  (leg) =>
                    `${leg.fromTitle} → ${leg.toTitle}: faltam cerca de ${leg.deficitMinutes} min`,
                )
                .join(" · ")}
            </span>
          </div>
        </div>
      )}
      <div className="trip-map">
        <MapContainer
          key={positions.map((p) => p.join(",")).join(";")}
          center={positions[0]}
          zoom={13}
          scrollWheelZoom={false}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          {(roadRoute?.geometry?.length || positions.length > 1) && (
            <Polyline
              positions={
                mode === "driving" && roadRoute?.geometry?.length
                  ? roadRoute.geometry
                  : positions
              }
              pathOptions={{
                color: "#3f7d00",
                weight: 3,
                dashArray: mode === "driving" && roadRoute ? undefined : "7 7",
              }}
            />
          )}
          {located.map((event, index) => (
            <CircleMarker
              key={event.id}
              center={[event.latitude, event.longitude]}
              radius={12}
              pathOptions={{
                color: TYPES[event.type]?.color || "#4d7c0f",
                fillOpacity: 0.9,
              }}
            >
              <Popup>
                <b>
                  {index + 1}. {event.title}
                </b>
                <br />
                {event.location}
              </Popup>
            </CircleMarker>
          ))}
        </MapContainer>
        <span className="map-note">
          {mode === "driving" && roadRoute
            ? `Rota viária ${roadRoute.provider} · ${roadRoute.distanceKm.toFixed(1)} km · ${roadRoute.durationMinutes} min`
            : "Linha indica a ordem, não a rota viária."}
        </span>
      </div>
      {legs.length > 0 && (
        <div className="route-legs">
          {legs.map((leg, index) => (
            <div
              className={
                tightLegIds.has(`${leg.fromId}-${leg.toId}`) ? "tight" : ""
              }
              key={`${leg.fromId}-${leg.toId}`}
            >
              <i>{index + 1}</i>
              <span>
                <b>
                  {leg.fromTitle} → {leg.toTitle}
                </b>
                <small>
                  {leg.distanceKm.toFixed(1)} km{" "}
                  {leg.real ? "pela rota" : "em linha reta"} ·{" "}
                  {leg.real ? "" : "cerca de "}
                  {leg.durationMinutes} min{leg.real ? "" : "*"}
                </small>
              </span>
              <a
                target="_blank"
                rel="noreferrer"
                href={`https://www.google.com/maps/dir/?api=1&origin=${leg.fromLatitude},${leg.fromLongitude}&destination=${leg.toLatitude},${leg.toLongitude}&travelmode=${mode === "cycling" ? "bicycling" : mode}`}
              >
                Abrir trajeto <ExternalLink />
              </a>
            </div>
          ))}
          <p>
            {mode === "driving" && roadRoute
              ? `Rota calculada por ${roadRoute.provider} em ${new Date(roadRoute.fetchedAt).toLocaleString("pt-BR")}.`
              : "*Estimativa inicial; confirme no aplicativo de navegação antes de sair."}
          </p>
        </div>
      )}
    </section>
  );
}
function Itinerary({ trip, update, open, edit }) {
  const days = daysBetween(trip.start, trip.end),
    [selected, setSelected] = useState(days[0] || ""),
    [dragging, setDragging] = useState(null),
    [copyOpen, setCopyOpen] = useState(false),
    [copyTarget, setCopyTarget] = useState(days[1] || days[0] || "");
  const all = trip.itinerary || [],
    events = sortPlanItems(
      all
        .map((e, i) => ({ ...e, _index: i }))
        .filter((e) => e.date === selected),
    ),
    conflicts = findConflictIds(all);
  const remove = (i) =>
    confirm("Remover este item do roteiro?") &&
    update({ itinerary: all.filter((_, n) => n !== i) });
  const move = (id, date, index) =>
    update({ itinerary: movePlanItem(all, id, date, index) });
  const duplicateDay = () => {
    const source = sortPlanItems(all.filter((x) => x.date === selected)),
      offset = all.filter((x) => x.date === copyTarget).length,
      copies = source.map((x, i) => ({
        ...x,
        id: newId(),
        date: copyTarget,
        sortOrder: offset + i,
        status: x.status === "reservado" ? "pesquisar" : x.status,
        booking: "",
      }));
    update({ itinerary: [...all, ...copies] });
    setCopyOpen(false);
    setSelected(copyTarget);
  };
  return (
    <div className="page">
      <Header
        title="Roteiro por dia"
        subtitle={`${trip.destination} · ${days.length} dias`}
        action={
          <div className="header-actions">
            <button
              className="outline small"
              disabled={!events.length || days.length < 2}
              onClick={() => {
                setCopyTarget(days.find((d) => d !== selected) || selected);
                setCopyOpen(true);
              }}
            >
              <Copy /> Duplicar dia
            </button>
            <button
              className="outline small"
              onClick={() => exportCalendar(trip)}
            >
              <Download /> Calendário
            </button>
            <button className="primary small" onClick={open}>
              <Plus /> Adicionar
            </button>
          </div>
        }
      />
      <div className="day-strip">
        {days.map((d, i) => (
          <button
            key={d}
            className={selected === d ? "active" : ""}
            onClick={() => setSelected(d)}
            onDragOver={(e) => e.preventDefault()}
            onDrop={() => {
              if (dragging) {
                move(dragging, d, all.filter((x) => x.date === d).length);
                setDragging(null);
                setSelected(d);
              }
            }}
          >
            <small>DIA {i + 1}</small>
            <b>{new Date(`${d}T12:00`).getDate()}</b>
            <span>
              {new Date(`${d}T12:00`)
                .toLocaleDateString("pt-BR", { weekday: "short" })
                .replace(".", "")}
            </span>
            <i>{all.filter((e) => e.date === d).length || ""}</i>
          </button>
        ))}
      </div>
      <TripMap events={events} />
      {conflicts.size > 0 && events.some((e) => conflicts.has(e.id)) && (
        <div className="conflict-banner">
          <AlertTriangle />
          <div>
            <b>Há horários sobrepostos neste dia</b>
            <span>Revise os itens destacados antes de fechar o roteiro.</span>
          </div>
        </div>
      )}
      {events.length === 0 ? (
        <Blank
          icon={Route}
          title="Este dia está livre"
          text="Planeje deslocamentos, refeições, passeios ou deixe um tempo livre."
          action={open}
        />
      ) : (
        <div className="timeline">
          {events.map((e, index) => (
            <React.Fragment key={e.id}>
              {index === 0 ||
              dayPart(events[index - 1].time) !== dayPart(e.time) ? (
                <h4 className="day-part">{dayPart(e.time)}</h4>
              ) : null}
              <article
                draggable
                onDragStart={() => setDragging(e.id)}
                onDragEnd={() => setDragging(null)}
                onDragOver={(x) => x.preventDefault()}
                onDrop={(x) => {
                  x.preventDefault();
                  if (dragging && dragging !== e.id)
                    move(dragging, selected, index);
                }}
                className={conflicts.has(e.id) ? "has-conflict" : ""}
              >
                <time>
                  {e.time || "—"}
                  <small>{e.duration ? `${e.duration} min` : ""}</small>
                </time>
                <TypeIcon type={e.type} />
                <Card>
                  <div className="event-top">
                    <div>
                      <span className="tag">
                        {TYPES[e.type]?.label || "Outro"}
                      </span>
                      <span className={`status ${e.status || "pesquisar"}`}>
                        {statusLabel(e.status)}
                      </span>
                      {conflicts.has(e.id) && (
                        <span className="conflict-tag">Conflito</span>
                      )}
                    </div>
                    <div>
                      <button
                        className="icon-btn"
                        onClick={() => edit(e._index)}
                      >
                        <Pencil />
                      </button>
                      <button
                        className="icon-btn danger-icon"
                        onClick={() => remove(e._index)}
                      >
                        <Trash2 />
                      </button>
                    </div>
                  </div>
                  <h3>{e.title}</h3>
                  {e.location && (
                    <p>
                      <MapPin /> {e.location}{" "}
                      <a
                        className="map-link"
                        href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(e.location)}`}
                        target="_blank"
                        rel="noreferrer"
                      >
                        Ver mapa
                      </a>
                    </p>
                  )}
                  {e.notes && <p className="notes">{e.notes}</p>}
                  <footer>
                    {Number(e.cost) > 0 && <b>{money(e.cost)}</b>}
                    {e.booking && (
                      <span>
                        <CheckCircle2 /> {e.booking}
                      </span>
                    )}
                    {e.link && (
                      <a href={e.link} target="_blank" rel="noreferrer">
                        <ExternalLink /> Abrir reserva
                      </a>
                    )}
                    <label className="move-day">
                      Mover para{" "}
                      <select
                        value={e.date}
                        onChange={(x) => {
                          move(
                            e.id,
                            x.target.value,
                            all.filter((y) => y.date === x.target.value).length,
                          );
                          setSelected(x.target.value);
                        }}
                      >
                        {days.map((d, i) => (
                          <option value={d} key={d}>
                            Dia {i + 1}
                          </option>
                        ))}
                      </select>
                    </label>
                  </footer>
                </Card>
              </article>
            </React.Fragment>
          ))}
        </div>
      )}
      {copyOpen && (
        <Modal title="Duplicar este dia" close={() => setCopyOpen(false)}>
          <div className="fields">
            <p>
              Os {events.length} itens serão copiados sem códigos de reserva. Os
              originais permanecem neste dia.
            </p>
            <label>
              Copiar para
              <select
                value={copyTarget}
                onChange={(e) => setCopyTarget(e.target.value)}
              >
                {days
                  .filter((d) => d !== selected)
                  .map((d, i) => (
                    <option value={d} key={d}>
                      {pretty(d)}
                    </option>
                  ))}
              </select>
            </label>
            <button className="primary" onClick={duplicateDay}>
              Duplicar planejamento
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
function Ideas({ trip, update, open, schedule }) {
  const ideas = trip.ideas || [],
    remove = (id) => update({ ideas: ideas.filter((x) => x.id !== id) });
  return (
    <div className="page">
      <Header
        title="Caixa de ideias"
        subtitle="Guarde possibilidades antes de decidir o dia"
        action={
          <button className="primary small" onClick={open}>
            <Plus /> Nova ideia
          </button>
        }
      />
      {!ideas.length ? (
        <Blank
          icon={Lightbulb}
          title="Salve o que chamou sua atenção"
          text="Passeios, restaurantes, bairros e qualquer possibilidade podem ficar aqui até você decidir."
          action={open}
        />
      ) : (
        <div className="idea-grid">
          {ideas.map((idea) => (
            <Card key={idea.id} className="idea-card">
              <div className="event-top">
                <TypeIcon type={idea.type} />
                <button
                  className="icon-btn danger-icon"
                  onClick={() => remove(idea.id)}
                >
                  <Trash2 />
                </button>
              </div>
              <span className="tag">{TYPES[idea.type]?.label}</span>
              <h3>{idea.title}</h3>
              {idea.location && (
                <p>
                  <MapPin /> {idea.location}
                </p>
              )}
              {idea.notes && <p className="notes">{idea.notes}</p>}
              <footer>
                {Number(idea.cost) > 0 && <b>{money(idea.cost)}</b>}
                {idea.link && (
                  <a href={idea.link} target="_blank" rel="noreferrer">
                    <ExternalLink /> Fonte
                  </a>
                )}
              </footer>
              <button
                className="outline schedule"
                onClick={() => schedule(idea)}
              >
                <CalendarDays /> Colocar no roteiro
              </button>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
function Comparisons({ trip, update, open, schedule }) {
  const options = trip.options || [],
    groups = options.reduce((all, option) => {
      (all[option.decision] ??= []).push(option);
      return all;
    }, {});
  const choose = (id) => {
    const selected = options.find((x) => x.id === id);
    update({
      options: options.map((x) =>
        x.decision === selected.decision ? { ...x, chosen: x.id === id } : x,
      ),
    });
  };
  const remove = (id) =>
    confirm("Remover esta alternativa?") &&
    update({ options: options.filter((x) => x.id !== id) });
  const updatePrice = (option) => {
    const entered = prompt("Novo preço observado", String(option.price || 0));
    if (entered === null || entered.trim() === "" || Number(entered) < 0)
      return;
    const observedAt = new Date().toISOString();
    update({
      options: options.map((item) =>
        item.id === option.id
          ? {
              ...item,
              price: Number(entered),
              costMin: Math.min(
                Number(item.costMin ?? entered),
                Number(entered),
              ),
              costMax: Math.max(
                Number(item.costMax ?? entered),
                Number(entered),
              ),
              observedAt,
              quoteDate: observedAt.slice(0, 10),
              priceHistory: [
                ...(item.priceHistory || []),
                {
                  price: Number(entered),
                  currency: item.costCurrency,
                  exchangeRate: item.exchangeRate,
                  observedAt,
                },
              ],
            }
          : item,
      ),
    });
  };
  return (
    <div className="page">
      <Header
        title="Comparar opções"
        subtitle="Decida antes de reservar, sem perder as alternativas"
        action={
          <button className="primary small" onClick={open}>
            <Plus /> Nova alternativa
          </button>
        }
      />
      {!options.length ? (
        <Blank
          icon={GitCompareArrows}
          title="Compare antes de escolher"
          text="Adicione voos, hotéis, transportes ou passeios da sua pesquisa."
          action={open}
        />
      ) : (
        <div className="comparison-groups">
          {Object.entries(groups).map(([decision, items]) => {
            const cheapest = Math.min(
              ...items.map(
                (x) =>
                  costRange(
                    x,
                    x.costScope === "person" && x.participantIds?.length
                      ? x.participantIds.length
                      : trip.travelers,
                    "price",
                  ).expected,
              ),
            );
            return (
              <section key={decision}>
                <div className="comparison-heading">
                  <div>
                    <small>DECISÃO</small>
                    <h2>{decision}</h2>
                  </div>
                  <span>{items.length} alternativa(s)</span>
                </div>
                <div className="option-grid">
                  {items.map((option) => {
                    const range = costRange(
                      option,
                      option.costScope === "person" &&
                        option.participantIds?.length
                        ? option.participantIds.length
                        : trip.travelers,
                      "price",
                    );
                    const history = option.priceHistory || [];
                    const prior = history.at(-2);
                    const priceDelta = prior
                      ? Number(option.price) - Number(prior.price)
                      : 0;
                    return (
                      <Card
                        className={`option-card ${option.chosen ? "chosen" : ""}`}
                        key={option.id}
                      >
                        {option.chosen && (
                          <span className="winner">
                            <Trophy /> Escolhida
                          </span>
                        )}
                        <div className="event-top">
                          <TypeIcon type={option.kind} />
                          <button
                            className="icon-btn danger-icon"
                            onClick={() => remove(option.id)}
                          >
                            <Trash2 />
                          </button>
                        </div>
                        <span className="tag">
                          {TYPES[option.kind]?.label} · observado{" "}
                          {pretty(option.observedAt?.slice(0, 10))}
                        </span>
                        <h3>{option.title}</h3>
                        <p className="provider">
                          {option.provider || "Fornecedor não informado"}
                        </p>
                        <strong className="option-price">
                          {money(range.expected, trip.currency)}
                        </strong>
                        {(range.min !== range.expected ||
                          range.max !== range.expected) && (
                          <small>
                            {money(range.min, trip.currency)} a{" "}
                            {money(range.max, trip.currency)}
                          </small>
                        )}
                        {option.costCurrency !== trip.currency && (
                          <small>
                            {money(option.price, option.costCurrency)} · cotação{" "}
                            {Number(option.exchangeRate || 1).toLocaleString(
                              "pt-BR",
                            )}
                            {option.quoteDate
                              ? ` em ${pretty(option.quoteDate)}`
                              : ""}
                          </small>
                        )}
                        <small>
                          {option.costScope === "person"
                            ? `Por pessoa × ${trip.travelers || 1}`
                            : "Total do grupo"}{" "}
                          ·{" "}
                          {option.costClass === "fixed"
                            ? "custo fixo"
                            : "custo diário"}
                        </small>
                        {priceDelta !== 0 && (
                          <small
                            className={
                              priceDelta > 0 ? "deadline-alert" : "best-price"
                            }
                          >
                            {priceDelta > 0 ? "Subiu" : "Caiu"}{" "}
                            {money(
                              Math.abs(
                                priceDelta * Number(option.exchangeRate || 1),
                              ),
                              trip.currency,
                            )}{" "}
                            desde a consulta anterior
                          </small>
                        )}
                        {option.bookingDeadline && (
                          <small
                            className={
                              daysUntil(option.bookingDeadline) <= 7
                                ? "deadline-alert"
                                : ""
                            }
                          >
                            Reservar até {pretty(option.bookingDeadline)} ·{" "}
                            {daysUntil(option.bookingDeadline) < 0
                              ? "prazo vencido"
                              : `${daysUntil(option.bookingDeadline)} dia(s)`}
                          </small>
                        )}
                        {option.cancellationDeadline && (
                          <small>
                            Cancelamento grátis até{" "}
                            {pretty(option.cancellationDeadline)}
                          </small>
                        )}
                        {range.expected === cheapest && items.length > 1 && (
                          <small className="best-price">Menor preço</small>
                        )}
                        <dl>
                          {option.kind === "transporte" && option.origin && (
                            <>
                              <dt>Trajeto</dt>
                              <dd>
                                {option.origin} →{" "}
                                {option.destination || "a definir"}
                              </dd>
                            </>
                          )}
                          {option.kind === "transporte" && option.departAt && (
                            <>
                              <dt>Horários</dt>
                              <dd>
                                {new Date(option.departAt).toLocaleString(
                                  "pt-BR",
                                )}{" "}
                                {option.arriveAt
                                  ? `→ ${new Date(option.arriveAt).toLocaleString("pt-BR")}`
                                  : ""}
                              </dd>
                            </>
                          )}
                          {option.kind === "transporte" &&
                            option.stops !== "" && (
                              <>
                                <dt>Paradas</dt>
                                <dd>{option.stops}</dd>
                              </>
                            )}
                          {option.kind === "hospedagem" && option.roomType && (
                            <>
                              <dt>Quarto</dt>
                              <dd>{option.roomType}</dd>
                            </>
                          )}
                          {option.kind === "hospedagem" && option.nights && (
                            <>
                              <dt>Diárias</dt>
                              <dd>{option.nights}</dd>
                            </>
                          )}
                          {option.kind === "passeio" && option.duration && (
                            <>
                              <dt>Duração</dt>
                              <dd>{option.duration} min</dd>
                            </>
                          )}
                          {option.cancellation && (
                            <>
                              <dt>Cancelamento</dt>
                              <dd>{option.cancellation}</dd>
                            </>
                          )}
                          {option.baggage && (
                            <>
                              <dt>Bagagem / inclusão</dt>
                              <dd>{option.baggage}</dd>
                            </>
                          )}
                          {option.pros && (
                            <>
                              <dt>Vantagens</dt>
                              <dd>{option.pros}</dd>
                            </>
                          )}
                          {option.cons && (
                            <>
                              <dt>Atenções</dt>
                              <dd>{option.cons}</dd>
                            </>
                          )}
                        </dl>
                        {option.url && (
                          <a
                            className="source-link"
                            href={option.url}
                            target="_blank"
                            rel="noreferrer"
                          >
                            <ExternalLink /> Ver fonte
                          </a>
                        )}
                        <div className="option-actions">
                          <button
                            className={option.chosen ? "selected" : ""}
                            onClick={() => choose(option.id)}
                          >
                            {option.chosen ? (
                              <>
                                <CheckCircle2 /> Escolhida
                              </>
                            ) : (
                              "Escolher"
                            )}
                          </button>
                          <button onClick={() => schedule(option)}>
                            <CalendarDays /> Levar ao roteiro
                          </button>
                          <button onClick={() => updatePrice(option)}>
                            <Wallet /> Atualizar preço
                          </button>
                        </div>
                      </Card>
                    );
                  })}
                </div>
              </section>
            );
          })}
        </div>
      )}
    </div>
  );
}
function Costs({ trip, edit }) {
  const events = trip.itinerary || [],
    summary = summarizeCosts(events, trip.participants || trip.travelers),
    total = summary.total.expected,
    contingency = (total * (Number(trip.contingencyPercent) || 0)) / 100,
    plannedWithReserve = total + contingency;
  const groups = Object.entries(TYPES)
    .map(([id, t]) => ({
      id,
      ...t,
      total: summarizeCosts(
        events.filter((e) => e.type === id),
        trip.participants || trip.travelers,
      ).total.expected,
    }))
    .filter((x) => x.total);
  return (
    <div className="page">
      <Header
        title="Custos previstos"
        subtitle="Uma estimativa do que será necessário para a viagem"
      />
      <div className="cost-hero">
        <div>
          <small>TOTAL PLANEJADO</small>
          <h2>{money(total, trip.currency)}</h2>
          <p>
            Faixa {money(summary.total.min, trip.currency)} —{" "}
            {money(summary.total.max, trip.currency)}
          </p>
          <p>
            {trip.travelers || 1} viajante(s) ·{" "}
            {money(total / (trip.travelers || 1), trip.currency)} por pessoa
            {contingency > 0 &&
              ` · ${money(contingency, trip.currency)} de contingência`}
          </p>
        </div>
        {trip.budget > 0 && (
          <div>
            <small>TETO DEFINIDO</small>
            <h3>{money(trip.budget, trip.currency)}</h3>
            <div className="progress">
              <i
                style={{
                  width: `${Math.min(100, (plannedWithReserve / trip.budget) * 100)}%`,
                }}
              />
            </div>
            <p>
              {plannedWithReserve > trip.budget
                ? `${money(plannedWithReserve - trip.budget, trip.currency)} acima do teto com a reserva`
                : `${money(trip.budget - plannedWithReserve, trip.currency)} disponíveis após a reserva`}
            </p>
          </div>
        )}
      </div>
      <div className="cost-grid">
        {groups.length ? (
          groups.map((g) => (
            <Card key={g.id}>
              <TypeIcon type={g.id} />
              <div>
                <span>{g.label}</span>
                <strong>{money(g.total, trip.currency)}</strong>
                <small>
                  {Math.round((g.total / total) * 100)}% do previsto
                </small>
              </div>
            </Card>
          ))
        ) : (
          <Blank
            icon={Wallet}
            title="Nenhum custo previsto"
            text="Ao adicionar itens ao roteiro, informe os valores estimados."
          />
        )}
      </div>
      <h3 className="section-title">Composição do planejamento</h3>
      <div className="cost-grid">
        {Object.entries(summary.byClass).map(([kind, value]) => (
          <Card key={kind}>
            <div>
              <span>
                {kind === "fixed" ? "Custos fixos" : "Custos diários"}
              </span>
              <strong>{money(value, trip.currency)}</strong>
            </div>
          </Card>
        ))}
        {Object.entries(summary.byDay)
          .filter(([day]) => day !== "Não informado")
          .map(([day, value]) => (
            <Card key={day}>
              <div>
                <span>{pretty(day)}</span>
                <strong>{money(value, trip.currency)}</strong>
                <small>Total do dia</small>
              </div>
            </Card>
          ))}
        {Object.entries(summary.byCity)
          .filter(([city]) => city !== "Não informado")
          .map(([city, value]) => (
            <Card key={city}>
              <div>
                <span>{city}</span>
                <strong>{money(value, trip.currency)}</strong>
                <small>Total da cidade</small>
              </div>
            </Card>
          ))}
      </div>
      <h3 className="section-title">Previsão por viajante</h3>
      <div className="cost-grid">
        {(trip.participants || []).map((participant) => (
          <Card key={participant.id}>
            <div>
              <span>{participant.name}</span>
              <strong>
                {money(summary.byTraveler[participant.id] || 0, trip.currency)}
              </strong>
              <small>Itens atribuídos e rateios do grupo</small>
            </div>
          </Card>
        ))}
      </div>
      <h3 className="section-title">Itens com custo</h3>
      <Card className="cost-list">
        {events
          .filter((e) => Number(e.cost) > 0)
          .sort((a, b) => Number(b.cost) - Number(a.cost))
          .map((e, i) => (
            <button key={i} onClick={() => edit(trip.itinerary.indexOf(e))}>
              <TypeIcon type={e.type} />
              <div>
                <b>{e.title}</b>
                <small>
                  {pretty(e.date)} · {TYPES[e.type]?.label}
                </small>
              </div>
              <strong>
                {money(
                  costRange(
                    e,
                    e.costScope === "person" && e.participantIds?.length
                      ? e.participantIds.length
                      : trip.travelers,
                  ).expected,
                  trip.currency,
                )}
              </strong>
              <ChevronRight />
            </button>
          ))}
      </Card>
    </div>
  );
}
function Checklist({ trip, update, open }) {
  const list = trip.checklist || [],
    toggle = (i) =>
      update({
        checklist: list.map((x, n) => (n === i ? { ...x, done: !x.done } : x)),
      }),
    remove = (i) => update({ checklist: list.filter((_, n) => n !== i) });
  return (
    <div className="page">
      <Header
        title="Antes de viajar"
        subtitle={`${list.filter((x) => x.done).length}/${list.length} tarefas concluídas`}
        action={
          <button className="primary small" onClick={open}>
            <Plus /> Nova tarefa
          </button>
        }
      />
      {!list.length ? (
        <Blank
          icon={ListChecks}
          title="Prepare sua viagem"
          text="Adicione documentos, reservas, mala e tudo que precisa resolver antes de sair."
          action={open}
        />
      ) : (
        <Card className="list">
          {list.map((x, i) => (
            <div className="task" key={i}>
              <button onClick={() => toggle(i)}>
                {x.done ? <CheckCircle2 className="ok" /> : <Circle />}
                <span>
                  <b className={x.done ? "done" : ""}>{x.name}</b>
                  <small>{x.category}</small>
                </span>
              </button>
              <button
                className="icon-btn danger-icon"
                onClick={() => remove(i)}
              >
                <X />
              </button>
            </div>
          ))}
        </Card>
      )}
    </div>
  );
}
function SettingsPage({
  trip,
  archived,
  restore,
  edit,
  duplicate,
  archive,
  remove,
}) {
  return (
    <div className="page">
      <Header title="Configurações da viagem" subtitle={trip.name} />
      <Card className="settings">
        <button onClick={edit}>
          <span>
            <b>Dados da viagem</b>
            <small>Destino, datas, viajantes e teto previsto</small>
          </span>
          <ChevronRight />
        </button>
        <button onClick={duplicate}>
          <span>
            <b>Duplicar viagem</b>
            <small>Cria uma cópia independente deste planejamento</small>
          </span>
          <ChevronRight />
        </button>
        <button onClick={archive}>
          <span>
            <b>Arquivar viagem</b>
            <small>Retira da lista ativa sem apagar os dados</small>
          </span>
          <ChevronRight />
        </button>
        <div>
          <b>Armazenamento</b>
          <span>Neste dispositivo</span>
        </div>
      </Card>
      {archived.length > 0 && (
        <>
          <h3 className="section-title">Viagens arquivadas</h3>
          <Card className="archived-list">
            {archived.map((t) => (
              <button key={t.id} onClick={() => restore(t.id)}>
                <span>
                  <b>{t.name}</b>
                  <small>{t.destination}</small>
                </span>
                <strong>Restaurar</strong>
              </button>
            ))}
          </Card>
        </>
      )}
      <button className="danger" onClick={remove}>
        <Trash2 /> Excluir definitivamente
      </button>
    </div>
  );
}
function Blank({ icon: I, title, text, action }) {
  return (
    <div className="blank">
      <span>
        <I />
      </span>
      <h2>{title}</h2>
      <p>{text}</p>
      {action && (
        <button className="outline" onClick={action}>
          <Plus /> Adicionar item
        </button>
      )}
    </div>
  );
}
function TripModal({ initial, onClose, onSave }) {
  const [f, setF] = useState({
    name: initial?.name || "",
    destination: initial?.destination || "",
    start: initial?.start || "",
    end: initial?.end || "",
    travelers: initial?.travelers || 1,
    participantNames:
      (initial?.participants || [])
        .map((participant) => participant.name)
        .join(", ") || "Viajante 1",
    budget: initial?.budget || "",
    currency: initial?.currency || "BRL",
    contingencyPercent: initial?.contingencyPercent || 0,
  });
  const valid = f.name && f.destination && f.start && f.end && f.end >= f.start;
  return (
    <Modal title={initial ? "Editar viagem" : "Nova viagem"} close={onClose}>
      <div className="fields">
        <label>
          Nome do plano
          <input
            value={f.name}
            onChange={(e) => setF({ ...f, name: e.target.value })}
            placeholder="Férias de julho"
          />
        </label>
        <label>
          Para onde você vai?
          <input
            value={f.destination}
            onChange={(e) => setF({ ...f, destination: e.target.value })}
            placeholder="Buenos Aires, Argentina"
          />
        </label>
        <div>
          <label>
            Data de ida
            <input
              type="date"
              value={f.start}
              onChange={(e) => setF({ ...f, start: e.target.value })}
            />
          </label>
          <label>
            Data de volta
            <input
              type="date"
              min={f.start}
              value={f.end}
              onChange={(e) => setF({ ...f, end: e.target.value })}
            />
          </label>
        </div>
        <div>
          <label>
            Moeda do planejamento
            <select
              value={f.currency}
              onChange={(e) => setF({ ...f, currency: e.target.value })}
            >
              <option value="BRL">Real brasileiro (BRL)</option>
              <option value="USD">Dólar americano (USD)</option>
              <option value="EUR">Euro (EUR)</option>
              <option value="GBP">Libra esterlina (GBP)</option>
              <option value="ARS">Peso argentino (ARS)</option>
              <option value="CLP">Peso chileno (CLP)</option>
            </select>
          </label>
          <label>
            Reserva de contingência (%)
            <input
              type="number"
              min="0"
              max="100"
              value={f.contingencyPercent}
              onChange={(e) =>
                setF({ ...f, contingencyPercent: Number(e.target.value) })
              }
            />
          </label>
        </div>
        <div>
          <label>
            Viajantes (separados por vírgula)
            <input
              value={f.participantNames}
              onChange={(e) => setF({ ...f, participantNames: e.target.value })}
              placeholder="Ana, Bruno, Clara"
            />
          </label>
          <label>
            Teto previsto (opcional)
            <input
              type="number"
              min="0"
              value={f.budget}
              onChange={(e) => setF({ ...f, budget: Number(e.target.value) })}
              placeholder="R$ 0"
            />
          </label>
        </div>
        <button
          className="primary"
          disabled={!valid || !f.participantNames.trim()}
          onClick={() => {
            const names = f.participantNames
              .split(",")
              .map((name) => name.trim())
              .filter(Boolean);
            onSave({
              ...f,
              participantNames: undefined,
              travelers: names.length,
              participants: names.map((name, index) => ({
                id: initial?.participants?.[index]?.id || newId(),
                name,
              })),
            });
          }}
        >
          {initial ? "Salvar alterações" : "Criar meu planejamento"}
        </button>
      </div>
    </Modal>
  );
}
let lastGeocodeRequest = 0;
function PlaceSearch({ value, destination, selected, onChange, onSelect }) {
  const [results, setResults] = useState([]),
    [loading, setLoading] = useState(false),
    [error, setError] = useState("");
  const search = async () => {
    const query = value.trim();
    if (query.length < 3) {
      setError("Digite ao menos 3 caracteres.");
      return;
    }
    const cacheKey = `${query}|${destination}`.toLowerCase(),
      cache = JSON.parse(
        localStorage.getItem("tripnext-geocode-cache") || "{}",
      );
    if (cache[cacheKey]) {
      setResults(cache[cacheKey]);
      setError("");
      return;
    }
    if (Date.now() - lastGeocodeRequest < 1100) {
      setError("Aguarde um instante antes de buscar novamente.");
      return;
    }
    setLoading(true);
    setError("");
    lastGeocodeRequest = Date.now();
    try {
      const params = new URLSearchParams({
        q: `${query}, ${destination}`,
        format: "jsonv2",
        limit: "5",
        addressdetails: "1",
        "accept-language": "pt-BR",
      });
      const response = await fetch(
        `https://nominatim.openstreetmap.org/search?${params}`,
      );
      if (!response.ok)
        throw new Error(`Busca indisponível (${response.status})`);
      const places = (await response.json()).map((item) => ({
        placeId: `osm:${item.osm_type}:${item.osm_id}`,
        displayName: item.display_name,
        latitude: Number(item.lat),
        longitude: Number(item.lon),
        category: item.type || item.category,
      }));
      cache[cacheKey] = places;
      localStorage.setItem("tripnext-geocode-cache", JSON.stringify(cache));
      setResults(places);
      if (!places.length)
        setError(
          "Nenhum lugar encontrado. Você ainda pode salvar o endereço digitado.",
        );
    } catch (cause) {
      setError(cause.message || "Não foi possível buscar agora.");
    } finally {
      setLoading(false);
    }
  };
  return (
    <div className="place-search">
      <label>
        Local / endereço
        <div className="place-input">
          <input
            value={value}
            onChange={(e) => {
              onChange(e.target.value);
              setResults([]);
            }}
            placeholder="Aeroporto, hotel ou ponto de encontro"
          />
          <button type="button" onClick={search} disabled={loading}>
            {loading ? "Buscando…" : "Buscar lugar"}
          </button>
        </div>
      </label>
      {selected && (
        <small className="place-confirmed">
          <CheckCircle2 /> Local confirmado no mapa
        </small>
      )}
      {error && <small className="place-error">{error}</small>}
      {results.length > 0 && (
        <div className="place-results">
          {results.map((place) => (
            <button
              type="button"
              key={place.placeId}
              onClick={() => {
                onSelect(place);
                setResults([]);
              }}
            >
              <MapPin />
              <span>
                <b>{place.displayName.split(",")[0]}</b>
                <small>{place.displayName}</small>
              </span>
            </button>
          ))}
        </div>
      )}
      <small className="osm-credit">
        Busca por{" "}
        <a
          href="https://www.openstreetmap.org/copyright"
          target="_blank"
          rel="noreferrer"
        >
          OpenStreetMap/Nominatim
        </a>
      </small>
    </div>
  );
}
function EventModal({ trip, initial, onClose, onSave }) {
  const [f, setF] = useState(
    initial || {
      type: "transporte",
      status: "pesquisar",
      title: "",
      location: "",
      latitude: null,
      longitude: null,
      placeId: "",
      date: trip.start,
      time: "09:00",
      duration: "",
      cost: "",
      costMin: "",
      costMax: "",
      costCurrency: trip.currency || "BRL",
      exchangeRate: 1,
      quoteDate: "",
      costScope: "group",
      costClass: "daily",
      participantIds: [],
      city: "",
      booking: "",
      link: "",
      notes: "",
    },
  );
  const set = (k, v) => setF((x) => ({ ...x, [k]: v }));
  return (
    <Modal
      title={initial ? "Editar item" : "Adicionar ao roteiro"}
      close={onClose}
    >
      <div className="fields">
        <div>
          <label>
            Tipo
            <select
              value={f.type}
              onChange={(e) => set("type", e.target.value)}
            >
              {Object.entries(TYPES).map(([id, t]) => (
                <option value={id} key={id}>
                  {t.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Situação
            <select
              value={f.status || "pesquisar"}
              onChange={(e) => set("status", e.target.value)}
            >
              <option value="pesquisar">Ainda pesquisando</option>
              <option value="reservar">Precisa reservar</option>
              <option value="reservado">Reservado</option>
              <option value="gratuito">Não precisa reservar</option>
            </select>
          </label>
        </div>
        <label>
          O que está planejando?
          <input
            value={f.title}
            onChange={(e) => set("title", e.target.value)}
            placeholder="Voo para Buenos Aires, Museu, jantar..."
          />
        </label>
        <PlaceSearch
          value={f.location || ""}
          destination={trip.destination}
          selected={Number.isFinite(f.latitude) && Number.isFinite(f.longitude)}
          onChange={(location) =>
            setF((x) => ({
              ...x,
              location,
              latitude: null,
              longitude: null,
              placeId: "",
            }))
          }
          onSelect={(place) =>
            setF((x) => ({
              ...x,
              location: place.displayName,
              latitude: place.latitude,
              longitude: place.longitude,
              placeId: place.placeId,
            }))
          }
        />
        <div>
          <label>
            Data
            <input
              type="date"
              min={trip.start}
              max={trip.end}
              value={f.date}
              onChange={(e) => set("date", e.target.value)}
            />
          </label>
          <label>
            Horário
            <input
              type="time"
              value={f.time || ""}
              onChange={(e) => set("time", e.target.value)}
            />
          </label>
        </div>
        <div>
          <label>
            Duração (minutos)
            <input
              type="number"
              min="0"
              value={f.duration || ""}
              onChange={(e) => set("duration", e.target.value)}
            />
          </label>
          <label>
            Custo esperado
            <input
              type="number"
              min="0"
              value={f.cost || ""}
              onChange={(e) => set("cost", e.target.value)}
              placeholder="0"
            />
          </label>
        </div>
        <div>
          <label>
            Estimativa mínima
            <input
              type="number"
              min="0"
              value={f.costMin ?? ""}
              onChange={(e) => set("costMin", e.target.value)}
              placeholder={f.cost || "0"}
            />
          </label>
          <label>
            Estimativa máxima
            <input
              type="number"
              min="0"
              value={f.costMax ?? ""}
              onChange={(e) => set("costMax", e.target.value)}
              placeholder={f.cost || "0"}
            />
          </label>
        </div>
        <div>
          <label>
            Moeda original
            <select
              value={f.costCurrency || trip.currency}
              onChange={(e) => set("costCurrency", e.target.value)}
            >
              {[trip.currency, "BRL", "USD", "EUR", "GBP", "ARS", "CLP"]
                .filter((x, i, a) => a.indexOf(x) === i)
                .map((currency) => (
                  <option key={currency}>{currency}</option>
                ))}
            </select>
          </label>
          <label>
            Cotação para {trip.currency}
            <input
              type="number"
              min="0"
              step="0.0001"
              value={f.exchangeRate ?? 1}
              onChange={(e) => set("exchangeRate", e.target.value)}
            />
          </label>
        </div>
        <div>
          <label>
            Data da cotação
            <input
              type="date"
              value={f.quoteDate || ""}
              onChange={(e) => set("quoteDate", e.target.value)}
            />
          </label>
          <label>
            Abrangência
            <select
              value={f.costScope || "group"}
              onChange={(e) => set("costScope", e.target.value)}
            >
              <option value="group">Total do grupo</option>
              <option value="person">Por pessoa</option>
            </select>
          </label>
        </div>
        <div>
          <label>
            Tipo de custo
            <select
              value={f.costClass || "daily"}
              onChange={(e) => set("costClass", e.target.value)}
            >
              <option value="daily">Custo diário</option>
              <option value="fixed">Custo fixo da viagem</option>
            </select>
          </label>
          <label>
            Cidade
            <input
              value={f.city || ""}
              onChange={(e) => set("city", e.target.value)}
              placeholder="Buenos Aires"
            />
          </label>
        </div>
        <label>
          Viajantes deste custo
          <select
            multiple
            value={f.participantIds || []}
            onChange={(e) =>
              set(
                "participantIds",
                Array.from(e.target.selectedOptions, (option) => option.value),
              )
            }
          >
            {(trip.participants || []).map((participant) => (
              <option value={participant.id} key={participant.id}>
                {participant.name}
              </option>
            ))}
          </select>
          <small>
            Sem seleção, o custo vale para todos. Use Ctrl/Cmd para selecionar
            mais de uma pessoa.
          </small>
        </label>
        <label>
          Reserva / confirmação
          <input
            value={f.booking || ""}
            onChange={(e) => set("booking", e.target.value)}
            placeholder="Código da reserva ou informação útil"
          />
        </label>
        <label>
          Link útil
          <input
            type="url"
            value={f.link || ""}
            onChange={(e) => set("link", e.target.value)}
            placeholder="https://..."
          />
        </label>
        <label>
          Observações
          <textarea
            value={f.notes || ""}
            onChange={(e) => set("notes", e.target.value)}
            placeholder="O que você precisa lembrar?"
          />
        </label>
        <button
          className="primary"
          disabled={!f.title || !f.date}
          onClick={() =>
            onSave({
              ...f,
              cost: Number(f.cost) || 0,
              costMin:
                f.costMin === "" ? Number(f.cost) || 0 : Number(f.costMin),
              costMax:
                f.costMax === "" ? Number(f.cost) || 0 : Number(f.costMax),
              exchangeRate: Number(f.exchangeRate) || 1,
              duration: Number(f.duration) || 0,
            })
          }
        >
          {initial ? "Salvar item" : "Adicionar ao plano"}
        </button>
      </div>
    </Modal>
  );
}
function TaskModal({ onClose, onSave }) {
  const [name, setName] = useState(""),
    [category, setCategory] = useState("Documentos");
  return (
    <Modal title="Nova tarefa" close={onClose}>
      <div className="fields">
        <label>
          O que precisa resolver?
          <input
            autoFocus
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Comprar seguro viagem"
          />
        </label>
        <label>
          Categoria
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          >
            <option>Documentos</option>
            <option>Reservas</option>
            <option>Mala</option>
            <option>Saúde</option>
            <option>Transporte</option>
            <option>Outros</option>
          </select>
        </label>
        <button
          className="primary"
          disabled={!name}
          onClick={() => onSave({ name, category, done: false })}
        >
          Adicionar tarefa
        </button>
      </div>
    </Modal>
  );
}
function IdeaModal({ onClose, onSave }) {
  const [f, setF] = useState({
      type: "passeio",
      title: "",
      location: "",
      cost: "",
      link: "",
      notes: "",
    }),
    set = (k, v) => setF((x) => ({ ...x, [k]: v }));
  return (
    <Modal title="Guardar uma ideia" close={onClose}>
      <div className="fields">
        <label>
          Tipo
          <select value={f.type} onChange={(e) => set("type", e.target.value)}>
            {Object.entries(TYPES).map(([id, t]) => (
              <option value={id} key={id}>
                {t.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Nome da ideia
          <input
            autoFocus
            value={f.title}
            onChange={(e) => set("title", e.target.value)}
            placeholder="Museu, restaurante, bate-volta..."
          />
        </label>
        <label>
          Local
          <input
            value={f.location}
            onChange={(e) => set("location", e.target.value)}
            placeholder="Bairro, cidade ou endereço"
          />
        </label>
        <label>
          Preço estimado
          <input
            type="number"
            min="0"
            value={f.cost}
            onChange={(e) => set("cost", e.target.value)}
          />
        </label>
        <label>
          Link da pesquisa
          <input
            type="url"
            value={f.link}
            onChange={(e) => set("link", e.target.value)}
            placeholder="https://..."
          />
        </label>
        <label>
          Por que salvar?
          <textarea
            value={f.notes}
            onChange={(e) => set("notes", e.target.value)}
            placeholder="Detalhes, prós, restrições..."
          />
        </label>
        <button
          className="primary"
          disabled={!f.title}
          onClick={() => onSave({ ...f, cost: Number(f.cost) || 0 })}
        >
          Guardar ideia
        </button>
      </div>
    </Modal>
  );
}
function OptionModal({ trip, onClose, onSave }) {
  const [f, setF] = useState({
      decision: "",
      kind: "transporte",
      title: "",
      provider: "",
      location: "",
      origin: "",
      destination: "",
      departAt: "",
      arriveAt: "",
      stops: "",
      roomType: "",
      nights: "",
      duration: "",
      price: "",
      costMin: "",
      costMax: "",
      costCurrency: "BRL",
      exchangeRate: 1,
      quoteDate: new Date().toISOString().slice(0, 10),
      costScope: "group",
      costClass: "fixed",
      participantIds: [],
      bookingDeadline: "",
      cancellationDeadline: "",
      cancellation: "",
      baggage: "",
      pros: "",
      cons: "",
      url: "",
    }),
    set = (key, value) => setF((x) => ({ ...x, [key]: value }));
  return (
    <Modal title="Nova alternativa" close={onClose}>
      <div className="fields">
        <label>
          O que você está decidindo?
          <input
            autoFocus
            value={f.decision}
            onChange={(e) => set("decision", e.target.value)}
            placeholder="Voo São Paulo → Buenos Aires"
          />
        </label>
        <div>
          <label>
            Modalidade
            <select
              value={f.kind}
              onChange={(e) => set("kind", e.target.value)}
            >
              <option value="transporte">Transporte</option>
              <option value="hospedagem">Hospedagem</option>
              <option value="passeio">Passeio</option>
            </select>
          </label>
          <label>
            Preço total previsto
            <input
              type="number"
              min="0"
              value={f.price}
              onChange={(e) => set("price", e.target.value)}
              placeholder="0"
            />
          </label>
        </div>
        <div>
          <label>
            Preço mínimo
            <input
              type="number"
              min="0"
              value={f.costMin}
              onChange={(e) => set("costMin", e.target.value)}
              placeholder={f.price || "0"}
            />
          </label>
          <label>
            Preço máximo
            <input
              type="number"
              min="0"
              value={f.costMax}
              onChange={(e) => set("costMax", e.target.value)}
              placeholder={f.price || "0"}
            />
          </label>
        </div>
        <div>
          <label>
            Moeda original
            <select
              value={f.costCurrency}
              onChange={(e) => set("costCurrency", e.target.value)}
            >
              {["BRL", "USD", "EUR", "GBP", "ARS", "CLP"].map((currency) => (
                <option key={currency}>{currency}</option>
              ))}
            </select>
          </label>
          <label>
            Cotação para a moeda da viagem
            <input
              type="number"
              min="0"
              step="0.0001"
              value={f.exchangeRate}
              onChange={(e) => set("exchangeRate", e.target.value)}
            />
          </label>
        </div>
        <div>
          <label>
            Data da consulta
            <input
              type="date"
              value={f.quoteDate}
              onChange={(e) => set("quoteDate", e.target.value)}
            />
          </label>
          <label>
            Preço informado
            <select
              value={f.costScope}
              onChange={(e) => set("costScope", e.target.value)}
            >
              <option value="group">Total do grupo</option>
              <option value="person">Por pessoa</option>
            </select>
          </label>
        </div>
        <label>
          Viajantes desta opção
          <select
            multiple
            value={f.participantIds}
            onChange={(e) =>
              set(
                "participantIds",
                Array.from(e.target.selectedOptions, (option) => option.value),
              )
            }
          >
            {(trip.participants || []).map((participant) => (
              <option value={participant.id} key={participant.id}>
                {participant.name}
              </option>
            ))}
          </select>
          <small>
            Sem seleção, a alternativa considera todos os viajantes.
          </small>
        </label>
        <div>
          <label>
            Prazo para reservar
            <input
              type="date"
              value={f.bookingDeadline}
              onChange={(e) => set("bookingDeadline", e.target.value)}
            />
          </label>
          <label>
            Cancelamento grátis até
            <input
              type="date"
              value={f.cancellationDeadline}
              onChange={(e) => set("cancellationDeadline", e.target.value)}
            />
          </label>
        </div>
        {f.kind === "transporte" && (
          <>
            <div>
              <label>
                Origem
                <input
                  value={f.origin}
                  onChange={(e) => set("origin", e.target.value)}
                  placeholder="GRU"
                />
              </label>
              <label>
                Destino
                <input
                  value={f.destination}
                  onChange={(e) => set("destination", e.target.value)}
                  placeholder="EZE"
                />
              </label>
            </div>
            <div>
              <label>
                Saída
                <input
                  type="datetime-local"
                  value={f.departAt}
                  onChange={(e) => set("departAt", e.target.value)}
                />
              </label>
              <label>
                Chegada
                <input
                  type="datetime-local"
                  value={f.arriveAt}
                  onChange={(e) => set("arriveAt", e.target.value)}
                />
              </label>
            </div>
            <label>
              Número de paradas
              <input
                type="number"
                min="0"
                value={f.stops}
                onChange={(e) => set("stops", e.target.value)}
              />
            </label>
          </>
        )}
        {f.kind === "hospedagem" && (
          <div>
            <label>
              Tipo de quarto
              <input
                value={f.roomType}
                onChange={(e) => set("roomType", e.target.value)}
                placeholder="Duplo com varanda"
              />
            </label>
            <label>
              Número de diárias
              <input
                type="number"
                min="1"
                value={f.nights}
                onChange={(e) => set("nights", e.target.value)}
              />
            </label>
          </div>
        )}
        {f.kind === "passeio" && (
          <label>
            Duração prevista (minutos)
            <input
              type="number"
              min="0"
              value={f.duration}
              onChange={(e) => set("duration", e.target.value)}
            />
          </label>
        )}
        <label>
          Nome da opção
          <input
            value={f.title}
            onChange={(e) => set("title", e.target.value)}
            placeholder="LATAM direto 10h20"
          />
        </label>
        <div>
          <label>
            Fornecedor
            <input
              value={f.provider}
              onChange={(e) => set("provider", e.target.value)}
              placeholder="LATAM, Booking..."
            />
          </label>
          <label>
            Local
            <input
              value={f.location}
              onChange={(e) => set("location", e.target.value)}
              placeholder="Aeroporto ou endereço"
            />
          </label>
        </div>
        <label>
          Cancelamento
          <input
            value={f.cancellation}
            onChange={(e) => set("cancellation", e.target.value)}
            placeholder="Grátis até 10/01, não reembolsável..."
          />
        </label>
        <label>
          Bagagem ou itens incluídos
          <input
            value={f.baggage}
            onChange={(e) => set("baggage", e.target.value)}
            placeholder="1 mala de 23 kg, café da manhã..."
          />
        </label>
        <div>
          <label>
            Vantagens
            <textarea
              value={f.pros}
              onChange={(e) => set("pros", e.target.value)}
              placeholder="Direto, bom horário..."
            />
          </label>
          <label>
            Atenções
            <textarea
              value={f.cons}
              onChange={(e) => set("cons", e.target.value)}
              placeholder="Sem reembolso, conexão longa..."
            />
          </label>
        </div>
        <label>
          Link da fonte
          <input
            type="url"
            value={f.url}
            onChange={(e) => set("url", e.target.value)}
            placeholder="https://..."
          />
        </label>
        <button
          className="primary"
          disabled={!f.decision || !f.title}
          onClick={() =>
            onSave({
              ...f,
              price: Number(f.price) || 0,
              costMin:
                f.costMin === "" ? Number(f.price) || 0 : Number(f.costMin),
              costMax:
                f.costMax === "" ? Number(f.price) || 0 : Number(f.costMax),
              exchangeRate: Number(f.exchangeRate) || 1,
              stops: f.stops === "" ? "" : Number(f.stops),
              nights: Number(f.nights) || 0,
              duration: Number(f.duration) || 0,
            })
          }
        >
          Adicionar para comparar
        </button>
      </div>
    </Modal>
  );
}
function Modal({ title, close, children }) {
  return (
    <div
      className="overlay"
      onMouseDown={(e) => e.target === e.currentTarget && close()}
    >
      <div className="modal">
        <div className="modal-head">
          <h2>{title}</h2>
          <button onClick={close}>×</button>
        </div>
        {children}
      </div>
    </div>
  );
}
function sortEvents(a, b) {
  return `${a.date}${String(a.sortOrder ?? 0).padStart(4, "0")}${a.time || ""}`.localeCompare(
    `${b.date}${String(b.sortOrder ?? 0).padStart(4, "0")}${b.time || ""}`,
  );
}
function statusLabel(status) {
  return {
    pesquisar: "Pesquisando",
    reservar: "Reservar",
    reservado: "Reservado",
    gratuito: "Sem reserva",
  }[status || "pesquisar"];
}
function newId() {
  return (
    globalThis.crypto?.randomUUID?.() ||
    `trip-${Date.now()}-${Math.random().toString(36).slice(2)}`
  );
}
function loadStore() {
  return migrateStoredData(
    localStorage.getItem("tripnext-store"),
    localStorage.getItem("tripnext-trip"),
    newId,
  );
}
function exportCalendar(trip) {
  const esc = (v) =>
      String(v || "")
        .replaceAll("\\", "\\\\")
        .replaceAll("\n", "\\n")
        .replaceAll(",", "\\,")
        .replaceAll(";", "\\;"),
    stamp = (d) =>
      d
        .toISOString()
        .replaceAll("-", "")
        .replaceAll(":", "")
        .replace(/\.\d{3}Z$/, "Z"),
    local = (d) =>
      `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, "0")}${String(d.getDate()).padStart(2, "0")}T${String(d.getHours()).padStart(2, "0")}${String(d.getMinutes()).padStart(2, "0")}00`;
  const body = (trip.itinerary || [])
    .sort(sortEvents)
    .map((e, i) => {
      const start = new Date(`${e.date}T${e.time || "09:00"}:00`),
        end = new Date(start.getTime() + (Number(e.duration) || 60) * 60000);
      return [
        "BEGIN:VEVENT",
        `UID:tripnext-${e.date}-${i}@tripnext`,
        `DTSTAMP:${stamp(new Date())}`,
        `DTSTART:${local(start)}`,
        `DTEND:${local(end)}`,
        `SUMMARY:${esc(e.title)}`,
        `LOCATION:${esc(e.location)}`,
        `DESCRIPTION:${esc([e.notes, e.booking, e.link].filter(Boolean).join(" | "))}`,
        "END:VEVENT",
      ].join("\r\n");
    })
    .join("\r\n");
  const file = [
      "BEGIN:VCALENDAR",
      "VERSION:2.0",
      "PRODID:-//TripNext//Planejador//PT",
      "CALSCALE:GREGORIAN",
      body,
      "END:VCALENDAR",
    ].join("\r\n"),
    url = URL.createObjectURL(
      new Blob([file], { type: "text/calendar;charset=utf-8" }),
    ),
    a = document.createElement("a");
  a.href = url;
  a.download = `${trip.name.replace(/[^a-z0-9]+/gi, "-").toLowerCase()}-roteiro.ics`;
  a.click();
  URL.revokeObjectURL(url);
}
createRoot(document.getElementById("root")).render(<App />);
