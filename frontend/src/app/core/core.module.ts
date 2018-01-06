import {NgModule} from '@angular/core';
import {HttpClientModule} from '@angular/common/http';
import {InstallationService} from './installation/installation.service';
import {InstallationGuard} from './installation/installation-guard.service';

@NgModule({
  imports: [
    HttpClientModule,
  ],
  providers: [
    InstallationService,
    InstallationGuard,
  ]
})
export class CoreModule {
}
