# language: pt
@F13 @referencias
Funcionalidade: Referências e parâmetros
  Para ajustar as regras sem alterar o código e manter os cálculos reproduzíveis
  Como ADMIN
  Quero manter parâmetros e tabelas de referência sem mudar o que já foi publicado

  Contexto:
    Dado o cenário de referência carregado
    E as respostas da massa para o "Hotel A" e o "Hotel B"

  Cenário: Nova vigência de parâmetros vale para os cálculos do período
    Dado que o ADMIN cadastrou os parâmetros de cálculo a partir de 01/04/2026 com mínimo de 60,00% dos leitos
    Quando o ADMIN pede a prévia da campanha "1º de Maio 2026"
    Então o campo "percentualMinimoPublicacao" da resposta é "60,00"
    E a prévia não aponta impedimento de leitos

  Cenário: Duas vigências na mesma data são recusadas
    Dado que o ADMIN cadastrou os parâmetros de cálculo a partir de 01/04/2026 com mínimo de 60,00% dos leitos
    Quando o ADMIN cadastra os parâmetros de cálculo a partir de 01/04/2026 com mínimo de 65,00% dos leitos
    Então a resposta tem status 422
    E a mensagem de erro é "Já existem parâmetros a partir desta data. Use outra data."

  Cenário: Parâmetros usados em publicação não mudam nem são excluídos
    Dado que o ADMIN encerrou a pesquisa "001/26"
    E o ADMIN lançou a resposta da massa para a "Pousada C"
    E a campanha "1º de Maio 2026" publicada
    Quando o ADMIN tenta alterar os parâmetros usados na campanha "1º de Maio 2026"
    Então a resposta tem status 422
    E a mensagem de erro é "Estes parâmetros já foram usados em resultados publicados. Cadastre uma nova vigência."
    Quando o ADMIN tenta excluir os parâmetros usados na campanha "1º de Maio 2026"
    Então a resposta tem status 422
    E a mensagem de erro é "Estes parâmetros já foram usados em resultados publicados e não podem ser excluídos."

  Cenário: Faixa usada em publicação não é excluída
    Dado que o ADMIN encerrou a pesquisa "001/26"
    E o ADMIN lançou a resposta da massa para a "Pousada C"
    E a campanha "1º de Maio 2026" publicada
    Quando o ADMIN chama DELETE "/api/referencias/faixas/BOA"
    Então a resposta tem status 422
    E a mensagem de erro é "A faixa Boa já foi usada em resultados publicados e não pode ser excluída."

  Cenário: Tipo de hospedagem em uso não é excluído
    Quando o ADMIN chama DELETE "/api/referencias/tipos-hospedagem/HOTEL"
    Então a resposta tem status 422
    E a mensagem de erro é "O tipo Hotel já é usado (estabelecimentos, resultados, diárias ou série histórica) e não pode ser excluído."

  Cenário: Novo tipo de hospedagem ganha o código a partir do nome
    Quando o ADMIN chama POST "/api/referencias/tipos-hospedagem" com:
      """
      { "codigo": null, "nome": "Chalé", "calculaReceita": false, "ordem": null }
      """
    Então a resposta tem status 201
    E o campo "codigo" da resposta é "CHALE"

  Cenário: Totais de RHC repetidos na mesma data são recusados
    Quando o ADMIN chama POST "/api/rhc" com:
      """
      { "vigenteDesde": "2026-01-01", "qtdRegistros": 3, "qtdUh": 4, "qtdLeitos": 12, "maxLeitosPorUh": 3 }
      """
    Então a resposta tem status 422
    E a mensagem de erro é "Já existem totais de RHC a partir desta data. Use outra data."
