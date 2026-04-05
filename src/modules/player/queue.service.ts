import { TrackPlayer } from "@/modules/player/player.utils"

import { logError, logInfo, logWarn } from "@/modules/logging/logging.service"

import { mapTrackToTrackPlayerInput } from "./player-adapter"
import {
  getImmediateQueueTrackIdsState,
  getIsPlayingState,
  getCurrentTrackState,
  getIsShuffledState,
  getOriginalQueueState,
  getOriginalQueueTrackIdsState,
  getQueueState,
  getQueueTrackIdsState,
  setIsPlayingState,
  setImmediateQueueTrackIdsState,
  setIsShuffledState,
  setOriginalQueueState,
  setOriginalQueueTrackIdsState,
  setQueueState,
  setQueueTrackIdsState,
  type Track,
  getTracksState,
} from "./player.store"
import {
  persistPlaybackSession,
  syncCurrentTrackFromPlayer,
} from "./player-session.service"
import { setActiveTrack } from "./player-runtime-state"

let queueMutationChain: Promise<void> = Promise.resolve()

function runSerializedQueueMutation(work: () => Promise<void>) {
  const run = queueMutationChain.then(work, work)
  queueMutationChain = run.then(
    () => undefined,
    () => undefined
  )
  return run
}

function dedupeTrackIds(ids: string[]) {
  return ids.filter((id, index) => ids.indexOf(id) === index)
}

function getTrackLookupMap() {
  const map = new Map<string, Track>()

  for (const track of getTracksState()) {
    if (!track.id || !track.uri || track.isDeleted) {
      continue
    }

    map.set(track.id, track)
  }

  for (const track of getQueueState()) {
    if (!track.id || !track.uri) {
      continue
    }

    if (!map.has(track.id)) {
      map.set(track.id, track)
    }
  }

  return map
}

function resolveTracksFromIds(trackIds: string[], trackLookup: Map<string, Track>) {
  const resolved: Track[] = []
  for (const trackId of dedupeTrackIds(trackIds)) {
    const track = trackLookup.get(trackId)
    if (!track) {
      continue
    }
    resolved.push(track)
  }

  return resolved
}

function buildEffectiveQueueIds(
  baseQueueTrackIds: string[],
  currentTrackId: string | null,
  immediateQueueTrackIds: string[]
) {
  const normalizedBaseIds = dedupeTrackIds(baseQueueTrackIds)
  const normalizedImmediateIds = dedupeTrackIds(immediateQueueTrackIds)
  const immediateSet = new Set(normalizedImmediateIds)

  if (!currentTrackId) {
    return dedupeTrackIds([
      ...normalizedImmediateIds,
      ...normalizedBaseIds.filter((id) => !immediateSet.has(id)),
    ])
  }

  const currentIndex = normalizedBaseIds.indexOf(currentTrackId)
  if (currentIndex < 0) {
    return dedupeTrackIds([
      currentTrackId,
      ...normalizedImmediateIds.filter((id) => id !== currentTrackId),
      ...normalizedBaseIds.filter(
        (id) => id !== currentTrackId && !immediateSet.has(id)
      ),
    ])
  }

  const playedAndCurrent = normalizedBaseIds.slice(0, currentIndex + 1)
  const upcomingBase = normalizedBaseIds.slice(currentIndex + 1)

  return dedupeTrackIds([
    ...playedAndCurrent,
    ...normalizedImmediateIds.filter((id) => id !== currentTrackId),
    ...upcomingBase.filter((id) => !immediateSet.has(id)),
  ])
}

function getQueueModelSnapshot() {
  return {
    queue: getQueueState(),
    queueTrackIds: getQueueTrackIdsState(),
    originalQueue: getOriginalQueueState(),
    originalQueueTrackIds: getOriginalQueueTrackIdsState(),
    immediateQueueTrackIds: getImmediateQueueTrackIdsState(),
    isShuffled: getIsShuffledState(),
  }
}

function restoreQueueModelSnapshot(snapshot: ReturnType<typeof getQueueModelSnapshot>) {
  setQueueState(snapshot.queue)
  setQueueTrackIdsState(snapshot.queueTrackIds)
  setOriginalQueueState(snapshot.originalQueue)
  setOriginalQueueTrackIdsState(snapshot.originalQueueTrackIds)
  setImmediateQueueTrackIdsState(snapshot.immediateQueueTrackIds)
  setIsShuffledState(snapshot.isShuffled)
}

