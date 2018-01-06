import {NgModule} from '@angular/core';
import {HttpClientModule} from '@angular/common/http';
import {InstallationService} from './installation/installation.service';
import {InstalledGuardService} from './installation/installed-guard.service';
import {NotInstalledGuardService} from './installation/not-installed-guard.service';

@NgModule({
  imports: [
    HttpClientModule,
  ],
  providers: [
    InstallationService,
    InstalledGuardService,
    NotInstalledGuardService,
  ]
})
export class CoreModule {
}
