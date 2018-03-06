import {HttpClientModule} from '@angular/common/http';
import {APP_INITIALIZER, NgModule} from '@angular/core';
import {TranslateLoader, TranslateModule} from '@ngx-translate/core';
import {InitializerService} from './initializer.service';
import {InstallationService} from './installation.service';
import {InstalledGuard} from './installed-guard.service';
import {NotInstalledGuard} from './not-installed-guard.service';
import {TranslateLoaderService} from './translate-loader.service';

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
  ],
  providers: [
    InstallationService,
    InstalledGuard,
    NotInstalledGuard,
    InitializerService,
    {
      provide: APP_INITIALIZER,
      useFactory: initialize,
      deps: [InitializerService],
      multi: true,
    }
  ]
})
export class CoreModule {
}
