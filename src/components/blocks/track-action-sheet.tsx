import { Image } from "expo-image"
import { useRouter } from "expo-router"
import { BottomSheet, Button, Card, Chip, Toast, useToast } from "heroui-native"
import * as React from "react"
import { useEffect, useState } from "react"
import { Linking, Text, View } from "react-native"
import { open as openFileViewer } from "react-native-file-viewer-turbo"

import { DeleteTrackDialog } from "@/components/blocks/delete-track-dialog"
import { PlaylistPickerSheet } from "@/components/blocks/playlist-picker-sheet"
import LocalAddIcon from "@/components/icons/local/add"
import LocalFavouriteIcon from "@/components/icons/local/favourite"
import LocalFavouriteSolidIcon from "@/components/icons/local/favourite-solid"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import LocalNextSolidIcon from "@/components/icons/local/next-solid"
import LocalPlaySolidIcon from "@/components/icons/local/play-solid"
import LocalPlaylistSolidIcon from "@/components/icons/local/playlist-solid"
import { MarqueeText } from "@/components/ui"
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
import { useTrack } from "@/modules/tracks/tracks.queries"
import {
  formatQualityLabel,
  normalizeCodecLabel,
  resolveAudioFormat,
} from "@/modules/tracks/track-metadata.utils"
import { resolvePlayableFileUri } from "@/utils/file-path"
import { formatDuration } from "@/utils/format"
import LocalDeleteSolidIcon from "../icons/local/delete-solid"

