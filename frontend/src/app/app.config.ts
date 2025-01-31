import {
  ApplicationConfig,
  importProvidersFrom,
  inject,
  provideAppInitializer,
  provideZoneChangeDetection
} from '@angular/core';
import {provideRouter} from '@angular/router';

import {routes} from './app.routes';
import {InitializerService} from './service/initializer.service';
import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi} from '@angular/common/http';
import {CookieModule} from 'ngx-cookie';
import {TranslateLoaderService} from './service/translate-loader.service';
import {TranslateLoader, TranslateModule} from '@ngx-translate/core';
import {SecurityInterceptor} from './service/security-interceptor.service';
import {ReactiveFormsModule} from '@angular/forms';
import {provideToastr} from 'ngx-toastr';
import {provideAnimations} from '@angular/platform-browser/animations';
import {NgbDateAdapter, NgbDateNativeAdapter} from '@ng-bootstrap/ng-bootstrap';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({eventCoalescing: true}),
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    provideAnimations(),
    provideToastr({
      timeOut: 1500,
      positionClass: 'toast-top-left',
    }),
    importProvidersFrom(
      ReactiveFormsModule,
      TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useClass: TranslateLoaderService,
        }
      }),
      CookieModule.withOptions(),
    ),
    {
      provide: NgbDateAdapter,
      useClass: NgbDateNativeAdapter
    },
    provideAppInitializer(() => {
      return (inject(InitializerService) as InitializerService).initialize();
    }),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: SecurityInterceptor,
      multi: true,
    }
  ]
};
