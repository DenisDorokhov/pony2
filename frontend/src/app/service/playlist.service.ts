import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {BehaviorSubject, forkJoin, Observable} from "rxjs";
import {Playlist, PlaylistSongs} from "../domain/library.model";
import {PlaylistDto, PlaylistSongsDto} from "../domain/library.dto";
import {map, tap} from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})
export class PlaylistService {

  likePlaylistSubject = new BehaviorSubject<PlaylistSongs | undefined>(undefined);
  playlistsSubject = new BehaviorSubject<Playlist[]>([]);

  constructor(
    private httpClient: HttpClient,
  ) {
  }

  initialize(): Observable<void> {
    return forkJoin({
      likePlaylist: this.httpClient.get<PlaylistSongsDto>('/api/playlists/like').pipe(
        map(dto => new PlaylistSongs(dto)),
        tap(likePlaylistSongs => this.likePlaylistSubject.next(likePlaylistSongs)),
      ),
      playlists: this.httpClient.get<PlaylistDto[]>('/api/playlists/normal').pipe(
        map(dtos => dtos.map(dto => new Playlist(dto))),
        tap(playlists => this.playlistsSubject.next(playlists)),
      )
    }).pipe(
      map(() => undefined),
    );
  }
}
