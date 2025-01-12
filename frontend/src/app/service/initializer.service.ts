import {Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {EMPTY, forkJoin} from 'rxjs';
import {catchError, mergeMap, tap} from 'rxjs/operators';
import {InstallationService} from './installation.service';
import {AuthenticationService} from './authentication.service';
import {PlaybackService} from './playback.service';
import {PlaylistService} from './playlist.service';
import {LibraryService} from './library.service';

@Injectable({
  providedIn: 'root'
})
export class InitializerService {

  constructor(
    private translateService: TranslateService,
    private installationService: InstallationService,
    private authenticationService: AuthenticationService,
    private playbackService: PlaybackService,
    private playlistService: PlaylistService,
    private libraryService: LibraryService,
  ) {
  }

  initialize(): Promise<any> {

    this.translateService.setDefaultLang('en');
    this.translateService.use('en');

    window.document.title = this.translateService.instant('noSongTitle');

    return this.installationService.getInstallationStatus()
      .pipe(
        mergeMap(installationStatus => {
          if (installationStatus.installed) {
            return this.authenticationService.authenticate()
              .pipe(
                mergeMap(() => forkJoin({
                  queueState: this.playbackService.restoreQueueState(),
                  library: this.libraryService.initialize(),
                  playlist: this.playlistService.initialize(),
                })),
                catchError(() => EMPTY),
              );
          } else {
            return EMPTY;
          }
        }),
        tap({
          next: () => {
            (window as any).ponyBootstrapSuccess = true;
          },
          error: () => {
            (window as any).ponyBootstrapError = true;
          }
        }),
      )
      .toPromise();
  }
}
