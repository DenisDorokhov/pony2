import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import 'rxjs-compat/add/operator/distinctUntilChanged';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';
import {ErrorDto} from '../common/error.dto';
import {AlbumSongsDto} from './album-songs.dto';
import {AlbumSongs} from './album-songs.model';
import {AlbumDto} from './album.dto';
import {Album} from './album.model';
import {ArtistSongsDto} from './artist-songs.dto';
import {ArtistSongs} from './artist-songs.model';
import {ArtistDto} from './artist.dto';
import {Artist} from './artist.model';
import {SongDto} from './song.dto';
import {Song} from './song.model';

export enum LibraryState {
  UNKNOWN,
  NON_EMPTY,
  EMPTY,
}

@Injectable()
export class LibraryService {

  private libraryStateSubject = new BehaviorSubject<LibraryState>(LibraryState.UNKNOWN);
  private selectedArtistSubject = new BehaviorSubject<Artist>(undefined);
  private selectedSongSubject = new BehaviorSubject<Song>(undefined);

  private static artistFromDto(artistDto: ArtistDto): Artist {
    return new Artist({
      id: artistDto.id,
      creationDate: artistDto.creationDate,
      updateDate: artistDto.updateDate,
      name: artistDto.name,
      artwork: artistDto.artwork
    });
  }

  private static albumFromDto(albumDto: AlbumDto, artist: Artist): Album {
    return new Album({
      id: albumDto.id,
      creationDate: albumDto.creationDate,
      updateDate: albumDto.updateDate,
      name: albumDto.name,
      year: albumDto.year,
      artwork: albumDto.artwork,
      artist: artist
    });
  }

  private static songFromDto(songDto: SongDto, album: Album): Song {
    return new Song({
      id: songDto.id,
      creationDate: songDto.creationDate,
      updateDate: songDto.updateDate,
      mimeType: songDto.mimeType,
      size: songDto.size,
      duration: songDto.duration,
      bitRate: songDto.bitRate,
      bitRateVariable: songDto.bitRateVariable,
      discNumber: songDto.discNumber,
      trackNumber: songDto.trackNumber,
      name: songDto.name,
      artistName: songDto.artistName,
      album: album,
      genre: songDto.genre
    });
  }

  private static albumSongsFromDto(albumSongsDto: AlbumSongsDto, artist: Artist) {
    const album = this.albumFromDto(albumSongsDto.album, artist);
    return new AlbumSongs({
        album: album,
        songs: albumSongsDto.songs.map(songDto => LibraryService.songFromDto(songDto, album))
    });
  }

  private static artistSongsFromDto(artistSongsDto: ArtistSongsDto): ArtistSongs {
    const artist = this.artistFromDto(artistSongsDto.artist);
    return new ArtistSongs({
      artist: artist,
      albums: artistSongsDto.albums.map(albumSongsDto => this.albumSongsFromDto(albumSongsDto, artist))
    });
  }

  constructor(private httpClient: HttpClient) {
  }

  get libraryState(): Observable<LibraryState> {
    return this.libraryStateSubject.asObservable()
      .distinctUntilChanged();
  }

  getArtists(): Observable<Artist[]> {
    return this.httpClient.get<Artist[]>('/api/library/artists')
      .catch(ErrorDto.observableFromHttpErrorResponse)
      .map(artistDtos => artistDtos.map(artistDto => LibraryService.artistFromDto(artistDto)))
      .do(artists => this.libraryStateSubject.next(artists.length > 0 ? LibraryState.NON_EMPTY : LibraryState.EMPTY));
  }

  getArtistSongs(artist: number): Observable<ArtistSongs> {
    return this.httpClient.get<ArtistSongsDto>(`/api/library/artistSongs/${artist}`)
      .catch(ErrorDto.observableFromHttpErrorResponse)
      .map(artistSongsDto => LibraryService.artistSongsFromDto(artistSongsDto));
  }

  get selectedArtist(): Observable<Artist> {
    return this.selectedArtistSubject.asObservable()
      .distinctUntilChanged();
  }

  selectArtist(artist: Artist) {
    this.selectedArtistSubject.next(artist);
  }

  deselectArtist() {
    this.selectedArtistSubject.next(undefined);
  }

  get selectedSong(): Observable<Song> {
    return this.selectedSongSubject.asObservable()
      .distinctUntilChanged();
  }

  selectSong(song: Song) {
    this.selectedSongSubject.next(song);
  }

  deselectSong() {
    this.selectedSongSubject.next(undefined);
  }
}
