import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {TokenStorageService} from './token-storage.service';

@Injectable()
export class TokenInterceptor implements HttpInterceptor {

  constructor(private tokenStorage: TokenStorageService) {
  }

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    let newRequest;
    if (this.tokenStorage.token) {
      newRequest = request.clone({
        setHeaders: {
          'Authorization': `Bearer ${this.tokenStorage.token}`
        }
      });
    } else {
      newRequest = request;
    }
    return next.handle(newRequest);
  }
}
