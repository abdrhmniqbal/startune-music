import type {
  NativeScrollEvent,
  NativeSyntheticEvent,
  RefreshControlProps,
  StyleProp,
  ViewStyle,
} from "react-native"

import type { Track } from "@/modules/player/player.store"
import {
  type Folder,
  type FolderBreadcrumb,
  FolderList,
} from "@/components/blocks/folder-list"

interface FolderTabProps {
  folders: Folder[]
  folderTracks: Track[]
  folderBreadcrumbs: FolderBreadcrumb[]
  onOpenFolder: (path: string) => void
  onBackFolder: () => void
  onNavigateToFolderPath: (path: string) => void
  onTrackPress: (track: Track) => void
  contentContainerStyle?: StyleProp<ViewStyle>
  resetScrollKey?: string
  refreshControl?: React.ReactElement<RefreshControlProps> | null
  onScroll?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollBeginDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollEndDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onMomentumScrollEnd?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
}

export function FolderTab({
  folders,
  folderTracks,
  folderBreadcrumbs,
  onOpenFolder,
  onBackFolder,
  onNavigateToFolderPath,
  onTrackPress,
  contentContainerStyle,
  resetScrollKey,
  refreshControl,
  onScroll,
  onScrollBeginDrag,
  onScrollEndDrag,
  onMomentumScrollEnd,
}: FolderTabProps) {
  return (
    <FolderList
      data={folders}
      tracks={folderTracks}
      breadcrumbs={folderBreadcrumbs}
      onFolderPress={(folder) => {
        if (folder.path) {
          onOpenFolder(folder.path)
        }
      }}
      onBackPress={onBackFolder}
      onBreadcrumbPress={onNavigateToFolderPath}
      onTrackPress={onTrackPress}
      contentContainerStyle={contentContainerStyle}
      resetScrollKey={resetScrollKey}
      refreshControl={refreshControl}
      onScroll={onScroll}
      onScrollBeginDrag={onScrollBeginDrag}
      onScrollEndDrag={onScrollEndDrag}
      onMomentumScrollEnd={onMomentumScrollEnd}
    />
  )
}