function applyQueueModel(options: {
  baseQueueTrackIds: string[]
  immediateQueueTrackIds: string[]
  effectiveQueueTrackIds?: string[]
  isShuffled?: boolean
}) {
  const trackLookup = getTrackLookupMap()
  const currentTrackId = getCurrentTrackState()?.id ?? null

  const normalizedBaseIds = dedupeTrackIds(options.baseQueueTrackIds).filter(
    (trackId) => trackLookup.has(trackId)
  )
  const normalizedImmediateIds = dedupeTrackIds(
    options.immediateQueueTrackIds
  ).filter((trackId) => trackLookup.has(trackId))
  const normalizedEffectiveIds = dedupeTrackIds(
    options.effectiveQueueTrackIds ||
      buildEffectiveQueueIds(
        normalizedBaseIds,
        currentTrackId,
        normalizedImmediateIds
      )
  ).filter((trackId) => trackLookup.has(trackId))

  const queueTracks = resolveTracksFromIds(normalizedEffectiveIds, trackLookup)
  const originalQueueTracks = resolveTracksFromIds(normalizedBaseIds, trackLookup)

  setQueueTrackIdsState(normalizedEffectiveIds)
  setOriginalQueueTrackIdsState(normalizedBaseIds)
  setImmediateQueueTrackIdsState(normalizedImmediateIds)
  setQueueState(queueTracks)
  setOriginalQueueState(originalQueueTracks)

  if (options.isShuffled !== undefined) {
    setIsShuffledState(options.isShuffled)
  }

  const currentTrack = getCurrentTrackState()
  if (currentTrack && !normalizedEffectiveIds.includes(currentTrack.id)) {
    const fallbackTrack = queueTracks[0] || null
    setActiveTrack(fallbackTrack)

    if (!fallbackTrack) {
      setIsPlayingState(false)
    }
  }
}

async function rebuildNativeQueueFromStore(currentTrackId?: string | null) {
  const queueTracks = getQueueState()
  const trackLookup = new Map(queueTracks.map((track) => [track.id, track]))
  const resolvedCurrentTrackId =
    currentTrackId && trackLookup.has(currentTrackId)
      ? currentTrackId
      : queueTracks[0]?.id || null
  const wasPlaying = getIsPlayingState()

  await TrackPlayer.reset()

  if (queueTracks.length === 0) {
    setActiveTrack(null)
    setIsPlayingState(false)
    return
  }

  await TrackPlayer.add(queueTracks.map(mapTrackToTrackPlayerInput))

  const targetIndex = resolvedCurrentTrackId
    ? queueTracks.findIndex((track) => track.id === resolvedCurrentTrackId)
    : 0

  if (targetIndex > 0) {
    await TrackPlayer.skip(targetIndex)
  }

  const activeTrack =
    targetIndex >= 0 ? (queueTracks[targetIndex] ?? queueTracks[0]) : queueTracks[0]
  setActiveTrack(activeTrack)
  setIsPlayingState(wasPlaying)

  if (wasPlaying) {
    await TrackPlayer.play()
  }
}

export async function addToQueue(track: Track) {
  await runSerializedQueueMutation(async () => {
    const snapshot = getQueueModelSnapshot()
    const queueTrackIds = getQueueTrackIdsState()

    if (queueTrackIds.includes(track.id)) {
      logInfo("Skipped addToQueue because track already exists", {
        trackId: track.id,
      })
      return
    }

    logInfo("Adding track to queue", {
      trackId: track.id,
      queueLength: queueTrackIds.length,
    })

    const baseQueueTrackIds =
      getOriginalQueueTrackIdsState().length > 0
        ? getOriginalQueueTrackIdsState()
        : queueTrackIds

    applyQueueModel({
      baseQueueTrackIds: [...baseQueueTrackIds, track.id],
      immediateQueueTrackIds: getImmediateQueueTrackIdsState(),
      effectiveQueueTrackIds: [...queueTrackIds, track.id],
    })

    try {
      await TrackPlayer.add(mapTrackToTrackPlayerInput(track))
      await persistPlaybackSession({ force: true })
      logInfo("Added track to queue", {
        trackId: track.id,
      })
    } catch (error) {
      restoreQueueModelSnapshot(snapshot)
      logError("Failed to add track to queue", error, {
        trackId: track.id,
        queueLength: queueTrackIds.length,
      })
      throw error
    }
  })
}

