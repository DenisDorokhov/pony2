import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';
import {ErrorDto} from '../common/error.dto';
import {ArtistSongsDto} from './artist-songs.dto';
import {ArtistDto} from './artist.dto';

@Injectable()
export class LibraryService {

  private selectedArtistSubject = new Subject<ArtistDto>();

  constructor(private httpClient: HttpClient) {
  }

  getArtists(): Observable<ArtistDto[]> {
    return this.httpClient.get<ArtistDto[]>('/api/library/artists')
      .catch(ErrorDto.observableFromHttpErrorResponse);
  }

  getArtistSongs(artist: number): Observable<ArtistSongsDto> {
    return this.httpClient.get<ArtistSongsDto>(`/api/library/artistSongs/${artist}`)
      .catch(ErrorDto.observableFromHttpErrorResponse);
  }

  get selectedArtist(): Observable<ArtistDto> {
    return this.selectedArtistSubject.asObservable();
  }

  selectArtist(artist: ArtistDto) {
    this.selectedArtistSubject.next(artist);
  }
}
