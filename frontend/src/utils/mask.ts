export function maskText(v: string, keepStart = 2, keepEnd = 2) {
  const s = v || ''
  if (s.length <= keepStart + keepEnd) return s
  return `${s.slice(0, keepStart)}***${s.slice(-keepEnd)}`
}

export function maskUserKey(v: string) {
  const [type, id] = v.split(':')
  if (!id) return maskText(v, 2, 2)
  return `${type}:${maskText(id, 1, 1)}`
}

