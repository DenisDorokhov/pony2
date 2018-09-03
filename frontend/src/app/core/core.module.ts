import {HTTP_INTERCEPTORS, HttpClientModule} from '@angular/common/http';
import {APP_INITIALIZER, NgModule} from '@angular/core';
import {TranslateLoader, TranslateModule} from '@ngx-translate/core';
import {CookieModule} from 'ngx-cookie';
import {ErrorTranslationService} from './common/error-translation.service';
import {InitializerService} from './common/initializer.service';
import {NotificationService} from './common/notification.service';
import {TranslateLoaderService} from './common/translate-loader.service';
import {InstallationService} from './installation/installation.service';
import {InstalledGuard} from './installation/installed-guard.service';
import {NotInstalledGuard} from './installation/not-installed-guard.service';
import {LibraryService} from './library/library.service';
import {PageTitleService} from './library/page-title.service';
import {PlaybackService} from './library/playback.service';
import {AuthenticatedGuard} from './user/authenticated-guard.service';
import {AuthenticationService} from './user/authentication.service';
import {NotAuthenticatedGuard} from './user/not-authenticated-guard.service';
import {TokenInterceptor} from './user/token-interceptor.service';
import {TokenStorageService} from './user/token-storage.service';

export function initialize(initializerService: InitializerService) {
  return () => initializerService.initialize();
}

@NgModule({
  imports: [
    HttpClientModule,
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useClass: TranslateLoaderService,
      }
    }),
    CookieModule.forRoot(),
  ],
  providers: [
    InitializerService,
    InstallationService,
    ErrorTranslationService,
    InstalledGuard,
    NotInstalledGuard,
    TokenStorageService,
    AuthenticationService,
    AuthenticatedGuard,
    NotAuthenticatedGuard,
    LibraryService,
    PlaybackService,
    NotificationService,
    PageTitleService,
    {
      provide: APP_INITIALIZER,
      useFactory: initialize,
      deps: [InitializerService],
      multi: true,
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: TokenInterceptor,
      multi: true,
    }
  ]
})
export class CoreModule {
}
