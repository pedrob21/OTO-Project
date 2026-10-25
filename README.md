# OTO-Project

Backend do sistema do Observatório de Turismo de Olímpia (Spring Boot + PostgreSQL):
inventário dos meios de hospedagem, pesquisas de ocupação com os parceiros, cálculo e
publicação dos resultados pela metodologia do OTO, série histórica e dados dos dashboards.

## Como rodar

1. Copie o arquivo`.env.example` renomeando para `.env` e preencha:
    - `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: o container do PostgreSQL cria esse
      banco e esse usuário na primeira subida, então não é preciso criar nada antes;
    - `JWT_SEGREDO`: segredo dos tokens de login, com pelo menos 32 caracteres
      (gere com `openssl rand -hex 32`). Sem ele a aplicação não sobe;
    - `ADMIN_EMAIL` e `ADMIN_SENHA`: o primeiro administrador, criado na subida se ainda não
      existir um usuário com esse e-mail.

   O `.env` não vai para o Git: cada pessoa cria o seu.
2. No IntelliJ, abra a pasta do projeto (a que tem o `build.gradle`). Com o Docker aberto, rode a
   task `bootRun` (aba Gradle → *Tasks → application → bootRun*), a classe `OtoProjectApplication`
   ou `./gradlew bootRun` no terminal. O Spring Boot sobe o PostgreSQL do `compose.yaml`, espera
   ele ficar pronto e conecta sozinho; o Flyway aplica as migrations em seguida. O `compose.yaml`
   e o `.env` são lidos da pasta em que a aplicação roda: com uma pasta acima aberta no IntelliJ,
   a subida falha com `No Docker Compose file found`.
3. Entre com o e-mail e a senha do administrador (`POST /api/auth/login`) e troque a senha
   pelo sistema (`PUT /api/me/senha`).

O banco continua rodando quando a aplicação para. Para desligar: `docker compose stop`.

Para rodar tudo no Docker, sem IDE (por exemplo, para o front-end usar a API):

```bash
docker compose --profile app up --build   # http://localhost:8080
```

As variáveis do `.env` do banco só são aplicadas quando o volume é criado. Para trocar a
senha do banco depois, recrie com `docker compose down -v` (isso apaga os dados locais).

## API

- Documentação interativa: http://localhost:8080/swagger-ui.html (use *Authorize* com o token do login).
- Exemplos de corpo e de resposta de cada rota, e o fluxo de uma pesquisa: [docs/API.md](docs/API.md).

### Autenticação (JWT)

Todas as rotas exigem login, menos as marcadas como **público** na tabela de rotas.

1. O front chama `POST /api/auth/login` com `{ "email": "...", "senha": "..." }` e recebe
   `{ "token": "...", "expiraEm": "...", "usuario": { ... } }`.
2. Em todas as outras chamadas, manda o token no cabeçalho `Authorization: Bearer <token>`.
3. O token vale 8 horas. Vencido ou inválido, a API responde **401** e o front volta para o login
   (não há renovação automática).

```js
const login = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, senha }),
});
const { token, usuario } = await login.json();

