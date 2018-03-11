import {Component, OnInit} from '@angular/core';
import {AuthenticationService} from '../core/user/authentication.service';
import {UserDto} from '../core/user/user.dto';

@Component({
  selector: 'pony-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.scss']
})
export class ToolbarComponent implements OnInit {

  currentUser: UserDto;

  constructor(private authenticationService: AuthenticationService) {
  }

  ngOnInit(): void {
    this.currentUser = this.authenticationService.currentUser;
  }

  refresh() {
    console.log('Refreshing.');
  }

  openProfile() {
    console.log('Opening profile.');
  }

  signOut() {
    console.log('Signing out.');
  }

  openSettings() {
    console.log('Opening settings.');
  }

  openScanning() {
    console.log('Opening scanning.');
  }

  openLog() {
    console.log('Opening log.');
  }

  openUsers() {
    console.log('Opening users.');
  }
}
