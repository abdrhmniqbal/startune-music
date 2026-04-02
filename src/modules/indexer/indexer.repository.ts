import type { IndexerScanProgress } from "./indexer.types"
import { and, eq, inArray, sql } from "drizzle-orm"

import { File } from "expo-file-system"
import * as MediaLibrary from "expo-media-library"
import { db } from "@/db/client"
import { albums, artists, genres, trackGenres, tracks } from "@/db/schema"
import {
  GENRE_COLORS,
  GENRE_SHAPES,
  type GenreShape,
} from "@/modules/genres/genres.constants"
import {
  ensureFolderFilterConfigLoaded,
  isAssetAllowedByFolderFilters,
} from "@/modules/settings/folder-filters"

import {
  ensureTrackDurationFilterConfigLoaded,
  isAssetAllowedByTrackDuration,
} from "@/modules/settings/track-duration-filter"
import { waitForIndexerResume } from "@/modules/indexer/indexer-runtime"
import { logError } from "@/modules/logging/logging.service"
import { removeTracksFromFavoritesAndPlaylists } from "@/modules/tracks/track-cleanup.repository"
import { extractMetadata, saveArtworkToCache } from "./metadata.repository"

const BATCH_SIZE = 10
const BATCH_CONCURRENCY = 4

function yieldToEventLoop() {
  return new Promise<void>((resolve) => {
    setTimeout(resolve, 0)
  })
}

export async function scanMediaLibrary(
  onProgress?: (progress: IndexerScanProgress) => void,
  forceFullScan = false,
  signal?: AbortSignal
): Promise<void> {
  if (signal?.aborted) return

  // Get all audio assets
  const assets: MediaLibrary.Asset[] = []
  let hasMore = true
  let endCursor: string | undefined

  while (hasMore) {
    if (signal?.aborted) return
    await waitForIndexerResume(signal)
    if (signal?.aborted) return

    const result = await MediaLibrary.getAssetsAsync({
      mediaType: MediaLibrary.MediaType.audio,
      first: 500,
      after: endCursor,
    })

    if (signal?.aborted) return

    assets.push(...result.assets)
    hasMore = result.hasNextPage
    endCursor = result.endCursor

    await yieldToEventLoop()
  }

  const folderFilterConfig = await ensureFolderFilterConfigLoaded()
  const durationFilterConfig = await ensureTrackDurationFilterConfigLoaded()
  const scopedAssets = assets.filter(
    (asset) =>
      isAssetAllowedByFolderFilters(asset.uri, folderFilterConfig) &&
      isAssetAllowedByTrackDuration(asset.duration, durationFilterConfig)
  )

  onProgress?.({
    phase: "scanning",
    current: 0,
    total: scopedAssets.length,
    currentFile: "",
  })

  // Get existing tracks to compare
  const existingTracks = await db.query.tracks.findMany({
    columns: { id: true, fileHash: true },
  })
  if (signal?.aborted) return

  const existingTrackMap = new Map(
    existingTracks.map((t) => [t.id, t.fileHash])
  )
  const currentAssetIds = new Set(scopedAssets.map((a) => a.id))

  // Find deleted tracks
  const deletedTrackIds = existingTracks
    .filter((t) => !currentAssetIds.has(t.id))
    .map((t) => t.id)

  if (deletedTrackIds.length > 0) {
    await removeTracksFromFavoritesAndPlaylists(deletedTrackIds)
    await db
      .update(tracks)
      .set({ isDeleted: 1 })
      .where(inArray(tracks.id, deletedTrackIds))
    if (signal?.aborted) return
  }

  // Filter assets to process
  const currentAssetHashMap = new Map<string, string>()
  const assetsToProcess = forceFullScan
    ? scopedAssets
    : scopedAssets.filter((asset) => {
        const existingHash = existingTrackMap.get(asset.id)
        const currentHash = generateAssetHash(asset)
        currentAssetHashMap.set(asset.id, currentHash)
        return !existingHash || existingHash !== currentHash
      })

  // Process in batches
  for (let i = 0; i < assetsToProcess.length; i += BATCH_SIZE) {
    if (signal?.aborted) return
    await waitForIndexerResume(signal)
    if (signal?.aborted) return

    const batch = assetsToProcess.slice(i, i + BATCH_SIZE)

    await processBatch(
      batch,
      (asset) => {
        onProgress?.({
          phase: "processing",
          current: i + batch.indexOf(asset) + 1,
          total: assetsToProcess.length,
          currentFile: asset.filename || "Unknown",
        })
      },
      signal,
      currentAssetHashMap
    )

    await yieldToEventLoop()
  }

  if (signal?.aborted) return

  await updateArtistCounts()
  if (signal?.aborted) return
  await updateAlbumCounts()
  if (signal?.aborted) return
  await updateGenreCounts()
  if (signal?.aborted) return

  onProgress?.({
    phase: "complete",
    current: assetsToProcess.length,
    total: assetsToProcess.length,
    currentFile: "",
  })

  if (signal?.aborted) return
  await db.delete(tracks).where(eq(tracks.isDeleted, 1))
}

