import {Component} from '@angular/core';
import {FormBuilder, FormGroup} from '@angular/forms';
import {Router} from '@angular/router';
import {ErrorDto} from '../core/common/error.dto';
import {AuthenticationService} from '../core/user/authentication.service';
import {Credentials} from '../core/user/credentials.model';

@Component({
  selector: 'pony-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {

  loginForm: FormGroup;
  error: ErrorDto;

  constructor(private authenticationService: AuthenticationService,
              private formBuilder: FormBuilder,
              private router: Router) {
    this.loginForm = formBuilder.group({
      email: '',
      password: '',
    });
  }

  login() {
    const credentials = <Credentials>this.loginForm.value;
    this.authenticationService.authenticate(credentials).subscribe(
      user => {
        console.log(`User ${user.email} has been authenticated.`);
        this.error = null;
        this.router.navigate(['/'], {replaceUrl: true});
      },
      (error: ErrorDto) => {
        console.log('Authentication failed.');
        this.error = error;
      }
    );
  }
}
