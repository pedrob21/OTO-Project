# Dados de demonstração

Popula o banco com dados **fictícios** para apresentar o painel, os indicadores e os relatórios. Fica desligado por padrão e não entra em nenhum fluxo do sistema: dá para apagar esta pasta inteira quando a demonstração não for mais necessária.

## O que é criado

| Dado | Conteúdo |
|---|---|
| Meios de hospedagem | 23 estabelecimentos inventados (5 resorts, 8 hotéis, 7 pousadas, hotel fazenda, flat e hostel), com capacidade. Uma pousada irregular e um hotel inativo mostram quem fica fora do cálculo, e o Hotel Portal do Parque tem uma ampliação há seis meses, para o histórico de capacidade. Todos os CNPJs começam com `99` |
| Hospedagem caseira | Totais do RHC vigentes desde 2022 (só se ainda não houver um com a mesma data) |
| Pesquisas | Uma pesquisa por mês, dos 13 meses anteriores ao atual até o retrasado, já encerradas, com a consolidada do mês e dos feriados que começaram nele. Cada feriado tem também a sua pesquisa de expectativa. Códigos `DEMO-M…` (mês) e `DEMO-X…` (expectativa) |
| Respostas | Taxa e diária de cada estabelecimento, com sazonalidade (férias de janeiro e julho em alta) e alguma falta de resposta, sempre acima dos 70% de leitos. Uma em cada dez foi corrigida (versão 2) |
| Resultados | Calculados pelo `ResultadoService`, o mesmo da publicação pela tela, e com as datas de publicação no passado |
| Série histórica | Taxa, fluxo e diária dos três anos anteriores ao primeiro mês do sistema, mês a mês e por feriado (consolidada e expectativa), com a fonte "Série fictícia para demonstração" |

As datas são contadas a partir do dia em que o populador roda. O **mês passado fica de fora** de propósito: é o mês que a equipe estaria pesquisando agora, livre para testar à mão a criação da pesquisa, os lançamentos, a prorrogação, o encerramento e a publicação.

Usuários não são criados. As pesquisas e publicações ficam no nome do primeiro ADMIN ativo (o do `ADMIN_EMAIL` serve). Os parceiros e visualizadores são criados pela tela de Usuários, e os parceiros podem ser ligados aos estabelecimentos da demonstração.

## Como rodar

1. O banco precisa ter um ADMIN. Num banco novo, defina `ADMIN_EMAIL` e `ADMIN_SENHA` (o administrador é criado na subida).
2. Suba a API com `OTO_DEMO_POPULAR=true`:
   ```bash
   OTO_DEMO_POPULAR=true ./gradlew bootRun
   ```
   No ambiente publicado, crie a variável `OTO_DEMO_POPULAR=true` no serviço da API e reinicie.
3. No log, a linha `Demonstração: 23 estabelecimentos, 30 campanhas publicadas…` confirma.
4. Remova a variável (ou deixe `false`).

Rodar de novo não duplica nada: se já houver estabelecimento da demonstração, o populador só avisa no log. Se algo der errado, a transação desfaz tudo e a API sobe normalmente, com o erro no log.

## Remover os dados

Apaga o que o populador criou e as respostas que tenham sido lançadas à mão para os estabelecimentos da demonstração. Os vínculos de parceiros com esses estabelecimentos também saem; os usuários continuam. Resultados já publicados de campanhas criadas à mão não são recalculados.

```sql
BEGIN;
CREATE TEMP TABLE demo_estab ON COMMIT DROP AS SELECT id FROM estabelecimento WHERE cnpj LIKE '99%';
CREATE TEMP TABLE demo_camp ON COMMIT DROP AS
    SELECT c.id FROM campanha c JOIN rodada_pesquisa r ON r.id = c.rodada_id WHERE r.codigo LIKE 'DEMO-%';
DELETE FROM resultado_campanha_faixa WHERE campanha_id IN (SELECT id FROM demo_camp);
DELETE FROM resultado_campanha_tipo  WHERE campanha_id IN (SELECT id FROM demo_camp);
DELETE FROM resultado_campanha       WHERE campanha_id IN (SELECT id FROM demo_camp);
DELETE FROM resposta_ocupacao_historico WHERE resposta_id IN (SELECT id FROM resposta_ocupacao
    WHERE campanha_id IN (SELECT id FROM demo_camp) OR estabelecimento_id IN (SELECT id FROM demo_estab));
DELETE FROM resposta_ocupacao WHERE campanha_id IN (SELECT id FROM demo_camp)
    OR estabelecimento_id IN (SELECT id FROM demo_estab);
DELETE FROM campanha_diaria WHERE campanha_id IN (SELECT id FROM demo_camp);
DELETE FROM campanha WHERE id IN (SELECT id FROM demo_camp);
DELETE FROM rodada_pesquisa WHERE codigo LIKE 'DEMO-%';
DELETE FROM estabelecimento_inventario_item WHERE estabelecimento_id IN (SELECT id FROM demo_estab);
DELETE FROM usuario_estabelecimento WHERE estabelecimento_id IN (SELECT id FROM demo_estab);
DELETE FROM capacidade_hospedagem WHERE estabelecimento_id IN (SELECT id FROM demo_estab);
DELETE FROM estabelecimento WHERE id IN (SELECT id FROM demo_estab);
DELETE FROM indicador_historico WHERE fonte = 'Série fictícia para demonstração';
COMMIT;
```

## Arquivos

- `PopuladorDemo.java`: liga com `oto.demo.popular`, roda depois da subida (`ApplicationReadyEvent`), grava tudo numa transação e publica as campanhas.
- `DadosDemo.java`: estabelecimentos, sazonalidade, calendário dos feriados (Páscoa calculada pelo algoritmo de Meeus) e a série histórica.
