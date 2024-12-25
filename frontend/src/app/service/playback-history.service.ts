import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {PlaybackHistorySongDto} from "../domain/library.dto";
import {map} from "rxjs/operators";
import {PlaybackHistorySong} from "../domain/library.model";
import {Observable} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class PlaybackHistoryService {

  constructor(
    private httpClient: HttpClient,
  ) {
  }

  addSongToHistory(songId: string): Observable<PlaybackHistorySong> {
    return this.httpClient.post<PlaybackHistorySongDto>('/api/history/' + songId, null).pipe(
      map(dto => new PlaybackHistorySong(dto)),
    );
  }
}
