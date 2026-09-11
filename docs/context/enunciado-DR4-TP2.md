# Trabalho Prático — Agregados e Domain Events (DR4-TP2)

> Transcrição íntegra do enunciado fornecido pelo professor. **Esta é a fonte de verdade
> acadêmica**. Em caso de divergência com a skill, com o `CLAUDE.md` ou com o README, prevalece
> este texto.

---

## Adaptação de escopo — Pet Friends → E-commerce (contexto de Pagamento)

O enunciado original escreve "projeto Pet Friends" nas questões **6, 7, 10 e 12**. O projeto
conduzido nesta disciplina é o **E-commerce** do DR4-TP1, a extração do **Contexto Delimitado de
Pagamento** de um monólito, publicada em
`https://github.com/andrebecker84/ecommerce-ddd-refactoring-TP1`. É esse o escopo adotado aqui, e é
dele que saem os trechos de código e o desenho de arquitetura.

**Nas questões abaixo, "projeto Pet Friends" foi substituído por "projeto E-commerce".** Só o nome
do projeto mudou: o texto das questões, a numeração e a rúbrica permanecem exatamente como o
professor os escreveu. A substituição é declarada uma vez no documento final, logo após a capa.

| Onde o enunciado dizia | Neste trabalho |
|---|---|
| "projeto Pet Friends" | projeto **E-commerce**, contexto de Pagamento (DR4-TP1) |
| entidade que representa um agregado | `Pagamento`, a Aggregate Root do contexto |
| outro agregado referenciado por ID | `Pedido` (`pedidoId`) e `Usuario` (`usuarioId`) |
| método de negócio que publica evento | `Pagamento.aprovar(...)` / `Pagamento.recusar(...)` |
| evento de domínio concreto | `PagamentoAprovado` (e `PagamentoRecusado`) |

---

## Formato da entrega

Para este TP, você deve elaborar um documento em PDF com a capa do Infnet, título do AT, escola,
turma, disciplina, professor e nome do aluno, seguindo o padrão de entrega de trabalhos.

**ATENÇÃO:**

- Este documento **não necessita de sumário / índice**.
- No PDF, **numere as questões e copie o enunciado de cada uma**, respondendo-as em seguida.
- A numeração é **sequencial**, como neste enunciado.

Relacione as questões abaixo, com enunciado e respostas, explicando-as **com suas próprias
palavras**, buscando ser o mais objetivo possível.

---

## Questões

1. Explique de forma sucinta qual a razão de criar Agregados.

2. O que significa "consistência transacional"?

3. Cite as 4 propriedades cruciais que definem transações.

4. O que são "invariantes de negócio"?

5. Porque um agregado só deve ter acesso a outro agregado pelo ID?

6. Crie um trecho de código Java de uma entidade que represente um agregado e faça referência a
   outro agregado dentro do escopo do projeto E-commerce.

   > **Escopo adotado:** `Pagamento` (Aggregate Root) referenciando `Pedido` e `Usuario` por
   > identidade, com `long pedidoId` e `long usuarioId`.

7. Elabore um trecho de código Java que mostre um método de negócio com a previsão de publicação
   de um evento de domínio dentro do escopo do projeto E-commerce.

   > **Escopo adotado:** `Pagamento.aprovar(codigoAutorizacao)`, que valida a invariante
   > (pagamento ainda `PENDENTE`), muda o estado para `APROVADO` e só então registra
   > `PagamentoAprovado`.

8. O que é "evento de domínio"?

9. Crie um trecho de código Java que mostre uma abstração de um objeto do tipo "evento de domínio".

   > **Escopo adotado:** interface `EventoDeDominio`, com identidade do evento, momento de ocorrência
   > e identificador do agregado de origem.

10. Crie um trecho de código Java que mostre a implementação de um "evento de domínio" dentro do
    escopo do projeto E-commerce.

    > **Escopo adotado:** `PagamentoAprovado` (record) implementando `EventoDeDominio`.

11. Qual é a diferença entre filas e tópicos e como estes elementos funcionam em conjunto?

12. Dê um exemplo de solução de arquitetura para publicação de eventos de domínio dentro do escopo
    do projeto E-commerce (ideal um desenho).

    > **Escopo adotado:** publicação de `PagamentoAprovado` do contexto de Pagamento para os
    > consumidores do e-commerce (Pedido, Notificações, Estoque/Fiscal), com desenho.

13. Explique qual a finalidade de uma Event Store no contexto de eventos de domínio.

14. O que é Event Sourcing e como ele se diferencia da persistência tradicional em bancos de dados
    relacionais? Explique como os eventos salvos são usados para recuperar o estado atual de um
    Agregado.

---

## Evidências de código

Para as questões **6, 7, 9 e 10**, indique o **link do GitHub** onde estão os códigos ou envie
**prints de tela**.
