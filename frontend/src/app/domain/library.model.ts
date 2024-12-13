import {AlbumDto, AlbumSongsDto, ArtistDto, ArtistSongsDto, SearchResultDto, SongDto} from './library.dto';

export class Song {

  id: string;
  creationDate: Date;
  updateDate: Date | undefined;
  path: string | undefined;
  mimeType: string;
  fileExtension: string;
  size: number;
  duration: number;
  durationInMinutes: string;
  bitRate: number;
  bitRateVariable: boolean;
  discNumber: number | undefined;
  trackNumber: number | undefined;
  name: string | undefined;
  artistName: string | undefined;
  genreName: string | undefined;
  genreId: string;
  album: Album;
  audioUrl: string;
  sizeMb: number;

  constructor(songDto: SongDto, album: Album) {
    this.id = songDto.id;
    this.creationDate = new Date(songDto.creationDate);
    this.updateDate = songDto.updateDate ? new Date(songDto.updateDate) : undefined;
    this.path = songDto.path;
    this.mimeType = songDto.mimeType;
    this.fileExtension = songDto.fileExtension;
    this.size = songDto.size;
    this.duration = songDto.duration;
    this.durationInMinutes = this.formatSecondsInMinutes(this.duration);
    this.bitRate = songDto.bitRate;
    this.bitRateVariable = songDto.bitRateVariable;
    this.discNumber = songDto.discNumber;
    this.trackNumber = songDto.trackNumber;
    this.name = songDto.name;
    this.artistName = songDto.artistName;
    this.genreName = songDto.genreName;
    this.genreId = songDto.genreId;
    this.album = album;
    this.audioUrl = '/api/file/audio/' + this.id;
    this.sizeMb = Song.roundToTwoDecimals(songDto.size / 1_000_000);
  }

  private static roundToTwoDecimals(num: number) {
    return Math.round((num + Number.EPSILON) * 100) / 100;
  }

  getRelativeDurationInMinutes(progress: number) {
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

  static compare(song1: Song, song2: Song): number {
    const normalizedDisc1 = !song1.discNumber || song1.discNumber < 1 ? 1 : song1.discNumber;
    const normalizedDisc2 = !song2.discNumber || song2.discNumber < 1 ? 1 : song2.discNumber;
    if (normalizedDisc1 > normalizedDisc2) {
      return 1;
    }
    if (normalizedDisc1 < normalizedDisc2) {
      return -1;
    }
    if ((song1.trackNumber ?? 0) > (song2.trackNumber ?? 0)) {
      return 1;
    }
    if ((song1.trackNumber ?? 0) < (song2.trackNumber ?? 0)) {
      return -1;
    }
    if ((song1.name ?? "") > (song2.name ?? "")) {
      return 1;
    }
    if ((song1.name ?? "") < (song2.name ?? "")) {
      return -1;
    }
    return 0;
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
    this.creationDate = new Date(albumDto.creationDate);
    this.updateDate = albumDto.updateDate ? new Date(albumDto.updateDate) : undefined;
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

  static compare(albumSongs1: AlbumSongs, albumSongs2: AlbumSongs): number {
    if ((albumSongs1.album.year ?? 0) < (albumSongs2.album.year ?? 0)) {
      return 1;
    }
    if ((albumSongs1.album.year ?? 0) > (albumSongs2.album.year ?? 0)) {
      return -1;
    }
    return 0;
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
    this.creationDate = new Date(artistDto.creationDate);
    this.updateDate = artistDto.updateDate ? new Date(artistDto.updateDate) : undefined;
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

export class SearchResult {

  artists: Artist[];
  albums: Album[];
  songs: Song[];

  constructor(searchResultDto: SearchResultDto) {
    this.artists = searchResultDto.artists.map(artist =>
      new Artist(artist));
    this.albums = searchResultDto.albumDetails.map(albumDetails =>
      new Album(albumDetails.album, new Artist(albumDetails.artist)));
    this.songs = searchResultDto.songDetails.map(songDetails =>
      new Song(
        songDetails.song,
        new Album(
          songDetails.albumDetails.album,
          new Artist(songDetails.albumDetails.artist)
        )
      ));
  }
}
