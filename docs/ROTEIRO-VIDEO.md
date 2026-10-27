# Roteiro do vídeo — Java Advanced, Entrega Final (até 15 minutos)

Todos os cinco integrantes precisam aparecer e falar (critério 4 da rubrica). Cada bloco abaixo tem um responsável sugerido. Troquem os nomes se preferirem outra divisão, mas mantenham a ordem: ela segue os critérios de avaliação.

**Antes de gravar**

- Abra a aplicação **online** (link do deploy) uns 2 minutos antes, para o Render "acordar".
- Deixe abertas em abas: aplicação logada como **tutor**, aplicação logada como **clínica** (outra janela anônima), Eureka (`:8761`), painel do RabbitMQ (`:15672`) e o README no GitHub.
- Se for demonstrar a mensageria e o Eureka, rode localmente com `docker compose up --build`.
- Teste o Assistente Vet uma vez antes de gravar, para garantir que a chave da IA está configurada.
- Não mostre senhas nem chaves na tela.

| Tempo | Quem | O que mostrar | Critério |
|---|---|---|---|
| 0:00–1:30 | **Felipe** | Apresentação do grupo (nome e RM de todos). O problema da Clyvo Vet: vacinas esquecidas, agenda em duplicidade, tutor sem retorno. A proposta do FutureVet em uma frase. | Narrativa |
| 1:30–3:00 | **João Caetano** | Arquitetura (diagrama do README): aplicação principal, microsserviço de IA, Eureka, RabbitMQ, Oracle. Por que a IA ficou em um microsserviço separado e por que os eventos saem só depois do commit. | Narrativa / decisões |
| 3:00–6:00 | **Ryan** | **Fluxo do tutor no deploy:** login, visão geral, cadastro de animal (mostrar validação com erro), agendamento. Tentar o mesmo horário e mostrar a recusa. Carteira: dose atrasada, registrar reforço, próxima dose calculada e dose anterior como "histórico". | Demonstração técnica |
| 6:00–8:00 | **Raul** | **Fluxo da clínica + mensageria:** entrar como clínica e abrir Notificações (aviso do agendamento do Ryan). Confirmar a consulta e voltar ao tutor: aviso "Consulta confirmada". No local, mostrar a fila `futurevet.notificacoes` no painel do RabbitMQ e o log "enviado ao RabbitMQ". | Demonstração / conceitos |
| 8:00–10:00 | **João Bueno** | **Assistente Vet (Spring AI):** perguntar "Alguma vacina dos meus pets está atrasada?" (a resposta cita o Bento). Depois, uma pergunta de continuação. Mostrar o Eureka com `FUTUREVET` e `FUTUREVET-IA` registrados e o `AssistenteClient` (OpenFeign) no código. | Conceitos / originalidade |
| 10:00–11:30 | **Felipe** | **Internacionalização:** trocar para English no menu e mostrar telas e uma mensagem de erro de regra em inglês. **Monitoramento:** tela `/admin/monitoramento` (status dos serviços, memória, eventos por canal) e `/actuator/health`. Mostrar que o tutor não acessa essa área. | Conceitos / UI-UX |
| 11:30–13:30 | **João Caetano** | **Integração multidisciplinar:** app mobile consumindo a mesma API do deploy (login JWT, animais, vacinas e consultas), coleira IoT PetLink, Docker/docker-compose e deploy (DevOps), testes automatizados e Azure Boards (QA), migrations Flyway sobre o modelo Oracle (Banco). Mostrar a tabela do README. | Integração |
| 13:30–14:45 | **Ryan** | Rodar `./mvnw verify` (ou mostrar o resultado: 38 + 2 testes passando). Mostrar o README: links, como executar, variáveis. Encerramento: o que o FutureVet entrega para a Clyvo Vet. | Organização / documentação |

## Dicas para não perder pontos

- **Erros visuais ou de fluxo durante a apresentação (-5):** ensaiem uma vez com o roteiro. Agendem a consulta para um dia de semana entre 08:00 e 18:00.
- **Concluir atendimento** só funciona depois do horário da consulta. Se quiserem mostrar, usem uma consulta confirmada cujo horário já passou. Senão, só expliquem a regra.
- **Assistente sem chave:** a tela mostra um aviso amigável. Em vez de esconder, expliquem que esse é o comportamento resiliente: a IA fora do ar não derruba o sistema.
- Falem com as próprias palavras sobre o código que mostrarem. O professor pode perguntar sobre qualquer trecho (veja `docs/ARQUITETURA.md`).
