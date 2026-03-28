import type {
  NativeScrollEvent,
  NativeSyntheticEvent,
  RefreshControlProps,
} from "react-native"
import * as React from "react"

import { type Album, AlbumGrid } from "@/components/blocks/album-grid"
import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import LocalVynilSolidIcon from "@/components/icons/local/vynil-solid"
import { EmptyState } from "@/components/ui/empty-state"
import { useThemeColors } from "@/hooks/use-theme-colors"
import {
  sortAlbums,
  type SortConfig,
} from "@/modules/library/library-sort.store"
import { useAlbums } from "@/modules/library/library.queries"

interface AlbumsTabProps {
  onAlbumPress?: (album: Album) => void
  sortConfig?: SortConfig
  contentBottomPadding?: number
  refreshControl?: React.ReactElement<RefreshControlProps> | null
  onScroll?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollBeginDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollEndDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onMomentumScrollEnd?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
}

export const AlbumsTab: React.FC<AlbumsTabProps> = ({
  onAlbumPress,
  sortConfig,
  contentBottomPadding = 0,
  refreshControl,
  onScroll,
  onScrollBeginDrag,
  onScrollEndDrag,
  onMomentumScrollEnd,
}) => {
  const theme = useThemeColors()
  const effectiveSortConfig: SortConfig = sortConfig ?? {
    field: "title",
    order: "asc",
  }
  const orderByField =
    effectiveSortConfig.field === "artist"
      ? "title"
      : effectiveSortConfig.field || "title"
  const order = effectiveSortConfig.order || "asc"

  const {
    data: albumsData,
    isLoading,
    isPending,
  } = useAlbums(orderByField as any, order)

  const albums: Album[] =
    albumsData?.map((album) => ({
      id: album.id,
      title: album.title,
      artist: album.artist?.name || "Unknown Artist",
      albumArtist: album.artist?.name,
      image: album.artwork || undefined,
      trackCount: album.trackCount || 0,
      year: album.year || 0,
      dateAdded: 0,
    })) || []
  const sortedAlbums = sortAlbums(albums, effectiveSortConfig) as Album[]

  const handleAlbumPress = (album: Album) => {
    onAlbumPress?.(album)
  }

  if (isLoading || isPending) {
    return <LibrarySkeleton type="albums" />
  }

  if (albums.length === 0) {
    return (
      <EmptyState
        icon={
          <LocalVynilSolidIcon
            fill="none"
            width={48}
            height={48}
            color={theme.muted}
          />
        }
        title="No Albums"
        message="Albums you add to your library will appear here."
      />
    )
  }

  return (
    <AlbumGrid
      data={sortedAlbums}
      onAlbumPress={handleAlbumPress}
      contentContainerStyle={{ paddingBottom: contentBottomPadding }}
      resetScrollKey={`${effectiveSortConfig.field}-${effectiveSortConfig.order}`}
      refreshControl={refreshControl}
      onScroll={onScroll}
      onScrollBeginDrag={onScrollBeginDrag}
      onScrollEndDrag={onScrollEndDrag}
      onMomentumScrollEnd={onMomentumScrollEnd}
    />
  )
}
