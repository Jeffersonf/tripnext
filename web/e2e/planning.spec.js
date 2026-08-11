import { test, expect } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => localStorage.clear());
  await page.goto("./");
});

test("cria viagem, guarda ideia e transforma em roteiro", async ({ page }) => {
  let geocodeCount = 0;
  await page.route("https://nominatim.openstreetmap.org/search?**", (route) => {
    const cafe = geocodeCount++ > 0;
    return route.fulfill({
      contentType: "application/json",
      body: JSON.stringify([
        {
          osm_type: "node",
          osm_id: cafe ? 456 : 123,
          display_name: cafe
            ? "Café Palermo, Buenos Aires, Argentina"
            : "MALBA, Palermo, Buenos Aires, Argentina",
          lat: cafe ? "-34.5810" : "-34.5764",
          lon: cafe ? "-58.4210" : "-58.4033",
          type: cafe ? "cafe" : "museum",
        },
      ]),
    });
  });
  await page.route(
    "https://router.project-osrm.org/route/v1/driving/**",
    (route) =>
      route.fulfill({
        contentType: "application/json",
        body: JSON.stringify({
          code: "Ok",
          routes: [
            {
              distance: 2400,
              duration: 420,
              geometry: {
                coordinates: [
                  [-58.4033, -34.5764],
                  [-58.412, -34.579],
                  [-58.421, -34.581],
                ],
              },
              legs: [{ distance: 2400, duration: 420 }],
            },
          ],
        }),
      }),
  );
  await page.getByRole("button", { name: "Começar a planejar" }).click();
  await page.getByLabel("Nome do plano").fill("Buenos Aires em família");
  await page.getByLabel("Para onde você vai?").fill("Buenos Aires, Argentina");
  await page.getByLabel("Data de ida").fill("2027-01-10");
  await page.getByLabel("Data de volta").fill("2027-01-13");
  await page.getByRole("button", { name: "Criar meu planejamento" }).click();
  await expect(
    page.getByRole("heading", { name: "Buenos Aires em família" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Ideias" }).click();
  await page.getByRole("button", { name: "Nova ideia" }).click();
  await page.getByLabel("Nome da ideia").fill("Museu de Arte Latino-Americana");
  await page.getByLabel("Local").fill("MALBA");
  await page.getByLabel("Preço estimado").fill("75");
  await page.getByRole("button", { name: "Guardar ideia" }).click();
  await page.getByRole("button", { name: "Colocar no roteiro" }).click();
  await page.getByRole("button", { name: "Buscar lugar" }).click();
  await page.getByRole("button", { name: /MALBA/ }).click();
  await page.getByRole("button", { name: "Salvar item" }).click();
  await page.getByRole("button", { name: "Roteiro", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Museu de Arte Latino-Americana" }),
  ).toBeVisible();
  await expect(page.getByText(/75,00/)).toBeVisible();
  await expect(page.locator(".leaflet-container")).toBeVisible();
  await page.getByRole("button", { name: "Adicionar", exact: true }).click();
  await page.getByLabel("O que está planejando?").fill("Café da tarde");
  await page.getByLabel("Local / endereço").fill("Café Palermo");
  await page.waitForTimeout(1200);
  await page.getByRole("button", { name: "Buscar lugar" }).click();
  await page.getByRole("button", { name: /Café Palermo/ }).click();
  await page.getByRole("button", { name: "Adicionar ao plano" }).click();
  await page.getByRole("button", { name: "Carro" }).click();
  await page.getByRole("button", { name: "Calcular rota real" }).click();
  await expect(page.getByText(/Rota viária OSRM/)).toBeVisible();
  await expect(page.getByText("O horário pode não fechar")).toBeVisible();
  await expect(
    page.getByRole("link", { name: /Abrir trajeto/ }),
  ).toHaveAttribute("href", /google\.com\/maps\/dir/);
  await page.getByRole("button", { name: "Duplicar dia" }).click();
  await page.getByRole("button", { name: "Duplicar planejamento" }).click();
  await expect(
    page.getByRole("heading", { name: "Museu de Arte Latino-Americana" }),
  ).toBeVisible();
});

test("mantém duas viagens independentes", async ({ page }) => {
  await page.getByRole("button", { name: "Começar a planejar" }).click();
  await page.getByLabel("Nome do plano").fill("Viagem A");
  await page.getByLabel("Para onde você vai?").fill("Curitiba");
  await page.getByLabel("Data de ida").fill("2027-02-01");
  await page.getByLabel("Data de volta").fill("2027-02-03");
  await page.getByRole("button", { name: "Criar meu planejamento" }).click();
  await page.getByRole("button", { name: "Nova viagem" }).click();
  await page.getByLabel("Nome do plano").fill("Viagem B");
  await page.getByLabel("Para onde você vai?").fill("Recife");
  await page.getByLabel("Data de ida").fill("2027-03-01");
  await page.getByLabel("Data de volta").fill("2027-03-04");
  await page.getByRole("button", { name: "Criar meu planejamento" }).click();
  await expect(page.locator(".trip-switcher select option")).toHaveCount(2);
  await page
    .locator(".trip-switcher select")
    .selectOption({ label: "Viagem A" });
  await expect(page.getByRole("heading", { name: "Viagem A" })).toBeVisible();
});

test("compara alternativas, escolhe uma e leva ao roteiro", async ({
  page,
}) => {
  await page.getByRole("button", { name: "Começar a planejar" }).click();
  await page.getByLabel("Nome do plano").fill("Férias");
  await page.getByLabel("Para onde você vai?").fill("Rio de Janeiro");
  await page.getByLabel("Data de ida").fill("2027-04-01");
  await page.getByLabel("Data de volta").fill("2027-04-03");
  await page.getByLabel("Viajantes (separados por vírgula)").fill("Ana, Bia");
  await page.getByRole("button", { name: "Criar meu planejamento" }).click();
  await page.getByRole("button", { name: "Comparar" }).click();
  for (const [title, price, room, nights] of [
    ["Hotel Praia", 1200, "Suíte vista mar", 2],
    ["Hotel Centro", 900, "Quarto duplo", 2],
  ]) {
    await page.getByRole("button", { name: /alternativa/i }).click();
    await page
      .getByLabel("O que você está decidindo?")
      .fill("Hospedagem no Rio");
    await page.getByLabel("Modalidade").selectOption("hospedagem");
    await page.getByLabel("Preço total previsto").fill(String(price));
    await page.getByLabel("Preço mínimo").fill(String(price - 100));
    await page.getByLabel("Preço máximo").fill(String(price + 100));
    await page.getByLabel("Preço informado").selectOption("person");
    await page
      .getByLabel("Viajantes desta opção")
      .selectOption({ label: "Ana" });
    await page.getByLabel("Prazo para reservar").fill("2027-03-20");
    await page.getByLabel("Nome da opção").fill(title);
    await page.getByLabel("Tipo de quarto").fill(room);
    await page.getByLabel("Número de diárias").fill(String(nights));
    await page.getByRole("button", { name: "Adicionar para comparar" }).click();
  }
  const chosenCard = page
    .getByRole("heading", { name: "Hotel Centro" })
    .locator('xpath=ancestor::section[contains(@class,"option-card")]');
  await expect(chosenCard.getByText("Menor preço")).toBeVisible();
  await expect(chosenCard.getByText("Quarto duplo")).toBeVisible();
  await expect(
    chosenCard.getByText(/R\$\s*800,00 a R\$\s*1\.000,00/),
  ).toBeVisible();
  await expect(chosenCard.getByText("Diárias", { exact: true })).toBeVisible();
  await expect(
    chosenCard.locator("dd").getByText("2", { exact: true }),
  ).toBeVisible();
  await expect(chosenCard.getByText(/Reservar até/)).toBeVisible();
  await chosenCard.getByRole("button", { name: "Escolher" }).click();
  await expect(
    chosenCard.getByRole("button", { name: "Escolhida" }),
  ).toBeVisible();
  page.once("dialog", (dialog) => dialog.accept("850"));
  await chosenCard.getByRole("button", { name: "Atualizar preço" }).click();
  await expect(chosenCard.getByText(/Caiu R\$\s*50,00/)).toBeVisible();
  await chosenCard.getByRole("button", { name: "Levar ao roteiro" }).click();
  await page.getByRole("button", { name: "Salvar item" }).click();
  await page.getByRole("button", { name: "Roteiro", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Hotel Centro" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Custos previstos" }).click();
  await expect(
    page.getByText(/Faixa R\$\s*800,00 — R\$\s*1\.000,00/),
  ).toBeVisible();
  await expect(page.getByText("Ana")).toBeVisible();
  await expect(page.getByText("Bia")).toBeVisible();
});

test("cria conta, sincroniza e recupera a viagem sem o armazenamento local", async ({
  page,
}) => {
  const email = `traveler-${Date.now()}-${Math.random().toString(16).slice(2)}@example.com`;
  await page.getByRole("button", { name: "Começar a planejar" }).click();
  await page.getByLabel("Nome do plano").fill("Viagem sincronizada");
  await page.getByLabel("Para onde você vai?").fill("Salvador");
  await page.getByLabel("Data de ida").fill("2027-06-01");
  await page.getByLabel("Data de volta").fill("2027-06-04");
  await page.getByRole("button", { name: "Criar meu planejamento" }).click();
  await page.getByRole("button", { name: "Ajustes" }).click();
  await page.getByRole("button", { name: "Entrar ou criar conta" }).click();
  await page.getByRole("button", { name: "Ainda não tenho conta" }).click();
  await page.getByLabel("Seu nome").fill("Viajante E2E");
  await page.getByLabel("E-mail").fill(email);
  await page.getByLabel("Senha").fill("senha-segura-123");
  await page.getByRole("button", { name: "Criar conta e entrar" }).click();
  await expect(page.getByText(/Conectado como Viajante E2E/)).toBeVisible();
  await page.getByRole("button", { name: "Sincronizar agora" }).click();
  await expect(page.getByText(/enviada\(s\)/)).toBeVisible();
  await page.evaluate(() => localStorage.removeItem("tripnext-store"));
  await page.reload();
  await page.getByRole("button", { name: "Buscar minhas viagens" }).click();
  await expect(
    page.getByRole("heading", { name: "Viagem sincronizada" }),
  ).toBeVisible();
});
