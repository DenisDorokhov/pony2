import {Component, OnInit} from '@angular/core';
import {TranslateModule} from "@ngx-translate/core";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {UserDto} from "../../../domain/user.dto";
import {DatePipe} from "@angular/common";
import {UserService} from "../../../service/user.service";
import {LoadingState} from "../../../domain/common.model";
import Logger from "js-logger";

@Component({
  standalone: true,
  imports: [TranslateModule, DatePipe],
  selector: 'pony-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {

  LoadingState = LoadingState;

  loadingState: LoadingState = LoadingState.LOADING;

  users: UserDto[] = [];

  constructor(
    private readonly userService: UserService,
    public readonly activeModal: NgbActiveModal
  ) {
  }

  ngOnInit(): void {
    this.userService.getAllUsers().subscribe({
      next: users => {
        this.users = users;
        this.loadingState = LoadingState.LOADED;
      },
      error: error => {
        this.loadingState = LoadingState.ERROR;
        Logger.error(`Could not load user list: "${error.message}".`);
      }
    });
  }
}
