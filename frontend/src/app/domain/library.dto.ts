import {PageDto} from './common.dto';
import UserPlaylist = RestoredPlaylistsDto.UserPlaylist;

export interface AlbumDto {
  id: string;
  creationDate: string;
  updateDate: string | undefined;
  name: string | undefined;
  year: number | undefined;
  artworkId: string | undefined;
  artistId: string;
}

export interface AlbumSongsDto {
  album: AlbumDto;
  songs: SongDto[];
}

export interface ArtistDto {
  id: string;
  creationDate: string;
  updateDate: string | undefined;
  name: string | undefined;
  artworkId: string | undefined;
  genres: GenreDto[],
}

export interface ArtistSongsDto {
  artist: ArtistDto;
  albumSongs: AlbumSongsDto[];
}

export interface SongDto {
  id: string;
  creationDate: string;
  updateDate: string | undefined;
  path: string;
  mimeType: string;
  fileExtension: string;
  size: number;
  duration: number;
  bitRate: number;
  bitRateVariable: boolean;
  discNumber: number | undefined;
  trackNumber: number | undefined;
  name: string | undefined;
  artistName: string | undefined;
  genreName: string | undefined;
  albumId: string;
  genreId: string;
}

export interface ScanStatisticsDto {
  date: string;
  duration: number;
  songSize: number;
  artworkSize: number;
  genreCount: number;
  artistCount: number;
  albumCount: number;
  songCount: number;
  artworkCount: number;
}

export enum ScanType {
  FULL = 'FULL',
  EDIT = 'EDIT',
}

export interface LogMessageDto {
  id: string;
  date: string;
  level: LogMessageDto.Level;
  pattern: string;
  arguments: string[];
  text: string;
}

export namespace LogMessageDto {

  export enum Level {
    INFO = 'INFO',
    WARN = 'WARN',
    ERROR = 'ERROR',
  }

  export function extractException(logMessage: LogMessageDto | undefined): string | undefined {
    if (!logMessage) {
      return undefined;
    }
    const regExp = /{}/g;
    const argCount = ((logMessage.pattern || '').match(regExp) || []).length;
    if (argCount < logMessage.arguments.length) {
      return logMessage.arguments[logMessage.arguments.length - 1];
    }
    return undefined;
  }
}

export interface ScanResultDto {
  id: string;
  date: string;
  scanType: ScanType;
  failedPaths: string[];
  processedAudioFileCount: number;
  duration: number;
  songSize: number;
  artworkSize: number;
  genreCount: number;
  artistCount: number;
  albumCount: number;
  songCount: number;
  artworkCount: number;
  createdArtistCount: number;
  updatedArtistCount: number;
  deletedArtistCount: number;
  createdAlbumCount: number;
  updatedAlbumCount: number;
  deletedAlbumCount: number;
  createdGenreCount: number;
  updatedGenreCount: number;
  deletedGenreCount: number;
  createdSongCount: number;
  updatedSongCount: number;
  deletedSongCount: number;
  createdArtworkCount: number;
  deletedArtworkCount: number;
}

export interface ScanJobDto {
  id: string;
  creationDate: string;
  updateDate: string | undefined;
  scanType: ScanType;
  status: ScanJobDto.Status;
  targetPaths: string[];
  logMessage: LogMessageDto | undefined;
  scanResult: ScanResultDto | undefined;
}

export namespace ScanJobDto {
  export enum Status {
    STARTING = 'STARTING',
    STARTED = 'STARTED',
    COMPLETE = 'COMPLETE',
    FAILED = 'FAILED',
    INTERRUPTED = 'INTERRUPTED',
  }
}

export interface ScanProgressDto {
  stepDescriptor: ScanProgressDto.StepDescriptor;
  files: string[];
  value: ScanProgressDto.Value | undefined;
}

export namespace ScanProgressDto {

  export enum Step {

