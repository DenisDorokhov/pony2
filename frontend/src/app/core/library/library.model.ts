import {AlbumDto, AlbumSongsDto, ArtistDto, ArtistSongsDto, SongDto} from './library.dto';

export class Song {

  id: number;
  creationDate: Date;
  updateDate: Date | undefined;
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
  genreId: number;
  album: Album;
  audioUrl: string;

  constructor(songDto: SongDto, album: Album) {
    this.id = songDto.id;
    this.creationDate = songDto.creationDate;
    this.updateDate = songDto.updateDate;
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
  }
}

export class Album {

  id: number;
  creationDate: Date;
  updateDate: Date | undefined;
  name: string | undefined;
  year: number | undefined;
  artworkId: number | undefined;
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

  id: number;
  creationDate: Date;
  updateDate: Date | undefined;
  name: string | undefined;
  artworkId: number | undefined;
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
