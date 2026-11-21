# language: pt
@F02 @acesso
Funcionalidade: Perfis e acesso
  Para proteger os dados do OTO e dos parceiros
  Como equipe do Observatório
  Quero que cada perfil veja e faça só o que lhe cabe, validado no back-end

  Contexto:
    Dado o cenário de referência carregado

  Esquema do Cenário: <ator> chama <método> <rota>
    Quando o <ator> chama <método> "<rota>"
    Então a resposta tem status <status>

    Exemplos: Gestão de usuários é só do ADMIN
      | ator         | método | rota          | status |
      | ADMIN        | GET    | /api/usuarios | 200    |
      | PARCEIRO A   | GET    | /api/usuarios | 403    |
      | VISUALIZADOR | GET    | /api/usuarios | 403    |
      | ANÔNIMO      | GET    | /api/usuarios | 401    |

    Exemplos: Respostas individuais só para o ADMIN
      | ator         | método | rota                                       | status |
      | ADMIN        | GET    | /api/campanhas/{1º de Maio 2026}/respostas | 200    |
      | PARCEIRO A   | GET    | /api/campanhas/{1º de Maio 2026}/respostas | 403    |
      | VISUALIZADOR | GET    | /api/campanhas/{1º de Maio 2026}/respostas | 403    |
      | VISUALIZADOR | GET    | /api/campanhas/{1º de Maio 2026}/previa    | 403    |

    Exemplos: O parceiro só acessa os próprios estabelecimentos
      | ator         | método | rota                             | status |
      | PARCEIRO A   | GET    | /api/estabelecimentos/{Hotel A}   | 200    |
      | PARCEIRO A   | GET    | /api/estabelecimentos/{Pousada C} | 403    |
      | PARCEIRO C   | GET    | /api/estabelecimentos/{Pousada C} | 200    |
      | VISUALIZADOR | GET    | /api/estabelecimentos/{Pousada C} | 200    |
      | PARCEIRO A   | GET    | /api/respostas/pendencias        | 200    |
      | VISUALIZADOR | GET    | /api/respostas/pendencias        | 403    |

    Exemplos: O Visualizador só lê
      | ator         | método | rota                                       | status |
      | VISUALIZADOR | POST   | /api/estabelecimentos/{Hotel A}/inativar   | 403    |
      | PARCEIRO A   | POST   | /api/estabelecimentos/{Hotel A}/inativar   | 403    |
      | ADMIN        | POST   | /api/estabelecimentos/{Hotel A}/inativar   | 204    |
      | PARCEIRO A   | POST   | /api/rodadas/{001/26}/encerrar             | 403    |
      | VISUALIZADOR | GET    | /api/rhc                                   | 200    |
      | PARCEIRO A   | GET    | /api/rhc                                   | 403    |

  Cenário: Parceiro não responde pela empresa de outro parceiro
    Quando o PARCEIRO C envia a resposta do "Hotel A": taxa "70,00"% e diária R$ "450,00"
    Então a resposta tem status 403

  Cenário: Erro de acesso segue o formato Problem Details
    Quando o VISUALIZADOR chama GET "/api/usuarios"
    Então o erro está no formato Problem Details com status 403
    E a mensagem de erro é "Seu perfil não tem acesso a este recurso."
