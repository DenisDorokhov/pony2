import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, forkJoin, Observable, Subscription} from 'rxjs';
import {Playlist, PlaylistSongs} from '../domain/library.model';
import {PlaylistCreateCommandDto, PlaylistDto, PlaylistSongsDto, PlaylistUpdateCommandDto} from '../domain/library.dto';
import {catchError, map, tap} from 'rxjs/operators';
import {ErrorDto} from '../domain/common.dto';

@Injectable({
  providedIn: 'root'
})
export class PlaylistService {

  playlistsSubject = new BehaviorSubject<Playlist[]>([]);
  likePlaylistSongsSubject = new BehaviorSubject<PlaylistSongs | undefined>(undefined);

  private requestPlaylistsSubscription: Subscription | undefined;

  constructor(
    private httpClient: HttpClient,
  ) {
  }

  initialize(): Observable<void> {
    return forkJoin({
      likePlaylist: this.httpClient.get<PlaylistSongsDto>('/api/playlists/like').pipe(
        map(dto => new PlaylistSongs(dto)),
        tap(likePlaylistSongs => this.likePlaylistSongsSubject.next(likePlaylistSongs)),
      ),
      playlists: this.requestPlaylists(),
    }).pipe(
      map(() => undefined),
    );
  }

  getTopPlaylists(): Playlist[] {
    const lastPlaylists = [...this.playlistsSubject.value];
    return lastPlaylists
      .sort((p1, p2) =>
        (p1.updateDate ?? p1.creationDate).getTime() > (p2.updateDate ?? p2.creationDate).getTime() ? -1 : 1
      )
      .slice(0, 3);
  }

  isLikedSong(songId: string): boolean {
    const likePlaylist = this.likePlaylistSongsSubject.value;
    if (likePlaylist) {
      return likePlaylist.songs.filter(next => next.song.id === songId).length > 0;
    }
    return false;
  }

  observePlaylists(): Observable<Playlist[]> {
    return this.playlistsSubject.asObservable();
  }

  observeLikePlaylist(): Observable<PlaylistSongs> {
    return this.likePlaylistSongsSubject.asObservable() as Observable<PlaylistSongs>;
  }

  requestPlaylists(): Observable<Playlist[]> {
    this.requestPlaylistsSubscription?.unsubscribe();
    this.requestPlaylistsSubscription = undefined;
    return this.httpClient.get<PlaylistDto[]>('/api/playlists').pipe(
      map(dtos => dtos.map(dto => new Playlist(dto))),
      tap(playlists => this.playlistsSubject.next(playlists)),
    );
  }

  getPlaylist(playlistId: string): Observable<PlaylistSongs> {
    return this.httpClient.get<PlaylistSongsDto>('/api/playlists/' + playlistId).pipe(
      map(dto => new PlaylistSongs(dto))
    );
  }

  createNormalPlaylist(command: PlaylistCreateCommandDto): Observable<PlaylistSongs> {
    return this.httpClient.post<PlaylistSongsDto>('/api/playlists/normal', command).pipe(
      map(dto => new PlaylistSongs(dto)),
      tap(() => this.requestPlaylists().subscribe()),
      catchError(ErrorDto.observableFromHttpErrorResponse),
    );
  }

  updatePlaylist(command: PlaylistUpdateCommandDto): Observable<PlaylistSongs> {
    return this.httpClient.put<PlaylistSongsDto>('/api/playlists', command).pipe(
      map(dto => new PlaylistSongs(dto)),
      tap(playlistSongs => {
        if (playlistSongs.playlist.type === PlaylistDto.Type.LIKE) {
          this.likePlaylistSongsSubject.next(playlistSongs);
        }
        this.requestPlaylists().subscribe();
      }),
      catchError(ErrorDto.observableFromHttpErrorResponse),
    );
  }

  deleteNormalPlaylist(playlistId: string): Observable<PlaylistSongs> {
    return this.httpClient.delete<PlaylistSongsDto>('/api/playlists/normal/' + playlistId).pipe(
      map(dto => new PlaylistSongs(dto)),
      tap(() => this.requestPlaylists().subscribe()),
    );
  }

  addSongToPlaylist(playlistId: string, songId: string): Observable<PlaylistSongs> {
    return this.httpClient.post<PlaylistSongsDto>('/api/playlists/' + playlistId + '/songs/' + songId, null).pipe(
      map(dto => new PlaylistSongs(dto)),
      tap(() => this.requestPlaylists().subscribe()),
    );
  }

  likeSong(songId: string): Observable<PlaylistSongs> {
    return this.httpClient.post<PlaylistSongsDto>('/api/playlists/like/songs/' + songId, null).pipe(
      map(dto => new PlaylistSongs(dto)),
      tap(likedSongs => this.likePlaylistSongsSubject.next(likedSongs)),
    );
  }

  unlikeSong(songId: string): Observable<PlaylistSongs> {
    return this.httpClient.delete<PlaylistSongsDto>('/api/playlists/like/songs/' + songId).pipe(
      map(dto => new PlaylistSongs(dto)),
      tap(likedSongs => this.likePlaylistSongsSubject.next(likedSongs)),
    );
  }
}
