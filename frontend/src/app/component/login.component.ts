import {Component} from "@angular/core";
import {FormBuilder, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {ErrorDto} from "../domain/common.dto";
import {AuthenticationService, Credentials} from "../service/authentication.service";
import {Router} from "@angular/router";
import Logger from "js-logger";
import {TranslateModule} from "@ngx-translate/core";
import {ErrorComponent} from "./common/error.component";

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, TranslateModule, ErrorComponent],
  selector: 'pony-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {

  loginForm: FormGroup;
  error: ErrorDto | undefined;

  constructor(
    private authenticationService: AuthenticationService,
    private router: Router,
    formBuilder: FormBuilder,
  ) {
    this.loginForm = formBuilder.group({
      email: '',
      password: '',
    });
  }

  login() {
    const credentials = <Credentials>this.loginForm.value;
    this.authenticationService.authenticate(credentials).subscribe(
      user => {
        Logger.info(`User ${user.email} has been authenticated.`);
        this.error = undefined;
        this.router.navigate(['/library'], {replaceUrl: true});
      },
      (error: ErrorDto) => {
        Logger.error(`Authentication failed: "${error.message}".`);
        this.error = error;
      }
    );
  }
}
