const typeToWeb = (value) => ({ FLIGHT: "transporte", CHECK_IN: "hospedagem", CHECK_OUT: "hospedagem", ACTIVITY: "passeio", RESTAURANT: "alimentacao", TRANSFER: "deslocamento", OTHER: "outro" })[value] || "outro";
const dateAtOffset = (start, offset) => { const date = new Date(`${start}T12:00:00Z`); date.setUTCDate(date.getUTCDate() + Number(offset || 0)); return date.toISOString().slice(0, 10); };
export const selectableProposalIds = (proposal) => [...(proposal?.itinerary || []), ...(proposal?.checklist || []), ...(proposal?.budgets || [])].filter(item => !["SKIP_DUPLICATE", "SKIP_UNCHANGED"].includes(item.action)).map(item => item.id);

export function applySelectedProposal(trip, proposal, selectedIds) {
  const selected = new Set(selectedIds), itinerary = [...(trip.itinerary || [])], checklist = [...(trip.checklist || [])], categoryBudgets = [...(trip.categoryBudgets || [])];
  for (const item of proposal.itinerary || []) if (selected.has(item.id)) {
    const index = itinerary.findIndex(value => value.id === item.targetId);
    if (item.action === "REMOVE") { if (index >= 0) itinerary.splice(index, 1); continue; }
    const next = { ...(index >= 0 ? itinerary[index] : {}), id: index >= 0 ? itinerary[index].id : item.id, title: item.title, type: typeToWeb(item.type), date: dateAtOffset(trip.start, item.dayOffset), time: item.time, location: item.location, cost: Number(item.estimatedCostMinor || 0) / 100, costMin: Number(item.estimatedCostMinor || 0) / 100, costMax: Number(item.estimatedCostMinor || 0) / 100, link: item.sourceUrl || "", notes: item.reason || "", quoteDate: proposal.generatedAt || new Date().toISOString(), status: "pesquisar", sortOrder: index >= 0 ? itinerary[index].sortOrder : itinerary.filter(value => value.date === dateAtOffset(trip.start, item.dayOffset)).length };
    if (index >= 0) itinerary[index] = next; else itinerary.push(next);
  }
  for (const item of proposal.checklist || []) if (selected.has(item.id)) {
    const index = checklist.findIndex(value => value.id === item.targetId);
    if (item.action === "REMOVE") { if (index >= 0) checklist.splice(index, 1); continue; }
    const next = { ...(index >= 0 ? checklist[index] : {}), id: index >= 0 ? checklist[index].id : item.id, name: item.name, category: item.category, done: index >= 0 ? checklist[index].done : false };
    if (index >= 0) checklist[index] = next; else checklist.push(next);
  }
  for (const item of proposal.budgets || []) if (selected.has(item.id)) {
    const next = { category: item.category, limitMinor: Math.round(Number(trip.budget || 0) * 100 * Number(item.percent || 0) / 100) }, index = categoryBudgets.findIndex(value => value.category === item.category);
    if (index >= 0) categoryBudgets[index] = next; else categoryBudgets.push(next);
  }
  return { ...trip, itinerary, checklist, categoryBudgets, updatedAt: new Date().toISOString() };
}
