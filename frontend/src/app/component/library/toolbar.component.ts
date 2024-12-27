import {Component, OnDestroy, OnInit} from '@angular/core';
import {CurrentUserComponent} from './modal/current-user.component';
import {LogComponent} from './modal/log.component';
import {ScanningComponent} from './modal/scanning.component';
import {SettingsComponent} from './modal/settings.component';
import {UserListComponent} from './modal/user-list.component';
import {UserDto} from "../../domain/user.dto";
import {AuthenticationService} from "../../service/authentication.service";
import {TranslateModule} from "@ngx-translate/core";
import {NgbDropdownModule, NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {CommonModule} from "@angular/common";
import {LibraryScanService} from "../../service/library-scan.service";
import {Subscription} from "rxjs";
import {FastSearchComponent} from "./fast-search.component";
import {ErrorDto} from "../../domain/common.dto";
import {QueueComponent} from "./modal/queue.component";
import {PlaybackMode, PlaybackService} from "../../service/playback.service";
import Role = UserDto.Role;
import {HistoryComponent} from "./modal/history.component";

@Component({
  standalone: true,
  imports: [CommonModule, TranslateModule, NgbDropdownModule, FastSearchComponent],
  selector: 'pony-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss']
})
export class ToolbarComponent implements OnInit, OnDestroy {

  PlaybackMode = PlaybackMode;

  currentUser: UserDto | undefined;

  scanRunning = false;
  playbackMode: PlaybackMode;

  private subscriptions: Subscription[] = [];

  constructor(
    private libraryScanService: LibraryScanService,
    private authenticationService: AuthenticationService,
    private playbackService: PlaybackService,
    private modal: NgbModal
  ) {
    this.playbackMode = this.playbackService.mode;
  }

  ngOnInit(): void {
    this.subscriptions.push(this.libraryScanService.observeScanStatistics().subscribe(scanStatistics => {
      if (scanStatistics === null && this.authenticationService.currentUser?.role === Role.ADMIN) {
        this.openScanning();
      }
    }));
    this.subscriptions.push(this.authenticationService.observeAuthentication().subscribe(user =>
      this.currentUser = user));
    this.subscriptions.push(this.playbackService.observeMode().subscribe(mode => this.playbackMode = mode))
    this.libraryScanService.observeScanJobProgress().subscribe(scanJobProgress =>
      this.scanRunning = scanJobProgress !== undefined && scanJobProgress !== null);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(next => next.unsubscribe());
  }

  startScanJob() {
    if (this.scanRunning) {
      this.openScanning();
    } else {
      this.libraryScanService.startScanJob().subscribe({
        next: () => this.openScanning(),
        error: (error: ErrorDto) => {
          if (error.code === ErrorDto.Code.CONCURRENT_SCAN) {
            this.openScanning();
          }
        }
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
    console.info('Signing out...');
    this.authenticationService.logout().subscribe();
  }

  openSettings() {
    this.modal.open(SettingsComponent, { size: 'lg' });
  }

  openScanning() {
    this.modal.open(ScanningComponent, { size: 'xl' });
  }

  openLog() {
    this.modal.open(LogComponent, { size: 'xl' });
  }

  openUsers() {
    this.modal.open(UserListComponent, { size: 'xl' });
  }

  openQueue() {
    this.modal.open(QueueComponent, { size: 'lg' });
  }

  setPlaybackMode(mode: PlaybackMode) {
    this.playbackService.mode = mode;
  }

  openPlaylists() {
    // TODO: implement
  }

  openHistory() {
    this.modal.open(HistoryComponent, { size: 'lg' });
  }
}
