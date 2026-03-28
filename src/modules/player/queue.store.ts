import { useMemo } from "react"

import { TrackPlayer } from "@/modules/player/player.utils"

import {
  $currentTrack,
  $isShuffled,
  $originalQueue,
  $queue,
  usePlayerStore,
  type Track,
} from "./player.store"
import {
  persistPlaybackSession,
  syncCurrentTrackFromPlayer,
} from "./player.service"

export { $isShuffled, $originalQueue, $queue } from "./player.store"

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

export const $queueInfo = {
  get: () =>
    buildQueueInfo($queue.get(), $currentTrack.get()),
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
  const queue = $queue.get()
  if (queue.some((t) => t.id === track.id)) return

  $queue.set([...queue, track])

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
  const queue = $queue.get()
  const currentTrack = $currentTrack.get()

  const filteredQueue = queue.filter((t) => t.id !== track.id)

  if (!currentTrack) {
    $queue.set([track, ...filteredQueue])
  } else {
    const currentIndex = filteredQueue.findIndex(
      (t) => t.id === currentTrack.id
    )
    const newQueue = [
      ...filteredQueue.slice(0, currentIndex + 1),
      track,
      ...filteredQueue.slice(currentIndex + 1),
    ]
    $queue.set(newQueue)
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
  const queue = $queue.get()
  $queue.set(queue.filter((t) => t.id !== trackId))

  const tpQueue = await TrackPlayer.getQueue()
  const index = tpQueue.findIndex((t) => t.id === trackId)
  if (index !== -1) {
    await TrackPlayer.remove(index)
  }
  await persistPlaybackSession({ force: true })
}

export async function clearQueue() {
  const currentTrack = $currentTrack.get()

  if (currentTrack) {
    $queue.set([currentTrack])
  } else {
    $queue.set([])
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
  $queue.set(tracks)
}

export async function moveInQueue(fromIndex: number, toIndex: number) {
  const previousQueue = $queue.get()
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
  $queue.set(queue)

  try {
    await TrackPlayer.move(fromIndex, toIndex)
    await syncCurrentTrackFromPlayer()
    await persistPlaybackSession({ force: true })
  } catch {
    $queue.set(previousQueue)
  }
}

export async function toggleShuffle() {
  const isShuffled = $isShuffled.get()
  const queue = $queue.get()
  const currentTrack = $currentTrack.get()

  if (queue.length <= 1) return

  if (!isShuffled) {
    // Store original queue before shuffling
    $originalQueue.set([...queue])

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
    $queue.set(shuffledQueue)
    $isShuffled.set(true)

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
    const originalQueue = $originalQueue.get()
    const currentIndex = currentTrack
      ? originalQueue.findIndex((t) => t.id === currentTrack.id)
      : 0

    // Keep played + current, restore original order for upcoming
    const playedAndCurrent = originalQueue.slice(0, currentIndex + 1)
    const originalUpcoming = originalQueue.slice(currentIndex + 1)

    const restoredQueue = [...playedAndCurrent, ...originalUpcoming]
    $queue.set(restoredQueue)
    $isShuffled.set(false)

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
