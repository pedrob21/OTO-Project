# OTO-Project

Backend do sistema do Observatório de Turismo de Olímpia (Spring Boot + PostgreSQL):
inventário dos meios de hospedagem, pesquisas de ocupação com os parceiros, cálculo e
publicação dos resultados pela metodologia do OTO, série histórica e dados dos dashboards.

## Como rodar

## Como rodar

1. Copie `.env.example` para `.env` e preencha:
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
- Guia para o front-end, com perfis, erros, exemplos e o fluxo de uma pesquisa: [docs/API.md](docs/API.md).

Perfis: `ADMIN` (tudo), `PARCEIRO` (inventário e respostas dos seus estabelecimentos, resultados
gerais) e `VISUALIZADOR` (só consulta). Respostas individuais de ocupação são sigilosas: só o
ADMIN e o próprio parceiro as veem.

A equipe do OTO (`ADMIN`) pode digitar ou corrigir à mão qualquer dado de entrada, inclusive os
que normalmente chegam pela pesquisa: respostas (também depois do encerramento, até publicar),
diárias médias por tipo, capacidade, RHC, parâmetros, catálogo do inventário, tabelas de
referência e série histórica. Os resultados não são digitados: saem sempre do cálculo. A lista
completa está em [docs/API.md](docs/API.md#preenchimento-manual-pela-equipe-do-oto).

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
