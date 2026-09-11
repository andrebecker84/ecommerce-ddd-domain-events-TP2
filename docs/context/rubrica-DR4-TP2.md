# Rúbrica — DR4-TP2

> Transcrição da rúbrica fornecida pelo professor. **Esta é a fonte de verdade acadêmica**. Em
> caso de divergência com a skill ou com o `CLAUDE.md`, prevalece este texto.
>
> Única adaptação, a mesma do enunciado: **"projeto Pet Friends" → "projeto E-commerce"**, contexto
> de Pagamento, herdado do DR4-TP1. Os critérios permanecem exatamente como escritos. O mapa de
> correspondência está em
> [`enunciado-DR4-TP2.md`](enunciado-DR4-TP2.md#adaptação-de-escopo--pet-friends--e-commerce-contexto-de-pagamento).

## Competência avaliada

**2. Projetar softwares usando "domain events".**

## Critérios

| # | Critério | Questões | Artefato no e-commerce |
|:---:|---|:---:|---|
| 1 | O aluno implementou corretamente um evento de domínio específico e nomeado em Java para o escopo do projeto E-commerce, fornecendo evidências do código? | 10 | `PagamentoAprovado` |
| 2 | O aluno elaborou adequadamente o código de uma abstração (interface ou classe abstrata) que padroniza os contratos dos objetos do tipo evento de domínio? | 9 | `EventoDeDominio` |
| 3 | O aluno definiu o conceito de evento de domínio com clareza, demonstrando compreender a anatomia e as propriedades inerentes a essa estrutura? | 8 | (discursiva) |
| 4 | O aluno codificou um método de negócio no Agregado que evidencia claramente o momento em que uma invariante é atendida e a mudança de estado resulta na publicação de um evento? | 6, 7 (apoiadas por 1–5) | `Pagamento.aprovar(...)` |
| 5 | O aluno definiu de forma objetiva a finalidade de uma Event Store como repositório especializado e imutável para o registro histórico no contexto da aplicação? | 13 | (discursiva) |
| 6 | O aluno propôs e desenhou uma solução de arquitetura de software para publicação de eventos, explicando o funcionamento e a diferença entre filas e tópicos? | 11, 12 | desenho da publicação |
| 7 | O aluno explicou com precisão o padrão Event Sourcing, diferenciando-o de bancos relacionais e detalhando o mecanismo de reconstrução de estado do Agregado? | 14 | (discursiva) |

## Observação de leitura

**Três dos sete critérios exigem código produzido e publicado, não apenas texto.** Os critérios
1, 2 e 4 pedem Java no escopo do projeto, e o enunciado exige link do GitHub ou print para as
questões 6, 7, 9 e 10. Resposta discursiva sozinha não os satisfaz.

O critério 6 pede explicitamente um **desenho** ("ideal um desenho", na questão 12). O diagrama é
entregável, não enfeite, e precisa vir acompanhado da explicação de fila × tópico.

O critério 4 é o mais exigente da rúbrica: não basta um método que publique evento. Ele precisa
**evidenciar o momento em que a invariante é atendida** e só então a mudança de estado gerar o
evento. A ordem importa e deve estar visível no código: validar invariante, mudar estado e
registrar evento. O `Pagamento` do TP1 já tem a invariante (`exigirPendente()`, que impede aprovar
ou recusar duas vezes); falta apenas o registro do evento depois da mudança de estado.

As questões 1 a 5 não aparecem isoladas em nenhum critério, mas sustentam o critério 4: são o
vocabulário (agregado, consistência transacional, ACID, invariante, referência por ID) que a
avaliação do método de negócio pressupõe.

**Nada nesta rúbrica exige mensageria em execução.** Os critérios 6 e 7 são de projeto e
explicação; Kafka, RabbitMQ ou Event Store rodando não pontuam a mais e consomem o tempo dos
critérios 1, 2 e 4, que são de código.
