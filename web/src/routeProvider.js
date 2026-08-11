const defaultBaseUrl='https://router.project-osrm.org';

export function routeCacheKey(points,profile='driving'){
  return `${profile}:${points.map(p=>`${Number(p.longitude).toFixed(5)},${Number(p.latitude).toFixed(5)}`).join(';')}`;
}

export async function fetchDrivingRoute(points,{fetcher=fetch,storage=localStorage,baseUrl=import.meta.env.VITE_ROUTE_API_BASE||defaultBaseUrl}={}){
  if(points.length<2)throw new Error('Confirme pelo menos duas paradas no mapa.');
  const key=routeCacheKey(points),cache=readCache(storage);
  if(cache[key])return {...cache[key],cached:true};
  const coordinates=points.map(p=>`${p.longitude},${p.latitude}`).join(';');
  const response=await fetcher(`${baseUrl}/route/v1/driving/${coordinates}?overview=full&geometries=geojson&steps=false`);
  if(!response.ok)throw new Error(`Serviço de rotas indisponível (${response.status}).`);
  const payload=await response.json(),route=payload.routes?.[0];
  if(payload.code!=='Ok'||!route)throw new Error(payload.message||'Não foi possível calcular uma rota entre essas paradas.');
  const value={provider:'OSRM',profile:'driving',distanceKm:route.distance/1000,durationMinutes:Math.round(route.duration/60),geometry:route.geometry.coordinates.map(([longitude,latitude])=>[latitude,longitude]),legs:(route.legs||[]).map(leg=>({distanceKm:leg.distance/1000,durationMinutes:Math.round(leg.duration/60)})),fetchedAt:new Date().toISOString()};
  cache[key]=value;storage.setItem('tripnext-route-cache',JSON.stringify(cache));return value;
}

function readCache(storage){try{return JSON.parse(storage.getItem('tripnext-route-cache')||'{}')}catch{return {}}}