const meusDados = await fetch('http://localhost:8080/api/me', {
  headers: { Authorization: `Bearer ${token}` },
});
```

O perfil vem em `usuario.perfil` (no login e em `GET /api/me`). O front pode usá-lo para mostrar
ou esconder telas, mas quem barra é a API: chamada fora do perfil recebe **403**.

O primeiro acesso não tem senha: o ADMIN cadastra o usuário e recebe um link de convite para
mandar a ele. A página de senha do front lê o token do link e chama
`POST /api/auth/definir-senha` com `{ "token": "...", "novaSenha": "..." }`.

O front precisa estar num endereço liberado em `CORS_ORIGENS` (no `.env`). O padrão libera
`localhost` e `127.0.0.1` em qualquer porta.

Erros vêm no formato Problem Details (RFC 9457), com a mensagem em `detail`:

| Status | Quando |
|---|---|
| 400 | Corpo ou parâmetro inválido; `campos` traz a mensagem de cada campo |
| 401 | Sem token, token vencido ou e-mail e senha errados no login |
| 403 | O perfil não pode usar a rota, ou o estabelecimento não é do parceiro |
| 404 | Registro não encontrado |
| 409 | Registro repetido no banco (raro) |
| 422 | Regra do OTO, como pesquisa fechada ou mínimo de leitos para publicar |

### Perfis

`ADMIN` (equipe do OTO, tudo), `PARCEIRO` (inventário e respostas dos seus estabelecimentos,
resultados gerais) e `VISUALIZADOR` (só consulta). Respostas individuais de ocupação são
sigilosas: só o ADMIN e o próprio parceiro as veem.

A equipe do OTO (`ADMIN`) pode digitar ou corrigir à mão qualquer dado de entrada, inclusive os
que normalmente chegam pela pesquisa: respostas (também depois do encerramento, até publicar),
diárias médias por tipo, capacidade, RHC, parâmetros, catálogo do inventário, tabelas de
referência e série histórica. Os resultados não são digitados: saem sempre do cálculo. A lista
completa está em [docs/API.md](docs/API.md#preenchimento-manual-pela-equipe-do-oto).

### Rotas

Na coluna **Quem pode**: **público** dispensa o token; **todos** é qualquer usuário logado;
"só os seus" quer dizer que o parceiro só acessa os estabelecimentos vinculados a ele.

**Login e conta**

| Método | Rota | Quem pode | Para quê |
|---|---|---|---|
| POST | `/api/auth/login` | público | Entrar: devolve o token e os dados do usuário |
| POST | `/api/auth/definir-senha` | público | Definir a senha pelo link de convite ou de redefinição |
| GET | `/api/me` | todos | Dados do usuário logado (para o parceiro, inclui os estabelecimentos) |
| PUT | `/api/me/senha` | todos | Trocar a própria senha (`senhaAtual`, `novaSenha`) |

**Usuários**

| Método | Rota | Quem pode | Para quê |
|---|---|---|---|
| GET | `/api/usuarios` | ADMIN | Listar usuários |
| GET | `/api/usuarios/{id}` | ADMIN | Ver um usuário |
| POST | `/api/usuarios` | ADMIN | Cadastrar usuário; devolve o link de convite para enviar |
| PUT | `/api/usuarios/{id}` | ADMIN | Alterar nome, perfil e estabelecimentos vinculados |
| POST | `/api/usuarios/{id}/inativar` · `/reativar` | ADMIN | Bloquear ou liberar o acesso |
| POST | `/api/usuarios/{id}/link-de-senha` | ADMIN | Gerar novo link de acesso (convite ou senha esquecida) |

**Tabelas de referência**

| Método | Rota | Quem pode | Para quê |
|---|---|---|---|
| GET | `/api/referencias` | todos | Tipos de hospedagem, feriados e faixas de ocupação (selects e legendas) |
| POST | `/api/referencias/tipos-hospedagem` | ADMIN | Cadastrar tipo de hospedagem |
| PUT · DELETE | `/api/referencias/tipos-hospedagem/{codigo}` | ADMIN | Alterar; excluir só se nunca foi usado |
| POST | `/api/referencias/feriados` | ADMIN | Cadastrar feriado ou evento |
| PUT · DELETE | `/api/referencias/feriados/{codigo}` | ADMIN | Alterar; excluir só se nunca foi usado |
| POST | `/api/referencias/faixas` | ADMIN | Cadastrar faixa de ocupação |
| PUT · DELETE | `/api/referencias/faixas/{codigo}` | ADMIN | Alterar; excluir só se nunca foi usada em publicação |

**Estabelecimentos e inventário**

| Método | Rota | Quem pode | Para quê |
|---|---|---|---|
| GET | `/api/estabelecimentos?tipo=&ativo=` | todos (parceiro: só os seus) | Lista resumida, com UH e leitos vigentes |
| GET | `/api/estabelecimentos/{id}` | todos (parceiro: só os seus) | Dados cadastrais e da Secretaria |
| POST | `/api/estabelecimentos` | ADMIN | Cadastrar, já com a capacidade inicial |
| PUT | `/api/estabelecimentos/{id}` | ADMIN, PARCEIRO (só os seus) | Alterar dados; o bloco da Secretaria só vale para o ADMIN |
| POST | `/api/estabelecimentos/{id}/inativar` · `/reativar` | ADMIN | Tirar ou voltar das pesquisas e dos cálculos |
| GET | `/api/estabelecimentos/{id}/capacidades` | todos (parceiro: só os seus) | Histórico de UH e leitos |
| POST | `/api/estabelecimentos/{id}/capacidades` | ADMIN, PARCEIRO (só os seus) | Nova capacidade a partir de uma data |
| PUT · DELETE | `/api/estabelecimentos/{id}/capacidades/{capacidadeId}` | ADMIN | Corrigir ou excluir um lançamento digitado errado |
| GET | `/api/estabelecimentos/{id}/inventario` | todos (parceiro: só os seus) | Itens que o estabelecimento possui |
| PUT | `/api/estabelecimentos/{id}/inventario` | ADMIN, PARCEIRO (só os seus) | Salvar os itens marcados (o parceiro não altera os grupos da Secretaria) |
| GET | `/api/inventario/catalogo` | todos | Perguntas do inventário (grupos e itens ativos) para montar o formulário |

**Catálogo do inventário**

| Método | Rota | Quem pode | Para quê |
|---|---|---|---|
| GET | `/api/inventario/catalogo/completo` | ADMIN | Catálogo inteiro, com grupos e itens inativos |
| POST | `/api/inventario/grupos` | ADMIN | Criar grupo de perguntas |
| PUT · DELETE | `/api/inventario/grupos/{id}` | ADMIN | Alterar; excluir só grupo sem itens |
| POST | `/api/inventario/grupos/{id}/inativar` · `/reativar` | ADMIN | Tirar ou voltar o grupo do formulário |
| POST | `/api/inventario/grupos/{grupoId}/itens` | ADMIN | Criar item no grupo |
| PUT · DELETE | `/api/inventario/itens/{id}` | ADMIN | Alterar; excluir só item que nenhum estabelecimento marcou |
| POST | `/api/inventario/itens/{id}/inativar` · `/reativar` | ADMIN | Tirar ou voltar o item do formulário |

**Hospedagem caseira (RHC) e parâmetros de cálculo**

| Método | Rota | Quem pode | Para quê |
|---|---|---|---|
| GET | `/api/rhc` | ADMIN, VISUALIZADOR | Totais de RHC por vigência |
| GET | `/api/rhc/vigente?data=` | ADMIN, VISUALIZADOR | Totais vigentes na data (padrão: hoje) |
| POST | `/api/rhc` | ADMIN | Lançar totais a partir de uma data |
| PUT · DELETE | `/api/rhc/{id}` | ADMIN | Corrigir ou excluir um lançamento |
| GET | `/api/parametros-calculo` | ADMIN, VISUALIZADOR | Todas as vigências dos parâmetros |
| GET | `/api/parametros-calculo/vigente?data=` | todos | Parâmetros vigentes na data (notas metodológicas) |
| POST | `/api/parametros-calculo` | ADMIN | Nova vigência |
| PUT · DELETE | `/api/parametros-calculo/{id}` | ADMIN | Corrigir ou excluir, se nenhuma publicação usou |

**Pesquisas e campanhas**

| Método | Rota | Quem pode | Para quê |
|---|---|---|---|
| GET | `/api/rodadas?status=` | ADMIN | Listar pesquisas, com as campanhas |
| GET | `/api/rodadas/{id}` | ADMIN | Ver uma pesquisa |
| POST | `/api/rodadas` | ADMIN | Criar pesquisa (em rascunho) |
| PUT · DELETE | `/api/rodadas/{id}` | ADMIN | Alterar ou excluir (só em rascunho) |
| POST | `/api/rodadas/{id}/campanhas` | ADMIN | Incluir campanha (um mês ou um feriado) |
| PUT · DELETE | `/api/rodadas/{id}/campanhas/{campanhaId}` | ADMIN | Alterar ou excluir campanha (só em rascunho) |
| POST | `/api/rodadas/{id}/abrir` | ADMIN | Abrir para respostas, com o prazo |
| POST | `/api/rodadas/{id}/prorrogar` | ADMIN | Mudar o prazo |
| POST | `/api/rodadas/{id}/encerrar` | ADMIN | Fechar para respostas dos parceiros |
| POST | `/api/rodadas/{id}/reabrir` | ADMIN | Reabrir (se nenhuma campanha foi publicada) |
| GET | `/api/rodadas/{id}/acompanhamento` | ADMIN | Quem respondeu e quem falta, com contatos e % de leitos |

**Respostas**

| Método | Rota | Quem pode | Para quê |
|---|---|---|---|
| GET | `/api/respostas/pendencias` | PARCEIRO | Campanhas abertas para cada estabelecimento dele (tela "Responder pesquisa") |
| PUT | `/api/campanhas/{campanhaId}/respostas/{estabelecimentoId}` | ADMIN, PARCEIRO (só os seus) | Enviar ou corrigir a resposta (`taxaOcupacao` e `diariaMedia`); vale o último envio |
| GET | `/api/respostas/minhas` | PARCEIRO | Respostas dele, com a taxa da cidade quando publicada |
| GET | `/api/respostas/{id}/historico` | ADMIN, PARCEIRO (só os seus) | Todas as versões enviadas |
| GET | `/api/campanhas/{campanhaId}/respostas` | ADMIN | Respostas da campanha (dado individual) |

O parceiro responde enquanto a pesquisa está aberta; o ADMIN também depois do encerramento,
até a campanha ser publicada.

**Diárias e resultados**

| Método | Rota | Quem pode | Para quê |
|---|---|---|---|
| GET · PUT | `/api/campanhas/{campanhaId}/diarias` | ADMIN | Diárias médias digitadas pela equipe (por tipo e a geral) |
| GET | `/api/campanhas/{id}/previa` | ADMIN | Simular o resultado e ver o que impede a publicação |
| POST | `/api/campanhas/{id}/publicacao` | ADMIN | Calcular e publicar |
| DELETE | `/api/campanhas/{id}/publicacao` | ADMIN | Cancelar uma publicação feita por engano |
| GET | `/api/campanhas/{id}/resultado` | todos | Resultado publicado (para quem não é ADMIN, tipos com menos de 3 respondentes vêm ocultos) |
| GET | `/api/campanhas/{id}/resultado.csv` | todos | O mesmo resultado em CSV para Excel |
| GET | `/api/campanhas/publicadas?modalidade=&tipoPeriodo=&ano=` | todos | Campanhas publicadas, mais recentes primeiro |

**Dashboards e série histórica**

| Método | Rota | Quem pode | Para quê |
|---|---|---|---|
| GET | `/api/dashboard/resumo?modalidade=` | todos | Cartões da tela inicial: último mês e último feriado, comparativo com o ano anterior e acumulado do ano |
| GET | `/api/dashboard/serie-mensal?indicador=&modalidade=&anoInicio=&anoFim=` | todos | Gráfico mês a mês, uma linha por ano |
| GET | `/api/dashboard/feriados?modalidade=&feriado=&anoInicio=&anoFim=` | todos | Gráfico dos feriados, ano a ano |
| GET | `/api/historico?indicador=&modalidade=&ano=&tipo=` | todos | Valores da série histórica (antes do sistema) |
| POST | `/api/historico` | ADMIN | Gravar um valor (substitui o da mesma combinação) |
| PUT · DELETE | `/api/historico/{id}` | ADMIN | Corrigir ou excluir um valor |
| POST | `/api/historico/importacao` | ADMIN | Importar CSV (multipart: `arquivo` e `fonte`) |

Parâmetros depois de `?` são opcionais. Valores aceitos: `modalidade` = `CONSOLIDADA` ou
`EXPECTATIVA`; `tipoPeriodo` = `MENSAL` ou `FERIADO`; `indicador` = `TAXA_OCUPACAO`,
`FLUXO_TURISTAS` ou `DIARIA_MEDIA`; `status` = `RASCUNHO`, `ABERTA` ou `ENCERRADA`; os códigos de
tipo e de feriado vêm de `GET /api/referencias`.

## Testes

```bash
./gradlew test
```

- `CalculadoraOcupacaoTest`: a metodologia de cálculo, sem banco.
- `OtoProjectApplicationTests`: sobe a aplicação inteira contra o PostgreSQL do `compose.yaml`
  (o Spring Boot inicia o container sozinho, então o Docker precisa estar aberto e o `.env`
  preenchido). Confere migrations, mapeamento das entidades, consultas e configuração.

## Organização do código

| Pacote | Conteúdo |
|---|---|
| `auth` | Login, convites e redefinição de senha (links de uso único) |
| `usuario` | Usuários, perfis e o administrador inicial |
| `estabelecimento` | Inventário dos meios de hospedagem, capacidade (UH e leitos) e RHC |
| `inventario` | Catálogo de serviços, equipamentos, acessibilidade e ODS |
| `pesquisa` | Pesquisas (rodadas), campanhas, respostas, diárias digitadas e parâmetros de cálculo |
| `calculo` | Metodologia (`CalculadoraOcupacao`), prévia, publicação e resultados |
| `historico` | Série histórica de 2017 em diante e importação de CSV |
| `dashboard` | Dados dos gráficos da tela inicial |
| `referencia` | Tipos de hospedagem, feriados e faixas de ocupação (mantidos pela equipe) |
| `comum` | Segurança (JWT), tratamento de erros, auditoria, OpenAPI e geração de códigos |

## Cálculo

`CalculadoraOcupacao` reproduz as fórmulas da planilha de tabulação do OTO (abas MH e
TABULAÇÃO): taxa ponderada pelas UH, fluxo de respondentes, não respondentes, RHC e day use,
representatividade pelos leitos, diária, RevPAR e receita por tipo, e a distribuição dos
respondentes por faixa. Com as mesmas entradas, os resultados batem com os da planilha.

A diária de cada tipo é a média das respostas ponderada pelos leitos. Quando a equipe digita a
diária da campanha (como na planilha, que usa a pesquisa de tarifas), vale a digitada.

Uma diferença esperada: o sistema sempre calcula os leitos úteis como `UH × (1 + máximo) / 2`.
Na planilha de 2026, algumas linhas da aba MH têm esse valor digitado à mão (arredondado, como
28 no lugar de 27,5, ou zerado); nesses casos os totais do sistema ficam um pouco diferentes.

## Banco de dados

O schema é criado e versionado pelo Flyway, com os scripts de
`src/main/resources/db/migration`, aplicados na subida da aplicação. O Hibernate só
valida as entidades (`ddl-auto=validate`); ele nunca cria tabelas.

Regras para migrations:

- Nome no formato `V<número>__descricao.sql` (dois sublinhados). Ex.: `V11__adicionar_coluna_x.sql`.
- Nunca altere uma migration que já foi aplicada (o Flyway confere o checksum e a aplicação não sobe).
  Para mudar algo, crie uma nova migration.
- Combine no grupo quem vai usar o próximo número, para evitar duas migrations com a mesma versão.
- Para recriar o banco local do zero: `docker compose down -v` e suba de novo.