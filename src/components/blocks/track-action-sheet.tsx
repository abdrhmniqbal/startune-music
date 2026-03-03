import * as React from "react"
import { useEffect, useState } from "react"
import { Image } from "expo-image"
import { useRouter } from "expo-router"
import {
  BottomSheet,
  Button,
  Card,
  Chip,
  PressableFeedback,
  Toast,
  useToast,
} from "heroui-native"
import { Linking, Text, View } from "react-native"
import { open as openFileViewer } from "react-native-file-viewer-turbo"
import { cn } from "tailwind-variants"

import { ICON_SIZES } from "@/constants/icon-sizes"
import { useThemeColors } from "@/hooks/use-theme-colors"
import {
  useIsFavorite,
  useToggleFavorite,
} from "@/modules/favorites/favorites.queries"
import { playTrack, type Track } from "@/modules/player/player.store"
import { addToQueue, playNext } from "@/modules/player/queue.store"
import {
  useAddTrackToPlaylist,
  useRemoveTrackFromPlaylist,
} from "@/modules/playlist/playlist.queries"
import {
  formatQualityLabel,
  normalizeCodecLabel,
  resolveAudioFormat,
} from "@/modules/tracks/track-metadata.utils"
import { resolvePlayableFileUri } from "@/utils/file-path"
import { formatDuration } from "@/utils/format"
import LocalAddIcon from "@/components/icons/local/add"
import LocalFavouriteIcon from "@/components/icons/local/favourite"
import LocalFavouriteSolidIcon from "@/components/icons/local/favourite-solid"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import LocalNextSolidIcon from "@/components/icons/local/next-solid"
import LocalPlaySolidIcon from "@/components/icons/local/play-solid"
import LocalPlaylistSolidIcon from "@/components/icons/local/playlist-solid"
import { PlaylistPickerSheet } from "@/components/blocks/playlist-picker-sheet"
import { MarqueeText } from "@/components/ui"

interface TrackActionSheetProps {
  track: Track | null
  isOpen: boolean
  onClose: () => void
  tracks?: Track[]
  onAddToPlaylist?: (track: Track) => void
}

