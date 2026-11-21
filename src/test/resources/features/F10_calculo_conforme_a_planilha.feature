# language: pt
@F10 @calculo @regressao
Funcionalidade: Cálculo dos indicadores conforme a planilha
  Para substituir a planilha de tabulação sem mudar os números
  Como equipe do OTO
  Quero que o sistema calcule os indicadores exatamente como a planilha

  Contexto:
    Dado o cenário de referência carregado
    E as respostas da massa para o "Hotel A" e o "Hotel B"

  Cenário: Indicadores calculados na prévia
    Quando o ADMIN pede a prévia da campanha "1º de Maio 2026"
    Então os indicadores calculados são:
      | indicador                          | valor      |
      | Taxa de ocupação (%)               | 60,00      |
      | Fluxo em meios de hospedagem       | 110,00     |
      | Fluxo da hospedagem caseira        | 9,60       |
      | Fluxo de day use                   | 21,35      |
      | Fluxo total de turistas            | 140,95     |
      | Diária média (R$)                  | 437,50     |
      | Receita da hospedagem (R$)         | 15.750,00  |
      | Receita fora da hospedagem (R$)    | 170.555,10 |
    E a faixa de ocupação é "Boa"
    E a prévia aponta o impedimento de 68,38% dos leitos

  Esquema do Cenário: O day use soma visitantes sem pernoite
    Dado o day use de <day use>% vigente no período
    Quando o ADMIN pede a prévia da campanha "1º de Maio 2026"
    Então o fluxo total de turistas é <fluxo total>

    Exemplos:
      | day use | fluxo total |
      | 0,00    | 119,60      |
      | 15,15   | 140,95      |
      | 30,00   | 170,86      |

  Cenário: Meios inativos e irregulares ficam fora do cálculo
    Quando o ADMIN pede a prévia da campanha "1º de Maio 2026"
    Então os indicadores calculados são:
      | indicador                       | valor |
      | Meios de hospedagem no cálculo  | 3     |
      | Leitos no cálculo (MH + RHC)    | 117   |
