import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {LogMessageDto, LogMessagePageDto} from '../domain/library.dto';
import {Observable} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LogService {

  constructor(
    private httpClient: HttpClient,
  ) {
  }

  getLog(
    minLevel: LogMessageDto.Level | undefined,
    pageIndex = 0, pageSize = 30
  ): Observable<LogMessagePageDto> {
    const params: any = {};
    params.minLevel = minLevel ?? LogMessageDto.Level.INFO;
    params.pageIndex = pageIndex;
    params.pageSize = pageSize;
    return this.httpClient.get<LogMessagePageDto>('/api/admin/log', { params: params });
  }
}