export const TrackActionSheet: React.FC<TrackActionSheetProps> = ({
  track,
  isOpen,
  onClose,
  tracks,
  onAddToPlaylist,
}) => {
  const router = useRouter()
  const { toast } = useToast()
  const theme = useThemeColors()
  const toggleFavoriteMutation = useToggleFavorite()
  const addTrackToPlaylistMutation = useAddTrackToPlaylist()
  const removeTrackFromPlaylistMutation = useRemoveTrackFromPlaylist()
  const [isPlaylistPickerOpen, setIsPlaylistPickerOpen] = useState(false)
  const [favoriteOverrides, setFavoriteOverrides] = useState<
    Record<string, boolean>
  >({})
  const favoriteTrackId = track?.id || ""
  const { data: isFavoriteData = track?.isFavorite ?? false } = useIsFavorite(
    "track",
    favoriteTrackId
  )
  const isFavorite = track
    ? (favoriteOverrides[track.id] ?? Boolean(isFavoriteData))
    : false
  const [resolvedFileUri, setResolvedFileUri] = useState<string | null>(null)

  const handlePlay = async () => {
    if (track) {
      playTrack(track, tracks)
      onClose()
    }
  }

  const handleToggleFavorite = () => {
    if (track) {
      const newState = !isFavorite
      setFavoriteOverrides((prev) => ({ ...prev, [track.id]: newState }))
      void toggleFavoriteMutation.mutateAsync({
        type: "track",
        itemId: track.id,
        isCurrentlyFavorite: isFavorite,
        name: track.title,
        subtitle: track.artist,
        image: track.image,
      })
    }
  }

  const handlePlayNext = async () => {
    if (track) {
      await playNext(track)
      onClose()
    }
  }

  const handleAddToQueue = async () => {
    if (track) {
      await addToQueue(track)
      onClose()
    }
  }

  const handleAddToPlaylist = () => {
    if (!track) {
      return
    }

    if (onAddToPlaylist) {
      onAddToPlaylist(track)
      onClose()
      return
    }

    setIsPlaylistPickerOpen(true)
  }

  const showPlaylistToast = (title: string, description?: string) => {
    toast.show({
      duration: 1800,
      component: (props) => (
        <Toast {...props} variant="accent" placement="bottom">
          <Toast.Title className="text-sm font-semibold">{title}</Toast.Title>
          {description ? (
            <Toast.Description className="text-xs text-muted">
              {description}
            </Toast.Description>
          ) : null}
        </Toast>
      ),
    })
  }

  const handleSelectPlaylist = async ({
    id,
    name,
    hasTrack,
  }: {
    id: string
    name: string
    hasTrack: boolean
  }) => {
    if (
      !track ||
      addTrackToPlaylistMutation.isPending ||
      removeTrackFromPlaylistMutation.isPending
    ) {
      return
    }

    if (hasTrack) {
      try {
        await removeTrackFromPlaylistMutation.mutateAsync({
          playlistId: id,
          trackId: track.id,
        })
        setIsPlaylistPickerOpen(false)
        onClose()
        showPlaylistToast("Removed from playlist", name)
      } catch {
        showPlaylistToast("Failed to remove track")
      }
      return
    }

    try {
      const result = await addTrackToPlaylistMutation.mutateAsync({
        playlistId: id,
        trackId: track.id,
      })

      setIsPlaylistPickerOpen(false)
      onClose()

      if (result.skipped) {
        showPlaylistToast("Already in playlist", name)
        return
      }

      showPlaylistToast("Added to playlist", name)
    } catch {
      showPlaylistToast("Failed to add track")
    }
  }

  const handleCreatePlaylist = () => {
    setIsPlaylistPickerOpen(false)
    onClose()
    router.push("/playlist/form")
  }

  const handleOpenArtist = () => {
    if (!track?.artist?.trim()) {
      return
    }

    onClose()
    router.push({
      pathname: "/artist/[name]",
      params: { name: track.artist.trim() },
    })
  }

  const handleOpenAlbum = () => {
    if (!track?.album?.trim()) {
      return
    }

    onClose()
    router.push({
      pathname: "/album/[name]",
      params: { name: track.album.trim() },
    })
  }

  const handleOpenFile = async () => {
    if (!track?.uri) {
      return
    }

    const resolvedUri = await resolvePlayableFileUri(track.uri)

    try {
      await openFileViewer(resolvedUri, {
        showOpenWithDialog: true,
        showAppsSuggestions: false,
      })
      onClose()
    } catch {
      try {
        const openableUri = encodeURI(resolvedUri)
        const canOpenFile = await Linking.canOpenURL(openableUri)
        if (canOpenFile) {
          onClose()
          await Linking.openURL(openableUri)
        }
      } catch {
        // Ignore: opening local files depends on OEM apps and URI permissions.
      }
    }
  }

  useEffect(() => {
    let cancelled = false

    const resolvePath = async () => {
      if (!track?.uri) {
        setResolvedFileUri(null)
        return
      }

      const resolvedUri = await resolvePlayableFileUri(track.uri)
      if (!cancelled) {
        setResolvedFileUri(resolvedUri)
      }
    }

    void resolvePath()

    return () => {
      cancelled = true
    }
  }, [track?.id, track?.uri])

  if (!track) {
    return (
      <BottomSheet isOpen={false} onOpenChange={() => {}}>
        <BottomSheet.Portal>
          <BottomSheet.Overlay />
          <BottomSheet.Content />
        </BottomSheet.Portal>
      </BottomSheet>
    )
  }

  const fallbackArtist = track.artist || "Unknown Artist"
  const fallbackAlbum = track.album || "Unknown Album"

  const fileName = (() => {
    if (track.filename) {
      return track.filename
    }

    const uriPart = track.uri.split("/").pop() || ""
    if (!uriPart) {
      return "Unknown file"
    }

    try {
      return decodeURIComponent(uriPart)
    } catch {
      return uriPart
    }
  })()
  const filePath = (() => {
    if (!track.uri) {
      return "Unknown file"
    }

    const uri = resolvedFileUri || track.uri
    const normalizedPath = uri.startsWith("file://")
      ? uri.slice("file://".length)
      : uri

    try {
      return decodeURIComponent(normalizedPath)
    } catch {
      return normalizedPath
    }
  })()
  const lastPlayed = (() => {
    if (!track.lastPlayedAt || !Number.isFinite(track.lastPlayedAt)) {
      return "Never"
    }

    const date = new Date(track.lastPlayedAt)
    if (Number.isNaN(date.getTime())) {
      return "Never"
    }

    return date.toLocaleString()
  })()
  const codecLabel = normalizeCodecLabel(track.audioCodec)
  const formatLabel = resolveAudioFormat(
    track.audioFormat,
    fileName,
    codecLabel
  )
  const qualityLabel = formatQualityLabel(
    track.audioSampleRate,
    track.audioBitrate
  )
  const durationLabel = formatDuration(track.duration || 0)
  const quickFacts = [
    { label: "Quality", value: qualityLabel },
    { label: "Codec", value: codecLabel || "Unknown" },
    { label: "Format", value: formatLabel },
  ]

  const metadataItems: Array<{
    label: string
    value: string
    fullWidth?: boolean
    onPress?: () => void
  }> = [
    {
      label: "Artist",
      value: fallbackArtist,
      fullWidth: fallbackArtist.length > 24,
      onPress: track.artist?.trim() ? handleOpenArtist : undefined,
    },
    {
      label: "Album",
      value: fallbackAlbum,
      fullWidth: fallbackAlbum.length > 24,
      onPress: track.album?.trim() ? handleOpenAlbum : undefined,
    },
    {
      label: "Genre",
      value: track.genre || "Unknown",
      fullWidth: (track.genre || "Unknown").length > 24,
    },
    { label: "Year", value: track.year ? String(track.year) : "Unknown" },
    {
      label: "Track / Disc",
      value:
        track.trackNumber || track.discNumber
          ? `${track.trackNumber ?? "?"} / ${track.discNumber ?? "?"}`
          : "Unknown",
    },
    { label: "Duration", value: durationLabel },
    { label: "Play Count", value: String(track.playCount || 0) },
    {
      label: "Last Played",
      value: lastPlayed,
      fullWidth: true,
    },
    {
      label: "File",
      value: filePath,
      fullWidth: true,
      onPress: track.uri
        ? () => {
            void handleOpenFile()
          }
        : undefined,
    },
  ]

  return (
    <>
      <BottomSheet
        isOpen={isOpen}
        onOpenChange={(open) => {
          if (open) {
            return
          }

          setIsPlaylistPickerOpen(false)
          onClose()
        }}
      >
        <BottomSheet.Portal>
          <BottomSheet.Overlay />
          <BottomSheet.Content
            snapPoints={["62%", "92%"]}
            enableDynamicSizing={false}
            contentContainerClassName="px-5 pt-2 pb-5"
            backgroundClassName="bg-surface"
          >
            <View className="mb-5 flex-row items-center gap-4">
              <View className="h-[72px] w-[72px] overflow-hidden rounded-xl bg-default">
                {track.image ? (
                  <Image
                    source={{ uri: track.image }}
                    style={{ width: "100%", height: "100%" }}
                    contentFit="cover"
                  />
                ) : (
                  <View className="h-full w-full items-center justify-center bg-default">
                    <LocalMusicNoteSolidIcon
                      fill="none"
                      width={ICON_SIZES.sheetArtworkFallback}
                      height={ICON_SIZES.sheetArtworkFallback}
                      color={theme.muted}
                    />
                  </View>
                )}
              </View>
              <View className="flex-1 gap-1">
                <Text className="text-xl leading-7 font-bold text-foreground">
                  {track.title}
                </Text>
                <Text className="text-sm text-muted">{fallbackArtist}</Text>
                <Text className="text-xs text-muted/90" numberOfLines={1}>
                  {fallbackAlbum}
                </Text>
              </View>
            </View>

            <View className="mb-2 flex-row gap-2">
              <Button
                variant="primary"
                onPress={handlePlay}
                className="h-12 flex-1"
              >
                <View className="flex-row items-center gap-2">
                  <LocalPlaySolidIcon
                    fill="none"
                    width={24}
                    height={24}
                    color="white"
                  />
                  <Text className="font-semibold text-white">Play</Text>
                </View>
              </Button>
              <Button
                variant="secondary"
                onPress={handleToggleFavorite}
                className="h-12 px-4"
                isIconOnly
              >
                {isFavorite ? (
                  <LocalFavouriteSolidIcon
                    fill="none"
                    width={28}
                    height={28}
                    color="#ef4444"
                  />
                ) : (
                  <LocalFavouriteIcon
                    fill="none"
                    width={28}
                    height={28}
                    color={theme.foreground}
                  />
                )}
              </Button>
            </View>

            <View className="mb-2 flex-row gap-2">
              <Button
                variant="secondary"
                onPress={handleAddToQueue}
                className="h-11 flex-1"
              >
                <View className="flex-row items-center gap-2">
                  <LocalAddIcon
                    fill="none"
                    width={20}
                    height={20}
                    color={theme.foreground}
                  />
                  <Text className="font-semibold text-foreground">
                    Add to Queue
                  </Text>
                </View>
              </Button>
              <Button
                variant="secondary"
                onPress={handlePlayNext}
                className="h-11 flex-1"
              >
                <View className="flex-row items-center gap-2">
                  <LocalNextSolidIcon
                    fill="none"
                    width={20}
                    height={20}
                    color={theme.foreground}
                  />
                  <Text className="font-semibold text-foreground">
                    Play Next
                  </Text>
                </View>
              </Button>
            </View>

            <Button
              variant="secondary"
              onPress={handleAddToPlaylist}
              className="mb-2 h-11 w-full"
            >
              <View className="flex-row items-center gap-2">
                <LocalPlaylistSolidIcon
                  fill="none"
                  width={20}
                  height={20}
                  color={theme.foreground}
                />
                <Text className="font-semibold text-foreground">
                  Add to Playlist
                </Text>
              </View>
            </Button>

            <View className="mt-2 border-t border-border/60 pt-3">
              <View className="mb-3 flex-row flex-wrap gap-2">
                {quickFacts.map((fact) => (
                  <Chip
                    key={fact.label}
                    size="sm"
                    variant="secondary"
                    color="default"
                  >
                    <Chip.Label className="text-xs">
                      {`${fact.label}: ${fact.value}`}
                    </Chip.Label>
                  </Chip>
                ))}
              </View>

              <View className="flex-row flex-wrap gap-2">
                {metadataItems.map((item) => {
                  const containerClassName = item.fullWidth
                    ? "w-full"
                    : "w-[48.5%]"

                  const content = (
                    <Card className="rounded-lg border border-border/40 bg-background/40 px-3 py-2">
                      <Text className="mb-1 text-xs font-medium text-muted uppercase">
                        {item.label}
                      </Text>
                      <MarqueeText
                        text={item.value}
                        className="text-sm leading-5 text-foreground"
                      />
                    </Card>
                  )

                  if (!item.onPress) {
                    return (
                      <View key={item.label} className={containerClassName}>
                        {content}
                      </View>
                    )
                  }

                  return (
                    <PressableFeedback
                      key={item.label}
                      onPress={item.onPress}
                      className={cn(containerClassName, "active:opacity-80")}
                    >
                      {content}
                    </PressableFeedback>
                  )
                })}
              </View>
            </View>
          </BottomSheet.Content>
        </BottomSheet.Portal>
      </BottomSheet>

      <PlaylistPickerSheet
        isOpen={isPlaylistPickerOpen}
        onOpenChange={setIsPlaylistPickerOpen}
        trackId={track.id}
        isSelecting={
          addTrackToPlaylistMutation.isPending ||
          removeTrackFromPlaylistMutation.isPending
        }
        onCreatePlaylist={handleCreatePlaylist}
        onSelectPlaylist={(playlist) => {
          void handleSelectPlaylist(playlist)
        }}
      />
    </>
  )
}

export default TrackActionSheet
