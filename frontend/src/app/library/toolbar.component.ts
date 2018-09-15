import {Component, OnInit, TemplateRef, ViewChild} from '@angular/core';
import * as Logger from 'js-logger';
import {BsModalService} from 'ngx-bootstrap';
import {AuthenticationService} from '../core/user/authentication.service';
import {UserDto} from '../core/user/user.dto';
import {CurrentUserComponent} from './current-user.component';
import {LogComponent} from './log.component';
import {ScanningComponent} from './scanning.component';
import {SettingsComponent} from './settings.component';
import {UserListComponent} from './user-list.component';

@Component({
  selector: 'pony-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss']
})
export class ToolbarComponent implements OnInit {

  currentUser: UserDto;

  @ViewChild('settingsTemplate') settingsTemplate: TemplateRef<any>;
  @ViewChild('scanningTemplate') scanningTemplate: TemplateRef<any>;
  @ViewChild('logTemplate') logTemplate: TemplateRef<any>;
  @ViewChild('userListTemplate') userListTemplate: TemplateRef<any>;
  @ViewChild('currentUserTemplate') currentUserTemplate: TemplateRef<any>;

  constructor(
    private authenticationService: AuthenticationService,
    private modalService: BsModalService
  ) {
  }

  ngOnInit(): void {
    this.currentUser = this.authenticationService.currentUser;
  }

  refresh() {
    Logger.info('Refreshing.');
  }

  openProfile() {
    this.modalService.show(CurrentUserComponent);
  }

  signOut() {
    Logger.info('Signing out...');
    this.authenticationService.logout().subscribe();
  }

  openSettings() {
    this.modalService.show(SettingsComponent);
  }

  openScanning() {
    this.modalService.show(ScanningComponent);
  }

  openLog() {
    this.modalService.show(LogComponent);
  }

  openUsers() {
    this.modalService.show(UserListComponent);
  }
}
