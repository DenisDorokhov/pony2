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
  albumDiscoveryData: AlbumDiscoveryDataDto;
  songs: SongDto[];
}

export interface AlbumDiscoveryDataDto {
  releaseType: AlbumDiscoveryDataDto.ReleaseType;
  releaseDate: string | undefined;
}

export namespace AlbumDiscoveryDataDto {
  export enum ReleaseType {
    LP = 'LP',
    EP = 'EP',
    SINGLE = 'SINGLE',
    LIVE = 'LIVE',
    COMPILATION = 'COMPILATION',
    UNKNOWN = 'UNKNOWN',
  }
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
  artistDiscoveryData: ArtistDiscoveryDataDto;
  albumSongs: AlbumSongsDto[];
}

export interface ArtistDiscoveryDataDto {
  countryCode: string | null;
  topSongIds: string[];
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

export enum DiscoveryType {
  FULL = 'FULL',
  ARTIST = 'ARTIST',
  ALBUM = 'ALBUM',
}

export interface DiscoveryJobDto {
  id: string;
  creationDate: string;
  updateDate: string | undefined;
  discoveryType: DiscoveryType;
  status: DiscoveryJobDto.Status;
  parameter: string | undefined;
  logMessage: LogMessageDto | undefined;
}

export namespace DiscoveryJobDto {
  export enum Status {
    STARTING = 'STARTING',
    STARTED = 'STARTED',
    COMPLETE = 'COMPLETE',
    MODERATE = 'MODERATE',
    FAILED = 'FAILED',
    INTERRUPTED = 'INTERRUPTED',
  }
}

export interface DiscoveryProgressDto {
  stepDescriptor: DiscoveryProgressDto.StepDescriptor;
  value: DiscoveryProgressDto.Value | undefined;
}

export namespace DiscoveryProgressDto {

  export enum Step {
    FULL_DISCOVERY = 'FULL_DISCOVERY',
    ARTIST_DISCOVERY = 'ARTIST_DISCOVERY',
    ALBUM_DISCOVERY = 'ALBUM_DISCOVERY',
  }

  export interface Value {
    itemsComplete: number;
    itemsTotal: number;
  }

  export interface StepDescriptor {
    step: DiscoveryProgressDto.Step;
    discoveryType: DiscoveryType;
    stepNumber: number;
    totalSteps: number;
  }
}

export interface DiscoveryJobProgressDto {
  discoveryJob: DiscoveryJobDto;
  discoveryProgress: DiscoveryProgressDto | undefined;
}

export interface DiscoveryJobPageDto extends PageDto {
  discoveryJobs: DiscoveryJobDto[];
}

export interface DiscoveryJobDetailsDto {
  discoveryJob: DiscoveryJobDto;
  taskCount: number;
}

export enum DiscoveryTaskType {
  SPOTIFY_ARTIST_DATA = 'SPOTIFY_ARTIST_DATA',
  SPOTIFY_TOP_TRACKS = 'SPOTIFY_TOP_TRACKS',
  MUSICBRAINZ_ALBUM_DATA = 'MUSICBRAINZ_ALBUM_DATA',
}

export interface DiscoveryTaskDto {
  id: string;
  creationDate: string;
  updateDate: string | undefined;
  status: DiscoveryTaskDto.Status;
  type: DiscoveryTaskType;
  parameter: string;
  result: string | undefined;
  rawRequest: string | undefined;
  rawResult: string | undefined;
}

export namespace DiscoveryTaskDto {
  export enum Status {
    STARTED = 'STARTED',
    COMPLETE = 'COMPLETE',
    FAILED = 'FAILED',
    INTERRUPTED = 'INTERRUPTED',
  }
}

export interface DiscoveryTaskSummaryDto {
  id: string;
  creationDate: string;
  updateDate: string | undefined;
  status: DiscoveryTaskDto.Status;
  type: DiscoveryTaskType;
  duration: number | undefined;
  targetId: string;
  artist: ArtistDto | undefined;
  album: AlbumDto | undefined;
}

export interface DiscoveryTaskPageDto extends PageDto {
  discoveryTasks: DiscoveryTaskSummaryDto[];
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

export interface RestoredHistoryDto {
  restoredSongCount: number;
  notFoundSongCount: number;
  notFoundUserEmails: string[];
}