async function processBatch(
  assets: MediaLibrary.Asset[],
  onFileStart?: (asset: MediaLibrary.Asset) => void,
  signal?: AbortSignal,
  precomputedHashMap?: Map<string, string>
): Promise<void> {
  let nextAssetIndex = 0
  const workerCount = Math.min(BATCH_CONCURRENCY, assets.length)

  await Promise.all(
    Array.from({ length: workerCount }, async () => {
      while (nextAssetIndex < assets.length) {
        if (signal?.aborted) return
        await waitForIndexerResume(signal)
        if (signal?.aborted) return

        const asset = assets[nextAssetIndex]
        nextAssetIndex += 1
        if (!asset) {
          continue
        }

      if (signal?.aborted) return

      onFileStart?.(asset)

      try {
        const fileHash =
          precomputedHashMap?.get(asset.id) || generateAssetHash(asset)
        const metadata = await extractMetadata(
          asset.uri,
          asset.filename || "",
          asset.duration
        )
        if (signal?.aborted) return
        const artworkPath = await saveArtworkToCache(metadata.artwork)
        if (signal?.aborted) return

        const artistId = metadata.artist
          ? await getOrCreateArtist(metadata.artist)
          : null

        const albumArtistId =
          metadata.albumArtist && metadata.albumArtist !== metadata.artist
            ? await getOrCreateArtist(metadata.albumArtist)
            : artistId

        const albumId =
          metadata.album && albumArtistId
            ? await getOrCreateAlbum(
                metadata.album,
                albumArtistId,
                artworkPath,
                metadata.year
              )
            : null

        // Get or create genres - use "Unknown" if no genres found
        const genresToProcess =
          metadata.genres.length > 0 ? metadata.genres : ["Unknown"]
        const genreIds = await Promise.all(
          genresToProcess.map((g) => getOrCreateGenre(g))
        )
        if (signal?.aborted) return

        // Insert track
        const now = Date.now()
        await db
          .insert(tracks)
          .values({
            id: asset.id,
            title: metadata.title,
            artistId,
            albumId,
            duration: metadata.duration,
            uri: asset.uri,
            trackNumber: metadata.trackNumber,
            discNumber: metadata.discNumber,
            year: metadata.year,
            filename: asset.filename || "",
            fileHash,
            audioBitrate: metadata.bitrate || null,
            audioSampleRate: metadata.sampleRate || null,
            audioCodec: metadata.codec || null,
            audioFormat: metadata.format || null,
            artwork: artworkPath,
            lyrics: metadata.lyrics || null,
            composer: metadata.composer || null,
            comment: metadata.comment || null,
            dateAdded: asset.creationTime || now,
            scanTime: now,
            isDeleted: 0,
            isFavorite: 0,
            playCount: 0,
            rating: null,
            lastPlayedAt: null,
            createdAt: now,
            updatedAt: now,
          })
          .onConflictDoUpdate({
            target: tracks.id,
            set: {
              title: metadata.title,
              artistId,
              albumId,
              duration: metadata.duration,
              trackNumber: metadata.trackNumber,
              discNumber: metadata.discNumber,
              year: metadata.year,
              fileHash,
              audioBitrate: metadata.bitrate || null,
              audioSampleRate: metadata.sampleRate || null,
              audioCodec: metadata.codec || null,
              audioFormat: metadata.format || null,
              artwork: artworkPath,
              lyrics: metadata.lyrics || null,
              composer: metadata.composer || null,
              scanTime: now,
              isDeleted: 0,
              updatedAt: now,
            },
          })
        if (signal?.aborted) return

        // Link genres
        if (genreIds.length > 0) {
          await db.delete(trackGenres).where(eq(trackGenres.trackId, asset.id))
          if (signal?.aborted) return

          await db.insert(trackGenres).values(
            genreIds.map((genreId) => ({
              trackId: asset.id,
              genreId,
            }))
          )
          if (signal?.aborted) return
        }
      } catch (error) {
        logError("Failed to index asset", error, {
          assetId: asset.id,
          filename: asset.filename,
        })
      }
      }
    })
  )
}

