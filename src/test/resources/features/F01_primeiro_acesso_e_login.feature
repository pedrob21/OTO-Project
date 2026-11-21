# language: pt
@F01 @smoke @acesso
Funcionalidade: Primeiro acesso e login
  Para usar o sistema com segurança
  Como usuário do OTO
  Quero entrar com e-mail e senha e definir minha senha pelo convite

  Contexto:
    Dado o cenário de referência carregado

  Cenário: Login com credenciais válidas devolve o token e o perfil
    Quando "admin@oto.test" entra com a senha "Senha@Teste1"
    Então a resposta tem status 200
    E o campo "usuario.perfil" da resposta é "ADMIN"
    E a resposta traz um token de acesso

  Esquema do Cenário: Login recusado sem revelar o motivo
    Quando "<email>" entra com a senha "<senha>"
    Então a resposta tem status 401
    E a mensagem de erro é "E-mail ou senha incorretos."

    Exemplos:
      | situação            | email              | senha        |
      | senha errada        | admin@oto.test     | errada-123   |
      | e-mail inexistente  | ninguem@oto.test   | Senha@Teste1 |
      | usuário inativo     | inativo@oto.test   | Senha@Teste1 |

  Cenário: Convite define a senha e libera o login
    Dado que o ADMIN cadastrou o usuário "nova.gestora@oto.test" com o perfil "VISUALIZADOR"
    Quando o convite é usado para definir a senha "NovaSenha123"
    Então a resposta tem status 204
    E "nova.gestora@oto.test" consegue entrar com a senha "NovaSenha123"

  Cenário: Link de convite já usado é recusado
    Dado que o ADMIN cadastrou o usuário "nova.gestora@oto.test" com o perfil "VISUALIZADOR"
    E o convite é usado para definir a senha "NovaSenha123"
    Quando o convite é usado para definir a senha "OutraSenha456"
    Então a resposta tem status 422
    E a mensagem de erro é "Este link expirou ou já foi usado. Peça um novo ao OTO."

  Cenário: Link de convite vencido é recusado
    Dado que o ADMIN cadastrou o usuário "nova.gestora@oto.test" com o perfil "VISUALIZADOR"
    E o convite venceu
    Quando o convite é usado para definir a senha "NovaSenha123"
    Então a resposta tem status 422
    E a mensagem de erro é "Este link expirou ou já foi usado. Peça um novo ao OTO."
    E "nova.gestora@oto.test" não consegue entrar com a senha "NovaSenha123"

  Cenário: Requisição sem login é recusada
    Quando o ANÔNIMO chama GET "/api/me"
    Então a resposta tem status 401
