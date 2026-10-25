# API do OTO — guia para o front-end

API REST em JSON do sistema do Observatório de Turismo de Olímpia. Com a aplicação no ar,
a documentação interativa fica em **http://localhost:8080/swagger-ui.html** (botão
*Authorize* com o token do login), e o contrato OpenAPI em `/v3/api-docs`.

## Convenções

| Assunto | Regra |
|---|---|
| Endereço base | `http://localhost:8080` em desenvolvimento |
| Datas | ISO 8601: `"2026-02-28"`; instantes em UTC: `"2026-03-01T13:45:00Z"`; horários `"14:00:00"` |
| Taxas e percentuais | de 0 a 100, com até duas casas (`72.5` = 72,5%) |
| Valores em dinheiro | reais, com duas casas (`410.00`) |
| Fluxo de turistas | em pernoites (não são pessoas distintas), como na metodologia do OTO |
| Códigos | tipos de hospedagem, feriados e faixas vêm de `GET /api/referencias`. Nos cadastros, o código é opcional: sem ele, é gerado do nome (`"Área de lazer"` → `AREA_DE_LAZER`) |
| Campos sem valor | vêm como `null` |

## Autenticação

1. `POST /api/auth/login` com e-mail e senha devolve um token (JWT), válido por 8 horas.
2. Envie o token em todas as outras chamadas: `Authorization: Bearer <token>`.
3. Token ausente, inválido ou vencido → **401**. Nesse caso, volte para a tela de login.

```http
POST /api/auth/login
Content-Type: application/json

{ "email": "ana@hotel.com", "senha": "minha-senha" }
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiraEm": "2026-10-21T22:00:00Z",
  "usuario": {
    "id": 7, "nome": "Ana", "email": "ana@hotel.com", "perfil": "PARCEIRO", "ativo": true,
    "primeiroAcessoPendente": false, "ultimoAcessoEm": "2026-10-21T14:00:00Z",
    "estabelecimentos": [ { "id": 12, "nomeFantasia": "Hotel Exemplo" } ]
  }
}
```

**Primeiro acesso e senha esquecida.** Não há envio de e-mail: o ADMIN cadastra o usuário e
recebe um link de acesso (`convite`) para mandar ao parceiro (por WhatsApp, como o OTO já faz).
O front monta o endereço da página de senha com o token, por exemplo
`https://<front>/definir-senha?token=<token>`, e essa página chama:

```http
POST /api/auth/definir-senha
{ "token": "<token do link>", "novaSenha": "pelo-menos-8-caracteres" }
```

Resposta **204**. O convite vale 7 dias e o link de redefinição, 24 horas; cada link só pode ser
usado uma vez. Para um link novo: `POST /api/usuarios/{id}/link-de-senha` (ADMIN).

## Perfis

