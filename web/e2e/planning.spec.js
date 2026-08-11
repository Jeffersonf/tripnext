import {test,expect} from '@playwright/test';

test.beforeEach(async({page})=>{await page.addInitScript(()=>localStorage.clear());await page.goto('./');});

test('cria viagem, guarda ideia e transforma em roteiro',async({page})=>{
  await page.route('https://nominatim.openstreetmap.org/search?**',route=>route.fulfill({contentType:'application/json',body:JSON.stringify([{osm_type:'node',osm_id:123,display_name:'MALBA, Palermo, Buenos Aires, Argentina',lat:'-34.5764',lon:'-58.4033',type:'museum'}])}));
  await page.getByRole('button',{name:'Começar a planejar'}).click();
  await page.getByLabel('Nome do plano').fill('Buenos Aires em família');
  await page.getByLabel('Para onde você vai?').fill('Buenos Aires, Argentina');
  await page.getByLabel('Data de ida').fill('2027-01-10');
  await page.getByLabel('Data de volta').fill('2027-01-13');
  await page.getByRole('button',{name:'Criar meu planejamento'}).click();
  await expect(page.getByRole('heading',{name:'Buenos Aires em família'})).toBeVisible();
  await page.getByRole('button',{name:'Ideias'}).click();
  await page.getByRole('button',{name:'Nova ideia'}).click();
  await page.getByLabel('Nome da ideia').fill('Museu de Arte Latino-Americana');
  await page.getByLabel('Local').fill('MALBA');
  await page.getByLabel('Preço estimado').fill('75');
  await page.getByRole('button',{name:'Guardar ideia'}).click();
  await page.getByRole('button',{name:'Colocar no roteiro'}).click();
  await page.getByRole('button',{name:'Buscar lugar'}).click();
  await page.getByRole('button',{name:/MALBA/}).click();
  await page.getByRole('button',{name:'Salvar item'}).click();
  await page.getByRole('button',{name:'Roteiro'}).click();
  await expect(page.getByRole('heading',{name:'Museu de Arte Latino-Americana'})).toBeVisible();
  await expect(page.getByText(/75,00/)).toBeVisible();
  await expect(page.locator('.leaflet-container')).toBeVisible();
  await page.getByRole('button',{name:'Duplicar dia'}).click();
  await page.getByRole('button',{name:'Duplicar planejamento'}).click();
  await expect(page.getByRole('heading',{name:'Museu de Arte Latino-Americana'})).toBeVisible();
});

test('mantém duas viagens independentes',async({page})=>{
  await page.getByRole('button',{name:'Começar a planejar'}).click();
  await page.getByLabel('Nome do plano').fill('Viagem A');
  await page.getByLabel('Para onde você vai?').fill('Curitiba');
  await page.getByLabel('Data de ida').fill('2027-02-01');
  await page.getByLabel('Data de volta').fill('2027-02-03');
  await page.getByRole('button',{name:'Criar meu planejamento'}).click();
  await page.getByRole('button',{name:'Nova viagem'}).click();
  await page.getByLabel('Nome do plano').fill('Viagem B');
  await page.getByLabel('Para onde você vai?').fill('Recife');
  await page.getByLabel('Data de ida').fill('2027-03-01');
  await page.getByLabel('Data de volta').fill('2027-03-04');
  await page.getByRole('button',{name:'Criar meu planejamento'}).click();
  await expect(page.locator('.trip-switcher select option')).toHaveCount(2);
  await page.locator('.trip-switcher select').selectOption({label:'Viagem A'});
  await expect(page.getByRole('heading',{name:'Viagem A'})).toBeVisible();
});
