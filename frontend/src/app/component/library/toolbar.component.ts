import {Component, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {CurrentUserComponent} from './modal/current-user.component';
import {LogComponent} from './modal/log.component';
import {ScanningComponent} from './modal/scanning.component';
import {SettingsComponent} from './modal/settings.component';
import {UserListComponent} from './modal/user-list.component';
import {UserDto} from "../../domain/user.dto";
import {LibraryService} from "../../service/library.service";
import {AuthenticationService} from "../../service/authentication.service";
import {TranslateModule} from "@ngx-translate/core";
import Logger from "js-logger";
import {NgbDropdownModule, NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {CommonModule} from "@angular/common";
import {LibraryScanService} from "../../service/library-scan.service";

@Component({
  standalone: true,
  imports: [CommonModule, TranslateModule, NgbDropdownModule, CurrentUserComponent, SettingsComponent, ScanningComponent, LogComponent, UserListComponent],
  selector: 'pony-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss']
})
export class ToolbarComponent implements OnInit {

  currentUser: UserDto | undefined;

  @ViewChild('settingsTemplate') settingsTemplate!: TemplateRef<any>;
  @ViewChild('scanningTemplate') scanningTemplate!: TemplateRef<any>;
  @ViewChild('logTemplate') logTemplate!: TemplateRef<any>;
  @ViewChild('userListTemplate') userListTemplate!: TemplateRef<any>;
  @ViewChild('currentUserTemplate') currentUserTemplate!: TemplateRef<any>;

  constructor(
    private libraryService: LibraryService,
    private libraryScanService: LibraryScanService,
    private authenticationService: AuthenticationService,
    private modal: NgbModal
  ) {
  }

  ngOnInit(): void {
    this.currentUser = this.authenticationService.currentUser;
    this.libraryScanService.observeScanStatistics().subscribe(scanStatistics => {
      if (scanStatistics === null) {
        this.openScanning();
      }
    })
  }

  refresh() {
    this.libraryService.requestRefresh();
  }

  openProfile() {
    this.modal.open(CurrentUserComponent).closed.subscribe(user => {
      if (user) {
        this.authenticationService.authenticate().subscribe(currentUser => {
          this.currentUser = currentUser;
        });
      }
    });
  }

  signOut() {
    Logger.info('Signing out...');
    this.authenticationService.logout().subscribe();
  }

  openSettings() {
    this.modal.open(SettingsComponent);
  }

  openScanning() {
    this.modal.open(ScanningComponent, { size: 'xl' });
  }

  openLog() {
    this.modal.open(LogComponent);
  }

  openUsers() {
    this.modal.open(UserListComponent, { size: 'xl' });
  }
}
