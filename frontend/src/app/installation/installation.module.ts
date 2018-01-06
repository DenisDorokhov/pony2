import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {InstallationComponent} from './installation.component';
import {InstallationRoutingModule} from './installation-routing.module';
import {ReactiveFormsModule} from '@angular/forms';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InstallationRoutingModule,
  ],
  declarations: [
    InstallationComponent
  ]
})
export class InstallationModule {
}
