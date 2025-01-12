import {Component} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {ErrorDto} from '../domain/common.dto';
import {AuthenticationService, Credentials} from '../service/authentication.service';
import {Router} from '@angular/router';
import {TranslateModule} from '@ngx-translate/core';
import {ErrorComponent} from './common/error.component';
import {mergeMap, tap} from 'rxjs/operators';
import {PlaybackService} from '../service/playback.service';
import {forkJoin} from 'rxjs';
import {PlaylistService} from '../service/playlist.service';
import {LibraryService} from '../service/library.service';

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
    private playlistService: PlaylistService,
    private libraryService: LibraryService,
    private router: Router,
    formBuilder: FormBuilder,
  ) {
    this.loginForm = formBuilder.group({
      email: '',
      password: '',
    });
  }

  login() {
    const credentials = this.loginForm.value as Credentials;
    this.authenticationService.authenticate(credentials).pipe(
      tap(user => console.info(`User ${user.email} has been authenticated.`)),
      mergeMap(() => forkJoin({
        queueState: this.playbackService.restoreQueueState(),
        library: this.libraryService.initialize(),
        playlist: this.playlistService.initialize(),
      })),
    ).subscribe({
      next: () => {
        this.error = undefined;
        this.router.navigate(['/library'], {replaceUrl: true});
      },
      error: (error: ErrorDto) => this.error = error
    });
  }
}
