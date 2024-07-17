import {AlbumDto, AlbumSongsDto, ArtistDto, ArtistSongsDto, SongDto} from './library.dto';

export class Song {

  id: string;
  creationDate: Date;
  updateDate: Date | undefined;
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
  genreId: string;
  album: Album;
  audioUrl: string;
  sizeMb: number;

  constructor(songDto: SongDto, album: Album) {
    this.id = songDto.id;
    this.creationDate = songDto.creationDate;
    this.updateDate = songDto.updateDate;
    this.path = songDto.path;
    this.mimeType = songDto.mimeType;
    this.fileExtension = songDto.fileExtension;
    this.size = songDto.size;
    this.duration = songDto.duration;
    this.bitRate = songDto.bitRate;
    this.bitRateVariable = songDto.bitRateVariable;
    this.discNumber = songDto.discNumber;
    this.trackNumber = songDto.trackNumber;
    this.name = songDto.name;
    this.artistName = songDto.artistName;
    this.genreId = songDto.genreId;
    this.album = album;
    this.audioUrl = '/api/file/audio/' + this.id;
    this.sizeMb = Song.roundToTwoDecimals(songDto.size / 1_000_000);
  }

  private static roundToTwoDecimals(num: number) {
    return Math.round((num + Number.EPSILON) * 100) / 100;
  }

  get durationInMinutes(): string {
    return this.formatSecondsInMinutes(this.duration);
  }

  relativeDurationInMinutes(progress: number) {
    return this.formatSecondsInMinutes(this.duration * progress);
  }

  private formatSecondsInMinutes(time: number): string {
    const minutes = Math.floor(time / 60);
    const seconds = Math.floor(time - minutes * 60);
    let buf = minutes + ':';
    if (seconds <= 9) {
      buf += '0';
    }
    buf += Math.floor(seconds);
    return buf;
  }
}

export class Album {

  id: string;
  creationDate: Date;
  updateDate: Date | undefined;
  name: string | undefined;
  year: number | undefined;
  artworkId: string | undefined;
  artist: Artist;
  smallArtworkUrl: string | undefined;
  largeArtworkUrl: string | undefined;

  constructor(albumDto: AlbumDto, artist: Artist) {
    this.id = albumDto.id;
    this.creationDate = albumDto.creationDate;
    this.updateDate = albumDto.updateDate;
    this.name = albumDto.name;
    this.year = albumDto.year;
    this.artworkId = albumDto.artworkId;
    this.smallArtworkUrl = this.artworkId ? '/api/file/artwork/small/' + this.artworkId : undefined;
    this.largeArtworkUrl = this.artworkId ? '/api/file/artwork/large/' + this.artworkId : undefined;
    this.artist = artist;
  }
}

export class AlbumSongs {

  album: Album;
  songs: Song[];

  constructor(albumSongsDto: AlbumSongsDto, artist: Artist) {
    this.album = new Album(albumSongsDto.album, artist);
    this.songs = albumSongsDto.songs
      .map(songDto => new Song(songDto, this.album));
  }
}

export class Artist {

  id: string;
  creationDate: Date;
  updateDate: Date | undefined;
  name: string | undefined;
  artworkId: string | undefined;
  smallArtworkUrl: string | undefined;
  largeArtworkUrl: string | undefined;

  constructor(artistDto: ArtistDto) {
    this.id = artistDto.id;
    this.creationDate = artistDto.creationDate;
    this.updateDate = artistDto.updateDate;
    this.name = artistDto.name;
    this.artworkId = artistDto.artworkId;
    this.smallArtworkUrl = this.artworkId ? '/api/file/artwork/small/' + this.artworkId : undefined;
    this.largeArtworkUrl = this.artworkId ? '/api/file/artwork/large/' + this.artworkId : undefined;
  }

  static equals(artist1: Artist | undefined, artist2: Artist | undefined) {
    if (artist1 === artist2) {
      return true;
    }
    if (!artist1 || !artist2) {
      return false;
    }
    return artist1.id === artist2.id;
  }
}

export class ArtistSongs {

  artist: Artist;
  albumSongs: AlbumSongs[];

  constructor(artistSongsDto: ArtistSongsDto) {
    this.artist = new Artist(artistSongsDto.artist);
    this.albumSongs = artistSongsDto.albumSongs
      .map(albumSongsDto => new AlbumSongs(albumSongsDto, this.artist));
  }
}
