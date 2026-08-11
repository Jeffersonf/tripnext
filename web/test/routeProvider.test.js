import test from 'node:test';
import assert from 'node:assert/strict';
import {fetchDrivingRoute,routeCacheKey} from '../src/routeProvider.js';

const points=[{latitude:-23.55,longitude:-46.63},{latitude:-23.56,longitude:-46.65}];
const memory=()=>{const values={};return {getItem:key=>values[key]||null,setItem:(key,value)=>values[key]=value}};

test('monta chave estável com coordenadas arredondadas',()=>{assert.equal(routeCacheKey(points),'driving:-46.63000,-23.55000;-46.65000,-23.56000');});
test('normaliza rota OSRM e reaproveita cache',async()=>{let calls=0;const storage=memory(),fetcher=async()=>{calls++;return {ok:true,json:async()=>({code:'Ok',routes:[{distance:3500,duration:600,geometry:{coordinates:[[-46.63,-23.55],[-46.65,-23.56]]},legs:[{distance:3500,duration:600}]}]})}};const first=await fetchDrivingRoute(points,{fetcher,storage,baseUrl:'https://routes.test'}),second=await fetchDrivingRoute(points,{fetcher,storage,baseUrl:'https://routes.test'});assert.equal(first.distanceKm,3.5);assert.equal(first.durationMinutes,10);assert.deepEqual(first.geometry[0],[-23.55,-46.63]);assert.equal(second.cached,true);assert.equal(calls,1);});
