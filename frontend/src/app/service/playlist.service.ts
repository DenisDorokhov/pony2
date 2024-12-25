import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {BehaviorSubject, forkJoin, Observable, Subscription} from "rxjs";
import {Playlist, PlaylistSongs} from "../domain/library.model";
import {PlaylistCreateCommandDto, PlaylistDto, PlaylistSongsDto, PlaylistUpdateCommandDto} from "../domain/library.dto";
import {catchError, map, tap} from "rxjs/operators";
import {ErrorDto} from "../domain/common.dto";

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
      .sort((p1, p2) => p1.creationDate.getTime() > p2.creationDate.getTime() ? -1 : 1)
      .slice(0, 3);
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
    return this.httpClient.get<PlaylistDto[]>('/api/playlists/normal').pipe(
      map(dtos => dtos.map(dto => new Playlist(dto))),
      tap(playlists => this.playlistsSubject.next(playlists)),
    );
  }

  createPlaylist(command: PlaylistCreateCommandDto): Observable<PlaylistSongs> {
    return this.httpClient.post<PlaylistSongsDto>('/api/playlists/normal', command).pipe(
      map(dto => new PlaylistSongs(dto)),
      tap(() => this.requestPlaylists().subscribe()),
      catchError(ErrorDto.observableFromHttpErrorResponse)
    );
  }

  updatePlaylist(command: PlaylistUpdateCommandDto): Observable<PlaylistSongs> {
    return this.httpClient.put<PlaylistSongsDto>('/api/playlists/normal', command).pipe(
      map(dto => new PlaylistSongs(dto)),
      tap(() => this.requestPlaylists().subscribe()),
      catchError(ErrorDto.observableFromHttpErrorResponse)
    );
  }

  addToPlaylist(playlistId: string, songId: string): Observable<PlaylistSongs> {
    return this.httpClient.post<PlaylistSongsDto>('/api/playlists/normal/' + playlistId + '/songs/' + songId, null).pipe(
      map(dto => new PlaylistSongs(dto)),
    );
  }
}
