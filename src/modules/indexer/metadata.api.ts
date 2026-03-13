import {
  getArtwork,
  getMetadata,
} from "@missingcore/react-native-metadata-retriever"
import { eq } from "drizzle-orm"
import { Directory, File, Paths } from "expo-file-system"

import { db } from "@/db/client"
import { artworkCache } from "@/db/schema"

export interface ExtractedMetadata {
  title: string
  artist?: string
  artists: string[]
  album?: string
  albumArtist?: string
  genres: string[]
  year?: number
  trackNumber?: number
  discNumber?: number
  duration: number
  bitrate?: number
  sampleRate?: number
  codec?: string
  format?: string
  composer?: string
  comment?: string
  lyrics?: string
  artwork?: string
}

const ARTWORK_DIR_NAME = "artwork"

// Define the fields we want to extract
const metadataFields = [
  "bitrate",
  "sampleRate",
  "codecs",
  "sampleMimeType",
  "title",
  "artist",
  "albumArtist",
  "albumTitle",
  "trackNumber",
  "discNumber",
  "genre",
  "composer",
  "description",
  "year",
  "artworkData",
] as const

const ID3_HEADER_SIZE = 10
const ID3_FRAME_HEADER_SIZE = 10
const ID3_TIMESTAMP_FORMAT_MILLISECONDS = 2

function decodeSyncSafeInteger(bytes: Uint8Array, offset: number) {
  return (
    ((bytes[offset] || 0) << 21) |
    ((bytes[offset + 1] || 0) << 14) |
    ((bytes[offset + 2] || 0) << 7) |
    (bytes[offset + 3] || 0)
  )
}

function decodeInteger(bytes: Uint8Array, offset: number) {
  return (
    ((bytes[offset] || 0) << 24) |
    ((bytes[offset + 1] || 0) << 16) |
    ((bytes[offset + 2] || 0) << 8) |
    (bytes[offset + 3] || 0)
  )
}

function decodeTextValue(bytes: Uint8Array, encoding: number) {
  if (bytes.length === 0) {
    return ""
  }

  try {
    switch (encoding) {
      case 0:
        return new TextDecoder("iso-8859-1").decode(bytes)
      case 1: {
        if (bytes.length >= 2) {
          const bomA = bytes[0]
          const bomB = bytes[1]
          if (bomA === 0xfe && bomB === 0xff) {
            const swapped = bytes.slice(2)
            for (let i = 0; i < swapped.length - 1; i += 2) {
              const current = swapped[i]
              swapped[i] = swapped[i + 1] || 0
              swapped[i + 1] = current || 0
            }
            return new TextDecoder("utf-16le").decode(swapped)
          }

          if (bomA === 0xff && bomB === 0xfe) {
            return new TextDecoder("utf-16le").decode(bytes.slice(2))
          }
        }
        return new TextDecoder("utf-16le").decode(bytes)
      }
      case 2: {
        const swapped = bytes.slice()
        for (let i = 0; i < swapped.length - 1; i += 2) {
          const current = swapped[i]
          swapped[i] = swapped[i + 1] || 0
          swapped[i + 1] = current || 0
        }
        return new TextDecoder("utf-16le").decode(swapped)
      }
      case 3:
      default:
        return new TextDecoder("utf-8").decode(bytes)
    }
  } catch {
    return ""
  }
}

function findEncodedTextTerminator(bytes: Uint8Array, encoding: number) {
  if (encoding === 0 || encoding === 3) {
    return bytes.indexOf(0)
  }

  for (let i = 0; i < bytes.length - 1; i += 1) {
    if (bytes[i] === 0 && bytes[i + 1] === 0) {
      return i
    }
  }

  return -1
}

function formatLrcTimestamp(timeSeconds: number) {
  const safe = Math.max(0, timeSeconds)
  const minutes = Math.floor(safe / 60)
  const seconds = Math.floor(safe % 60)
  const centiseconds = Math.floor((safe - Math.floor(safe)) * 100)

  return `[${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}.${String(centiseconds).padStart(2, "0")}]`
}

