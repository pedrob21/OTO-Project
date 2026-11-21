# language: pt
@F06 @smoke @pesquisa
Funcionalidade: Ciclo da pesquisa
  Para organizar cada rodada de coleta
  Como ADMIN
  Quero criar, abrir, encerrar e reabrir pesquisas, cada uma com suas campanhas

  Contexto:
    Dado o cenário de referência carregado

  Cenário: Pesquisa nova fica em rascunho e pode ser editada
    Quando o ADMIN cria a pesquisa "002/26" com a campanha mensal "Consolidada junho/2026" de 01/06/2026 a 30/06/2026
    Então a pesquisa "002/26" está com o status "RASCUNHO"
    Quando o ADMIN chama PUT "/api/rodadas/{002/26}" com:
      """
      { "codigo": "002/26", "titulo": "Pesquisa de junho de 2026", "dataAbertura": null, "prazoResposta": null }
      """
    Então a resposta tem status 200
    E o campo "titulo" da resposta é "Pesquisa de junho de 2026"

  Cenário: Pesquisa sem campanha não é aberta
    Dado que o ADMIN criou a pesquisa "003/26" sem campanhas
    Quando o ADMIN abre a pesquisa "003/26" com prazo de 10 dias
    Então a resposta tem status 422
    E a mensagem de erro é "Inclua ao menos uma campanha antes de abrir a pesquisa."

  Cenário: Pesquisa aberta recebe respostas e não pode mais ser editada
    Dado que o ADMIN criou a pesquisa "002/26" com a campanha mensal "Consolidada junho/2026" de 01/06/2026 a 30/06/2026
    Quando o ADMIN abre a pesquisa "002/26" com prazo de 10 dias
    Então a resposta tem status 200
    E a pesquisa "002/26" está com o status "ABERTA"
    Quando o ADMIN chama PUT "/api/rodadas/{002/26}" com:
      """
      { "codigo": "002/26", "titulo": "Outro título", "dataAbertura": null, "prazoResposta": null }
      """
    Então a resposta tem status 422
    E a mensagem de erro é "A pesquisa 002/26 já foi aberta e não pode mais ser alterada."

  Cenário: Pesquisa encerrada pode ser reaberta enquanto nada foi publicado
    Dado que o ADMIN criou a pesquisa "002/26" com a campanha mensal "Consolidada junho/2026" de 01/06/2026 a 30/06/2026
    E que o ADMIN abriu a pesquisa "002/26" com prazo de 10 dias
    Quando o ADMIN encerra a pesquisa "002/26"
    Então a pesquisa "002/26" está com o status "ENCERRADA"
    Quando o ADMIN reabre a pesquisa "002/26" com prazo de 5 dias
    Então a resposta tem status 200
    E a pesquisa "002/26" está com o status "ABERTA"

  Cenário: Prorrogação para uma data que já passou é recusada
    Quando o ADMIN prorroga a pesquisa "001/26" para ontem
    Então a resposta tem status 422
    E a mensagem de erro é "O novo prazo de resposta não pode ser anterior a hoje."

  Cenário: Prorrogação para um prazo anterior ao atual é recusada
    Dado que o ADMIN criou a pesquisa "002/26" com a campanha mensal "Consolidada junho/2026" de 01/06/2026 a 30/06/2026
    E que o ADMIN abriu a pesquisa "002/26" com prazo de 10 dias
    Quando o ADMIN prorroga a pesquisa "002/26" com prazo de 5 dias
    Então a resposta tem status 422
    E a mensagem de erro começa com "O novo prazo deve ser posterior ao prazo atual ("

  Cenário: Prorrogação para o mesmo prazo é recusada
    Dado que o ADMIN criou a pesquisa "002/26" com a campanha mensal "Consolidada junho/2026" de 01/06/2026 a 30/06/2026
    E que o ADMIN abriu a pesquisa "002/26" com prazo de 10 dias
    Quando o ADMIN prorroga a pesquisa "002/26" com prazo de 10 dias
    Então a resposta tem status 422
    E a mensagem de erro começa com "O novo prazo deve ser posterior ao prazo atual ("

  Cenário: Reabertura para uma data que já passou é recusada
    Dado que o ADMIN criou a pesquisa "002/26" com a campanha mensal "Consolidada junho/2026" de 01/06/2026 a 30/06/2026
    E que o ADMIN abriu a pesquisa "002/26" com prazo de 10 dias
    E que o ADMIN encerrou a pesquisa "002/26"
    Quando o ADMIN reabre a pesquisa "002/26" para ontem
    Então a resposta tem status 422
    E a mensagem de erro é "O novo prazo de resposta não pode ser anterior a hoje."

  Cenário: Só se encerra uma pesquisa aberta
    Dado que o ADMIN criou a pesquisa "002/26" com a campanha mensal "Consolidada junho/2026" de 01/06/2026 a 30/06/2026
    Quando o ADMIN encerra a pesquisa "002/26"
    Então a resposta tem status 422
    E a mensagem de erro é "Só é possível encerrar uma pesquisa aberta."

  Cenário: Campanha com mais diárias que dias no período é recusada
    Dado que o ADMIN criou a pesquisa "002/26" sem campanhas
    Quando o ADMIN chama POST "/api/rodadas/{002/26}/campanhas" com:
      """
      { "nome": "Consolidada junho/2026", "modalidade": "CONSOLIDADA", "tipoPeriodo": "MENSAL",
        "feriadoCodigo": null, "periodoInicio": "2026-06-01", "periodoFim": "2026-06-30", "qtdDiarias": 31 }
      """
    Então a resposta tem status 422
    E a mensagem de erro é "A quantidade de diárias (31) passa o número de dias do período (30)."
