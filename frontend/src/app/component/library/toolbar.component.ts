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
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {CommonModule} from "@angular/common";

@Component({
  standalone: true,
  imports: [CommonModule, TranslateModule, CurrentUserComponent, SettingsComponent, ScanningComponent, LogComponent, UserListComponent],
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
    private authenticationService: AuthenticationService,
    private modalService: NgbModal
  ) {
  }

  ngOnInit(): void {
    this.currentUser = this.authenticationService.currentUser;
  }

  refresh() {
    this.libraryService.requestRefresh();
  }

  openProfile() {
    this.modalService.open(CurrentUserComponent);
  }

  signOut() {
    Logger.info('Signing out...');
    this.authenticationService.logout().subscribe();
  }

  openSettings() {
    this.modalService.open(SettingsComponent);
  }

  openScanning() {
    this.modalService.open(ScanningComponent);
  }

  openLog() {
    this.modalService.open(LogComponent);
  }

  openUsers() {
    this.modalService.open(UserListComponent);
  }
}