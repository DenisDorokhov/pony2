import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {SearchResultDto} from "../domain/library.dto";

@Injectable({
  providedIn: 'root'
})
export class SearchService {

  constructor(
    private httpClient: HttpClient
  ) {
  }

  search(query: string): Observable<SearchResultDto> {
    return this.httpClient.get<SearchResultDto>('/api/library/search', { params: {query} });
  }
}
