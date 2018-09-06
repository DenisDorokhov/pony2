import {Component, OnInit, TemplateRef, ViewChild} from '@angular/core';
import {BsModalRef, BsModalService} from 'ngx-bootstrap';
import {AuthenticationService} from '../core/user/authentication.service';
import {UserDto} from '../core/user/user.dto';

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
  
  settingsModal: BsModalRef;
  scanningModal: BsModalRef;
  logModal: BsModalRef;
  userListModal: BsModalRef;
  currentUserModal: BsModalRef;

  constructor(
    private authenticationService: AuthenticationService,
    private modalService: BsModalService
  ) {
  }

  ngOnInit(): void {
    this.currentUser = this.authenticationService.currentUser;
  }

  refresh() {
    console.log('Refreshing.');
  }

  openProfile() {
    this.currentUserModal = this.modalService.show(this.currentUserTemplate);
  }

  signOut() {
    console.log('Signing out...');
    this.authenticationService.logout().subscribe();
  }

  openSettings() {
    this.settingsModal = this.modalService.show(this.settingsTemplate);
  }

  openScanning() {
    this.scanningModal = this.modalService.show(this.scanningTemplate);
  }

  openLog() {
    this.logModal = this.modalService.show(this.logTemplate);
  }

  openUsers() {
    this.userListModal = this.modalService.show(this.userListTemplate);
  }
}
