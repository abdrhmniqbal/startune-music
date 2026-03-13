import { File } from "expo-file-system"

interface TrackLyricsSourceInput {
  uri?: string
  lyrics?: string
}

interface SidecarCacheEntry {
  modificationTime: number | null
  lyrics?: string
}

const sidecarLyricsCache = new Map<string, SidecarCacheEntry>()

function getSidecarCandidates(uri: string): string[] {
  const sanitizedUri = uri.split("#")[0]?.split("?")[0] || uri
  const lastSlashIndex = sanitizedUri.lastIndexOf("/")
  const lastDotIndex = sanitizedUri.lastIndexOf(".")

  if (lastDotIndex <= lastSlashIndex) {
    return []
  }

  const basePath = sanitizedUri.slice(0, lastDotIndex)
  return [
    `${basePath}.ttml`,
    `${basePath}.TTML`,
    `${basePath}.xml`,
    `${basePath}.XML`,
    `${basePath}.lrc`,
    `${basePath}.LRC`,
  ]
}

function decodeUtf16Be(bytes: Uint8Array): string {
  const swapped = bytes.slice()
  for (let i = 0; i < swapped.length - 1; i += 2) {
    const current = swapped[i]
    swapped[i] = swapped[i + 1] || 0
    swapped[i + 1] = current || 0
  }
  return new TextDecoder("utf-16le").decode(swapped)
}

function decodeLyricsBytes(bytes: Uint8Array): string {
  if (bytes.length === 0) {
    return ""
  }

  try {
    if (bytes[0] === 0xef && bytes[1] === 0xbb && bytes[2] === 0xbf) {
      return new TextDecoder("utf-8").decode(bytes.slice(3))
    }

    if (bytes[0] === 0xff && bytes[1] === 0xfe) {
      return new TextDecoder("utf-16le").decode(bytes.slice(2))
    }

    if (bytes[0] === 0xfe && bytes[1] === 0xff) {
      return decodeUtf16Be(bytes.slice(2))
    }

    return new TextDecoder("utf-8").decode(bytes)
  } catch {
    return ""
  }
}

function normalizeLyricsText(raw: string | undefined): string | undefined {
  if (!raw) {
    return undefined
  }

  const normalized = raw
    .replace(/^\uFEFF/, "")
    .replace(/\r\n?/g, "\n")
    .trim()

  return normalized.length > 0 ? normalized : undefined
}

async function readSidecarLyrics(
  candidateUri: string
): Promise<string | undefined> {
  try {
    const sidecarFile = new File(candidateUri)
    const info = sidecarFile.info()
    if (!info.exists) {
      sidecarLyricsCache.delete(candidateUri)
      return undefined
    }

    const cached = sidecarLyricsCache.get(candidateUri)
    const modificationTime = info.modificationTime ?? null

    if (cached && cached.modificationTime === modificationTime) {
      return cached.lyrics
    }

    const bytes = await sidecarFile.bytes()
    const decoded = decodeLyricsBytes(bytes)
    const normalized = normalizeLyricsText(decoded)

    sidecarLyricsCache.set(candidateUri, {
      modificationTime,
      lyrics: normalized,
    })

    return normalized
  } catch {
    return undefined
  }
}

export async function resolveTrackLyricsSource(
  track: TrackLyricsSourceInput | null | undefined
): Promise<string | undefined> {
  if (!track) {
    return undefined
  }

  const uri = track.uri?.trim()
  if (uri) {
    const sidecarCandidates = getSidecarCandidates(uri)
    for (const candidate of sidecarCandidates) {
      const lyricsFromFile = await readSidecarLyrics(candidate)
      if (lyricsFromFile) {
        return lyricsFromFile
      }
    }
  }

  return normalizeLyricsText(track.lyrics)
}
