import { TrackPlayer } from "@/modules/player/player.utils"

import { logError, logInfo, logWarn } from "@/modules/logging/logging.service"

import { mapTrackToTrackPlayerInput } from "./player-adapter"
import {
  getCurrentTrackState,
  getIsShuffledState,
  getOriginalQueueState,
  getQueueState,
  setIsShuffledState,
  setOriginalQueueState,
  setQueueState,
  type Track,
} from "./player.store"
import {
  persistPlaybackSession,
  syncCurrentTrackFromPlayer,
} from "./player-session.service"

export async function addToQueue(track: Track) {
  const queue = getQueueState()
  if (queue.some((queuedTrack) => queuedTrack.id === track.id)) {
    logInfo("Skipped addToQueue because track already exists", {
      trackId: track.id,
    })
    return
  }

  logInfo("Adding track to queue", {
    trackId: track.id,
    queueLength: queue.length,
  })

  try {
    setQueueState([...queue, track])
    await TrackPlayer.add(mapTrackToTrackPlayerInput(track))
    await persistPlaybackSession({ force: true })
    logInfo("Added track to queue", {
      trackId: track.id,
    })
  } catch (error) {
    setQueueState(queue)
    logError("Failed to add track to queue", error, {
      trackId: track.id,
      queueLength: queue.length,
    })
    throw error
  }
}

export async function queueTrackNext(track: Track) {
  const queue = getQueueState()
  const currentTrack = getCurrentTrackState()
  const filteredQueue = queue.filter((queuedTrack) => queuedTrack.id !== track.id)

  logInfo("Queueing track to play next", {
    trackId: track.id,
    queueLength: queue.length,
    currentTrackId: currentTrack?.id ?? null,
  })

  const previousQueue = queue

  if (!currentTrack) {
    setQueueState([track, ...filteredQueue])
  } else {
    const currentIndex = filteredQueue.findIndex(
      (queuedTrack) => queuedTrack.id === currentTrack.id
    )

    setQueueState([
      ...filteredQueue.slice(0, currentIndex + 1),
      track,
      ...filteredQueue.slice(currentIndex + 1),
    ])
  }

  const trackPlayerQueue = await TrackPlayer.getQueue()
  const currentTrackPlayerIndex = await TrackPlayer.getCurrentTrack()
  const insertIndex =
    currentTrackPlayerIndex !== null
      ? Math.min(currentTrackPlayerIndex + 1, trackPlayerQueue.length)
      : 0

  try {
    await TrackPlayer.add(mapTrackToTrackPlayerInput(track), insertIndex)
    await persistPlaybackSession({ force: true })
    logInfo("Queued track to play next", {
      trackId: track.id,
      insertIndex,
    })
  } catch (error) {
    setQueueState(previousQueue)
    logError("Failed to queue track next", error, {
      trackId: track.id,
      insertIndex,
      queueLength: queue.length,
    })
    throw error
  }
}

export async function removeFromQueue(trackId: string) {
  const queue = getQueueState()
  logInfo("Removing track from queue", {
    trackId,
    queueLength: queue.length,
  })

  setQueueState(queue.filter((track) => track.id !== trackId))

  try {
    const trackPlayerQueue = await TrackPlayer.getQueue()
    const trackIndex = trackPlayerQueue.findIndex((track) => track.id === trackId)

    if (trackIndex !== -1) {
      await TrackPlayer.remove(trackIndex)
    } else {
      logWarn("Track not found in native queue while removing", { trackId })
    }

    await persistPlaybackSession({ force: true })
    logInfo("Removed track from queue", { trackId })
  } catch (error) {
    setQueueState(queue)
    logError("Failed to remove track from queue", error, {
      trackId,
      queueLength: queue.length,
    })
    throw error
  }
}

export async function clearQueue() {
  const currentTrack = getCurrentTrackState()
  const previousQueue = getQueueState()

  logInfo("Clearing queue", {
    queueLength: previousQueue.length,
    currentTrackId: currentTrack?.id ?? null,
  })

  setQueueState(currentTrack ? [currentTrack] : [])

  try {
    await TrackPlayer.reset()

    if (currentTrack) {
      await TrackPlayer.add(mapTrackToTrackPlayerInput(currentTrack))
    }

    await persistPlaybackSession({ force: true })
    logInfo("Queue cleared", {
      keptCurrentTrack: Boolean(currentTrack),
    })
  } catch (error) {
    setQueueState(previousQueue)
    logError("Failed to clear queue", error, {
      queueLength: previousQueue.length,
      currentTrackId: currentTrack?.id ?? null,
    })
    throw error
  }
}

