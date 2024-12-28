import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable, throwError} from 'rxjs';
import {TokenStorageService} from './token-storage.service';
import {catchError} from 'rxjs/operators';
import {NotificationService} from './notification.service';
import {TranslateService} from '@ngx-translate/core';

@Injectable({
  providedIn: 'root'
})
export class SecurityInterceptor implements HttpInterceptor {

  constructor(
    private tokenStorage: TokenStorageService,
    private notificationService: NotificationService,
    private translateService: TranslateService,
  ) {
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
    return next.handle(newRequest).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 &&
          !error.url?.endsWith('/api/authentication') &&
          !error.url?.endsWith('/api/user')
        ) {
          this.notificationService.error(
            this.translateService.instant('notification.authenticationErrorTitle'),
            this.translateService.instant('notification.authenticationErrorText')
          );
        }
        if (error.status === 403) {
          this.notificationService.error(
            this.translateService.instant('notification.authorizationErrorTitle'),
            this.translateService.instant('notification.authorizationErrorText')
          );
        }
        return throwError(() => error);
      })
    );
  }
}