function extractId3SyncedLyrics(frameBytes: Uint8Array) {
  if (frameBytes.length <= 6) {
    return undefined
  }

  const encoding = frameBytes[0] || 0
  const timestampFormat = frameBytes[4] || 0
  if (timestampFormat !== ID3_TIMESTAMP_FORMAT_MILLISECONDS) {
    return undefined
  }

  const descriptorAndData = frameBytes.slice(6)
  const descriptorEnd = findEncodedTextTerminator(descriptorAndData, encoding)
  const dataStart =
    descriptorEnd >= 0
      ? descriptorEnd + (encoding === 0 || encoding === 3 ? 1 : 2)
      : 0
  const payload = descriptorAndData.slice(dataStart)
  if (payload.length === 0) {
    return undefined
  }

  const lines: Array<{ time: number; text: string }> = []
  let offset = 0
  while (offset < payload.length) {
    const remaining = payload.slice(offset)
    const textEnd = findEncodedTextTerminator(remaining, encoding)
    if (textEnd < 0) {
      break
    }

    const textBytes = remaining.slice(0, textEnd)
    const text = decodeTextValue(textBytes, encoding).trim()
    const separatorLength = encoding === 0 || encoding === 3 ? 1 : 2
    const timestampOffset = offset + textEnd + separatorLength
    if (timestampOffset + 4 > payload.length) {
      break
    }

    const timestamp = decodeInteger(payload, timestampOffset) >>> 0
    offset = timestampOffset + 4

    if (!text) {
      continue
    }

    lines.push({
      time: timestamp / 1000,
      text,
    })
  }

  if (lines.length === 0) {
    return undefined
  }

  return lines
    .sort((a, b) => a.time - b.time)
    .map((line) => `${formatLrcTimestamp(line.time)}${line.text}`)
    .join("\n")
}

function extractId3Lyrics(bytes: Uint8Array) {
  if (
    bytes.length < ID3_HEADER_SIZE ||
    bytes[0] !== 0x49 ||
    bytes[1] !== 0x44 ||
    bytes[2] !== 0x33
  ) {
    return undefined
  }

  const version = bytes[3] || 0
  const tagSize = decodeSyncSafeInteger(bytes, 6)
  const tagEnd = Math.min(bytes.length, ID3_HEADER_SIZE + tagSize)
  let offset = ID3_HEADER_SIZE

  while (offset + ID3_FRAME_HEADER_SIZE <= tagEnd) {
    const frameId = String.fromCharCode(
      bytes[offset] || 0,
      bytes[offset + 1] || 0,
      bytes[offset + 2] || 0,
      bytes[offset + 3] || 0
    ).replace(/\0/g, "")

    if (!frameId) {
      break
    }

    const frameSize =
      version === 4
        ? decodeSyncSafeInteger(bytes, offset + 4)
        : decodeInteger(bytes, offset + 4)
    if (frameSize <= 0) {
      break
    }

    const frameStart = offset + ID3_FRAME_HEADER_SIZE
    const frameEnd = Math.min(tagEnd, frameStart + frameSize)
    if (frameEnd <= frameStart) {
      break
    }

    if (frameId === "SYLT") {
      const frameBytes = bytes.slice(frameStart, frameEnd)
      const syncedLyrics = extractId3SyncedLyrics(frameBytes)
      if (syncedLyrics) {
        return syncedLyrics
      }
    }

    if (frameId === "USLT") {
      const frameBytes = bytes.slice(frameStart, frameEnd)
      const encoding = frameBytes[0] || 0
      const descriptorAndLyrics = frameBytes.slice(4)
      const descriptorEnd = findEncodedTextTerminator(
        descriptorAndLyrics,
        encoding
      )
      const lyricsStart =
        descriptorEnd >= 0
          ? descriptorEnd + (encoding === 0 || encoding === 3 ? 1 : 2)
          : 0
      const lyricsBytes = descriptorAndLyrics.slice(lyricsStart)
      const lyrics = decodeTextValue(lyricsBytes, encoding).trim()
      if (lyrics) {
        return lyrics
      }
    }

    offset = frameEnd
  }

  return undefined
}

function getAtomType(bytes: Uint8Array, offset: number) {
  return String.fromCharCode(
    bytes[offset] || 0,
    bytes[offset + 1] || 0,
    bytes[offset + 2] || 0,
    bytes[offset + 3] || 0
  )
}

function findMp4AtomData(
  bytes: Uint8Array,
  targetType: string,
  start = 0,
  end = bytes.length
): Uint8Array | null {
  let offset = start

  while (offset + 8 <= end) {
    const size = decodeInteger(bytes, offset)
    const type = getAtomType(bytes, offset + 4)
    if (size < 8) {
      break
    }

    const atomEnd = Math.min(end, offset + size)
    const childStart = type === "meta" ? offset + 12 : offset + 8

    if (type === targetType) {
      return bytes.slice(childStart, atomEnd)
    }

    if (
      type === "moov" ||
      type === "udta" ||
      type === "meta" ||
      type === "ilst"
    ) {
      const nested = findMp4AtomData(bytes, targetType, childStart, atomEnd)
      if (nested) {
        return nested
      }
    }

    offset = atomEnd
  }

  return null
}

function extractMp4Lyrics(bytes: Uint8Array) {
  const lyricAtom = findMp4AtomData(
    bytes,
    String.fromCharCode(0xa9, 0x6c, 0x79, 0x72)
  )
  if (!lyricAtom) {
    return undefined
  }

  let offset = 0
  while (offset + 16 <= lyricAtom.length) {
    const childSize = decodeInteger(lyricAtom, offset)
    const childType = getAtomType(lyricAtom, offset + 4)
    if (childSize < 16) {
      break
    }

    const childEnd = Math.min(lyricAtom.length, offset + childSize)
    if (childType === "data") {
      const payload = lyricAtom.slice(offset + 16, childEnd)
      const lyrics = new TextDecoder("utf-8").decode(payload).trim()
      if (lyrics) {
        return lyrics
      }
    }

    offset = childEnd
  }

  return undefined
}

