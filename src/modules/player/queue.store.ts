import { useMemo } from "react"

import { TrackPlayer } from "@/modules/player/player.utils"

import {
  getCurrentTrackState,
  getIsShuffledState,
  getOriginalQueueState,
  getQueueState,
  setIsShuffledState,
  setOriginalQueueState,
  setQueueState,
  usePlayerStore,
  type Track,
} from "./player.store"
import {
  persistPlaybackSession,
  syncCurrentTrackFromPlayer,
} from "./player.service"

function buildQueueInfo(queue: Track[], currentTrack: Track | null) {
  const currentIndex = currentTrack
    ? queue.findIndex((track) => track.id === currentTrack.id)
    : -1

  return {
    queue,
    currentIndex,
    length: queue.length,
    upNext: currentIndex >= 0 ? queue.slice(currentIndex + 1) : queue,
    hasNext: currentIndex < queue.length - 1,
    hasPrevious: currentIndex > 0,
  }
}

export function useQueueInfo() {
  const queue = usePlayerStore((state) => state.queue)
  const currentTrack = usePlayerStore((state) => state.currentTrack)

  return useMemo(
    () => buildQueueInfo(queue, currentTrack),
    [currentTrack, queue]
  )
}

export async function addToQueue(track: Track) {
  const queue = getQueueState()
  if (queue.some((t) => t.id === track.id)) return

  setQueueState([...queue, track])

  await TrackPlayer.add({
    id: track.id,
    url: track.uri,
    title: track.title,
    artist: track.artist,
    album: track.album,
    artwork: track.image,
    duration: track.duration,
  })
  await persistPlaybackSession({ force: true })
}

export async function playNext(track: Track) {
  const queue = getQueueState()
  const currentTrack = getCurrentTrackState()

  const filteredQueue = queue.filter((t) => t.id !== track.id)

  if (!currentTrack) {
    setQueueState([track, ...filteredQueue])
  } else {
    const currentIndex = filteredQueue.findIndex(
      (t) => t.id === currentTrack.id
    )
    const newQueue = [
      ...filteredQueue.slice(0, currentIndex + 1),
      track,
      ...filteredQueue.slice(currentIndex + 1),
    ]
    setQueueState(newQueue)
  }

  const tpQueue = await TrackPlayer.getQueue()
  const currentTpTrack = await TrackPlayer.getCurrentTrack()
  const insertIndex =
    currentTpTrack !== null ? Math.min(currentTpTrack + 1, tpQueue.length) : 0

  await TrackPlayer.add(
    {
      id: track.id,
      url: track.uri,
      title: track.title,
      artist: track.artist,
      album: track.album,
      artwork: track.image,
      duration: track.duration,
    },
    insertIndex
  )
  await persistPlaybackSession({ force: true })
}

export async function removeFromQueue(trackId: string) {
  const queue = getQueueState()
  setQueueState(queue.filter((t) => t.id !== trackId))

  const tpQueue = await TrackPlayer.getQueue()
  const index = tpQueue.findIndex((t) => t.id === trackId)
  if (index !== -1) {
    await TrackPlayer.remove(index)
  }
  await persistPlaybackSession({ force: true })
}

export async function clearQueue() {
  const currentTrack = getCurrentTrackState()

  if (currentTrack) {
    setQueueState([currentTrack])
  } else {
    setQueueState([])
  }

  await TrackPlayer.reset()

  if (currentTrack) {
    await TrackPlayer.add({
      id: currentTrack.id,
      url: currentTrack.uri,
      title: currentTrack.title,
      artist: currentTrack.artist,
      album: currentTrack.album,
      artwork: currentTrack.image,
      duration: currentTrack.duration,
    })
  }
  await persistPlaybackSession({ force: true })
}

export function setQueue(tracks: Track[]) {
  setQueueState(tracks)
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

  const queue = [...previousQueue]
  const [moved] = queue.splice(fromIndex, 1)
  if (!moved) {
    return
  }

  queue.splice(toIndex, 0, moved)
  setQueueState(queue)

  try {
    await TrackPlayer.move(fromIndex, toIndex)
    await syncCurrentTrackFromPlayer()
    await persistPlaybackSession({ force: true })
  } catch {
    setQueueState(previousQueue)
  }
}

export async function toggleShuffle() {
  const isShuffled = getIsShuffledState()
  const queue = getQueueState()
  const currentTrack = getCurrentTrackState()

  if (queue.length <= 1) return

  if (!isShuffled) {
    // Store original queue before shuffling
    setOriginalQueueState([...queue])

    // Find current track position
    const currentIndex = currentTrack
      ? queue.findIndex((t) => t.id === currentTrack.id)
      : 0

    // Split queue: keep played + current tracks, shuffle only upcoming
    const playedAndCurrent = queue.slice(0, currentIndex + 1)
    const upcoming = queue.slice(currentIndex + 1)

    // Fisher-Yates shuffle for upcoming tracks only
    for (let i = upcoming.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1))
      ;[upcoming[i], upcoming[j]] = [upcoming[j], upcoming[i]]
    }

    const shuffledQueue = [...playedAndCurrent, ...upcoming]
    setQueueState(shuffledQueue)
    setIsShuffledState(true)

    // Update TrackPlayer queue without interrupting playback
    // Remove all tracks after current and add shuffled upcoming tracks
    const tpQueue = await TrackPlayer.getQueue()
    const currentTpIndex = await TrackPlayer.getCurrentTrack()

    if (currentTpIndex !== null && currentTpIndex !== undefined) {
      // Remove upcoming tracks from TrackPlayer
      for (let i = tpQueue.length - 1; i > currentTpIndex; i--) {
        await TrackPlayer.remove(i)
      }

      // Add shuffled upcoming tracks
      for (const track of upcoming) {
        await TrackPlayer.add({
          id: track.id,
          url: track.uri,
          title: track.title,
          artist: track.artist,
          album: track.album,
          artwork: track.image,
          duration: track.duration,
        })
      }
      await persistPlaybackSession({ force: true })
    }
  } else {
    // Restore original queue order
    const originalQueue = getOriginalQueueState()
    const currentIndex = currentTrack
      ? originalQueue.findIndex((t) => t.id === currentTrack.id)
      : 0

    // Keep played + current, restore original order for upcoming
    const playedAndCurrent = originalQueue.slice(0, currentIndex + 1)
    const originalUpcoming = originalQueue.slice(currentIndex + 1)

    const restoredQueue = [...playedAndCurrent, ...originalUpcoming]
    setQueueState(restoredQueue)
    setIsShuffledState(false)

    // Update TrackPlayer queue without interrupting playback
    const tpQueue = await TrackPlayer.getQueue()
    const currentTpIndex = await TrackPlayer.getCurrentTrack()

    if (currentTpIndex !== null && currentTpIndex !== undefined) {
      // Remove all tracks after current
      for (let i = tpQueue.length - 1; i > currentTpIndex; i--) {
        await TrackPlayer.remove(i)
      }

      // Add original upcoming tracks in order
      for (const track of originalUpcoming) {
        await TrackPlayer.add({
          id: track.id,
          url: track.uri,
          title: track.title,
          artist: track.artist,
          album: track.album,
          artwork: track.image,
          duration: track.duration,
        })
      }
      await persistPlaybackSession({ force: true })
    }
  }
}
