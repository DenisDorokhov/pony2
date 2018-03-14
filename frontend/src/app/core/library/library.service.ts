import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {ErrorDto} from '../common/error.dto';
import {ArtistDto} from './artist.dto';

@Injectable()
export class LibraryService {

  constructor(private httpClient: HttpClient) {
  }

  getArtists(): Observable<ArtistDto[]> {
    return this.httpClient.get<ArtistDto[]>('/api/library/artists')
      .catch(ErrorDto.observableFromHttpErrorResponse);
  }
}
