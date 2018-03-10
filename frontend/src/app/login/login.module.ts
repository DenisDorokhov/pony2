import {NgModule} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {SharedModule} from '../shared/shared.module';
import {LoginRoutingModule} from './login-routing.module';
import {LoginComponent} from './login.component';

@NgModule({
  imports: [
    ReactiveFormsModule,
    LoginRoutingModule,
    SharedModule,
  ],
  declarations: [
    LoginComponent,
  ]
})
export class LoginModule {
}
