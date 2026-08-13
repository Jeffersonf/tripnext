# TripNext API

Backend do planejamento local-first. O servidor autentica usuários, autoriza acesso por viagem e recebe operações offline com chave idempotente, controle de versão e tombstones.

## Executar localmente

1. Copie `.env.example` para `.env`, troque `AUTH_SECRET` e configure `GEMINI_API_KEY`. O `.env` é ignorado pelo Git.
2. Execute `docker compose up --build` nesta pasta.
3. Consulte `GET http://localhost:8787/health`.

Sem Docker, configure um PostgreSQL em `DATABASE_URL`, execute `npm ci`, `npm run migrate` e `npm start`. O processo aplica automaticamente as migrations antes de servir a API.

## Contrato e testes

- `openapi.yaml`: contrato HTTP versionado.
- `migrations/`: migrations PostgreSQL executadas uma única vez.
- `npm test`: autenticação, isolamento, idempotência, conflito, tombstone, OpenAPI e CORS.

O armazenamento em memória existe somente para testes. `src/server.js` sempre exige PostgreSQL e um `AUTH_SECRET` com pelo menos 32 caracteres.

## Copiloto de planejamento

`POST /api/ai/plan` exige sessão e acesso à viagem. O cliente envia o documento atual e recebe uma proposta estruturada e revisável; o endpoint nunca altera a viagem. A chave Gemini é lida exclusivamente de `GEMINI_API_KEY` no servidor e não é devolvida, registrada nem incluída no APK ou no JavaScript. Em produção, use o gerenciador de segredos da hospedagem em vez de arquivo `.env`.
