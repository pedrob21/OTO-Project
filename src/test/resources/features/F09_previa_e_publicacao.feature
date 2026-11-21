# language: pt
@F09 @smoke @publicacao
Funcionalidade: Prévia e publicação
  Para não divulgar resultado sem representatividade
  Como ADMIN
  Quero conferir a prévia e só publicar com a pesquisa encerrada e 70% dos leitos respondentes

  Contexto:
    Dado o cenário de referência carregado
    E as respostas da massa para o "Hotel A" e o "Hotel B"

  Cenário: Prévia lista o que impede a publicação
    Quando o ADMIN pede a prévia da campanha "1º de Maio 2026"
    Então a resposta tem status 200
    E a prévia não permite publicar
    E a prévia aponta o impedimento "A pesquisa 001/26 precisa ser encerrada antes da publicação."
    E a prévia aponta o impedimento de 68,38% dos leitos

  Cenário: Publicação com a pesquisa aberta é recusada
    Quando o ADMIN publica a campanha "1º de Maio 2026"
    Então a resposta tem status 422
    E a mensagem de erro é "Encerre a pesquisa 001/26 antes de publicar."

  Cenário: Publicação abaixo de 70% dos leitos é bloqueada
    Dado que o ADMIN encerrou a pesquisa "001/26"
    Quando o ADMIN publica a campanha "1º de Maio 2026"
    Então a resposta tem status 422
    E a mensagem de erro é "Leitos respondentes: 68,38%. O mínimo para publicar é 70,00%."
    E o VISUALIZADOR não encontra o resultado da campanha "1º de Maio 2026"

  Cenário: Com a resposta da Pousada C a campanha atinge o mínimo e é publicada
    Dado que o ADMIN encerrou a pesquisa "001/26"
    E o ADMIN lançou a resposta da massa para a "Pousada C"
    Quando o ADMIN publica a campanha "1º de Maio 2026"
    Então a resposta tem status 200
    E os indicadores calculados são:
      | indicador                   | valor |
      | Taxa de ocupação (%)        | 55,00 |
      | Leitos respondentes (%)     | 89,74 |
    E o VISUALIZADOR vê a campanha "1º de Maio 2026" entre as publicadas

  Cenário: Campanha publicada não recebe novas respostas
    Dado que o ADMIN encerrou a pesquisa "001/26"
    E o ADMIN lançou a resposta da massa para a "Pousada C"
    E a campanha "1º de Maio 2026" publicada
    Quando o ADMIN envia a resposta da "Pousada C": taxa "45,00"% e diária R$ "300,00"
    Então a resposta tem status 422
    E a mensagem de erro é "A campanha já foi publicada. Cancele a publicação para mudar as respostas."

  Cenário: Pesquisa com campanha publicada não é reaberta
    Dado que o ADMIN encerrou a pesquisa "001/26"
    E o ADMIN lançou a resposta da massa para a "Pousada C"
    E a campanha "1º de Maio 2026" publicada
    Quando o ADMIN reabre a pesquisa "001/26" com prazo de 5 dias
    Então a resposta tem status 422
    E a mensagem de erro é "A pesquisa tem campanha publicada. Cancele a publicação antes de reabrir."

  Cenário: Cancelamento da publicação tira o resultado do ar e libera a reabertura
    Dado que o ADMIN encerrou a pesquisa "001/26"
    E o ADMIN lançou a resposta da massa para a "Pousada C"
    E a campanha "1º de Maio 2026" publicada
    Quando o ADMIN cancela a publicação da campanha "1º de Maio 2026"
    Então a resposta tem status 204
    E o VISUALIZADOR não encontra o resultado da campanha "1º de Maio 2026"
    Quando o ADMIN reabre a pesquisa "001/26" com prazo de 5 dias
    Então a resposta tem status 200
