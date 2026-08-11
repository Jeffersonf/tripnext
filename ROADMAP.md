# Roadmap — TripNext

## Web — planejamento da viagem

- [x] Roteiro organizado por cada dia da viagem.
- [x] Transporte, hospedagem, passeios, alimentação e deslocamentos previstos.
- [x] Valores estimados por item e consolidação por categoria e por pessoa.
- [x] Reserva, link, duração, local e observações em cada item.
- [x] Edição e exclusão de itens do roteiro.
- [x] Build web de produção aprovado em 2026-08-11.

## 0.1.0 — Fundação local

### Concluído

- [x] Projeto Android Kotlin, SDK 36, minSdk 26, Java 17, Compose e Material 3.
- [x] Tema visual único **Embarque**, derivado do protótipo `tripnext-prototype.jsx`.
- [x] Paridade visual revisada no aparelho: três abas, cartão de embarque com código de barras, cartões slate com borda e FAB rosa.
- [x] Especificação `tripnext-android-code` integrada: cinco abas, oito categorias, Grupo, Ajustes e conjunto completo de dados demonstrativos.
- [x] Brief definitivo `tripnext-design-spec-codex.md` aplicado e validado nas cinco telas em aparelho físico.
- [x] Fontes reais: Space Grotesk Bold, IBM Plex Sans Regular/Medium e IBM Plex Mono Regular/Medium em `res/font`.
- [x] Tokens hex exatos, boarding pass com linha tracejada/código de barras irregular, timeline conectada e despesas em card único com divisores.
- [x] Padrões do Finanza Next adaptados ao domínio de viagens: Central de recursos, navegação por módulo, badges de estado, estados vazios acionáveis e captura rápida em bottom sheet responsivo.
- [x] Linguagem visual Modern do Finanza adaptada: barra inferior em cápsula flutuante, item ativo elevado, títulos amplos, perfil e preferências organizadas em grupos.
- [x] Modo de privacidade funcional em Ajustes, ocultando orçamento, percentuais, categorias e valores das despesas durante a sessão.
- [x] Dashboard Modern refinado no aparelho: saudação e avatar, boarding pass preservado, orçamento em destaque, atalhos de planejamento e próximo compromisso acima das listas detalhadas.
- [x] Seletor persistente com 7 temas: Embarque, Modern claro/escuro, Classic claro/escuro e Web claro/escuro, usando as paletas reais do Finanza Next.
- [x] Planejador central da viagem com progresso geral, prioridades imediatas e 9 etapas: destino/datas, orçamento, transporte, hospedagem, documentos, roteiro diário, reservas, grupo e mala.
- [x] Criação funcional de viagem em 3 etapas (identidade, datas e orçamento), persistida no Room e mantida como viagem ativa após reiniciar.
- [x] Geração automática do plano inicial: orçamento distribuído em 8 categorias e checklist essencial de documentos/eletrônicos.
- [x] Copiloto Gemini 3 Flash integrado ao planejador, com tentativa de Google Search grounding, chave configurável, plano contextual e fallback identificado quando a cota de busca estiver indisponível.
- [x] Dados demonstrativos Lisboa/Porto removidos integralmente; o app inicia vazio com ação principal para planejar uma viagem real.
- [x] Copiloto com resposta JSON estruturada, revisão, contadores e importação de eventos, tarefas e orçamento diretamente para o Room.
- [x] Inclusão manual funcional de compromissos no Itinerário e itens categorizados no Checklist.
- [x] Site responsivo em React/Vite com criação de viagem, painel, itinerário, checklist, ajustes e persistência no navegador.
- [x] Room com Viagem, Despesa, OrçamentoCategoria, Meta, EventoItinerário, ReservaParcelada, ItemChecklist, VeículoViagem e ParticipanteViagem.
- [x] `TripRepository` como acesso único aos dados, contrato `TripRemoteRepository` e fila offline deduplicada.
- [x] `AppUiState` agregado e observável.
- [x] Início, viagens, resumo de orçamento, despesas, itinerário diário/calendário, checklist e configurações.
- [x] Captura rápida em três passos: valor → categoria → descrição.
- [x] Atalho estático e widget para abrir a captura rápida.
- [x] Regras JVM para orçamento, rateio/saldos e parcelamento.
- [x] Smoke test instrumentado da navegação principal.

### Registro de validação

| Data | Comando | Resultado |
|---|---|---|
| 2026-08-10 | `gradlew testDebugUnitTest` | **PASS** — 7 testes JVM |
| 2026-08-10 | `gradlew assembleDebug` | **PASS** — APK debug gerado |
| 2026-08-10 | `gradlew testDebugUnitTest assembleDebug` | **PASS** — 7 testes JVM e APK reinstalado no aparelho `23049PCD8G` |

### Próximos passos

- [ ] Transformar cada etapa do planejador em fluxo CRUD guiado, com prazos, responsáveis, notas, links e anexos.
- [ ] Ampliar o assistente com viajantes, estilo, interesses, ritmo e preferências para personalizar o plano inicial.
- [ ] Pesquisa e comparação de opções de voo, hospedagem e deslocamento, com favoritos e decisão final.
- [ ] Google Places Text Search para transformar sugestões do Copiloto em lugares estruturados, favoritos e eventos de itinerário.
- [ ] Mover credenciais e chamadas de IA para backend próprio antes de produção; não distribuir chave de API no APK.
- [ ] Tarefas pré-viagem com lembretes: documentos, vistos, vacinas, câmbio, chip, seguro e check-in.
- [ ] CRUD completo de viagens, metas, participantes, reservas e veículos.
- [ ] Divisão customizada, aprovação de gastos e tela de acerto de contas.
- [ ] Importação e extração de reservas em PDF.
- [ ] API própria, autenticação/2FA, papéis e implementação remota idempotente.
- [ ] WorkManager com restrição de rede e política de retry para sincronização.
- [ ] Widget com contagem regressiva, compromisso e orçamento lidos do Room.
- [ ] Mais dois temas visuais e testes instrumentados dos fluxos de criação.

### Fidelidade e limitações visuais

- **Fiel:** cores, tipografia, tamanhos de referência, margens de 16dp, bordas, raios, cinco abas, FAB, oito categorias e estados das cinco telas.
- **Adaptação Compose:** o `backdrop-blur` CSS da barra inferior foi representado por uma cápsula navy elevada, com borda sutil e seleção interna. Blur em tempo real foi evitado para manter renderização consistente desde o Android 8 (minSdk 26).
- **Adaptação responsiva:** larguras em pixels do protótipo foram convertidas para `dp`; listas continuam roláveis e respeitam a área segura do aparelho.
