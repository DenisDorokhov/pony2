import {CommonModule} from '@angular/common';
import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {SharedModule} from '../shared/shared.module';
import {InstallationRoutingModule} from './installation-routing.module';
import {InstallationComponent} from './installation.component';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    InstallationRoutingModule,
    SharedModule,
  ],
  declarations: [
    InstallationComponent,
  ]
})
export class InstallationModule {
}