export async function queueTrackNext(track: Track) {
  await runSerializedQueueMutation(async () => {
    const snapshot = getQueueModelSnapshot()
    const currentTrackId = getCurrentTrackState()?.id ?? null
    const queueTrackIds = getQueueTrackIdsState()
    const immediateQueueTrackIds = [
      ...getImmediateQueueTrackIdsState().filter((trackId) => trackId !== track.id),
      track.id,
    ]
    const baseQueueTrackIds =
      getOriginalQueueTrackIdsState().length > 0
        ? getOriginalQueueTrackIdsState().filter((trackId) => trackId !== track.id)
        : queueTrackIds.filter((trackId) => trackId !== track.id)

    const effectiveQueueTrackIds = buildEffectiveQueueIds(
      baseQueueTrackIds,
      currentTrackId,
      immediateQueueTrackIds
    )

    logInfo("Queueing track to play next", {
      trackId: track.id,
      queueLength: queueTrackIds.length,
      currentTrackId,
    })

    applyQueueModel({
      baseQueueTrackIds,
      immediateQueueTrackIds,
      effectiveQueueTrackIds,
    })

    try {
      const trackPlayerQueue = await TrackPlayer.getQueue()
      const existingIndex = trackPlayerQueue.findIndex(
        (queueItem) => queueItem.id === track.id
      )
      if (existingIndex >= 0) {
        await TrackPlayer.remove(existingIndex)
      }

      const currentTrackPlayerIndex = await TrackPlayer.getCurrentTrack()
      const insertIndex =
        currentTrackPlayerIndex !== null
          ? Math.min(currentTrackPlayerIndex + 1, trackPlayerQueue.length)
          : 0

      await TrackPlayer.add(mapTrackToTrackPlayerInput(track), insertIndex)
      await persistPlaybackSession({ force: true })
      logInfo("Queued track to play next", {
        trackId: track.id,
        insertIndex,
      })
    } catch (error) {
      restoreQueueModelSnapshot(snapshot)
      logError("Failed to queue track next", error, {
        trackId: track.id,
        queueLength: queueTrackIds.length,
      })
      throw error
    }
  })
}

export async function removeFromQueue(trackId: string) {
  await runSerializedQueueMutation(async () => {
    const snapshot = getQueueModelSnapshot()
    const queueTrackIds = getQueueTrackIdsState()
    const currentTrackId = getCurrentTrackState()?.id ?? null
    const wasCurrentTrack = currentTrackId === trackId

    const nextQueueTrackIds = queueTrackIds.filter((id) => id !== trackId)
    const nextBaseQueueTrackIds =
      getOriginalQueueTrackIdsState().length > 0
        ? getOriginalQueueTrackIdsState().filter((id) => id !== trackId)
        : nextQueueTrackIds
    const nextImmediateQueueTrackIds = getImmediateQueueTrackIdsState().filter(
      (id) => id !== trackId
    )

    logInfo("Removing track from queue", {
      trackId,
      queueLength: queueTrackIds.length,
      wasCurrentTrack,
    })

    applyQueueModel({
      baseQueueTrackIds: nextBaseQueueTrackIds,
      immediateQueueTrackIds: nextImmediateQueueTrackIds,
      effectiveQueueTrackIds: nextQueueTrackIds,
    })

    try {
      if (wasCurrentTrack) {
        const nextCurrentTrackId = nextQueueTrackIds[0] || null
        await rebuildNativeQueueFromStore(nextCurrentTrackId)
      } else {
        const trackPlayerQueue = await TrackPlayer.getQueue()
        const trackIndex = trackPlayerQueue.findIndex((track) => track.id === trackId)

        if (trackIndex !== -1) {
          await TrackPlayer.remove(trackIndex)
        } else {
          logWarn("Track not found in native queue while removing", { trackId })
        }
      }

      await persistPlaybackSession({ force: true })
      logInfo("Removed track from queue", { trackId })
    } catch (error) {
      restoreQueueModelSnapshot(snapshot)
      logError("Failed to remove track from queue", error, {
        trackId,
        queueLength: queueTrackIds.length,
        wasCurrentTrack,
      })
      throw error
    }
  })
}

