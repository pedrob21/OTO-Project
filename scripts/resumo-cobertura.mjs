// Monta o resumo de cobertura do JaCoCo em Markdown para comentar no PR.
// Uso: node scripts/resumo-cobertura.mjs > resumo.md
// Lê build/reports/jacoco/test/jacocoTestReport.xml (ou o caminho passado como argumento).

import { existsSync, readFileSync } from 'node:fs'

const MARCADOR = '<!-- resumo-cobertura-back -->'
const ARQUIVO = process.argv[2] ?? 'build/reports/jacoco/test/jacocoTestReport.xml'

const MEDIDAS = [
  ['LINE', 'Linhas'],
  ['BRANCH', 'Branches'],
  ['METHOD', 'Métodos'],
  ['INSTRUCTION', 'Instruções'],
]

function decimal(valor, casas) {
  return valor.toFixed(casas).replace('.', ',')
}

function percentual({ coberto, perdido }) {
  const total = coberto + perdido
  return total === 0 ? 100 : (coberto / total) * 100
}

function contadores(trecho) {
  const mapa = {}
  for (const [, tipo, perdido, coberto] of trecho.matchAll(
    /<counter type="(\w+)" missed="(\d+)" covered="(\d+)"\/>/g,
  )) {
    mapa[tipo] = { perdido: Number(perdido), coberto: Number(coberto) }
  }
  return mapa
}

function celula(contador) {
  if (!contador) return '-'
  return `${decimal(percentual(contador), 2)}%`
}

function lerRelatorio(xml) {
  const semPackages = xml.replace(/<package [\s\S]*?<\/package>/g, '')
  const total = contadores(semPackages)
  const pacotes = [...xml.matchAll(/<package name="([^"]*)">([\s\S]*?)<\/package>/g)].map(
    ([, nome, corpo]) => {
      const semClasses = corpo
        .replace(/<class [\s\S]*?<\/class>/g, '')
        .replace(/<sourcefile [\s\S]*?<\/sourcefile>/g, '')
      return { nome: nome.replaceAll('/', '.'), medidas: contadores(semClasses) }
    },
  )
  return { total, pacotes }
}

function rodape() {
  const { GITHUB_SERVER_URL, GITHUB_REPOSITORY, GITHUB_RUN_ID, COMMIT_DO_PR, GITHUB_SHA } =
    process.env
  if (!GITHUB_RUN_ID) return ''
  const execucao = `${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}`
  return `\n_Commit \`${(COMMIT_DO_PR || GITHUB_SHA)?.slice(0, 7)}\`. [Relatório HTML completo no artefato \`cobertura-jacoco\` da execução](${execucao})._`
}

const cabecalho = [MARCADOR, '## Cobertura de testes (JaCoCo)', '']

if (!existsSync(ARQUIVO)) {
  console.log([...cabecalho, '_Cobertura não disponível nesta execução._', rodape()].join('\n'))
} else {
  const { total, pacotes } = lerRelatorio(readFileSync(ARQUIVO, 'utf8'))
  const linhasTotal = MEDIDAS.map(([tipo, nome]) => {
    const c = total[tipo]
    return c
      ? `| ${nome} | ${celula(c)} | ${c.coberto} de ${c.coberto + c.perdido} |`
      : `| ${nome} | - | - |`
  })
  const linhasPacotes = pacotes
    .sort((a, b) => a.nome.localeCompare(b.nome))
    .map(({ nome, medidas }) => `| \`${nome}\` | ${MEDIDAS.map(([tipo]) => celula(medidas[tipo])).join(' | ')} |`)

  console.log(
    [
      ...cabecalho,
      '| Medida | Cobertura | Cobertos |',
      '| --- | ---: | ---: |',
      ...linhasTotal,
      '',
      '### Por pacote',
      '',
      `| Pacote | ${MEDIDAS.map(([, nome]) => nome).join(' | ')} |`,
      `| --- | ${MEDIDAS.map(() => '---:').join(' | ')} |`,
      ...linhasPacotes,
      rodape(),
    ].join('\n'),
  )
}
