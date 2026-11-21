# language: pt
@F07 @pesquisa
Funcionalidade: Respostas
  Para cumprir a pesquisa sem planilhas nem formulários externos
  Como parceiro
  Quero responder e corrigir a ocupação do meu estabelecimento até o fechamento

  Contexto:
    Dado o cenário de referência carregado

  Cenário: Vale o último envio e o histórico guarda as versões
    Quando o PARCEIRO A envia a resposta do "Hotel A": taxa "70,00"% e diária R$ "450,00"
    E o PARCEIRO A envia a resposta do "Hotel A": taxa "80,00"% e diária R$ "500,00"
    Então a resposta tem status 200
    E a resposta do "Hotel A" está na versão 2 com taxa de 80,00%
    E o histórico da resposta do "Hotel A" tem 2 versões

  Cenário: Parceiro vê os próprios estabelecimentos nas pendências
    Quando o PARCEIRO A chama GET "/api/respostas/pendencias"
    Então a resposta tem status 200
    E a resposta lista os estabelecimentos "Hotel A, Hotel B"

  Cenário: Parceiro não responde depois do encerramento
    Dado que o ADMIN encerrou a pesquisa "001/26"
    Quando o PARCEIRO A envia a resposta do "Hotel B": taxa "50,00"% e diária R$ "400,00"
    Então a resposta tem status 422
    E a mensagem de erro é "A pesquisa 001/26 não está aberta para respostas."

  Cenário: ADMIN lança a resposta recebida fora do sistema depois do encerramento
    Dado que o ADMIN encerrou a pesquisa "001/26"
    Quando o ADMIN envia a resposta da "Pousada C": taxa "40,00"% e diária R$ "300,00"
    Então a resposta tem status 200
    E a resposta da "Pousada C" foi enviada pelo usuário "admin@oto.test"

  Esquema do Cenário: Estabelecimento <motivo> não participa da pesquisa
    Quando o ADMIN envia a resposta do "<estabelecimento>": taxa "60,00"% e diária R$ "200,00"
    Então a resposta tem status 422
    E a mensagem de erro é "Este estabelecimento está inativo ou irregular e não participa das pesquisas."

    Exemplos:
      | estabelecimento | motivo    |
      | Flat D          | inativo   |
      | Hostel E        | irregular |

  Cenário: Taxa acima de 100% é recusada
    Quando o PARCEIRO A envia a resposta do "Hotel A": taxa "101,00"% e diária R$ "450,00"
    Então a resposta tem status 400
    E o erro aponta o campo "taxaOcupacao"
