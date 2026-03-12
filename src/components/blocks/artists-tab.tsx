import type {
  NativeScrollEvent,
  NativeSyntheticEvent,
  RefreshControlProps,
} from "react-native"
import * as React from "react"

import { type Artist, ArtistGrid } from "@/components/blocks/artist-grid"
import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import LocalUserSolidIcon from "@/components/icons/local/user-solid"
import { EmptyState } from "@/components/ui"
import { useThemeColors } from "@/hooks/use-theme-colors"
import {
  sortArtists,
  type SortConfig,
} from "@/modules/library/library-sort.store"
import { useArtists } from "@/modules/library/library.queries"

interface ArtistsTabProps {
  onArtistPress?: (artist: Artist) => void
  sortConfig?: SortConfig
  contentBottomPadding?: number
  refreshControl?: React.ReactElement<RefreshControlProps> | null
  onScroll?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollBeginDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollEndDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onMomentumScrollEnd?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
}

export const ArtistsTab: React.FC<ArtistsTabProps> = ({
  onArtistPress,
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
    field: "name",
    order: "asc",
  }
  const orderByField = effectiveSortConfig.field || "name"
  const order = effectiveSortConfig.order || "asc"

  const {
    data: artistsData,
    isLoading,
    isPending,
  } = useArtists(orderByField as any, order)

  const artists: Artist[] =
    artistsData?.map((artist) => ({
      id: artist.id,
      name: artist.name,
      trackCount: artist.trackCount || 0,
      image:
        artist.artwork ||
        artist.trackArtwork ||
        artist.albumArtwork ||
        undefined,
      dateAdded: 0,
    })) || []
  const sortedArtists = sortArtists(artists, effectiveSortConfig) as Artist[]

  const handleArtistPress = (artist: Artist) => {
    onArtistPress?.(artist)
  }

  if (isLoading || isPending) {
    return <LibrarySkeleton type="artists" />
  }

  if (artists.length === 0) {
    return (
      <EmptyState
        icon={
          <LocalUserSolidIcon
            fill="none"
            width={48}
            height={48}
            color={theme.muted}
          />
        }
        title="No Artists"
        message="Artists from your music library will appear here."
      />
    )
  }

  return (
    <ArtistGrid
      data={sortedArtists}
      onArtistPress={handleArtistPress}
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