async function getOrCreateArtist(name: string): Promise<string> {
  const sortName = generateSortName(name)
  const existing = await db.query.artists.findFirst({
    where: eq(artists.name, name),
  })

  if (existing) {
    return existing.id
  }

  const id = generateId()
  await db.insert(artists).values({
    id,
    name,
    sortName,
    createdAt: Date.now(),
    updatedAt: Date.now(),
  })

  return id
}

async function getOrCreateAlbum(
  title: string,
  artistId: string,
  artwork?: string,
  year?: number
): Promise<string> {
  const existing = await db.query.albums.findFirst({
    where: and(eq(albums.title, title), eq(albums.artistId, artistId)),
  })

  if (existing) {
    return existing.id
  }

  const id = generateId()
  await db.insert(albums).values({
    id,
    title,
    artistId,
    year: year || null,
    artwork: artwork || null,
    createdAt: Date.now(),
    updatedAt: Date.now(),
  })

  return id
}

async function getOrCreateGenre(name: string): Promise<string> {
  const existing = await db.query.genres.findFirst({
    where: eq(genres.name, name),
  })

  if (existing) {
    return existing.id
  }

  const id = generateId()
  const { color, shape } = await selectGenreVisuals(name)
  try {
    await db.insert(genres).values({
      id,
      name,
      color,
      shape,
      createdAt: Date.now(),
    })
  } catch {
    // Backward compatibility for databases that have not applied genre visual columns yet.
    await db.insert(genres).values({
      id,
      name,
      createdAt: Date.now(),
    })
  }

  return id
}

async function selectGenreVisuals(
  name: string
): Promise<{ color: string; shape: GenreShape }> {
  let existingVisuals: Array<{ color: string; shape: GenreShape }> = []
  try {
    const rows = await db.query.genres.findMany({
      columns: {
        color: true,
        shape: true,
      },
    })
    existingVisuals = rows.map((row) => ({
      color: row.color,
      shape: row.shape as GenreShape,
    }))
  } catch {
    // If columns are missing before migration, return deterministic defaults.
    const hash = hashString(name)
    return {
      color: GENRE_COLORS[hash % GENRE_COLORS.length],
      shape:
        GENRE_SHAPES[
          Math.floor(hash / GENRE_COLORS.length) % GENRE_SHAPES.length
        ],
    }
  }

  const usedCombinations = new Set(
    existingVisuals.map((visual) => `${visual.color}::${visual.shape}`)
  )

  const colorUsage = new Map<string, number>()
  const shapeUsage = new Map<GenreShape, number>()
  for (const color of GENRE_COLORS) {
    colorUsage.set(color, 0)
  }
  for (const shape of GENRE_SHAPES) {
    shapeUsage.set(shape, 0)
  }
  for (const visual of existingVisuals) {
    colorUsage.set(visual.color, (colorUsage.get(visual.color) ?? 0) + 1)
    shapeUsage.set(visual.shape, (shapeUsage.get(visual.shape) ?? 0) + 1)
  }

  // Prioritize unique colors first, then prefer least-used shapes
  // so both color and shape distribution stay balanced.
  const colorsByUsage = [...GENRE_COLORS].sort(
    (a, b) => (colorUsage.get(a) ?? 0) - (colorUsage.get(b) ?? 0)
  )
  const shapesByUsage = [...GENRE_SHAPES].sort(
    (a, b) => (shapeUsage.get(a) ?? 0) - (shapeUsage.get(b) ?? 0)
  )

  for (const color of colorsByUsage) {
    for (const shape of shapesByUsage) {
      const key = `${color}::${shape}`
      if (!usedCombinations.has(key)) {
        return { color, shape }
      }
    }
  }

  // If all combinations are used, deterministic overlap based on the genre name.
  const hash = hashString(name)
  const color = GENRE_COLORS[hash % GENRE_COLORS.length]
  const shape =
    GENRE_SHAPES[Math.floor(hash / GENRE_COLORS.length) % GENRE_SHAPES.length]
  return { color, shape }
}

