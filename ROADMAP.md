# TripNext — Roadmap de produto e engenharia

Atualizado em **11 de agosto de 2026**.

## Visão

O TripNext deve levar uma viagem da ideia até o embarque. O produto principal não é um diário financeiro: é um espaço para descobrir lugares, comparar opções, decidir, organizar cada dia, guardar reservas e usar o plano durante a viagem.

### Promessa principal

> Em poucos minutos, transformar destino, datas, perfil e orçamento estimado em um roteiro viável, editável, compartilhável e disponível offline.

### Jornada que o produto precisa resolver

1. **Imaginar:** escolher destino, datas, viajantes, interesses e ritmo.
2. **Pesquisar:** encontrar lugares, passeios, hospedagens e transportes com dados atuais.
3. **Comparar:** salvar alternativas, preços previstos, vantagens e restrições.
4. **Decidir:** selecionar opções e marcar o que precisa ser reservado.
5. **Montar:** distribuir lugares por dia, visualizar no mapa e calcular deslocamentos.
6. **Confirmar:** centralizar reservas, documentos, códigos e prazos.
7. **Preparar:** concluir checklist, lembretes e pendências.
8. **Viajar:** consultar agenda, mapas, reservas e próximos passos mesmo offline.
9. **Reaproveitar:** duplicar o plano, registrar avaliações e compartilhar um guia.

## Direção de produto

### Prioridade

- Planejamento de roteiro e logística.
- Custos **previstos**, por opção, categoria, dia e viajante.
- Pesquisa que gera dados utilizáveis dentro do plano.
- Reservas, documentos e tarefas pré-viagem.
- Uso no celular durante a viagem.

### Fora do caminho crítico

- Registro diário de gastos realizados.
- Conciliação financeira e extratos.
- Controle de faturas ou contas bancárias.
- Rede social pública antes do planejador individual estar maduro.

## Referências de categoria

O Wanderlog é uma referência funcional, não visual. Seu produto oficial reúne roteiro e mapa, colaboração, importação de reservas, recomendações, checklists, orçamento, otimização de rota e acesso offline. O TripNext deve alcançar esse núcleo com identidade própria e maior foco em transformar pesquisa/IA em decisões gravadas no roteiro.

