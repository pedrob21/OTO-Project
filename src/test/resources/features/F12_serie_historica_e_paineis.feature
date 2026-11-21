# language: pt
@F12 @historico
Funcionalidade: Série histórica e painéis
  Para comparar os resultados com os anos anteriores
  Como ADMIN e gestora da Secretaria
  Quero importar a série histórica e ver no painel o que vale de cada fonte

  Contexto:
    Dado o cenário de referência carregado

  Cenário: Importação válida grava todas as linhas
    Quando o ADMIN importa a série histórica do arquivo:
      """
      indicador;modalidade;ano;mes;feriado;tipo;valor;observacao
      TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;63,45;
      FLUXO_TURISTAS;CONSOLIDADA;2024;;CARNAVAL;;98.512;
      DIARIA_MEDIA;CONSOLIDADA;2024;7;;HOTEL;412,90;tarifa de balcão
      """
    Então a resposta tem status 200
    E a importação informa 3 linhas lidas, 3 inseridas e 0 atualizadas
    E a série histórica tem 3 valores
    E a série histórica guarda o fluxo de turistas de 98.512,00 no feriado "CARNAVAL" de 2024

  Cenário: Importação com erro não grava nada
    Quando o ADMIN importa a série histórica do arquivo:
      """
      indicador;modalidade;ano;mes;feriado;tipo;valor;observacao
      TAXA_OCUPACAO;CONSOLIDADA;2024;1;;;63,45;
      FLUXO_TURISTAS;CONSOLIDADA;2024;;CARNAVALL;;98.512;
      """
    Então a resposta tem status 422
    E a importação aponta o erro "feriado inválido: CARNAVALL (veja /api/referencias)" na linha 3
    E a série histórica tem 0 valores

  Cenário: Arquivo salvo pelo Excel do Windows mantém os acentos e reimportar atualiza
    Quando o ADMIN importa a série histórica do arquivo salvo pelo Excel do Windows:
      """
      DIARIA_MEDIA;CONSOLIDADA;2024;7;;HOTEL;412,90;tarifa de balcão
      """
    E o ADMIN importa a série histórica do arquivo salvo pelo Excel do Windows:
      """
      DIARIA_MEDIA;CONSOLIDADA;2024;7;;HOTEL;415,00;tarifa de balcão
      """
    Então a importação informa 1 linhas lidas, 0 inseridas e 1 atualizadas
    E a série histórica guarda a observação "tarifa de balcão"

  Cenário: No painel, o resultado do sistema prevalece sobre o histórico do mesmo período
    Dado que o ADMIN gravou na série histórica a taxa de ocupação de 50,00% do feriado "DIA_DO_TRABALHO" de 2026
    E as respostas da massa para o "Hotel A" e o "Hotel B"
    E que o ADMIN encerrou a pesquisa "001/26"
    E o ADMIN lançou a resposta da massa para a "Pousada C"
    E a campanha "1º de Maio 2026" publicada
    Quando o VISUALIZADOR consulta o painel do feriado "DIA_DO_TRABALHO" de 2026
    Então o painel mostra a taxa de ocupação de 55,00% com origem "SISTEMA"

  Cenário: O resumo compara o último feriado com o mesmo feriado do ano anterior
    Dado que o ADMIN gravou na série histórica a taxa de ocupação de 50,00% do feriado "DIA_DO_TRABALHO" de 2025
    E as respostas da massa para o "Hotel A" e o "Hotel B"
    E que o ADMIN encerrou a pesquisa "001/26"
    E o ADMIN lançou a resposta da massa para a "Pousada C"
    E a campanha "1º de Maio 2026" publicada
    Quando o VISUALIZADOR consulta o resumo do painel
    Então o campo "ultimoFeriado.taxaOcupacao" da resposta é "55,00"
    E o campo "ultimoFeriado.anoAnterior.ano" da resposta é "2025"
    E o campo "ultimoFeriado.anoAnterior.diferencaTaxaPontos" da resposta é "5,00"
    E o campo "ultimoFeriado.anoAnterior.origem" da resposta é "HISTORICO"
