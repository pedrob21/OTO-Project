# language: pt
@F11 @smoke @publicacao @sigilo
Funcionalidade: Divulgação com sigilo
  Para que os parceiros confiem no sistema ao responder
  Como parceiro
  Quero que a ocupação do meu estabelecimento nunca possa ser identificada nos resultados

  Contexto:
    Dado o cenário de referência carregado
    E as respostas da massa para o "Hotel A" e o "Hotel B"
    E que o ADMIN encerrou a pesquisa "001/26"
    E o ADMIN lançou a resposta da massa para a "Pousada C"

  Cenário: Tipos com menos de 3 respondentes ficam ocultos para quem não é ADMIN
    Dado a campanha "1º de Maio 2026" publicada
    Quando o VISUALIZADOR consulta o resultado da campanha "1º de Maio 2026"
    Então a resposta tem status 200
    E o campo "taxaOcupacao" da resposta é "55,00"
    E o tipo "HOTEL" aparece com os valores ocultos
    E o tipo "POUSADA" aparece com os valores ocultos

  Cenário: O ADMIN vê todos os tipos
    Dado a campanha "1º de Maio 2026" publicada
    Quando o ADMIN consulta o resultado da campanha "1º de Maio 2026"
    Então o tipo "HOTEL" aparece com a taxa de ocupação de 60,00%
    E o tipo "POUSADA" aparece com a taxa de ocupação de 40,00%

  Cenário: Tipo com 3 respondentes aparece para todos
    Dado que o ADMIN cadastrou o hotel "Hotel F" com o CNPJ "66666666000106", 10 UH, 20 leitos e no máximo 2 pessoas por UH
    E o ADMIN enviou a resposta do "Hotel F": taxa "70,00"% e diária R$ "350,00"
    E a campanha "1º de Maio 2026" publicada
    Quando o PARCEIRO C consulta o resultado da campanha "1º de Maio 2026"
    Então o tipo "HOTEL" aparece com a taxa de ocupação de 62,50%
    E o tipo "POUSADA" aparece com os valores ocultos

  Cenário: CSV do resultado sai no formato do Excel e sem dados individuais
    Dado a campanha "1º de Maio 2026" publicada
    Quando o VISUALIZADOR baixa o CSV do resultado da campanha "1º de Maio 2026"
    Então a resposta tem status 200
    E o CSV está no formato do Excel em português
    E o CSV contém a linha "Taxa de ocupação (%);55,00"
    E o CSV contém a linha "Hotel;2;2;30;;;;"
    E o CSV não contém "Hotel A"

  Cenário: Parceiro não vê o histórico da resposta de outro parceiro
    Quando o PARCEIRO C consulta o histórico da resposta do "Hotel A"
    Então a resposta tem status 403