    FULL_PREPARING = 'FULL_PREPARING',
    FULL_SEARCHING_MEDIA = 'FULL_SEARCHING_MEDIA',
    FULL_CLEANING_SONGS = 'FULL_CLEANING_SONGS', // Has value.
    FULL_CLEANING_ARTWORKS = 'FULL_CLEANING_ARTWORKS', // Has value.
    FULL_IMPORTING = 'FULL_IMPORTING', // Has value.
    FULL_SEARCHING_ARTWORKS = 'FULL_SEARCHING_ARTWORKS', // Has value.
    FULL_CLEANING_ARTIST_GENRES = 'FULL_CLEANING_ARTIST_GENRES', // Has value.

    EDIT_PREPARING = 'EDIT_PREPARING',
    EDIT_WRITING = 'EDIT_WRITING', // Has value.
    EDIT_SEARCHING_ARTWORKS = 'EDIT_SEARCHING_ARTWORKS', // Has value.
    EDIT_CLEANING_ARTIST_GENRES = 'EDIT_CLEANING_ARTIST_GENRES', // Has value.
  }

  export interface Value {
    itemsComplete: number
    itemsTotal: number;
  }

  export interface StepDescriptor {
    step: ScanProgressDto.Step;
    scanType: ScanType;
    stepNumber: number;
    totalSteps: number;
  }
}

export interface ScanJobProgressDto {
  scanJob: ScanJobDto;
  scanProgress: ScanProgressDto | undefined;
}

export interface ScanJobPageDto extends PageDto {
  scanJobs: ScanJobDto[];
}

export interface LogMessagePageDto extends PageDto {
  logMessages: LogMessageDto[];
}

export interface GenreDto {
  id: string;
  creationDate: string;
  updateDate: string | undefined;
  name: string | undefined;
  artworkId: string | undefined;
}

export interface AlbumDetailsDto {
  album: AlbumDto;
  artist: ArtistDto;
}

export interface SongDetailsDto {
  song: SongDto;
  albumDetails: AlbumDetailsDto;
  genre: GenreDto;
}

export interface SearchResultDto {
  genres: GenreDto[];
  artists: ArtistDto[];
  albumDetails: AlbumDetailsDto[];
  songDetails: SongDetailsDto[];
}

export interface PlaylistDto {
  id: string;
  creationDate: string;
  updateDate: string | undefined;
  name: string | undefined;
  type: PlaylistDto.Type;
}

export namespace PlaylistDto {

  export enum Type {
    NORMAL = 'NORMAL',
    LIKE = 'LIKE',
  }
}

export interface PlaylistSongDto {
  id: string;
  creationDate: string;
  song: SongDetailsDto;
}

export interface PlaylistSongsDto {
  playlist: PlaylistDto;
  songs: PlaylistSongDto[];
}

export interface PlaylistCreateCommandDto {
  name: string;
  songIds: string[];
}

export interface PlaylistUpdateCommandDto {
  id: string;
  overrideName: string;
  overriddenSongIds: PlaylistUpdateCommandDto.SongId[] | undefined;
}

export namespace PlaylistUpdateCommandDto {

  export interface SongId {
    id: string | undefined;
    songId: string;
  }
}

export interface PlaybackHistorySongDto {
  id: string;
  creationDate: string;
  song: SongDetailsDto;
}

export interface PlaybackHistoryDto {
  songs: PlaybackHistorySongDto[];
  statistics: PlaybackHistoryDto.Statistics;
}

export namespace PlaybackHistoryDto {
  export interface Statistics {
    totalCount: number;
  }
}

export interface RestoredPlaylistsDto {
  userPlaylists: UserPlaylist[];
  notFoundUserEmails: string[];
  notFoundSongPaths: string[];
}

export namespace RestoredPlaylistsDto {
  export interface UserPlaylist {
    userEmail: string;
    playlist: PlaylistDto;
  }
}

export interface RandomSongsRequestDto {
  count: number;
  lastArtistId?: string;
  genreIds: string[];
}

export interface BackupDto {
    fileContent: string;
}
