const idFallback=()=>`item-${Date.now()}-${Math.random().toString(36).slice(2)}`;

export function normalizeTrip(trip,idFactory=idFallback,now=new Date().toISOString()){
  return {
    name:'',destination:'',start:'',end:'',travelers:1,budget:0,checklist:[],ideas:[],itinerary:[],
    ...trip,
    id:trip?.id||idFactory(),createdAt:trip?.createdAt||now,updatedAt:trip?.updatedAt||now,
    itinerary:(trip?.itinerary||[]).map((item,index)=>({...item,id:item.id||idFactory(),sortOrder:Number.isFinite(item.sortOrder)?item.sortOrder:index})),
    ideas:(trip?.ideas||[]).map(item=>({...item,id:item.id||idFactory()})),
    checklist:trip?.checklist||[]
  };
}

export function migrateStoredData(storeText,legacyText,idFactory=idFallback,now=new Date().toISOString()){
  try{
    const saved=storeText?JSON.parse(storeText):null;
    if(Array.isArray(saved?.trips)){
      const trips=saved.trips.map(t=>normalizeTrip(t,idFactory,now));
      const active=trips.some(t=>t.id===saved.activeTripId&&!t.archived)?saved.activeTripId:trips.find(t=>!t.archived)?.id||null;
      return {version:3,trips,activeTripId:active};
    }
    const legacy=legacyText?JSON.parse(legacyText):null;
    if(legacy){const trip=normalizeTrip(legacy,idFactory,now);return {version:3,trips:[trip],activeTripId:trip.id};}
  }catch{}
  return {version:3,trips:[],activeTripId:null};
}

export function sortPlanItems(items){return [...items].sort((a,b)=>a.date.localeCompare(b.date)||(Number(a.sortOrder)||0)-(Number(b.sortOrder)||0)||(a.time||'').localeCompare(b.time||''));}

export function dayPart(time){const hour=Number((time||'12:00').split(':')[0]);return hour<12?'Manhã':hour<18?'Tarde':'Noite';}

export function findConflictIds(items){
  const conflicts=new Set();
  const timed=items.filter(x=>x.date&&x.time&&Number(x.duration)>0);
  for(let i=0;i<timed.length;i++)for(let j=i+1;j<timed.length;j++){
    if(timed[i].date!==timed[j].date)continue;
    const startA=minutes(timed[i].time),endA=startA+Number(timed[i].duration),startB=minutes(timed[j].time),endB=startB+Number(timed[j].duration);
    if(startA<endB&&startB<endA){conflicts.add(timed[i].id);conflicts.add(timed[j].id);}
  }
  return conflicts;
}

export function movePlanItem(items,itemId,targetDate,targetIndex){
  const moving=items.find(x=>x.id===itemId);if(!moving)return items;
  const others=items.filter(x=>x.id!==itemId),target=sortPlanItems(others.filter(x=>x.date===targetDate));
  target.splice(Math.max(0,Math.min(targetIndex,target.length)),0,{...moving,date:targetDate});
  const order=new Map(target.map((x,index)=>[x.id,index]));
  return others.filter(x=>x.date!==targetDate).concat(target).map(x=>order.has(x.id)?{...x,sortOrder:order.get(x.id)}:x);
}

function minutes(time){const [h,m]=time.split(':').map(Number);return h*60+m;}
