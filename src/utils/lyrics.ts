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
