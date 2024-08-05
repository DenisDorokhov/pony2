import {APP_INITIALIZER, ApplicationConfig, importProvidersFrom, provideZoneChangeDetection} from '@angular/core';
import {provideRouter, withHashLocation} from '@angular/router';

import {routes} from './app.routes';
import {InitializerService} from "./service/initializer.service";
import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi} from "@angular/common/http";
import {CookieModule} from "ngx-cookie";
import {TranslateLoaderService} from "./service/translate-loader.service";
import {TranslateLoader, TranslateModule} from "@ngx-translate/core";
import {SecurityInterceptor} from "./service/security-interceptor.service";
import {ReactiveFormsModule} from "@angular/forms";
import {provideToastr} from "ngx-toastr";
import {provideAnimations} from "@angular/platform-browser/animations";
import {NgbDateAdapter, NgbDateNativeAdapter} from "@ng-bootstrap/ng-bootstrap";

export function initialize(initializerService: InitializerService) {
  return () => initializerService.initialize();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withHashLocation()),
    provideHttpClient(withInterceptorsFromDi()),
    provideAnimations(),
    provideToastr({
      timeOut: 1500
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
    {
      provide: APP_INITIALIZER,
      useFactory: initialize,
      deps: [InitializerService],
      multi: true,
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: SecurityInterceptor,
      multi: true,
    }
  ]
};
