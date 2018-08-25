import {AlbumDto, AlbumSongsDto, ArtistDto, ArtistSongsDto, SongDto} from './library.dto';

export class Song {

  id: number;
  creationDate: Date;
  updateDate: Date;
  mimeType: string;
  size: number;
  duration: number;
  bitRate: number;
  bitRateVariable: boolean;
  discNumber: number;
  trackNumber: number;
  name: string;
  artistName: string;
  album: Album;
  genreId: number;

  constructor(songDto: SongDto, album: Album) {
    this.id = songDto.id;
    this.creationDate = songDto.creationDate;
    this.updateDate = songDto.updateDate;
    this.mimeType = songDto.mimeType;
    this.size = songDto.size;
    this.duration = songDto.duration;
    this.bitRate = songDto.bitRate;
    this.bitRateVariable = songDto.bitRateVariable;
    this.discNumber = songDto.discNumber;
    this.trackNumber = songDto.trackNumber;
    this.name = songDto.name;
    this.artistName = songDto.artistName;
    this.album = album;
    this.genreId = songDto.genreId;
  }
}

export class Album {

  id: number;
  creationDate: Date;
  updateDate: Date;
  name: string;
  year: number;
  artworkId: number;
  artist: Artist;

  constructor(albumDto: AlbumDto, artist: Artist) {
    this.id = albumDto.id;
    this.creationDate = albumDto.creationDate;
    this.updateDate = albumDto.updateDate;
    this.name = albumDto.name;
    this.year = albumDto.year;
    this.artworkId = albumDto.artworkId;
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
  updateDate: Date;
  name: string;
  artworkId: number;

  constructor(artistDto: ArtistDto) {
    this.id = artistDto.id;
    this.creationDate = artistDto.creationDate;
    this.updateDate = artistDto.updateDate;
    this.name = artistDto.name;
    this.artworkId = artistDto.artworkId;
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
