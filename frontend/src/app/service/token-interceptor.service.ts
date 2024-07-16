import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {TokenStorageService} from './token-storage.service';

@Injectable({
  providedIn: 'root'
})
export class TokenInterceptor implements HttpInterceptor {

  constructor(private tokenStorage: TokenStorageService) {
  }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    let newRequest;
    if (this.tokenStorage.accessToken) {
      newRequest = request.clone({
        setHeaders: {
          'Authorization': `Bearer ${this.tokenStorage.accessToken}`
        }
      });
    } else {
      newRequest = request;
    }
    return next.handle(newRequest);
  }
}
