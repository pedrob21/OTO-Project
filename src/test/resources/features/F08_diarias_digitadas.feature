# language: pt
@F08 @calculo
Funcionalidade: Diárias digitadas pela equipe
  Para usar a tarifa pesquisada pelo OTO quando ela for mais confiável
  Como ADMIN
  Quero digitar a diária média de um tipo ou a geral, substituindo a média das respostas

  Contexto:
    Dado o cenário de referência carregado
    E as respostas da massa para o "Hotel A" e o "Hotel B"

  Cenário: Diária digitada do tipo substitui a média no RevPAR e na receita do tipo
    Quando o ADMIN digita para a campanha "1º de Maio 2026" a diária média de R$ 480,00 do tipo "HOTEL"
    E o ADMIN pede a prévia da campanha "1º de Maio 2026"
    Então o tipo "HOTEL" na prévia tem diária de R$ 480,00, RevPAR de R$ 288,00 e receita de R$ 17.280,00

  Cenário: Diária geral digitada substitui a média geral
    Quando o ADMIN digita para a campanha "1º de Maio 2026" a diária média de R$ 510,00 do tipo "geral"
    E o ADMIN pede a prévia da campanha "1º de Maio 2026"
    Então os indicadores calculados são:
      | indicador         | valor  |
      | Diária média (R$) | 510,00 |
    E a prévia avisa "Diária média geral digitada pela equipe (R$ 510,00): substitui a média das respostas."

  Cenário: Lista vazia volta para a média das respostas
    Dado que o ADMIN digitou para a campanha "1º de Maio 2026" a diária média de R$ 510,00 do tipo "geral"
    Quando o ADMIN apaga as diárias digitadas da campanha "1º de Maio 2026"
    E o ADMIN pede a prévia da campanha "1º de Maio 2026"
    Então os indicadores calculados são:
      | indicador         | valor  |
      | Diária média (R$) | 437,50 |

  Cenário: Depois da publicação as diárias não mudam
    Dado que o ADMIN encerrou a pesquisa "001/26"
    E o ADMIN lançou a resposta da massa para a "Pousada C"
    E a campanha "1º de Maio 2026" publicada
    Quando o ADMIN digita para a campanha "1º de Maio 2026" a diária média de R$ 480,00 do tipo "HOTEL"
    Então a resposta tem status 422
    E a mensagem de erro é "A campanha já foi publicada. Cancele a publicação para mudar as diárias."
