import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {SharedModule} from '../shared/shared.module';
import {InstallationRoutingModule} from './installation-routing.module';
import {InstallationComponent} from './installation.component';

@NgModule({
  imports: [
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