async function extractEmbeddedLyrics(uri: string) {
  try {
    const file = new File(uri)
    if (!file.exists) {
      return undefined
    }

    const bytes = await file.bytes()
    return extractId3Lyrics(bytes) || extractMp4Lyrics(bytes) || undefined
  } catch {
    return undefined
  }
}

export async function extractMetadata(
  uri: string,
  filename: string,
  duration: number
): Promise<ExtractedMetadata> {
  try {
    // Get metadata from the file
    const metadata = await getMetadata(uri, metadataFields)

    // Get artwork separately
    const artwork = await getArtwork(uri).catch(() => null)
    const lyrics = await extractEmbeddedLyrics(uri)

    const bitrate =
      typeof metadata.bitrate === "number" && Number.isFinite(metadata.bitrate)
        ? Math.round(metadata.bitrate)
        : undefined
    const sampleRate =
      typeof metadata.sampleRate === "number" &&
      Number.isFinite(metadata.sampleRate)
        ? Math.round(metadata.sampleRate)
        : undefined
    const codec =
      typeof metadata.codecs === "string" && metadata.codecs.length > 0
        ? metadata.codecs
        : undefined
    const format =
      typeof metadata.sampleMimeType === "string" &&
      metadata.sampleMimeType.length > 0
        ? metadata.sampleMimeType
        : undefined

    return {
      title: metadata.title || cleanFilename(filename),
      artist: metadata.artist || undefined,
      artists: metadata.artist ? parseMultiValue(metadata.artist) : [],
      album: metadata.albumTitle || undefined,
      albumArtist: metadata.albumArtist || metadata.artist || undefined,
      genres: metadata.genre ? parseMultiValue(metadata.genre) : [],
      year: metadata.year || undefined,
      trackNumber: metadata.trackNumber || undefined,
      discNumber: metadata.discNumber || undefined,
      duration,
      bitrate,
      sampleRate,
      codec,
      format,
      composer: metadata.composer || undefined,
      comment: metadata.description || undefined,
      lyrics: lyrics || undefined,
      artwork: artwork || undefined,
    }
  } catch {
    return {
      title: cleanFilename(filename),
      artists: [],
      genres: [],
      duration,
    }
  }
}

export async function saveArtworkToCache(
  artworkData: string | undefined
): Promise<string | undefined> {
  if (!artworkData) return undefined

  try {
    // If already a file path, return as-is
    if (artworkData.startsWith("file://") || artworkData.startsWith("/")) {
      return artworkData
    }

    // Generate hash from artwork data
    const hash = generateArtworkHash(artworkData)

    // Check if already cached
    const existing = await db.query.artworkCache.findFirst({
      where: eq(artworkCache.hash, hash),
    })

    if (existing) {
      const existingFile = new File(existing.path)
      if (existingFile.exists) {
        return existing.path
      }
    }

    // Save to cache directory
    const cacheDir = new Directory(Paths.cache, ARTWORK_DIR_NAME)
    if (!cacheDir.exists) {
      cacheDir.create({ intermediates: true, idempotent: true })
    }

    const artworkFile = new File(cacheDir, `${hash}.jpg`)
    if (!artworkFile.exists) {
      artworkFile.create({ intermediates: true, overwrite: true })
    }

    // Remove data URI prefix if present
    let base64Data = artworkData
    if (artworkData.startsWith("data:")) {
      base64Data = artworkData.split(",")[1] || ""
    }

    if (!base64Data) return undefined

    artworkFile.write(base64Data, {
      encoding: "base64",
    })

    // Save to database
    await db.insert(artworkCache).values({
      hash,
      path: artworkFile.uri,
      mimeType: "image/jpeg",
      source: "embedded",
      createdAt: Date.now(),
    })

    return artworkFile.uri
  } catch {
    return undefined
  }
}

// Parse multi-value fields (semicolon or slash delimited)
function parseMultiValue(value: string | null): string[] {
  if (!value) return []
  return value
    .split(/[;/]/)
    .map((v) => v.trim())
    .filter((v) => v.length > 0)
}

function cleanFilename(filename: string): string {
  return filename.replace(/\.[^/.]+$/, "").replace(/[_-]/g, " ")
}

function generateArtworkHash(data: string): string {
  const sample = data.slice(0, 1024)
  let hash = 0
  for (let i = 0; i < sample.length; i++) {
    hash = (hash << 5) - hash + data.charCodeAt(i)
    hash |= 0
  }
  return `${Math.abs(hash).toString(16)}_${data.length}`
}
