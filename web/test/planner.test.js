import test from 'node:test';
import assert from 'node:assert/strict';
import {migrateStoredData,findConflictIds,movePlanItem,sortPlanItems,dayPart} from '../src/planner.js';

test('migra uma viagem legada sem perder roteiro',()=>{let n=0;const data=migrateStoredData(null,JSON.stringify({name:'Chile',itinerary:[{title:'Voo',date:'2027-01-10',time:'08:00'}]}),()=>`id-${++n}`,'2026-08-11T00:00:00Z');assert.equal(data.version,3);assert.equal(data.trips[0].name,'Chile');assert.equal(data.trips[0].itinerary[0].title,'Voo');assert.ok(data.trips[0].itinerary[0].id);});
test('migra coleção v2 e preserva viagem ativa',()=>{const raw={version:2,activeTripId:'b',trips:[{id:'a',name:'A'},{id:'b',name:'B'}]};const data=migrateStoredData(JSON.stringify(raw),null);assert.equal(data.version,3);assert.equal(data.activeTripId,'b');assert.equal(data.trips.length,2);});
test('detecta sobreposição real e ignora eventos encostados',()=>{const ids=findConflictIds([{id:'a',date:'2027-01-01',time:'09:00',duration:90},{id:'b',date:'2027-01-01',time:'10:00',duration:60},{id:'c',date:'2027-01-01',time:'11:00',duration:30}]);assert.deepEqual([...ids].sort(),['a','b']);});
test('move item para outro dia na posição solicitada',()=>{const items=[{id:'a',date:'2027-01-01',sortOrder:0},{id:'b',date:'2027-01-02',sortOrder:0}];const moved=movePlanItem(items,'a','2027-01-02',1);assert.deepEqual(sortPlanItems(moved).map(x=>x.id),['b','a']);assert.equal(moved.find(x=>x.id==='a').date,'2027-01-02');});
test('classifica blocos do dia',()=>{assert.equal(dayPart('08:00'),'Manhã');assert.equal(dayPart('14:00'),'Tarde');assert.equal(dayPart('20:00'),'Noite');});
