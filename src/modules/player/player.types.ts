export interface LyricLine {
  time: number
  text: string
}

export interface Track {
  id: string
  title: string
  artist?: string
  artistId?: string
  albumArtist?: string
  album?: string
  albumId?: string
  duration: number
  uri: string
  image?: string
  audioBitrate?: number
  audioSampleRate?: number
  audioCodec?: string
  audioFormat?: string
  lyrics?: string
  fileHash?: string
  scanTime?: number
  isDeleted?: boolean
  playCount?: number
  lastPlayedAt?: number
  year?: number
  filename?: string
  dateAdded?: number
  isFavorite?: boolean
  discNumber?: number
  trackNumber?: number
  genre?: string
}

export interface Album {
  id: string
  title: string
  artist: string
  albumArtist?: string
  image?: string
  trackCount: number
  year: number
  dateAdded: number
}

export interface Artist {
  id: string
  name: string
  trackCount: number
  image?: string
  dateAdded: number
}