interface MetadataValueSegment {
  value: string
  onPress?: () => void
}

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
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
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
  const { data: fullTrackData } = useTrack(track?.id ?? "")

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

  const handleOpenDeleteDialog = () => {
    if (!track) {
      return
    }

    setIsPlaylistPickerOpen(false)
    setIsDeleteDialogOpen(true)
    onClose()
  }

  const showActionToast = (title: string, description?: string) => {
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

  const showPlaylistToast = (title: string, description?: string) => {
    showActionToast(title, description)
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

  const handleOpenArtist = (artistName: string) => {
    const normalizedArtistName = artistName.trim()
    if (!normalizedArtistName) {
      return
    }

    onClose()
    router.push({
      pathname: "/artist/[name]",
      params: { name: normalizedArtistName },
    })
  }

  const handleOpenAlbum = (albumName: string) => {
    const normalizedAlbumName = albumName.trim()
    if (!normalizedAlbumName) {
      return
    }

    onClose()
    router.push({
      pathname: "/album/[name]",
      params: { name: normalizedAlbumName },
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
  const splitCommaValues = (value: string | undefined) =>
    (value || "")
      .split(",")
      .map((item) => item.trim())
      .filter((item) => item.length > 0)
  const dedupeValues = (values: string[]) => {
    const seen = new Set<string>()
    return values.filter((value) => {
      const key = value.toLowerCase()
      if (seen.has(key)) {
        return false
      }

      seen.add(key)
      return true
    })
  }
  const artistNames = (() => {
    const relationNames = [
      fullTrackData?.artist?.name?.trim(),
      ...(fullTrackData?.featuredArtists?.map((entry) =>
        entry.artist?.name?.trim()
      ) ?? []),
    ].filter((value): value is string => Boolean(value))

    if (relationNames.length > 0) {
      return dedupeValues(relationNames)
    }

    const fallbackNames = splitCommaValues(track.artist)
    return fallbackNames.length > 0 ? dedupeValues(fallbackNames) : []
  })()
  const albumNames = (() => {
    const relationAlbumName = fullTrackData?.album?.title?.trim()
    if (relationAlbumName) {
      return [relationAlbumName]
    }

    const fallbackAlbumName = track.album?.trim()
    return fallbackAlbumName ? [fallbackAlbumName] : []
  })()
  const genreNames = (() => {
    const names =
      fullTrackData?.genres
        ?.map((entry) => entry.genre?.name?.trim())
        .filter((value): value is string => Boolean(value))
        .filter((value, index, all) => all.indexOf(value) === index) ?? []

    if (names.length > 0) {
      return names.slice(0, 2)
    }

    const fallbackGenreNames = splitCommaValues(track.genre)
    if (fallbackGenreNames.length > 0) {
      return dedupeValues(fallbackGenreNames).slice(0, 2)
    }

    return []
  })()
  const quickFacts = [
    { label: "Quality", value: qualityLabel },
    { label: "Codec", value: codecLabel || "Unknown" },
    { label: "Format", value: formatLabel },
  ]

  const metadataItems: Array<{
    label: string
    segments: MetadataValueSegment[]
    fullWidth?: boolean
  }> = [
    {
      label: "Artist",
      segments:
        artistNames.length > 0
          ? artistNames.map((name) => ({
              value: name,
              onPress: () => handleOpenArtist(name),
            }))
          : [{ value: "Unknown Artist" }],
      fullWidth:
        (artistNames.length > 0 ? artistNames.join(", ") : "Unknown Artist")
          .length > 24,
    },
    {
      label: "Album",
      segments:
        albumNames.length > 0
          ? albumNames.map((name) => ({
              value: name,
              onPress: () => handleOpenAlbum(name),
            }))
          : [{ value: "Unknown Album" }],
      fullWidth:
        (albumNames.length > 0 ? albumNames.join(", ") : "Unknown Album")
          .length > 24,
    },
    {
      label: "Genre",
      segments:
        genreNames.length > 0
          ? genreNames.map((genreName) => ({
              value: genreName,
              onPress: () => {
                onClose()
                router.push({
                  pathname: "/(main)/(search)/genre/[name]",
                  params: { name: genreName },
                })
              },
            }))
          : [{ value: "Unknown" }],
      fullWidth:
        (genreNames.length > 0 ? genreNames.join(", ") : "Unknown").length > 24,
    },
    {
      label: "Year",
      segments: [{ value: track.year ? String(track.year) : "Unknown" }],
    },
    {
      label: "Track / Disc",
      segments: [
        {
          value:
            track.trackNumber || track.discNumber
              ? `${track.trackNumber ?? "?"} / ${track.discNumber ?? "?"}`
              : "Unknown",
        },
      ],
    },
    { label: "Duration", segments: [{ value: durationLabel }] },
    {
      label: "Play Count",
      segments: [{ value: String(track.playCount || 0) }],
    },
    {
      label: "Last Played",
      segments: [{ value: lastPlayed }],
      fullWidth: true,
    },
    {
      label: "File",
      segments: [
        {
          value: filePath,
          onPress: track.uri
            ? () => {
                void handleOpenFile()
              }
            : undefined,
        },
      ],
      fullWidth: true,
    },
  ]
  const metadataLayoutItems = metadataItems.map((item) => ({
    ...item,
    displayValue: item.segments.map((segment) => segment.value).join(", "),
    isFullWidth: Boolean(item.fullWidth),
  }))

  let pendingHalfWidthIndex: number | null = null
  for (let i = 0; i < metadataLayoutItems.length; i += 1) {
    const currentItem = metadataLayoutItems[i]
    if (!currentItem) {
      continue
    }

    if (currentItem.isFullWidth) {
      pendingHalfWidthIndex = null
      continue
    }

    if (pendingHalfWidthIndex !== null) {
      pendingHalfWidthIndex = null
      continue
    }

    const nextItem = metadataLayoutItems[i + 1]
    const nextCanPairInSameRow = Boolean(nextItem && !nextItem.isFullWidth)

    if (!nextCanPairInSameRow) {
      currentItem.isFullWidth = true
      pendingHalfWidthIndex = null
      continue
    }

    pendingHalfWidthIndex = i
  }

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
              <View className="h-18 w-18 overflow-hidden rounded-xl bg-default">
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

            <Button
              variant="danger"
              onPress={handleOpenDeleteDialog}
              className="mb-2 h-11 w-full"
            >
              <View className="flex-row items-center gap-2">
                <LocalDeleteSolidIcon
                  fill="none"
                  width={20}
                  height={20}
                  color="white"
                />
                <Text className="font-semibold text-white">
                  Delete from Device
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
                {metadataLayoutItems.map((item) => {
                  const containerClassName = item.isFullWidth
                    ? "w-full"
                    : "w-[48.5%]"
                  const hasNavigableValues = item.segments.some((segment) =>
                    Boolean(segment.onPress)
                  )
                  const navigableTextStyle = hasNavigableValues
                    ? {
                        textDecorationLine: "underline" as const,
                        textDecorationStyle: "dotted" as const,
                      }
                    : undefined

                  const content = (
                    <Card className="rounded-lg border border-border/40 bg-background/40 px-3 py-2">
                      <Text className="mb-1 text-xs font-medium text-muted uppercase">
                        {item.label}
                      </Text>
                      {hasNavigableValues ? (
                        <Text
                          className="text-sm leading-5 text-foreground"
                          numberOfLines={1}
                        >
                          {item.segments.map((segment, segmentIndex) => (
                            <React.Fragment
                              key={`${item.label}-${segment.value}-${segmentIndex}`}
                            >
                              {segment.onPress ? (
                                <Text
                                  className="text-sm leading-5 text-foreground"
                                  suppressHighlighting
                                  style={navigableTextStyle}
                                  onPress={segment.onPress}
                                >
                                  {segment.value}
                                </Text>
                              ) : (
                                <Text className="text-sm leading-5 text-foreground">
                                  {segment.value}
                                </Text>
                              )}
                              {segmentIndex < item.segments.length - 1 ? (
                                <Text className="text-sm leading-5 text-foreground">
                                  {", "}
                                </Text>
                              ) : null}
                            </React.Fragment>
                          ))}
                        </Text>
                      ) : (
                        <MarqueeText
                          text={item.displayValue}
                          className="text-sm leading-5 text-foreground"
                        />
                      )}
                    </Card>
                  )

                  return (
                    <View key={item.label} className={containerClassName}>
                      {content}
                    </View>
                  )
                })}
              </View>
            </View>
          </BottomSheet.Content>
        </BottomSheet.Portal>
      </BottomSheet>

      <DeleteTrackDialog
        track={track}
        isOpen={isDeleteDialogOpen}
        onOpenChange={setIsDeleteDialogOpen}
        onDeleted={() => {
          setIsPlaylistPickerOpen(false)
          onClose()
        }}
      />

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
