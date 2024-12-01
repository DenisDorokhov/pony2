import {Component} from "@angular/core";
import {FormBuilder, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {ErrorDto} from "../domain/common.dto";
import {AuthenticationService, Credentials} from "../service/authentication.service";
import {Router} from "@angular/router";
import {TranslateModule} from "@ngx-translate/core";
import {ErrorComponent} from "./common/error.component";
import {mergeMap, tap} from "rxjs/operators";
import {PlaybackService} from "../service/playback.service";

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
    private playbackService: PlaybackService,
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
    this.authenticationService.authenticate(credentials).pipe(
      tap(user => console.info(`User ${user.email} has been authenticated.`)),
      mergeMap(() => this.playbackService.restoreQueueState())
    ).subscribe({
      next: () => {
        this.error = undefined;
        this.router.navigate(['/library'], {replaceUrl: true});
      },
      error: (error: ErrorDto) => {
        console.error(`Authentication failed: "${error.message}".`);
        this.error = error;
      }
    });
  }
}
