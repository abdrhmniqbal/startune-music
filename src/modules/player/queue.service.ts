import { TrackPlayer } from "@/modules/player/player.utils"

import { logError } from "@/modules/logging/logging.service"

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
} from "./player.service"

function mapTrackToTrackPlayerInput(track: Track) {
  return {
    id: track.id,
    url: track.uri,
    title: track.title,
    artist: track.artist,
    album: track.album,
    artwork: track.image,
    duration: track.duration,
  }
}

export async function addToQueue(track: Track) {
  const queue = getQueueState()
  if (queue.some((queuedTrack) => queuedTrack.id === track.id)) {
    return
  }

  setQueueState([...queue, track])
  await TrackPlayer.add(mapTrackToTrackPlayerInput(track))
  await persistPlaybackSession({ force: true })
}

export async function queueTrackNext(track: Track) {
  const queue = getQueueState()
  const currentTrack = getCurrentTrackState()
  const filteredQueue = queue.filter((queuedTrack) => queuedTrack.id !== track.id)

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

  await TrackPlayer.add(mapTrackToTrackPlayerInput(track), insertIndex)
  await persistPlaybackSession({ force: true })
}

export async function removeFromQueue(trackId: string) {
  const queue = getQueueState()
  setQueueState(queue.filter((track) => track.id !== trackId))

  const trackPlayerQueue = await TrackPlayer.getQueue()
  const trackIndex = trackPlayerQueue.findIndex((track) => track.id === trackId)

  if (trackIndex !== -1) {
    await TrackPlayer.remove(trackIndex)
  }

  await persistPlaybackSession({ force: true })
}

export async function clearQueue() {
  const currentTrack = getCurrentTrackState()
  setQueueState(currentTrack ? [currentTrack] : [])

  await TrackPlayer.reset()

  if (currentTrack) {
    await TrackPlayer.add(mapTrackToTrackPlayerInput(currentTrack))
  }

  await persistPlaybackSession({ force: true })
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

    const trackPlayerQueue = await TrackPlayer.getQueue()
    const currentTrackPlayerIndex = await TrackPlayer.getCurrentTrack()

    if (currentTrackPlayerIndex !== null && currentTrackPlayerIndex !== undefined) {
      for (
        let queueIndex = trackPlayerQueue.length - 1;
        queueIndex > currentTrackPlayerIndex;
        queueIndex -= 1
      ) {
        await TrackPlayer.remove(queueIndex)
      }

      for (const track of upcoming) {
        await TrackPlayer.add(mapTrackToTrackPlayerInput(track))
      }

      await persistPlaybackSession({ force: true })
    }

    return
  }

  const originalQueue = getOriginalQueueState()
  const currentIndex = currentTrack
    ? originalQueue.findIndex((track) => track.id === currentTrack.id)
    : 0

  const playedAndCurrent = originalQueue.slice(0, currentIndex + 1)
  const originalUpcoming = originalQueue.slice(currentIndex + 1)
  setQueueState([...playedAndCurrent, ...originalUpcoming])
  setIsShuffledState(false)

  const trackPlayerQueue = await TrackPlayer.getQueue()
  const currentTrackPlayerIndex = await TrackPlayer.getCurrentTrack()

  if (currentTrackPlayerIndex !== null && currentTrackPlayerIndex !== undefined) {
    for (
      let queueIndex = trackPlayerQueue.length - 1;
      queueIndex > currentTrackPlayerIndex;
      queueIndex -= 1
    ) {
      await TrackPlayer.remove(queueIndex)
    }

    for (const track of originalUpcoming) {
      await TrackPlayer.add(mapTrackToTrackPlayerInput(track))
    }

    await persistPlaybackSession({ force: true })
  }
}
