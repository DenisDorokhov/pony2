import {Component, inject, OnDestroy, OnInit} from '@angular/core';
import {CurrentUserComponent} from './modal/current-user.component';
import {LogComponent} from './modal/log.component';
import {ScanningComponent} from './modal/scanning.component';
import {DiscoveryComponent} from './modal/discovery.component';
import {SettingsComponent} from './modal/settings.component';
import {UserListComponent} from './modal/user-list.component';
import {UserDto} from '../../domain/user.dto';
import {AuthenticationService} from '../../service/authentication.service';
import {TranslateModule} from '@ngx-translate/core';
import {NgbDropdownModule, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {LibraryScanService} from '../../service/library-scan.service';
import {LibraryDiscoveryService} from '../../service/library-discovery.service';
import {Subscription} from 'rxjs';
import {FastSearchComponent} from './fast-search.component';
import {QueueComponent} from './modal/queue.component';
import {PlaybackMode, PlaybackService} from '../../service/playback.service';
import {HistoryComponent} from './modal/history.component';
import {PlaylistComponent} from './modal/playlist.component';
import {Theme, ThemeService} from '../../service/theme.service';
import Role = UserDto.Role;

@Component({
  imports: [TranslateModule, NgbDropdownModule, FastSearchComponent],
  selector: 'pony-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss']
})
export class ToolbarComponent implements OnInit, OnDestroy {

  private readonly libraryScanService = inject(LibraryScanService);
  private readonly libraryDiscoveryService = inject(LibraryDiscoveryService);
  private readonly authenticationService = inject(AuthenticationService);
  private readonly playbackService = inject(PlaybackService);
  private readonly themeService = inject(ThemeService);
  private readonly modal = inject(NgbModal);

  PlaybackMode = PlaybackMode;

  currentUser: UserDto | undefined;

  scanRunning = false;
  discoveryEnabled = true;
  discoveryRunning = false;
  scanToDiscoveryTransition = false;
  playbackMode: PlaybackMode;
  currentTheme: Theme;

  private subscriptions: Subscription[] = [];

  constructor() {
    this.playbackMode = this.playbackService.mode;
    this.currentTheme = this.themeService.theme;
  }

  ngOnInit(): void {
    this.subscriptions.push(this.libraryScanService.observeScanStatistics().subscribe(scanStatistics => {
      if (scanStatistics === null && this.authenticationService.currentUser?.role === Role.ADMIN) {
        this.openScanning();
      }
    }));
    this.subscriptions.push(this.authenticationService.observeAuthentication().subscribe(user =>
      this.currentUser = user));
    this.subscriptions.push(this.playbackService.observeMode().subscribe(mode => this.playbackMode = mode));
    this.subscriptions.push(this.themeService.observeTheme().subscribe(theme => this.currentTheme = theme));
    this.subscriptions.push(this.libraryScanService.observeScanJobProgress().subscribe(scanJobProgress => {
      if (scanJobProgress === null && this.scanRunning && this.discoveryEnabled) {
        this.scanToDiscoveryTransition = true;
      }
      this.scanRunning = scanJobProgress !== undefined && scanJobProgress !== null;
    }));
    this.subscriptions.push(this.libraryDiscoveryService.observeDiscoveryStatus().subscribe(discoveryStatus => {
      this.discoveryEnabled = discoveryStatus?.discoveryEnabled !== false;
      this.discoveryRunning = !!discoveryStatus?.progress;
      this.scanToDiscoveryTransition = false;
    }));
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(next => next.unsubscribe());
  }

  startScanJob() {
    if (this.scanRunning) {
      this.openScanning();
    } else if (this.discoveryRunning) {
      this.openDiscovery();
    } else {
      this.libraryScanService.startScanJob().subscribe({
        next: () => this.openScanning()
      });
    }
  }

  openProfile() {
    this.modal.open(CurrentUserComponent).closed.subscribe(user => {
      if (user) {
        this.authenticationService.authenticate().subscribe();
      }
    });
  }

  signOut() {
    this.authenticationService.logout().subscribe();
  }

  openSettings() {
    this.modal.open(SettingsComponent, {size: 'lg'});
  }

  openScanning() {
    this.modal.open(ScanningComponent, {size: 'xl'});
  }

  openDiscovery() {
    this.modal.open(DiscoveryComponent, {size: 'xl'});
  }

  openLog() {
    this.modal.open(LogComponent, {size: 'xl'});
  }

  openUsers() {
    this.modal.open(UserListComponent, {size: 'xl'});
  }

  openQueue() {
    this.modal.open(QueueComponent, {size: 'lg'});
  }

  setPlaybackMode(mode: PlaybackMode) {
    this.playbackService.mode = mode;
  }

  openPlaylists() {
    this.modal.open(PlaylistComponent, {size: 'lg'});
  }

  openHistory() {
    this.modal.open(HistoryComponent, {size: 'lg'});
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }
}
