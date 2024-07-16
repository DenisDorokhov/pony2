import {APP_INITIALIZER, ApplicationConfig, importProvidersFrom, provideZoneChangeDetection} from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import {InitializerService} from "./service/initializer.service";
import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi} from "@angular/common/http";
import {CookieModule} from "ngx-cookie";
import {TranslateLoaderService} from "./service/translate-loader.service";
import {TranslateLoader, TranslateModule} from "@ngx-translate/core";
import {TokenInterceptor} from "./service/token-interceptor.service";
import {ReactiveFormsModule} from "@angular/forms";

export function initialize(initializerService: InitializerService) {
  return () => initializerService.initialize();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withInterceptorsFromDi()
    ),
    importProvidersFrom(
      ReactiveFormsModule,
      TranslateModule.forRoot({
        loader: {
          provide: TranslateLoader,
          useClass: TranslateLoaderService,
        }
      }),
      CookieModule.withOptions()
    ),
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
};