export async function moveInQueue(fromIndex: number, toIndex: number) {
  const previousQueue = getQueueState()
  if (
    fromIndex < 0 ||
    toIndex < 0 ||
    fromIndex >= previousQueue.length ||
    toIndex >= previousQueue.length ||
    fromIndex === toIndex
  ) {
    return
  }

  const nextQueue = [...previousQueue]
  const [movedTrack] = nextQueue.splice(fromIndex, 1)
  if (!movedTrack) {
    return
  }

  nextQueue.splice(toIndex, 0, movedTrack)
  setQueueState(nextQueue)

  try {
    await TrackPlayer.move(fromIndex, toIndex)
    await syncCurrentTrackFromPlayer()
    await persistPlaybackSession({ force: true })
  } catch (error) {
    setQueueState(previousQueue)
    logError("Failed to reorder player queue", error, {
      fromIndex,
      toIndex,
      queueLength: previousQueue.length,
    })
  }
}

export async function toggleShuffle() {
  const isShuffled = getIsShuffledState()
  const queue = getQueueState()
  const currentTrack = getCurrentTrackState()

  if (queue.length <= 1) {
    return
  }

  if (!isShuffled) {
    setOriginalQueueState([...queue])

    const currentIndex = currentTrack
      ? queue.findIndex((track) => track.id === currentTrack.id)
      : 0

    const playedAndCurrent = queue.slice(0, currentIndex + 1)
    const upcoming = queue.slice(currentIndex + 1)

    for (let index = upcoming.length - 1; index > 0; index -= 1) {
      const nextIndex = Math.floor(Math.random() * (index + 1))
      ;[upcoming[index], upcoming[nextIndex]] = [
        upcoming[nextIndex],
        upcoming[index],
      ]
    }

    setQueueState([...playedAndCurrent, ...upcoming])
    setIsShuffledState(true)

    try {
      const [trackPlayerQueue, currentTrackPlayerIndex] = await Promise.all([
        TrackPlayer.getQueue(),
        TrackPlayer.getCurrentTrack(),
      ])

      if (
        currentTrackPlayerIndex === null ||
        currentTrackPlayerIndex < 0 ||
        currentTrackPlayerIndex >= trackPlayerQueue.length
      ) {
        await syncCurrentTrackFromPlayer()
        await persistPlaybackSession({ force: true })
        return
      }

      const indexesToRemove = Array.from(
        { length: Math.max(0, trackPlayerQueue.length - currentTrackPlayerIndex - 1) },
        (_, index) => currentTrackPlayerIndex + 1 + index
      )

      if (indexesToRemove.length > 0) {
        await TrackPlayer.remove(indexesToRemove)
      }

      if (upcoming.length > 0) {
        await TrackPlayer.add(upcoming.map(mapTrackToTrackPlayerInput))
      }

      await syncCurrentTrackFromPlayer()
      await persistPlaybackSession({ force: true })
    } catch (error) {
      logError("Failed to enable queue shuffle", error, {
        queueLength: queue.length,
        currentTrackId: currentTrack?.id ?? null,
      })
      setQueueState(queue)
      setIsShuffledState(false)
    }

    return
  }

  const originalQueue = getOriginalQueueState()
  if (originalQueue.length === 0) {
    setIsShuffledState(false)
    return
  }
  const currentIndex = currentTrack
    ? originalQueue.findIndex((track) => track.id === currentTrack.id)
    : 0

  if (currentIndex < 0) {
    setQueueState(originalQueue)
    setIsShuffledState(false)
    await syncCurrentTrackFromPlayer()
    await persistPlaybackSession({ force: true })
    return
  }

  const playedAndCurrent = originalQueue.slice(0, currentIndex + 1)
  const originalUpcoming = originalQueue.slice(currentIndex + 1)
  setQueueState([...playedAndCurrent, ...originalUpcoming])
  setIsShuffledState(false)

  try {
    const [trackPlayerQueue, currentTrackPlayerIndex] = await Promise.all([
      TrackPlayer.getQueue(),
      TrackPlayer.getCurrentTrack(),
    ])

    if (
      currentTrackPlayerIndex === null ||
      currentTrackPlayerIndex < 0 ||
      currentTrackPlayerIndex >= trackPlayerQueue.length
    ) {
      await syncCurrentTrackFromPlayer()
      await persistPlaybackSession({ force: true })
      return
    }

    const indexesToRemove = Array.from(
      { length: Math.max(0, trackPlayerQueue.length - currentTrackPlayerIndex - 1) },
      (_, index) => currentTrackPlayerIndex + 1 + index
    )

    if (indexesToRemove.length > 0) {
      await TrackPlayer.remove(indexesToRemove)
    }

    if (originalUpcoming.length > 0) {
      await TrackPlayer.add(originalUpcoming.map(mapTrackToTrackPlayerInput))
    }

    await syncCurrentTrackFromPlayer()
    await persistPlaybackSession({ force: true })
  } catch (error) {
    logError("Failed to disable queue shuffle", error, {
      queueLength: queue.length,
      originalQueueLength: originalQueue.length,
      currentTrackId: currentTrack?.id ?? null,
    })
    setQueueState(queue)
    setIsShuffledState(true)
  }
}
