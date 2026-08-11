# TripNext

Aplicativo Android nativo para organização e planejamento de viagens. A primeira fase funciona offline com Room; o contrato `TripRemoteRepository` e a fila deduplicada deixam a sincronização pronta para uma API futura.

## Ambiente

- Android Studio / SDK 36
- JDK 17 ou superior
- `./gradlew test` para testes JVM
- `./gradlew assembleDebug` para gerar o APK de desenvolvimento

O tema **Embarque** foi inspirado no protótipo fornecido: fundo azul-noturno, cartões de viagem como cartão de embarque, rosa para ações e verde-água para progresso.
