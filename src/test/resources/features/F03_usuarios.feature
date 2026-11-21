# language: pt
@F03 @usuarios
Funcionalidade: Usuários
  Para que cada parceiro acesse apenas o que é seu
  Como ADMIN
  Quero cadastrar, alterar e desativar usuários vinculando o parceiro ao estabelecimento

  Contexto:
    Dado o cenário de referência carregado

  Cenário: Parceiro é cadastrado com o estabelecimento e recebe um convite
    Quando o ADMIN cadastra o parceiro "gerente@hotelb.test" do estabelecimento "Hotel B"
    Então a resposta tem status 201
    E o campo "convite.tipo" da resposta é "CONVITE"
    E o campo "usuario.primeiroAcessoPendente" da resposta é "verdadeiro"
    E o campo "usuario.estabelecimentos[0].nomeFantasia" da resposta é "Hotel B"

  Cenário: Parceiro sem estabelecimento é recusado
    Quando o ADMIN cadastra o parceiro "sem.vinculo@oto.test" sem estabelecimento
    Então a resposta tem status 422
    E a mensagem de erro é "Informe ao menos um estabelecimento para o parceiro."

  Cenário: E-mail repetido é recusado, mesmo com letras maiúsculas
    Quando o ADMIN cadastra o usuário "Parceiro.A@OTO.test" com o perfil "VISUALIZADOR"
    Então a resposta tem status 422
    E a mensagem de erro é "Já existe um usuário com este e-mail."

  Cenário: Usuário desativado não consegue entrar
    Quando o ADMIN chama POST "/api/usuarios/{visualizador@oto.test}/inativar"
    Então a resposta tem status 204
    E "visualizador@oto.test" não consegue entrar com a senha "Senha@Teste1"

  Cenário: ADMIN não tira o próprio perfil de administrador
    Quando o ADMIN chama PUT "/api/usuarios/{admin@oto.test}" com:
      """
      { "nome": "Equipe OTO", "perfil": "VISUALIZADOR", "estabelecimentoIds": [] }
      """
    Então a resposta tem status 422
    E a mensagem de erro é "Você não pode tirar o seu próprio perfil de administrador."
