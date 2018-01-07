import {HttpClientModule} from '@angular/common/http';
import {NgModule} from '@angular/core';
import {InstallationService} from './installation.service';
import {InstalledGuard} from './installed-guard.service';
import {NotInstalledGuard} from './not-installed-guard.service';

@NgModule({
  imports: [
    HttpClientModule,
  ],
  providers: [
    InstallationService,
    InstalledGuard,
    NotInstalledGuard,
  ]
})
export class CoreModule {
}
