export function downloadText(filename: string, content: string, mime = 'text/plain;charset=utf-8') {
  const blob = new Blob([content], { type: mime })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

export function downloadJson(filename: string, data: unknown) {
  downloadText(filename, JSON.stringify(data, null, 2), 'application/json;charset=utf-8')
}

const esc = (v: unknown) => {
  const s = v == null ? '' : String(v)
  return `"${s.replace(/"/g, '""')}"`
}

export function downloadCsv(filename: string, rows: Array<Record<string, unknown>>) {
  const keys = Array.from(
    rows.reduce((set, r) => {
      Object.keys(r).forEach((k) => set.add(k))
      return set
    }, new Set<string>()),
  )
  const header = keys.map(esc).join(',')
  const body = rows.map((r) => keys.map((k) => esc(r[k])).join(',')).join('\n')
  downloadText(filename, `${header}\n${body}\n`, 'text/csv;charset=utf-8')
}
