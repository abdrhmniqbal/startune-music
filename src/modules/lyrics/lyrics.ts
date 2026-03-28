interface LyricsLine {
  id: string
  text: string
  isSpacer: boolean
}

interface SyncedLyricsLine {
  id: string
  time: number
  text: string
}

export interface TTMLWord {
  text: string
  begin: number
  end: number
}

export interface TTMLLine {
  id: string
  begin: number
  end: number
  words: TTMLWord[]
}

function hasMoreThanOneDistinctTime(values: number[]) {
  const distinctValues = new Set(
    values
      .filter((value) => Number.isFinite(value))
      .map((value) => Math.round(value * 1000))
  )
  return distinctValues.size > 1
}

interface JsonTimedLyricEntry {
  text?: unknown
  time?: unknown
  timestamp?: unknown
  start?: unknown
}

const LRC_METADATA_HEADER_LINE_REGEX =
  /^\[(id|ti|ar|al|au|lr|length|by|offset|re|tool|re\/tool|ve)\s*:[^\]\r\n]*\]$/gim
const LRC_COMMENT_LINE_REGEX = /^\s*#.*$/gm

function normalizeJsonLyrics(raw: string): string | null {
  try {
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) {
      return null
    }

    const lines = parsed
      .map((entry) => {
        if (
          entry &&
          typeof entry === "object" &&
          "text" in entry &&
          typeof entry.text === "string"
        ) {
          return entry.text
        }

        if (typeof entry === "string") {
          return entry
        }

        return null
      })
      .filter((value): value is string => value !== null)

    return lines.length > 0 ? lines.join("\n") : null
  } catch {
    return null
  }
}

export function normalizeLyricsText(raw: string | null | undefined) {
  if (!raw) {
    return undefined
  }

  const trimmed = raw.trim()
  if (!trimmed) {
    return undefined
  }

  const maybeJson =
    trimmed.startsWith("[") || trimmed.startsWith("{")
      ? normalizeJsonLyrics(trimmed)
      : null
  const source = maybeJson ?? trimmed

  const normalized = source
    .replace(/^\uFEFF/, "")
    .replace(/\r\n?/g, "\n")
    .replace(LRC_METADATA_HEADER_LINE_REGEX, "")
    .replace(LRC_COMMENT_LINE_REGEX, "")
    .replace(/\[\d{1,2}:\d{2}(?:\.\d{1,3})?\]/g, "")
    .replace(/\n{3,}/g, "\n\n")
    .trim()

  return normalized.length > 0 ? normalized : undefined
}

function stripLyricsMetadataHeaders(raw: string) {
  return raw
    .replace(/^\uFEFF/, "")
    .replace(/\r\n?/g, "\n")
    .replace(LRC_METADATA_HEADER_LINE_REGEX, "")
    .replace(LRC_COMMENT_LINE_REGEX, "")
    .trim()
}

function parseJsonSyncedLyrics(raw: string): SyncedLyricsLine[] {
  try {
    const parsed = JSON.parse(raw) as unknown
    if (!Array.isArray(parsed)) {
      return []
    }

    const lines = parsed
      .map((entry, index) => {
        if (!entry || typeof entry !== "object") {
          return null
        }

        const candidate = entry as JsonTimedLyricEntry
        const text =
          typeof candidate.text === "string" ? candidate.text.trim() : ""
        const timeValue =
          typeof candidate.time === "number"
            ? candidate.time
            : typeof candidate.timestamp === "number"
              ? candidate.timestamp
              : typeof candidate.start === "number"
                ? candidate.start
                : Number.NaN

        if (!text || !Number.isFinite(timeValue)) {
          return null
        }

        return {
          id: `json-${index}-${timeValue}-${text}`,
          text,
          time: Math.max(0, timeValue),
        }
      })
      .filter((line): line is SyncedLyricsLine => line !== null)

    return lines.sort((a, b) => a.time - b.time)
  } catch {
    return []
  }
}

export function splitLyricsLines(raw: string | null | undefined): LyricsLine[] {
  const lyrics = normalizeLyricsText(raw)
  if (!lyrics) {
    return []
  }

  return lyrics.split("\n").map((line, index) => {
    const text = line.trim()
    return {
      id: `${index}-${text || "spacer"}`,
      text,
      isSpacer: text.length === 0,
    }
  })
}