| Perfil | O que pode fazer |
|---|---|
| `ADMIN` | Tudo: usuários, inventário, pesquisas, publicação, parâmetros, série histórica. Também digita à mão qualquer dado que normalmente vem da pesquisa (veja [Preenchimento manual](#preenchimento-manual-pela-equipe-do-oto)) |
| `PARCEIRO` | Ver e editar o inventário dos **seus** estabelecimentos, responder às pesquisas abertas, ver os resultados gerais e as próprias respostas |
| `VISUALIZADOR` | Ver inventário, resultados e dashboards. Nunca altera nada e não vê respostas individuais |

Resposta individual de ocupação é sigilosa: só o ADMIN e o próprio parceiro a veem. Nos
resultados publicados, para quem não é ADMIN, os tipos de hospedagem com menos de 3
respondentes vêm com os valores nulos e `"omitido": true`.

## Erros

Todos os erros vêm no formato Problem Details (RFC 9457):

```json
{ "status": 422, "title": "Operação não permitida", "detail": "A pesquisa 005/26 não está aberta para respostas." }
```

| Status | Quando | O que mostrar |
|---|---|---|
| 400 | Corpo ou parâmetro inválido. Traz `campos` com a mensagem de cada campo | Marcar os campos do formulário |
| 401 | Sem login, token vencido, ou e-mail/senha errados no login | Voltar ao login |
| 403 | O perfil não pode fazer isso, ou o estabelecimento não é do parceiro | "Sem permissão" |
| 404 | Registro não existe | "Não encontrado" |
| 409 | Registro repetido no banco (caso raro, normalmente vira 422 antes) | `detail` |
| 422 | Regra do OTO (ex.: pesquisa fechada, CNPJ repetido, mínimo de leitos para publicar) | `detail` |

Exemplo de 400 (as mensagens seguem o idioma do navegador, pelo cabeçalho `Accept-Language`):

```json
{
  "status": 400, "title": "Dados inválidos", "detail": "Corrija os campos indicados.",
  "campos": { "dados.nomeFantasia": "não deve estar em branco", "capacidade.qtdUh": "deve ser maior que 0" }
}
```

## Preenchimento manual pela equipe do OTO

Todo dado de entrada pode ser digitado ou corrigido à mão pelo perfil `ADMIN`, além do que
chega pela pesquisa ou pelo parceiro:

| Dado | Pela pesquisa ou pelo parceiro | À mão pela equipe (ADMIN) |
|---|---|---|
| Respostas de ocupação (taxa e diária) | Parceiro, com a pesquisa aberta | `PUT /api/campanhas/{id}/respostas/{estabelecimentoId}`, também depois do encerramento, até publicar |
| Diária média por tipo e geral | Média das respostas | `PUT /api/campanhas/{id}/diarias` (substitui a média no cálculo) |
| Dados do estabelecimento | Parceiro altera os seus | `POST` e `PUT /api/estabelecimentos` |
| Capacidade (UH e leitos) | Parceiro inclui nova vigência | Inclui, corrige ou exclui um lançamento: `/api/estabelecimentos/{id}/capacidades` |
| Itens do inventário | Parceiro marca os seus | `PUT /api/estabelecimentos/{id}/inventario`, inclusive os grupos da Secretaria |
| Catálogo do inventário (perguntas) | — | `/api/inventario/grupos` e `/api/inventario/itens` |
| Hospedagem caseira (RHC) | — | `POST`, `PUT` e `DELETE /api/rhc` |
| Parâmetros de cálculo | — | `POST`, `PUT` e `DELETE /api/parametros-calculo` |
| Tipos de hospedagem, feriados e faixas | — | `/api/referencias/tipos-hospedagem`, `/feriados` e `/faixas` |
| Série histórica | Importação de CSV | `POST`, `PUT` e `DELETE /api/historico` |
| Usuários, pesquisas e campanhas | — | `/api/usuarios` e `/api/rodadas` |

**Não são digitados:** os resultados (taxa, fluxo, receita), que sempre saem do cálculo: para
mudar um número, corrija o dado de entrada e publique de novo (cancele a publicação antes, se
já estiver publicada). Também ficam fora a auditoria, o histórico de versões das respostas, os
links de acesso e as senhas (cada usuário define a sua pelo link).

Correções valem para o que ainda vai ser calculado. Resultados já publicados guardam os números
da publicação e não mudam sozinhos.

---

## Endpoints

### Referências e conta

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| GET | `/api/referencias` | todos | Tipos de hospedagem, feriados e faixas de ocupação (para selects e legendas) |
| POST | `/api/referencias/tipos-hospedagem` | ADMIN | Novo tipo (201) |
| PUT · DELETE | `/api/referencias/tipos-hospedagem/{codigo}` | ADMIN | Altera nome, `calculaReceita` e ordem; exclui só se nunca foi usado (204) |
| POST | `/api/referencias/feriados` | ADMIN | Novo feriado ou evento (201) |
| PUT · DELETE | `/api/referencias/feriados/{codigo}` | ADMIN | Altera nome e ordem; exclui só se nunca foi usado (204) |
| POST | `/api/referencias/faixas` | ADMIN | Nova faixa de ocupação (201) |
| PUT · DELETE | `/api/referencias/faixas/{codigo}` | ADMIN | Altera nome e percentual mínimo; exclui só se nunca foi usada em publicação (204) |
| GET | `/api/me` | todos | Dados do usuário logado (inclui os estabelecimentos do parceiro) |
| PUT | `/api/me/senha` | todos | Troca de senha: `{ "senhaAtual": "...", "novaSenha": "..." }` → 204 |

```json
// GET /api/referencias (trecho)
{ "tiposHospedagem": [ { "codigo": "HOTEL", "nome": "Hotel", "calculaReceita": true, "ordem": 1 } ],
  "feriados": [ { "codigo": "CARNAVAL", "nome": "Carnaval", "ordem": 1 } ],
  "faixasOcupacao": [ { "codigo": "RUIM", "nome": "Ruim", "percentualMinimo": 0.00 } ] }

// POST /api/referencias/tipos-hospedagem — sem código, vira CHALE; sem ordem, vai para o fim
{ "codigo": null, "nome": "Chalé", "calculaReceita": false, "ordem": null }

// POST /api/referencias/feriados
{ "codigo": null, "nome": "Festa Junina", "ordem": null }

// POST /api/referencias/faixas
{ "codigo": null, "nome": "Média", "percentualMinimo": 50.00 }
```

O código não muda depois de criado (é a referência usada nos estabelecimentos, campanhas,
resultados e série histórica); no `PUT`, mande o mesmo código ou `null`. `calculaReceita` diz se
o tipo entra na diária, no RevPAR e na receita. Nas faixas, cada percentual mínimo é único e a
primeira faixa começa sempre em 0% (dela só o nome muda). Excluir algo já usado devolve **422**.

### Usuários (ADMIN)

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/usuarios` | Lista |
| GET | `/api/usuarios/{id}` | Detalhe |
| POST | `/api/usuarios` | Cria e devolve o convite (201) |
| PUT | `/api/usuarios/{id}` | Altera nome, perfil e estabelecimentos |
| POST | `/api/usuarios/{id}/inativar` · `/reativar` | 204. Usuário inativo não entra |
| POST | `/api/usuarios/{id}/link-de-senha` | Novo link de acesso (convite ou redefinição) |

```json
// POST /api/usuarios — PARCEIRO precisa de ao menos um estabelecimento; os outros perfis, de nenhum
{ "nome": "Ana Souza", "email": "ana@hotel.com", "perfil": "PARCEIRO", "estabelecimentoIds": [12] }

// 201
{ "usuario": { "id": 7, "nome": "Ana Souza", "...": "..." },
  "convite": { "token": "f3K9...", "tipo": "CONVITE", "expiraEm": "2026-10-28T14:00:00Z" } }
```

### Estabelecimentos (inventário)

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| GET | `/api/estabelecimentos?tipo=HOTEL&ativo=true` | todos | Lista resumida (o parceiro só vê os seus). UH e leitos da capacidade vigente hoje |
| GET | `/api/estabelecimentos/{id}` | todos | Detalhe completo |
| POST | `/api/estabelecimentos` | ADMIN | Cria com a capacidade inicial (201) |
| PUT | `/api/estabelecimentos/{id}` | ADMIN, PARCEIRO | Altera. O bloco `secretaria` só vale para o ADMIN |
| POST | `/api/estabelecimentos/{id}/inativar` · `/reativar` | ADMIN | 204. Não há exclusão |
| GET | `/api/estabelecimentos/{id}/capacidades` | todos | Histórico de UH e leitos |
| POST | `/api/estabelecimentos/{id}/capacidades` | ADMIN, PARCEIRO | Nova capacidade a partir de uma data (201) |
| PUT | `/api/estabelecimentos/{id}/capacidades/{capacidadeId}` | ADMIN | Corrige um lançamento digitado errado (mesmo corpo do POST) |
| DELETE | `/api/estabelecimentos/{id}/capacidades/{capacidadeId}` | ADMIN | Exclui um lançamento feito por engano (204). A única capacidade não sai |
| GET | `/api/estabelecimentos/{id}/inventario` | todos | Itens que o estabelecimento possui |
| PUT | `/api/estabelecimentos/{id}/inventario` | ADMIN, PARCEIRO | Substitui os itens marcados |
| GET | `/api/inventario/catalogo` | todos | Perguntas do inventário (grupos e itens ativos) para montar o formulário |

```json
// POST /api/estabelecimentos — todos os campos de "dados" são opcionais, menos nomeFantasia
{
  "dados": {
    "nomeFantasia": "Hotel Exemplo", "razaoSocial": "Exemplo Hotelaria Ltda", "cnpj": "12345678000190",
    "endereco": "Rua das Águas, 100", "bairro": "Centro", "telefoneComercial": "17 3281-0000",
    "emailComercial": "contato@hotelexemplo.com", "site": "https://hotelexemplo.com",
    "cadasturExigencia": "OBRIGATORIO", "possuiCadastur": true, "cadasturNumero": "12.345678.10.0001-2",
    "funcionariosFixos": 25, "funcionamento24h": true, "horarioCheckIn": "14:00:00", "horarioCheckOut": "12:00:00"
  },
  "secretaria": {
    "tipoHospedagemCodigo": "HOTEL", "situacao": "REGULAR", "contatoNome": "Maria",
    "aceitaPesquisas": true, "telefonePesquisas": "17 99999-0000", "emailPesquisas": "gerencia@hotelexemplo.com",
    "destinoInteligente": false, "dadosPddt": false
  },
  "capacidade": { "vigenteDesde": "2026-01-01", "qtdUh": 40, "qtdUhPcd": 2, "qtdLeitos": 120, "maxLeitosPorUh": 4 }
}
```

Campos de `dados` (todos do inventário oficial): `nomeFantasia`, `razaoSocial`, `cnpj` (14 dígitos, sem
pontuação), `dataAbertura`, `proprietarios`, `cnae`, `endereco`, `bairro`, `regiao`, `localizacao`,
`latitude`, `longitude`, `telefoneComercial`, `emailComercial`, `site`, `facebook`, `instagram`,
`cadasturExigencia` (`OBRIGATORIO`, `OPCIONAL`, `NAO_CONSTA`), `possuiCadastur`, `cadasturNumero`,
`cadasturValidade`, `funcionariosFixos`, `funcionariosTemporarios`, `funcionamento24h`, `horarioCheckIn`,
`horarioCheckOut`, `formasReserva`, `formasPagamento`.

`situacao`: `REGULAR` ou `IRREGULAR` (irregular fica fora das pesquisas e dos cálculos).
`maxLeitosPorUh` é a capacidade máxima de pessoas por UH; os leitos úteis do cálculo são
`UH × (1 + máximo) / 2`, e a resposta já traz `leitosUteis`.

```json
// PUT /api/estabelecimentos/{id}/inventario — a lista completa do que o estabelecimento possui
{ "itens": [ { "itemId": 1, "complemento": null }, { "itemId": 6, "complemento": "Alemão" } ] }
```

O que não vier na lista é desmarcado. `complemento` só vale para itens com
`permiteComplemento: true` (como "Outros"). Para o PARCEIRO, os grupos com
`exclusivoSecretaria: true` (indicadores ODS) ficam como estão, mesmo que venham na lista.

### Catálogo do inventário (ADMIN)

As perguntas do inventário (grupos e itens) são mantidas pela equipe, sem mexer no banco.

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/inventario/catalogo/completo` | Catálogo inteiro, com o que está inativo e os grupos ainda sem itens |
| POST | `/api/inventario/grupos` | Novo grupo (201) |
| PUT | `/api/inventario/grupos/{id}` | Altera o grupo; devolve o grupo com os itens |
| POST | `/api/inventario/grupos/{id}/inativar` · `/reativar` | 204. Grupo inativo sai do formulário |
| DELETE | `/api/inventario/grupos/{id}` | Só grupo sem itens (204) |
| POST | `/api/inventario/grupos/{grupoId}/itens` | Novo item no grupo (201) |
| PUT | `/api/inventario/itens/{id}` | Altera o item |
| POST | `/api/inventario/itens/{id}/inativar` · `/reativar` | 204. Item inativo sai do formulário |
| DELETE | `/api/inventario/itens/{id}` | Só item que nenhum estabelecimento marcou (204) |

```json
// POST /api/inventario/grupos — categoria: SERVICOS_EQUIPAMENTOS, ACESSIBILIDADE ou SUSTENTABILIDADE
{ "categoria": "SERVICOS_EQUIPAMENTOS", "codigo": null, "nome": "Área de lazer",
  "exclusivoSecretaria": false, "ordem": null }

// POST /api/inventario/grupos/28/itens
{ "codigo": null, "nome": "Outros", "descricao": "Quais?", "permiteComplemento": true, "ordem": null }

// 201 (item)
{ "id": 160, "codigo": "OUTROS", "nome": "Outros", "descricao": "Quais?", "permiteComplemento": true,
  "ordem": 1, "ativo": true }
```

Sem `codigo`, ele é gerado do nome; sem `ordem`, o novo vai para o fim e o alterado fica onde
está. No catálogo, grupos e itens também trazem `ordem` e `ativo`. O que os estabelecimentos
marcaram em um item inativo fica guardado e volta se ele for reativado. Um item com texto
digitado pelos estabelecimentos não pode deixar de aceitar complemento: crie outro e inative o antigo.

### Hospedagem caseira (RHC)

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| GET | `/api/rhc` | ADMIN, VISUALIZADOR | Totais por vigência |
| GET | `/api/rhc/vigente?data=2026-02-28` | ADMIN, VISUALIZADOR | Totais vigentes na data (padrão: hoje) |
| POST | `/api/rhc` | ADMIN | `{ "vigenteDesde", "qtdRegistros", "qtdUh", "qtdLeitos", "maxLeitosPorUh" }` (201) |
| PUT | `/api/rhc/{id}` | ADMIN | Corrige um lançamento digitado errado (mesmo corpo do POST) |
| DELETE | `/api/rhc/{id}` | ADMIN | Exclui um lançamento (204) |

### Parâmetros de cálculo

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| GET | `/api/parametros-calculo` | ADMIN, VISUALIZADOR | Todas as vigências |
| GET | `/api/parametros-calculo/vigente?data=` | todos | Vigentes na data (para notas metodológicas) |
| POST | `/api/parametros-calculo` | ADMIN | Nova vigência (201) |
| PUT | `/api/parametros-calculo/{id}` | ADMIN | Corrige a vigência, só se nenhum resultado publicado a usou (mesmo corpo do POST) |
| DELETE | `/api/parametros-calculo/{id}` | ADMIN | Só se nenhum resultado publicado usou (204) |

```json
{ "vigenteDesde": "2027-01-01", "percentualDayUse": 15.15, "diasPermanenciaMedia": 4, "diasEmParques": 2,
  "gastoMedioDiario": 250.00, "gastoMedioIngresso": 105.00, "percentualMinimoLeitosPublicacao": 70.00,
  "fonte": "Pesquisa de Demanda Turística 2026" }
```

### Pesquisas e campanhas (ADMIN)

Uma **pesquisa** (rodada, ex.: 005/26) é o envio aos parceiros e junta uma ou mais
**campanhas** (cada período medido: um mês ou um feriado, consolidado ou expectativa).
Ciclo: `RASCUNHO` → `ABERTA` (recebe respostas; o parceiro pode corrigir) → `ENCERRADA`
(as campanhas podem ser publicadas). Uma pesquisa encerrada pode ser reaberta enquanto
nenhuma campanha dela estiver publicada.

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/rodadas?status=ABERTA` | Lista, com as campanhas |
| GET | `/api/rodadas/{id}` | Detalhe |
| POST | `/api/rodadas` | Cria em rascunho (201): `{ "codigo": "005/26", "titulo": "...", "dataAbertura": null, "prazoResposta": null }` |
| PUT | `/api/rodadas/{id}` | Altera (só em rascunho) |
| DELETE | `/api/rodadas/{id}` | Exclui (só em rascunho) |
| POST | `/api/rodadas/{id}/campanhas` | Inclui campanha (só em rascunho, 201) |
| PUT · DELETE | `/api/rodadas/{id}/campanhas/{campanhaId}` | Altera ou exclui campanha (só em rascunho) |
| POST | `/api/rodadas/{id}/abrir` | `{ "dataAbertura": null, "prazoResposta": "2026-03-10" }` (sem data de abertura, vale hoje) |
| POST | `/api/rodadas/{id}/prorrogar` | `{ "prazoResposta": "2026-03-15" }` |
| POST | `/api/rodadas/{id}/encerrar` | Fecha para respostas |
| POST | `/api/rodadas/{id}/reabrir` | `{ "prazoResposta": "2026-03-20" }` |
| GET | `/api/rodadas/{id}/acompanhamento` | Quem respondeu e quem falta, com os contatos e o % de leitos respondentes |

```json
// POST /api/rodadas/{id}/campanhas — campanha mensal: do primeiro ao último dia do mês
{ "nome": "Consolidada fevereiro/2026", "modalidade": "CONSOLIDADA", "tipoPeriodo": "MENSAL",
  "feriadoCodigo": null, "periodoInicio": "2026-02-01", "periodoFim": "2026-02-28", "qtdDiarias": 28 }

// campanha de feriado
{ "nome": "Carnaval 2026", "modalidade": "CONSOLIDADA", "tipoPeriodo": "FERIADO",
  "feriadoCodigo": "CARNAVAL", "periodoInicio": "2026-02-13", "periodoFim": "2026-02-17", "qtdDiarias": 5 }
```

### Respostas

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| GET | `/api/respostas/pendencias` | PARCEIRO | Campanhas abertas para cada estabelecimento do parceiro, respondidas ou não (tela "Responder pesquisa") |
| PUT | `/api/campanhas/{campanhaId}/respostas/{estabelecimentoId}` | ADMIN, PARCEIRO | Envia ou corrige a resposta; vale o último envio |
| GET | `/api/respostas/minhas` | PARCEIRO | Respostas do parceiro com a taxa geral da cidade, quando publicada |
| GET | `/api/respostas/{id}/historico` | ADMIN, PARCEIRO | Todas as versões enviadas |
| GET | `/api/campanhas/{campanhaId}/respostas` | ADMIN | Respostas da campanha (dado individual) |

```json
// PUT /api/campanhas/20/respostas/12 — diariaMedia é opcional
{ "taxaOcupacao": 72.5, "diariaMedia": 410.00 }

// 200
{ "id": 301, "campanhaId": 20, "estabelecimentoId": 12, "nomeFantasia": "Hotel Exemplo",
  "taxaOcupacao": 72.50, "diariaMedia": 410.00, "versao": 2, "enviadoPor": 7, "enviadoEm": "2026-03-05T18:20:00Z" }
```

O ADMIN também lança respostas recebidas por telefone ou WhatsApp (fica registrado quem enviou).
O parceiro responde enquanto a pesquisa está `ABERTA`; o ADMIN, também depois do encerramento,
enquanto a campanha não for publicada. Só estabelecimentos ativos e regulares respondem.

### Resultados

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| GET | `/api/campanhas/{id}/previa` | ADMIN | Simula o resultado com as respostas atuais e diz o que ainda impede a publicação |
| POST | `/api/campanhas/{id}/publicacao` | ADMIN | Calcula e grava o resultado. Exige a pesquisa encerrada e o mínimo de leitos respondentes (70%) |
| DELETE | `/api/campanhas/{id}/publicacao` | ADMIN | Cancela uma publicação feita por engano (204) |
| GET | `/api/campanhas/{id}/resultado` | todos | Resultado publicado (404 se ainda não foi publicado) |
| GET | `/api/campanhas/{id}/resultado.csv` | todos | O mesmo em CSV para Excel (`;`, vírgula decimal) |
| GET | `/api/campanhas/publicadas?modalidade=&tipoPeriodo=&ano=` | todos | Campanhas publicadas, mais recentes primeiro |
| GET | `/api/campanhas/{id}/diarias` | ADMIN | Diárias médias digitadas pela equipe para a campanha |
| PUT | `/api/campanhas/{id}/diarias` | ADMIN | Substitui as diárias digitadas (só antes da publicação) |

**Diária digitada pela equipe.** Como na planilha de tabulação, a equipe pode informar a diária
média de cada tipo e a geral (ex.: da pesquisa de tarifas no Booking). A diária digitada substitui
a média das respostas no cálculo do tipo (RevPAR e receita) e na diária geral; a prévia avisa
quais estão valendo. A lista enviada substitui a anterior; lista vazia volta tudo para a média
das respostas.

```json
// PUT /api/campanhas/20/diarias — tipoHospedagemCodigo null = diária média geral
{ "diarias": [ { "tipoHospedagemCodigo": "HOTEL", "diariaMedia": 480.00 },
               { "tipoHospedagemCodigo": null, "diariaMedia": 510.00 } ],
  "fonte": "Pesquisa de tarifas Booking 02/2026" }

// 200 — a geral vem primeiro
[ { "tipoHospedagemCodigo": null, "diariaMedia": 510.00, "fonte": "Pesquisa de tarifas Booking 02/2026",
    "registradoPor": 1, "registradoEm": "2026-03-11T14:00:00Z" },
  { "tipoHospedagemCodigo": "HOTEL", "diariaMedia": 480.00, "fonte": "Pesquisa de tarifas Booking 02/2026",
    "registradoPor": 1, "registradoEm": "2026-03-11T14:00:00Z" } ]
```

```json
// GET /api/campanhas/20/previa
{
  "campanhaId": 20, "qtdMh": 74, "qtdRespondentes": 41, "leitosTotal": 13508, "leitosRespondentes": 9720,
  "percentualLeitosRespondentes": 71.96, "percentualMinimoPublicacao": 70.00, "podePublicar": false,
  "impedimentos": [ "A pesquisa 005/26 precisa ser encerrada antes da publicação." ],
  "avisos": [ "Hotel X: sem UH/leitos cadastrados para 2026-02-28; fica fora do cálculo." ],
  "resultado": { "...": "mesmo formato do resultado publicado" }
}

// GET /api/campanhas/20/resultado
{
  "campanha": { "id": 20, "rodadaId": 5, "nome": "Consolidada fevereiro/2026", "modalidade": "CONSOLIDADA",
                "tipoPeriodo": "MENSAL", "feriadoCodigo": null, "periodoInicio": "2026-02-01",
                "periodoFim": "2026-02-28", "qtdDiarias": 28, "publicada": true, "publicadaEm": "2026-03-12T15:00:00Z" },
  "publicado": true,
  "taxaOcupacao": 64.73, "faixa": { "codigo": "OTIMA", "nome": "Ótima" },
  "qtdMh": 74, "qtdMhRespondentes": 41, "percentualMhRespondentes": 55.41,
  "leitosTotal": 13508, "leitosRespondentes": 9720, "percentualLeitosRespondentes": 71.96,
  "fluxoMh": 58931.79, "fluxoRhc": 9282.65, "fluxoDayUse": 12179.71, "fluxoTotal": 80394.15, "fluxoMedioDiario": 16078.83,
  "diariaMedia": 812.40, "receitaHospedagem": 17137719.18, "receitaSemHospedagem": 97276924.77,
  "parametroCalculoId": 1, "calculadoEm": "2026-03-12T15:00:00Z",
  "porTipo": [
    { "tipoHospedagemCodigo": "HOTEL", "nome": "Hotel", "qtdMh": 20, "qtdMhRespondentes": 9, "qtdUh": 980,
      "taxaOcupacao": 40.35, "diariaMedia": 657.13, "revpar": 265.12, "receitaEstimada": 776805.30, "omitido": false },
    { "tipoHospedagemCodigo": "HOTEL_FAZENDA", "nome": "Hotel fazenda", "qtdMh": 2, "qtdMhRespondentes": 1, "qtdUh": 60,
      "taxaOcupacao": null, "diariaMedia": null, "revpar": null, "receitaEstimada": null, "omitido": true }
  ],
  "distribuicaoFaixas": [
    { "codigo": "EXCELENTE", "nome": "Excelente", "percentualMinimo": 81.00, "qtdMh": 6, "percentual": 20.00 },
    { "codigo": "OTIMA", "nome": "Ótima", "percentualMinimo": 61.00, "qtdMh": 6, "percentual": 20.00 }
  ]
}
```

Como o OTO calcula (igual à planilha de tabulação): taxa geral = média das taxas dos
respondentes ponderada pelas UH; fluxo em MH = leitos úteis × taxa × diárias de cada
respondente, mais os leitos úteis dos não respondentes × taxa geral × diárias; RHC = leitos
úteis do RHC × taxa geral × diárias; day use = parcela do dia que não pernoita (15,15% hoje);
RevPAR = diária × taxa; receita por tipo = RevPAR × UH do tipo × diárias (hotel, resort e pousada);
receita fora da hospedagem = fluxo total × (ingresso × dias em parques + gasto diário × dias de
permanência). A diária de cada tipo é a média das respostas ponderada pelos leitos, ou a digitada
pela equipe. O resultado fica congelado na publicação.

### Dashboards (todos os perfis)

Só valores gerais da cidade. Junta os resultados publicados no sistema com a série histórica;
quando os dois têm o mesmo período, vale o do sistema. Cada valor traz `origem`: `SISTEMA` ou `HISTORICO`.

| Método | Caminho | Uso na tela inicial |
|---|---|---|
| GET | `/api/dashboard/resumo?modalidade=CONSOLIDADA` | Cartões: último mês e último feriado publicados, com o comparativo do ano anterior e o acumulado do ano |
| GET | `/api/dashboard/serie-mensal?indicador=TAXA_OCUPACAO&modalidade=CONSOLIDADA&anoInicio=2024&anoFim=2026` | Gráfico de linhas mês a mês, uma linha por ano |
| GET | `/api/dashboard/feriados?modalidade=CONSOLIDADA&feriado=CARNAVAL&anoInicio=2024&anoFim=2026` | Gráfico de barras dos feriados por ano (sem `feriado`, todos os que têm dados) |

`indicador`: `TAXA_OCUPACAO`, `FLUXO_TURISTAS` ou `DIARIA_MEDIA`. Sem anos, o intervalo é dos
últimos três anos.

```json
// GET /api/dashboard/resumo
{
  "modalidade": "CONSOLIDADA",
  "ultimoMes": {
    "campanhaId": 21, "nome": "Consolidada fevereiro/2026", "tipoPeriodo": "MENSAL", "feriadoCodigo": null,
    "periodoInicio": "2026-02-01", "periodoFim": "2026-02-28", "taxaOcupacao": 66.00,
    "faixa": { "codigo": "OTIMA", "nome": "Ótima" }, "fluxoTotal": 90000.00, "fluxoMedioDiario": 3214.29,
    "diariaMedia": 512.00, "receitaHospedagem": 1234567.00, "receitaSemHospedagem": 108900000.00,
    "percentualLeitosRespondentes": 75.00,
    "anoAnterior": { "ano": 2025, "taxaOcupacao": 55.00, "fluxoTotal": 80000.00, "diariaMedia": 400.00,
                     "diferencaTaxaPontos": 11.00, "variacaoFluxoPercentual": 12.50,
                     "variacaoDiariaPercentual": 28.00, "origem": "HISTORICO" }
  },
  "ultimoFeriado": { "...": "mesmo formato" },
  "acumuladoAno": { "ano": 2026, "mesesComTaxa": 2, "taxaOcupacaoMedia": 68.00, "mesesComFluxo": 2, "fluxoTotal": 190000.00 }
}

// GET /api/dashboard/serie-mensal (trecho)
{ "indicador": "TAXA_OCUPACAO", "modalidade": "CONSOLIDADA",
  "anos": [ { "ano": 2026, "meses": [ { "mes": 1, "valor": 70.00, "origem": "SISTEMA" },
                                      { "mes": 3, "valor": null, "origem": null } ] } ] }
```

### Série histórica

| Método | Caminho | Perfil | Descrição |
|---|---|---|---|
| GET | `/api/historico?indicador=&modalidade=&ano=&tipo=` | todos | Valores importados |
| POST | `/api/historico` | ADMIN | Grava um valor (substitui o da mesma combinação) |
| PUT | `/api/historico/{id}` | ADMIN | Corrige um valor, inclusive o indicador, o período e o tipo (mesmo corpo do POST) |
| DELETE | `/api/historico/{id}` | ADMIN | Exclui um valor (204) |
| POST | `/api/historico/importacao` | ADMIN | Importa CSV (multipart: `arquivo` e `fonte`) |

```json
// POST /api/historico — informe o mês OU o feriado; sem tipo = valor geral
{ "indicador": "TAXA_OCUPACAO", "modalidade": "CONSOLIDADA", "ano": 2024, "mes": 7,
  "feriadoCodigo": null, "tipoHospedagemCodigo": null, "valor": 68.40, "observacao": null,
  "fonte": "Indicadores do Turismo 2017-2025" }
```

**CSV da importação** (como o Excel em português salva: `;` e vírgula decimal; UTF-8 ou o
padrão do Windows). O cabeçalho é opcional:

```
indicador;modalidade;ano;mes;feriado;tipo;valor;observacao
TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;63,45;
FLUXO_TURISTAS;CONSOLIDADA;2024;;CARNAVAL;;98.512;
DIARIA_MEDIA;CONSOLIDADA;2024;7;;HOTEL;412,90;tarifa de balcão
```

Ponto é separador de milhar (`98.512` = 98512) e vírgula, de decimal. Com qualquer erro, nada é
gravado e a resposta é **422** com a lista de erros por linha:

```json
{ "linhasLidas": 3, "inseridos": 0, "atualizados": 0,
  "erros": [ { "linha": 3, "mensagem": "feriado inválido: CARNAVALL (veja /api/referencias)" } ] }
```

Sem erros, **200** com `inseridos` e `atualizados` (gravar de novo a mesma combinação atualiza o valor).

---

## Fluxo de uma pesquisa, de ponta a ponta

1. ADMIN cria a pesquisa (`POST /api/rodadas`) e inclui as campanhas (`POST /api/rodadas/{id}/campanhas`).
2. ADMIN abre (`POST /api/rodadas/{id}/abrir`) e avisa os parceiros.
3. Parceiro entra, vê `GET /api/respostas/pendencias` e responde com
   `PUT /api/campanhas/{campanhaId}/respostas/{estabelecimentoId}`. Pode corrigir até o encerramento.
4. ADMIN acompanha em `GET /api/rodadas/{id}/acompanhamento` e confere `GET /api/campanhas/{id}/previa`.
   Se quiser, digita as diárias (`PUT /api/campanhas/{id}/diarias`).
5. ADMIN encerra (`POST /api/rodadas/{id}/encerrar`), lança as respostas que chegaram depois por
   telefone ou WhatsApp (mesmo `PUT` das respostas) e publica cada campanha
   (`POST /api/campanhas/{id}/publicacao`).
6. O resultado aparece em `GET /api/campanhas/{id}/resultado` e nos dashboards.
