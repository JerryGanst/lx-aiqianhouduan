// Shared utilities for processing {{CITE:n}} placeholders and neutralizing raw URLs

// Extract last path segment from a file path or title-like string
export const getLastPathSegment = (path) => {
  if (!path) return ''
  const normalized = String(path).replace(/\\/g, '/').split('/')
  const last = normalized.filter(Boolean).pop()
  return last || ''
}

// Add spaces around bare http/https URLs so they don't glue to CJK text/punctuation
export const addSpacesAroundUrls = (str) => {
  let s = String(str)
  const urlBody = "[^\\s<>,，。；;：:！!？?、（）()\\[\\]{}]+"
  const urlHead = new RegExp(`(^|[^\\s])((?:https?:\\/\\/)${urlBody})`, 'g')
  const urlTail = new RegExp(`((?:https?:\\/\\/)${urlBody})(?=[^\\s])`, 'g')
  s = s.replace(urlHead, (m, pre, url) => (pre ? pre + ' ' : '') + url)
  s = s.replace(urlTail, '$1 ')
  return s
}

// Helper: detect extension from url or title
const detectExt = (url, title) => {
  const decode = (s) => { try { return decodeURIComponent(s) } catch { return s } }
  const pick = (s) => (s || '').split('?')[0].split('#')[0]
  const a = pick(decode(url || ''))
  const b = pick(decode(title || ''))
  const target = a || b || ''
  const m = target.match(/\.([a-zA-Z0-9]+)$/)
  return m ? m[1].toLowerCase() : ''
}

// Replace {{CITE:1,2}} with links or page text based on options
// options:
// - mode: 'viewer' | 'direct' | 'pageOnly'
// - includePagesInText: boolean (append (第x/…页) to link text)
// - keepPlaceholderWhenEmpty: boolean (if true and no valid mapping, keep original {{CITE:..}})
// - viewerBase: string (default '/pdf-viewer.html')
export const replaceCitationsWithLinks = (text, sources = [], options = {}) => {
  if (!text) return ''
  const {
    mode = 'viewer',
    includePagesInText = true,
    keepPlaceholderWhenEmpty = false,
    viewerBase = '/pdf-viewer.html'
  } = options

  const safeText = addSpacesAroundUrls(text)

  return String(safeText).replace(/\{\{CITE:\s*([\d\s,]+)\}\}/g, (_, ids) => {
    const idArray = ids.split(',').map(id => id.trim()).filter(Boolean)

    // Aggregate by (title + url) to combine pages
    const linkMap = new Map()

    idArray.forEach(id => {
      const index = Number(id) - 1
      if (!Number.isInteger(index) || index < 0 || index >= sources.length) return
      const src = sources[index] || {}
      const title = getLastPathSegment(src.document_title)
      // Normalize url; some contexts may omit
      const rawUrl = src.fileUrl || src.file_url || ''
      const pageVal = src.page

      const key = `${title}||${rawUrl}`
      if (!linkMap.has(key)) {
        linkMap.set(key, { title, url: rawUrl, pages: new Set() })
      }
      const entry = linkMap.get(key)
      if (Array.isArray(pageVal)) {
        pageVal.forEach(p => { if (p !== undefined && p !== null) entry.pages.add(p) })
      } else if (pageVal !== undefined && pageVal !== null) {
        entry.pages.add(pageVal)
      }
    })

    const chunks = []
    linkMap.forEach(entry => {
      const pageArr = Array.from(entry.pages).sort((a, b) => a - b)
      const pageSuffix = includePagesInText && pageArr.length > 0
        ? ` (第${pageArr.join('/')}页)`
        : ''

      if (mode === 'pageOnly') {
        if (pageArr.length > 0) chunks.push(`(第${pageArr.join('/')}页)`)
        return
      }

      let href = entry.url || ''
      if (mode === 'viewer' && href && /^https?:/i.test(href)) {
        const firstPage = pageArr.length > 0 ? pageArr[0] : 1
        const ext = detectExt(entry.url, entry.title)
        if (ext === 'pdf') {
          href = `${viewerBase}?src=${encodeURIComponent(entry.url)}&page=${encodeURIComponent(firstPage)}`
        } else if (ext === 'ppt' || ext === 'pptx') {
          href = `/ppt-viewer.html?src=${encodeURIComponent(entry.url)}&page=${encodeURIComponent(firstPage)}`
        } else {
          href = entry.url
        }
      }

      const titleText = entry.title || '附件'
      if (href) {
        chunks.push(`[${titleText}${pageSuffix}](${href})`)
      }
    })

    if (chunks.length === 0) {
      return keepPlaceholderWhenEmpty ? `{{CITE:${ids}}}` : ''
    }
    return chunks.join(', ')
  })
}