export function parseSyncedLyricsLines(
  raw: string | null | undefined
): SyncedLyricsLine[] {
  if (!raw) {
    return []
  }

  const trimmed = raw.trim()
  if (!trimmed) {
    return []
  }

  if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
    const jsonLines = parseJsonSyncedLyrics(trimmed)
    if (jsonLines.length > 0) {
      return jsonLines
    }
  }

  const normalized = stripLyricsMetadataHeaders(trimmed)
  if (!normalized) {
    return []
  }

  const lines = normalized.split("\n")
  const parsed: SyncedLyricsLine[] = []

  for (const [index, line] of lines.entries()) {
    const timestampMatches = [
      ...line.matchAll(/\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]/g),
    ]
    if (timestampMatches.length === 0) {
      continue
    }

    const text = line
      .replace(/\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]/g, "")
      .trim()
    if (!text) {
      continue
    }

    for (const [matchIndex, match] of timestampMatches.entries()) {
      const minutes = Number(match[1] || 0)
      const seconds = Number(match[2] || 0)
      const fractionText = match[3] || "0"
      const fractionScale =
        fractionText.length === 3 ? 1000 : fractionText.length === 2 ? 100 : 10
      const fraction = Number(fractionText) / fractionScale
      const time = minutes * 60 + seconds + fraction

      parsed.push({
        id: `${index}-${matchIndex}-${time}-${text}`,
        time,
        text,
      })
    }
  }

  return parsed.sort((a, b) => a.time - b.time)
}

export function hasMeaningfulSyncedLyricsTiming(lines: SyncedLyricsLine[]) {
  if (lines.length === 0) {
    return false
  }

  if (lines.some((line) => line.time > 0)) {
    return true
  }

  return hasMoreThanOneDistinctTime(lines.map((line) => line.time))
}

function parseTTMLTimestamp(raw: string): number {
  const parts = raw.split(":")
  if (parts.length === 3) {
    const hours = Number(parts[0] || 0)
    const minutes = Number(parts[1] || 0)
    const seconds = Number.parseFloat(parts[2] || "0")
    return hours * 3600 + minutes * 60 + seconds
  }
  if (parts.length === 2) {
    const minutes = Number(parts[0] || 0)
    const seconds = Number.parseFloat(parts[1] || "0")
    return minutes * 60 + seconds
  }
  return Number.parseFloat(raw) || 0
}

export function isTTML(raw: string): boolean {
  const trimmed = raw.trim()
  return (
    trimmed.includes("<?xml") ||
    trimmed.includes("<tt") ||
    trimmed.includes("<html")
  )
}

export function parseTTMLLines(raw: string | null | undefined): TTMLLine[] {
  if (!raw) {
    return []
  }

  const trimmed = raw.trim()
  if (!isTTML(trimmed)) {
    return []
  }

  const lines: TTMLLine[] = []
  const pRegex =
    /<p\s[^>]*begin="([^"]+)"[^>]*end="([^"]+)"[^>]*>([\s\S]*?)<\/p>/g
  let pMatch: RegExpExecArray | null

  let lineIndex = 0
  while ((pMatch = pRegex.exec(trimmed)) !== null) {
    const pBegin = parseTTMLTimestamp(pMatch[1] || "0")
    const pEnd = parseTTMLTimestamp(pMatch[2] || "0")
    const innerContent = pMatch[3] || ""

    const words: TTMLWord[] = []
    const spanRegex =
      /<span\s[^>]*begin="([^"]+)"[^>]*end="([^"]+)"[^>]*>([\s\S]*?)<\/span>/g
    let spanMatch: RegExpExecArray | null

    while ((spanMatch = spanRegex.exec(innerContent)) !== null) {
      const begin = parseTTMLTimestamp(spanMatch[1] || "0")
      const end = parseTTMLTimestamp(spanMatch[2] || "0")
      const text = (spanMatch[3] || "")
        .replace(/<[^>]+>/g, "")
        .replace(/&amp;/g, "&")
        .replace(/&lt;/g, "<")
        .replace(/&gt;/g, ">")
        .replace(/&quot;/g, '"')
        .replace(/&apos;/g, "'")

      if (text) {
        words.push({ text, begin, end })
      }
    }

    if (words.length === 0) {
      const plainText = innerContent
        .replace(/<[^>]+>/g, "")
        .replace(/&amp;/g, "&")
        .replace(/&lt;/g, "<")
        .replace(/&gt;/g, ">")
        .replace(/&quot;/g, '"')
        .replace(/&apos;/g, "'")
        .trim()

      if (plainText) {
        words.push({ text: plainText, begin: pBegin, end: pEnd })
      }
    }

    if (words.length > 0) {
      lines.push({
        id: `ttml-${lineIndex}`,
        begin: pBegin,
        end: pEnd,
        words,
      })
      lineIndex++
    }
  }

  return lines.sort((a, b) => a.begin - b.begin)
}

export function hasMeaningfulTTMLTiming(lines: TTMLLine[]) {
  if (lines.length === 0) {
    return false
  }

  const lineHasDuration = lines.some(
    (line) => line.end > line.begin || line.begin > 0 || line.end > 0
  )
  if (lineHasDuration) {
    return true
  }

  const words = lines.flatMap((line) => line.words)
  if (
    words.some((word) => word.end > word.begin || word.begin > 0 || word.end > 0)
  ) {
    return true
  }

  return (
    hasMoreThanOneDistinctTime(lines.flatMap((line) => [line.begin, line.end])) ||
    hasMoreThanOneDistinctTime(words.flatMap((word) => [word.begin, word.end]))
  )
}
