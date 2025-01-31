import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BackupDto, PlaybackHistoryDto, PlaybackHistorySongDto, RestoredHistoryDto} from '../domain/library.dto';
import {map} from 'rxjs/operators';
import {PlaybackHistory, PlaybackHistorySong} from '../domain/library.model';
import {Observable} from 'rxjs';
import FileSaver from 'file-saver';

@Injectable({
  providedIn: 'root'
})
export class PlaybackHistoryService {

  constructor(
    private httpClient: HttpClient,
  ) {
  }

  getHistory(): Observable<PlaybackHistory> {
    return this.httpClient.get<PlaybackHistoryDto>('/api/history').pipe(
      map(dto => new PlaybackHistory(dto))
    );
  }

  addSongToHistory(songId: string): Observable<PlaybackHistorySong> {
    return this.httpClient.post<PlaybackHistorySongDto>('/api/history/' + songId, null).pipe(
      map(dto => new PlaybackHistorySong(dto)),
    );
  }

  backupHistory(): Observable<void> {
    return this.httpClient.get<BackupDto>('/api/admin/history/backup').pipe(
      map(backup => {
        const file = new File([backup.fileContent], 'history-' + window.location.host + '-' + new Date().toISOString(), {type: 'text/plain;charset=utf-8'});
        FileSaver.saveAs(file);
      }),
      map(() => undefined),
    );
  }

  restoreHistory(file: File): Observable<RestoredHistoryDto> {
    const formData: FormData = new FormData();
    formData.append('file', file, file.name);
    return this.httpClient.post<RestoredHistoryDto>('/api/admin/history/restore', formData);
  }
}
