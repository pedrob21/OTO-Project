# language: pt
@F04 @inventario
Funcionalidade: Estabelecimentos
  Para manter o inventário que serve de base ao cálculo
  Como ADMIN ou parceiro
  Quero cadastrar e atualizar os meios de hospedagem, respeitando os dados da Secretaria

  Contexto:
    Dado o cenário de referência carregado

  Cenário: Parceiro altera os dados cadastrais, mas não os da Secretaria
    Quando o PARCEIRO A altera o "Hotel A" com o telefone comercial "17 3000-0000" e a situação "IRREGULAR"
    Então a resposta tem status 200
    E o "Hotel A" tem o telefone comercial "17 3000-0000" e a situação "REGULAR"

  Cenário: ADMIN altera também os dados da Secretaria
    Quando o ADMIN altera o "Hotel A" com o telefone comercial "17 3000-0000" e a situação "IRREGULAR"
    Então a resposta tem status 200
    E o "Hotel A" tem o telefone comercial "17 3000-0000" e a situação "IRREGULAR"

  Cenário: Parceiro não altera o estabelecimento de outro parceiro
    Quando o PARCEIRO C altera o "Hotel A" com o telefone comercial "17 3000-0000" e a situação "REGULAR"
    Então a resposta tem status 403
    E o "Hotel A" tem o telefone comercial "17 3281-1001" e a situação "REGULAR"

  Cenário: CNPJ repetido é recusado
    Quando o ADMIN cadastra o hotel "Hotel Novo" com o CNPJ "11111111000101", 10 UH, 20 leitos e no máximo 2 pessoas por UH
    Então a resposta tem status 422
    E a mensagem de erro é "Já existe um estabelecimento com este CNPJ."

  Cenário: Nova capacidade vale a partir da data informada, sem apagar a anterior
    Quando o PARCEIRO A registra para o "Hotel B" a capacidade de 22 UH e 55 leitos, no máximo 3 por UH, a partir de 01/06/2026
    Então a resposta tem status 201
    E o campo "leitosUteis" da resposta é "44,0"
    E o "Hotel B" tem 2 vigências de capacidade

  Cenário: Duas capacidades na mesma data são recusadas
    Dado que o PARCEIRO A registrou para o "Hotel B" a capacidade de 22 UH e 55 leitos, no máximo 3 por UH, a partir de 01/06/2026
    Quando o PARCEIRO A registra para o "Hotel B" a capacidade de 25 UH e 60 leitos, no máximo 3 por UH, a partir de 01/06/2026
    Então a resposta tem status 422
    E a mensagem de erro é "Já existe uma capacidade a partir desta data. Use outra data."
