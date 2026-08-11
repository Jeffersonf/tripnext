# TripNext

Planejador de viagens com cliente Android local-first, aplicação web e API própria. Room e `localStorage` mantêm o plano disponível offline; o backend PostgreSQL recebe operações idempotentes sem sobrescrever conflitos silenciosamente.

## Ambiente

- Android Studio / SDK 36
- JDK 17 ou superior
- `./gradlew test` para testes JVM
- `./gradlew assembleDebug` para gerar o APK de desenvolvimento
- `cd web && npm ci && npm test` para validar o planejador web
- `cd server && npm ci && npm test` para validar autenticação e sincronização

## Estrutura

- `app/`: Android Kotlin, Compose e Room.
- `web/`: planejador React/Vite publicado no GitHub Pages.
- `server/`: Express, PostgreSQL, migrations e OpenAPI. Veja [server/README.md](server/README.md).

O tema **Embarque** foi inspirado no protótipo fornecido: fundo azul-noturno, cartões de viagem como cartão de embarque, rosa para ações e verde-água para progresso.