export async function clearQueue() {
  await runSerializedQueueMutation(async () => {
    const snapshot = getQueueModelSnapshot()
    const currentTrack = getCurrentTrackState()

    logInfo("Clearing queue", {
      queueLength: snapshot.queueTrackIds.length,
      currentTrackId: currentTrack?.id ?? null,
    })

    const remainingTrackIds = currentTrack ? [currentTrack.id] : []

    applyQueueModel({
      baseQueueTrackIds: remainingTrackIds,
      immediateQueueTrackIds: [],
      effectiveQueueTrackIds: remainingTrackIds,
    })

    try {
      await rebuildNativeQueueFromStore(currentTrack?.id ?? null)
      await persistPlaybackSession({ force: true })
      logInfo("Queue cleared", {
        keptCurrentTrack: Boolean(currentTrack),
      })
    } catch (error) {
      restoreQueueModelSnapshot(snapshot)
      logError("Failed to clear queue", error, {
        queueLength: snapshot.queueTrackIds.length,
        currentTrackId: currentTrack?.id ?? null,
      })
      throw error
    }
  })
}

export async function moveInQueue(fromIndex: number, toIndex: number) {
  await runSerializedQueueMutation(async () => {
    const snapshot = getQueueModelSnapshot()
    const queueTrackIds = [...getQueueTrackIdsState()]

    if (
      fromIndex < 0 ||
      toIndex < 0 ||
      fromIndex >= queueTrackIds.length ||
      toIndex >= queueTrackIds.length ||
      fromIndex === toIndex
    ) {
      return
    }

    const [movedTrackId] = queueTrackIds.splice(fromIndex, 1)
    if (!movedTrackId) {
      return
    }

    queueTrackIds.splice(toIndex, 0, movedTrackId)

    applyQueueModel({
      baseQueueTrackIds: queueTrackIds,
      immediateQueueTrackIds: [],
      effectiveQueueTrackIds: queueTrackIds,
      isShuffled: false,
    })

    try {
      await TrackPlayer.move(fromIndex, toIndex)
      await syncCurrentTrackFromPlayer()
      await persistPlaybackSession({ force: true })
    } catch (error) {
      restoreQueueModelSnapshot(snapshot)
      logError("Failed to reorder player queue", error, {
        fromIndex,
        toIndex,
        queueLength: snapshot.queueTrackIds.length,
      })
    }
  })
}

export async function toggleShuffle() {
  await runSerializedQueueMutation(async () => {
    const snapshot = getQueueModelSnapshot()
    const queueTrackIds = getQueueTrackIdsState()
    const isShuffled = getIsShuffledState()
    const currentTrackId = getCurrentTrackState()?.id ?? null

    if (queueTrackIds.length <= 1) {
      return
    }

    if (!isShuffled) {
      const baseQueueTrackIds = [...queueTrackIds]
      const currentIndex = currentTrackId
        ? baseQueueTrackIds.findIndex((trackId) => trackId === currentTrackId)
        : 0

      const playedAndCurrent =
        currentIndex >= 0 ? baseQueueTrackIds.slice(0, currentIndex + 1) : []
      const upcoming =
        currentIndex >= 0
          ? [...baseQueueTrackIds.slice(currentIndex + 1)]
          : [...baseQueueTrackIds]

      for (let index = upcoming.length - 1; index > 0; index -= 1) {
        const nextIndex = Math.floor(Math.random() * (index + 1))
        ;[upcoming[index], upcoming[nextIndex]] = [
          upcoming[nextIndex],
          upcoming[index],
        ]
      }

      const shuffledBaseTrackIds = [...playedAndCurrent, ...upcoming]

      applyQueueModel({
        baseQueueTrackIds,
        immediateQueueTrackIds: [],
        effectiveQueueTrackIds: shuffledBaseTrackIds,
        isShuffled: true,
      })

      try {
        await rebuildNativeQueueFromStore(currentTrackId)
        await syncCurrentTrackFromPlayer()
        await persistPlaybackSession({ force: true })
      } catch (error) {
        restoreQueueModelSnapshot(snapshot)
        logError("Failed to enable queue shuffle", error, {
          queueLength: queueTrackIds.length,
          currentTrackId,
        })
      }

      return
    }

    const baseQueueTrackIds =
      getOriginalQueueTrackIdsState().length > 0
        ? getOriginalQueueTrackIdsState()
        : queueTrackIds
    const immediateQueueTrackIds = getImmediateQueueTrackIdsState()

    applyQueueModel({
      baseQueueTrackIds,
      immediateQueueTrackIds,
      isShuffled: false,
    })

    try {
      await rebuildNativeQueueFromStore(currentTrackId)
      await syncCurrentTrackFromPlayer()
      await persistPlaybackSession({ force: true })
    } catch (error) {
      restoreQueueModelSnapshot(snapshot)
      logError("Failed to disable queue shuffle", error, {
        queueLength: queueTrackIds.length,
        originalQueueLength: baseQueueTrackIds.length,
        currentTrackId,
      })
    }
  })
}
