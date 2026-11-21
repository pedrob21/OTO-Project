# language: pt
@F05 @inventario
Funcionalidade: Inventário e catálogo
  Para manter o inventário oficial atualizado
  Como parceiro ou ADMIN
  Quero marcar os itens de cada estabelecimento sem perder o que é da Secretaria

  Contexto:
    Dado o cenário de referência carregado

  Cenário: Parceiro marca os itens do próprio estabelecimento
    Quando o PARCEIRO A marca no inventário do "Hotel A" os itens:
      | item                   | complemento       |
      | GERAL.ESTACIONAMENTO   | Coberto, 20 vagas |
      | GERAL.MANOBRISTA       |                   |
    Então a resposta tem status 200
    E o inventário do "Hotel A" tem os itens:
      | item                   | complemento       |
      | GERAL.ESTACIONAMENTO   | Coberto, 20 vagas |
      | GERAL.MANOBRISTA       |                   |

  Cenário: Grupo exclusivo da Secretaria é preservado quando o parceiro envia o inventário
    Dado que o ADMIN marcou no inventário do "Hotel A" os itens:
      | item                               | complemento |
      | INDICADORES_ODS.ENERGIA_RENOVAVEL  |             |
      | GERAL.MANOBRISTA                   |             |
    Quando o PARCEIRO A marca no inventário do "Hotel A" os itens:
      | item                        | complemento       |
      | GERAL.ESTACIONAMENTO        | Coberto, 20 vagas |
      | INDICADORES_ODS.SELO_IDOSO  |                   |
    Então a resposta tem status 200
    E o inventário do "Hotel A" tem os itens:
      | item                               | complemento       |
      | GERAL.ESTACIONAMENTO               | Coberto, 20 vagas |
      | INDICADORES_ODS.ENERGIA_RENOVAVEL  |                   |

  Cenário: Item que não aceita complemento é recusado
    Quando o PARCEIRO A marca no inventário do "Hotel A" os itens:
      | item              | complemento |
      | GERAL.MANOBRISTA  | Dois turnos |
    Então a resposta tem status 422
    E a mensagem de erro é "O item \"Manobrista\" não aceita complemento."

  Cenário: Item marcado não sai do catálogo, só é inativado
    Dado que o PARCEIRO A marcou no inventário do "Hotel A" os itens:
      | item                   | complemento       |
      | GERAL.ESTACIONAMENTO   | Coberto, 20 vagas |
    Quando o ADMIN tenta excluir do catálogo o item "GERAL.ESTACIONAMENTO"
    Então a resposta tem status 422
    E a mensagem de erro é "Há estabelecimentos com este item marcado. Inative o item em vez de excluir."
    Quando o ADMIN inativa no catálogo o item "GERAL.ESTACIONAMENTO"
    Então a resposta tem status 204
    E o formulário do inventário não oferece o item "GERAL.ESTACIONAMENTO"
    E o inventário do "Hotel A" tem os itens:
      | item                   | complemento       |
      | GERAL.ESTACIONAMENTO   | Coberto, 20 vagas |

  Cenário: Grupo com itens não é excluído
    Quando o ADMIN tenta excluir do catálogo o grupo "GERAL"
    Então a resposta tem status 422
    E a mensagem de erro é "O grupo tem itens. Exclua os itens antes ou inative o grupo."
