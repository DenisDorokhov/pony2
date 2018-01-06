import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {InstallationComponent} from './installation.component';
import {InstallationRoutingModule} from './installation-routing.module';

@NgModule({
  imports: [
    CommonModule,
    InstallationRoutingModule,
  ],
  declarations: [
    InstallationComponent
  ]
})
export class InstallationModule {
}
