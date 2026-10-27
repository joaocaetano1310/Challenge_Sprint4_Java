# Validação — Entrega Final (Sprint 4)

## Build e testes automatizados

- Java 17 (Temurin 17.0.13), Maven 3.9.11, Spring Boot 3.5.16.
- `mvn verify` na aplicação principal: **38 testes, 0 falhas**.
- `mvn verify` no microsserviço `futurevet-ia`: **2 testes, 0 falhas**.
- `mvn package` no `servidor-eureka`: compilado.

| Classe | Testes | O que garante |
|---|---|---|
| `FluxosTest` | 18 | Fluxos da Sprint 3: login, CSRF, isolamento entre tutores, agenda (conflito, concorrência, confirmação, conclusão, reagendamento), carteira e reforço, CRUDs, cadastro. Inclui as telas novas e o bloqueio do monitoramento para tutor. |
| `MigrationTest` | 2 | Migração de banco legado preserva dados e protege senhas; horário inválido bloqueia a migração. |
| `Sprint4Test` | 15 | i18n (troca de idioma, cookie, telas e regras em inglês); notificações (tutor → clínica e clínica → tutor, "marcar como lidas" só afeta o próprio usuário, operação recusada não gera aviso, consumidor da fila); Assistente (contexto só com dados do tutor, histórico da conversa, IA fora do ar com mensagem amigável, pergunta vazia não chama o serviço); Actuator (health público, métricas só da clínica); API (JWT nas rotas novas, 503 quando a IA cai, contrato do app mobile mantido). |
| `ProdutorEventosTest` | 3 | Publicação no exchange certo, entrega local sem broker e *fallback* quando o RabbitMQ cai. |
| `AssistenteServiceTest` (IA) | 2 | Prompt com instruções de sistema, contexto e idioma; texto do usuário não é interpretado como template; sem chave → 503. |

## Teste ponta a ponta com os serviços reais

Executado com os quatro componentes rodando ao mesmo tempo: **RabbitMQ 3.9**, **Eureka**, **futurevet-ia** e **futurevet** (perfil demo, `RABBITMQ_ENABLED=true`, `EUREKA_ENABLED=true`, `IA_URL` vazio).

1. Eureka listou `FUTUREVET` e `FUTUREVET-IA`.
2. Tutor fez login e agendou uma consulta. O log mostrou "Evento CONSULTA_SOLICITADA enviado ao RabbitMQ" e "Notificação CONSULTA_SOLICITADA registrada (canal RABBITMQ)". A fila `futurevet.notificacoes` tinha 1 consumidor e 0 mensagens pendentes.
3. Clínica viu "Nova solicitação de consulta" e confirmou. O tutor recebeu "Consulta confirmada".
4. Assistente chamado pelo nome `futurevet-ia` via Eureka/OpenFeign. Sem chave configurada, a tela mostrou o aviso amigável e a API respondeu 503.
5. Monitoramento mostrou Aplicação, Banco, IA e Mensageria como "Operando", com 2 eventos recebidos pela fila.
6. `/actuator/health` respondeu `{"status":"UP"}`. `/actuator/metrics` sem login redirecionou para o login.
7. Nenhum `ERROR` no log da aplicação.

O microsserviço de IA também foi testado contra um servidor que imita a API da OpenAI. A chamada saiu para `/v1/chat/completions` com `Bearer <chave>`, modelo, temperatura 0.4 e 600 tokens. As mensagens foram *system* (instruções + dados do usuário) e *user* (pergunta), e a resposta voltou para a tela de chat.

As telas foram conferidas em 1366 px (desktop) e 390 px (celular), em português e inglês.

## Limitações

- A chamada a um provedor de IA real (Groq/OpenAI) depende da chave do grupo e não foi feita neste ambiente. Configure `IA_API_KEY` e teste uma pergunta antes da gravação.
- A migration `V4__notificacoes.sql` usa SQL padrão Oracle (sequence, TIMESTAMP, constraints). Foi validada no H2 em modo Oracle, não no Oracle da FIAP. Siga `ORACLE.md` antes de aplicar no banco oficial.
- O deploy no Render precisa ser atualizado pelo grupo depois do merge na branch `main` (veja o README).