- [Visão geral do Wanderlog](https://wanderlog.com/)
- [Central de ajuda e organização das funcionalidades](https://help.wanderlog.com/hc/en-us)
- [Recursos offline, otimização e importação](https://wanderlog.com/pro)
- [Assistente de planejamento integrado ao roteiro](https://wanderlog.com/trip-plan-assistant)

## Métrica principal

**Viagens prontas para embarque por mês**: viagem futura com transporte de chegada, hospedagem, ao menos 60% dos dias planejados e nenhuma pendência crítica vencida.

Métricas auxiliares:

- tempo até o primeiro roteiro útil;
- percentual de sugestões adicionadas ao plano;
- dias com roteiro e deslocamentos válidos;
- reservas centralizadas por viagem;
- usuários que retornam semanalmente durante o planejamento;
- viagens acessadas offline;
- viagens com dois ou mais colaboradores.

## Estado atual — versão 0.2

### Web publicada

- [x] GitHub Pages com deploy automático.
- [x] Criação e edição de viagem, destino, datas e viajantes.
- [x] Roteiro separado por dia.
- [x] Transporte, hospedagem, passeio, alimentação e deslocamento.
- [x] Horário, duração, local, notas, link e código de reserva.
- [x] Custos previstos totais, por categoria e por pessoa.
- [x] Status pesquisando, reservar, reservado e sem reserva.
- [x] Diagnóstico de transporte, hospedagem, dias vazios e reservas pendentes.
- [x] Link do local para o Google Maps.
- [x] Exportação do roteiro em calendário `.ics`.
- [x] Checklist de preparação.
- [x] Persistência local no navegador.
- [x] Layout responsivo para computador e celular.

### Android existente

- [x] Kotlin, Compose, Material 3, SDK 36, Java 17 e minSdk 26.
- [x] Room com entidades principais do domínio.
- [x] Repositório único, estado agregado e fila offline deduplicada.
- [x] Criação de viagem, itinerário, checklist e planejamento central.
- [x] Sete temas derivados das linguagens Embarque e Finanza Next.
- [x] Gemini com resposta estruturada e importação para Room.
- [x] Widget e atalho de captura.
- [x] Testes JVM de orçamento, rateio e parcelamento.

---

## Fase 1 — Planejador diário realmente utilizável (P0)

Objetivo: substituir planilhas e notas para uma viagem individual.

### Roteiro

- [x] Arrastar e soltar itens dentro do dia.
- [x] Mover um item para outro dia.
- [x] Duplicar um dia inteiro sem copiar códigos de reserva.
- [x] Itens sem data em uma caixa de ideias.
- [x] Blocos manhã, tarde e noite, além de horário exato.
- [ ] Duração estimada e intervalo entre atividades.
- [x] Detectar choque de horários.
- [ ] Detectar atividade fora do horário de funcionamento.
- [x] Permitir tempo livre intencional sem gerar alerta.
- [ ] Observações, tags, prioridade e acessibilidade.
- [ ] Histórico simples de desfazer/refazer.

### Lugares e mapa

- [ ] Mapa lado a lado com o roteiro no desktop.
- [ ] Mapa alternável com o roteiro no celular.
- [ ] Marcadores por dia, categoria e ordem.
- [ ] Busca de lugares com autocomplete.
- [ ] Salvar coordenadas, place ID, endereço e fuso horário.
- [ ] Exibir distância e duração entre paradas.
- [ ] Modos a pé, carro, bicicleta e transporte público.
- [ ] Abrir o trecho no Google Maps, Apple Maps ou Waze.
- [ ] Alertar quando o roteiro exigir deslocamento inviável.

### Custos previstos

- [ ] Moeda da viagem e moeda original por item.
- [ ] Conversão com cotação e data de referência.
- [ ] Custo por pessoa versus custo do grupo.
- [ ] Faixa de preço mínimo/esperado/máximo.
- [ ] Alternativas comparáveis antes da decisão.
- [ ] Custos fixos separados dos custos diários.
- [ ] Total por dia, cidade, categoria e viajante.
- [ ] Reserva de contingência configurável.

### Critérios de aceite da fase

- Um usuário cria uma viagem de sete dias e organiza todo o roteiro sem editar JSON ou sair do app.
- Todo local agendado aparece no mapa.
- Conflitos e deslocamentos impossíveis ficam visíveis.
- Alterar a ordem recalcula rota, horários e custo estimado.
- O plano continua íntegro após fechar e reabrir o navegador/app.

---

## Fase 2 — Pesquisa que entra no plano (P0)

Objetivo: não ser apenas um campo de busca; cada resultado deve virar ideia, comparação ou item do roteiro.

### Descoberta

- [ ] Busca por atrações, restaurantes, bairros e experiências.
- [ ] Resultados com foto, descrição curta, avaliação, quantidade de avaliações e faixa de preço.
- [ ] Horários, site oficial, telefone e acessibilidade.
- [ ] Filtros por interesse, preço, duração, distância e “aberto no dia”.
- [ ] Guias por destino e listas temáticas.
- [ ] Lugares próximos aos itens já escolhidos.
- [ ] Caixa de ideias com listas personalizadas.
- [ ] Ações: salvar, comparar, agendar, descartar e compartilhar.

### Comparação

- [ ] Quadro de opções para voo, ônibus, trem, hotel, carro e passeio.
- [ ] Campos comparáveis por modalidade.
- [ ] Preço observado e momento da consulta.
- [ ] Política de cancelamento e bagagem.
- [ ] Prós, contras e notas pessoais.
- [ ] Marcar uma opção como escolhida sem apagar as alternativas.
- [ ] Alertas de prazo e mudança relevante de preço quando o provedor permitir.

### Integrações previstas

- Google Places API para lugares e detalhes.
- Google Routes API ou provedor equivalente para matriz de rotas.
- APIs oficiais/afiliadas para voos, hospedagens e passeios quando disponíveis.
- Links profundos para fornecedores quando a compra não puder acontecer no TripNext.
- Cache próprio e identificação clara da fonte e do horário da consulta.

### Critérios de aceite da fase

- Uma pesquisa salva um objeto estruturado, não apenas texto.
- O usuário agenda um resultado com um toque e pode desfazer.
- Preço e disponibilidade nunca são apresentados como garantidos sem fonte e timestamp.
- Resultados patrocinados são identificados.

---

## Fase 3 — Copiloto que executa (P0)

Objetivo: a IA deve propor mudanças e gravá-las somente após revisão do usuário.

### Perfil da viagem

- [ ] Origem, destinos e múltiplas cidades.
- [ ] Adultos, crianças, idades e necessidades de mobilidade.
- [ ] Interesses e coisas a evitar.
- [ ] Ritmo leve, equilibrado ou intenso.
- [ ] Preferência de horários e tempo de descanso.
- [ ] Estilo de alimentação e restrições.
- [ ] Orçamento e categorias prioritárias.
- [ ] Transporte preferido e limite de caminhada.

### Capacidades

- [ ] Gerar plano inicial por dia com justificativa curta.
- [ ] Usar apenas lugares verificáveis e devolver place IDs.
- [ ] Sugerir rotas coerentes por proximidade.
- [ ] Replanejar quando um dia muda.
- [ ] Oferecer três versões: econômica, equilibrada e confortável.
- [ ] Completar apenas lacunas selecionadas.
- [ ] Explicar conflitos, sazonalidade e riscos.
- [ ] Converter conversa em alterações estruturadas.
- [ ] Mostrar diff antes de aplicar: adicionar, mover, remover e alterar custo.
- [ ] Permitir aceitar por item ou aceitar tudo.
- [ ] Registrar a origem da sugestão e quando foi gerada.

### Segurança e qualidade

- [ ] Gemini chamado exclusivamente pelo backend.
- [ ] Chaves fora de APK, JavaScript e histórico Git.
- [ ] Saída validada por JSON Schema.
- [ ] Limites de custo, rate limiting e cota por usuário.
- [ ] Grounding/busca para fatos atuais.
- [ ] Avisos para vistos, saúde e segurança com fontes oficiais.
- [ ] Não inventar preço, horário, reserva ou disponibilidade.
- [ ] Avaliação automatizada de roteiros por viabilidade e alucinação.

### Critérios de aceite da fase

- Pelo menos 80% dos itens gerados têm local estruturado e verificável.
- Nenhuma sugestão modifica o plano sem confirmação.
- O usuário consegue aplicar só parte do plano.
- Falha da IA não bloqueia o planejamento manual.

---

## Fase 4 — Reservas e documentos (P1)

Objetivo: concentrar tudo o que será necessário no embarque e no check-in.

- [ ] Entidades específicas para voo, hospedagem, trem, ônibus, carro e passeio.
- [ ] Trechos, terminais, assentos, bagagem, check-in e localizadores.
- [ ] Check-in/check-out com múltiplas hospedagens.
- [ ] Política de cancelamento e prazo de reembolso.
- [ ] Anexos PDF, imagem, voucher e ingresso.
- [ ] Importação manual de PDF e imagem com revisão.
- [ ] Encaminhamento de e-mail para importar confirmação.
- [ ] Integração opcional com Gmail/Outlook com consentimento granular.
- [ ] Detecção de duplicidade.
- [ ] Lembretes de check-in, pagamento e cancelamento gratuito.
- [ ] Status ao vivo de voo quando houver provedor licenciado.
- [ ] Cofre offline dos documentos essenciais.
- [ ] Ocultação de códigos sensíveis em telas compartilhadas.

Critérios de aceite:

- Importação nunca cria reserva definitiva sem tela de revisão.
- Dados extraídos mostram confiança e trecho de origem.
- Reserva confirmada aparece automaticamente no dia correto.
- Documentos essenciais podem ser abertos sem internet.

---

## Fase 5 — Colaboração (P1)

Objetivo: permitir que um grupo planeje sem perder controle ou contexto.

- [ ] Conta e autenticação por e-mail, Google e passkey.
- [ ] Convite por link ou e-mail.
- [ ] Papéis: organizador, editor, leitor e convidado.
- [ ] Sincronização em tempo real.
- [ ] Presença e indicação de quem está editando.
- [ ] Comentários e menções em lugares, dias e decisões.
- [ ] Enquetes para datas, hospedagem e passeios.
- [ ] Aprovação opcional para mudanças críticas.
- [ ] Registro de atividade e restauração de versão.
- [ ] Preferências individuais dentro da viagem.
- [ ] Custos previstos divididos por participante.
- [ ] Link público somente leitura com expiração.

Critérios de aceite:

- Duas pessoas editam a mesma viagem sem sobrescrever dados.
- Conflitos têm resolução previsível e histórico.
- Participante removido perde acesso imediatamente.
- Dados privados permanecem invisíveis no link público.

---

## Fase 6 — Modo viagem e offline (P1)

Objetivo: transformar o planejamento em copiloto durante a viagem.

- [ ] Pacote offline por viagem.
- [ ] Roteiro, reservas, documentos e checklist offline.
- [ ] Cache de mapas e rotas dentro das regras do provedor.
- [ ] Tela “Agora”: próximo compromisso, saída recomendada e confirmação.
- [ ] Notificações de embarque, check-in e deslocamento.
- [ ] Mudança de fuso horário segura.
- [ ] Telefones e endereços essenciais.
- [ ] Acesso rápido a seguro e contatos de emergência.
- [ ] Registro local de alterações e sincronização posterior.
- [ ] Widget de contagem regressiva e próximo compromisso.
- [ ] Atalhos para voucher, mapa e contato da hospedagem.

Critérios de aceite:

- Reiniciar o celular sem rede não remove o plano baixado.
- Alterações offline sincronizam sem duplicar operações.
- Horários permanecem corretos ao atravessar fusos.

---

## Fase 7 — Múltiplas viagens, histórico e guias (P2)

- [ ] Lista de próximas, rascunhos, passadas e arquivadas.
- [ ] Alternar viagem ativa sem perder contexto.
- [ ] Duplicar viagem como modelo.
- [ ] Modelos para fim de semana, road trip e viagem internacional.
- [ ] Linha do tempo de viagens anteriores.
- [ ] Avaliação privada dos lugares visitados.
- [ ] Transformar uma viagem concluída em guia compartilhável.
- [ ] Importar roteiro público para uma nova viagem.
- [ ] Busca global entre viagens, reservas e lugares.
- [ ] Backup, exportação JSON e exclusão completa dos dados.

---

## Fase 8 — Plataforma e sustentabilidade (P2)

- [ ] Plano gratuito útil sem bloquear o roteiro básico.
- [ ] Pro para IA ampliada, offline avançado, otimização e anexos maiores.
- [ ] Assinatura via Play Billing e web.
- [ ] Afiliados identificados para hospedagem, atividades e transporte.
- [ ] Sem venda de dados pessoais ou localização.
- [ ] Central de privacidade e consentimentos.
- [ ] Painel de suporte e diagnóstico com dados minimizados.
- [ ] Feature flags e experimentos éticos.
- [ ] Analytics de produto sem capturar conteúdo sensível.

---

## Arquitetura alvo

### Clientes

- Android nativo em Kotlin/Compose.
- Web/PWA em React, evoluindo a implementação atual.
- Design system compartilhado por tokens, não por componentes binários.
- Contratos de API e schemas gerados para reduzir divergência.

### Backend

- API própria em FastAPI ou Node/TypeScript.
- PostgreSQL com PostGIS para lugares e geometria.
- Object storage para anexos.
- Redis/fila para importação, IA, notificações e sincronização.
- WebSocket ou serviço equivalente para colaboração.
- Jobs idempotentes e observáveis.

### Domínios principais

- `User`, `Session`, `Trip`, `Destination`, `Participant`.
- `Day`, `PlanItem`, `Place`, `RouteLeg`, `IdeaList`.
- `OptionComparison`, `EstimatedCost`, `ExchangeRate`.
- `Reservation`, `TravelSegment`, `LodgingStay`, `Attachment`.
- `Checklist`, `Task`, `Reminder`, `Comment`, `Vote`.
- `AiProposal`, `PlanChange`, `SyncOperation`, `AuditEvent`.

### Sincronização

- Local-first em Android e PWA.
- IDs UUID gerados no cliente.
- `updatedAt`, versão e tombstone para exclusões.
- Fila offline deduplicada por chave idempotente.
- Estratégia explícita por tipo de conflito.
- Cache de leitura com invalidação e retry exponencial.

### APIs iniciais

- `GET/POST /api/trips`
- `GET/PATCH/DELETE /api/trips/{tripId}`
- `GET/POST /api/trips/{tripId}/days`
- `GET/POST/PATCH /api/trips/{tripId}/items`
- `POST /api/trips/{tripId}/items/reorder`
- `GET/POST /api/trips/{tripId}/ideas`
- `GET /api/places/search`
- `POST /api/routes/matrix`
- `POST /api/trips/{tripId}/ai/proposals`
- `POST /api/ai/proposals/{proposalId}/apply`
- `POST /api/import/reservation`
- `GET/POST /api/trips/{tripId}/participants`
- `POST /api/sync/push` e `POST /api/sync/pull`

## Qualidade e operação

### Testes

- [ ] Unidade: datas, fusos, moedas, conflitos, custos e permissões.
- [ ] Contrato: clientes versus OpenAPI.
- [ ] Integração: banco, fila, provedores e idempotência.
- [ ] UI: criação, pesquisa, agendamento, reordenação e offline.
- [ ] E2E web e Android para a jornada completa.
- [ ] Golden/screenshot tests para temas principais.
- [ ] Testes de migração Room e banco remoto.
- [ ] Testes de acessibilidade e navegação por teclado.
- [ ] Evals do Copiloto com destinos, perfis e idiomas variados.

### Observabilidade

- [ ] Crash reporting e web error tracking.
- [ ] Logs estruturados sem chaves, vouchers ou documentos.
- [ ] Métricas de latência, erro, cache e custo por provedor.
- [ ] Rastreamento de jobs de importação e IA.
- [ ] Alertas de falha de sincronização e aumento de custo.
- [ ] Página de status e runbooks.

### Segurança e privacidade

- [ ] Segredos somente no backend/secret manager.
- [ ] TLS, criptografia em repouso e rotação de credenciais.
- [ ] Autorização por viagem em todas as consultas.
- [ ] URLs assinadas e temporárias para anexos.
- [ ] 2FA/passkeys e encerramento remoto de sessões.
- [ ] LGPD: consentimento, portabilidade, retenção e exclusão.
- [ ] Threat model para convites, links públicos, PDFs e prompt injection.
- [ ] Dependabot, análise estática e inventário de dependências.

## Plano de execução sugerido

| Marco | Escopo | Resultado demonstrável |
|---|---|---|
| M1 | Caixa de ideias, drag-and-drop e múltiplas viagens web | Planejamento manual completo |
| M2 | Places + mapa + rotas | Roteiro espacialmente viável |
| M3 | Comparação e custos previstos | Decisão de opções dentro do app |
| M4 | Backend, login e sync | Mesmo plano no web e Android |
| M5 | Copiloto com proposals/diff | IA que realmente monta o roteiro |
| M6 | Reservas e anexos | Central de embarque/check-in |
| M7 | Colaboração | Grupo planejando em tempo real |
| M8 | Offline e modo viagem | Produto útil durante a viagem |

## Próximo sprint recomendado — M1

Ordem exata de implementação:

1. [x] Migrar `localStorage` de uma viagem única para coleção versionada de viagens.
2. [x] Criar seletor de viagem ativa, arquivar, duplicar e excluir.
3. [x] Criar caixa de ideias sem data.
4. [x] Permitir mover uma ideia para um dia.
5. [x] Implementar drag-and-drop e persistir `sortOrder`.
6. [x] Adicionar blocos manhã/tarde/noite e detecção de choque.
7. [x] Adicionar opção de “tempo livre intencional”.
8. [x] Criar testes E2E desta jornada.
9. [x] Migrar a mesma estrutura para Room no Android.

### Definição de pronto do M1

- Build web e Android aprovados.
- Migração preserva os dados já existentes do usuário.
- Fluxo completo funciona em tela de 360 px e desktop.
- Operações destrutivas pedem confirmação.
- Nenhuma credencial entra no cliente ou no Git.
- `ROADMAP.md` e documentação técnica atualizados.

## Registro de validação

| Data | Comando | Resultado |
|---|---|---|
| 2026-08-10 | `gradlew testDebugUnitTest` | **PASS** — 7 testes JVM |
| 2026-08-10 | `gradlew assembleDebug` | **PASS** — APK debug gerado |
| 2026-08-10 | `gradlew testDebugUnitTest assembleDebug` | **PASS** — APK instalado no aparelho `23049PCD8G` |
| 2026-08-11 | `npm run build` | **PASS** — web React/Vite |
| 2026-08-11 | `npm test` | **PASS** — 5 testes de migração e planejamento web |
| 2026-08-11 | `npm run test:e2e` | **PASS** — 2 jornadas completas em Chromium |
| 2026-08-11 | `gradlew testDebugUnitTest` | **PASS** — 10 testes JVM após Room v2 |
| 2026-08-11 | GitHub Actions Pages | **PASS** — publicação automática |

## Princípios permanentes

- Planejamento antes de controle financeiro.
- Pesquisa precisa terminar em ação dentro do app.
- IA propõe; o usuário confirma.
- Fonte, preço e disponibilidade sempre têm data.
- Offline é requisito de viagem, não recurso cosmético.
- Privacidade prevalece sobre conveniência.
- O TripNext aprende padrões de produto, mas mantém identidade e implementação próprias.