async function updateArtistCounts(): Promise<void> {
  await db.run(sql`
    UPDATE artists 
    SET track_count = (
      SELECT COUNT(*) FROM tracks 
      WHERE tracks.artist_id = artists.id AND tracks.is_deleted = 0
    ),
    album_count = (
      SELECT COUNT(DISTINCT album_id) FROM tracks 
      WHERE tracks.artist_id = artists.id AND tracks.is_deleted = 0
    ),
    artwork = COALESCE(
      (
        SELECT t.artwork FROM tracks t
        WHERE t.artist_id = artists.id
          AND t.is_deleted = 0
          AND t.artwork IS NOT NULL
        ORDER BY COALESCE(t.last_played_at, 0) DESC, COALESCE(t.date_added, 0) DESC
        LIMIT 1
      ),
      (
        SELECT a.artwork FROM tracks t
        JOIN albums a ON a.id = t.album_id
        WHERE t.artist_id = artists.id
          AND t.is_deleted = 0
          AND a.artwork IS NOT NULL
        ORDER BY COALESCE(t.last_played_at, 0) DESC, COALESCE(t.date_added, 0) DESC
        LIMIT 1
      ),
      artists.artwork
    ),
    updated_at = ${Date.now()}
  `)
}

async function updateAlbumCounts(): Promise<void> {
  await db.run(sql`
    UPDATE albums 
    SET track_count = (
      SELECT COUNT(*) FROM tracks 
      WHERE tracks.album_id = albums.id AND tracks.is_deleted = 0
    ),
    duration = (
      SELECT COALESCE(SUM(duration), 0) FROM tracks 
      WHERE tracks.album_id = albums.id AND tracks.is_deleted = 0
    ),
    updated_at = ${Date.now()}
  `)
}

async function updateGenreCounts(): Promise<void> {
  await db.run(sql`
    UPDATE genres 
    SET track_count = (
      SELECT COUNT(*) FROM track_genres tg
      JOIN tracks t ON tg.track_id = t.id
      WHERE tg.genre_id = genres.id AND t.is_deleted = 0
    )
  `)
}

function generateAssetHash(asset: MediaLibrary.Asset): string {
  const info = getFileInfo(asset.uri)
  const modificationTime =
    info?.modificationTime ?? asset.modificationTime ?? asset.creationTime ?? 0
  const size = info?.size ?? asset.duration ?? 0

  return generateFileHash(
    asset.uri,
    modificationTime,
    size,
    asset.filename || ""
  )
}

function getFileInfo(
  uri: string
): { size: number; modificationTime: number | null } | null {
  try {
    const file = new File(uri)
    return file.info()
  } catch {
    return null
  }
}

function generateFileHash(
  uri: string,
  modTime: number,
  size: number,
  filename: string
): string {
  const fingerprint = `${uri}|${modTime}|${size}|${filename}`
  let hashA = 5381
  let hashB = 52711

  for (let i = 0; i < fingerprint.length; i++) {
    const char = fingerprint.charCodeAt(i)
    hashA = ((hashA << 5) + hashA) ^ char
    hashB = ((hashB << 5) + hashB) ^ char
  }

  const partA = (hashA >>> 0).toString(16).padStart(8, "0")
  const partB = (hashB >>> 0).toString(16).padStart(8, "0")
  return `${partA}${partB}`
}

function generateSortName(name: string): string {
  const articles = ["The", "A", "An"]
  for (const article of articles) {
    if (name.startsWith(`${article} `)) {
      return `${name.slice(article.length + 1)}, ${article}`
    }
  }
  return name
}

function generateId(): string {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
}

function hashString(value: string): number {
  let hash = 0
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) >>> 0
  }
  return hash
}
