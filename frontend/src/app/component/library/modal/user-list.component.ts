import {Component, OnInit} from '@angular/core';
import {TranslateModule} from "@ngx-translate/core";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {UserDto, UserPageDto} from "../../../domain/user.dto";
import {CommonModule} from "@angular/common";
import {UserService} from "../../../service/user.service";
import {LoadingState} from "../../../domain/common.model";
import Logger from "js-logger";
import {ErrorIndicatorComponent} from "../../common/error-indicator.component";
import {LoadingIndicatorComponent} from "../../common/loading-indicator.component";

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, ErrorIndicatorComponent, LoadingIndicatorComponent],
  selector: 'pony-user-list',
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {

  LoadingState = LoadingState;
  UserRole = UserDto.Role;

  loadingState: LoadingState = LoadingState.LOADING;

  users: UserDto[] = [];
  page: UserPageDto | undefined;
  emptyRowCount = 5;

  constructor(
    private readonly userService: UserService,
    public readonly activeModal: NgbActiveModal
  ) {
  }

  ngOnInit(): void {
    this.loadPage();
  }

  private loadPage(pageIndex = 0, pageSize = 5) {
    this.userService.getAllUsers(pageIndex, pageSize).subscribe({
      next: userPage => {
        this.users = userPage.users;
        this.page = userPage;
        this.emptyRowCount = Math.max(0, 5 - this.users.length);
        this.loadingState = LoadingState.LOADED;
      },
      error: error => {
        this.loadingState = LoadingState.ERROR;
        Logger.error(`Could not load user list: "${error.message}".`);
      }
    });
  }

  loadPreviousPage() {
    if (this.page && this.page.pageIndex > 0) {
      this.loadPage(this.page.pageIndex - 1);
    }
  }

  loadNextPage() {
    if (this.page && this.page.pageIndex < (this.page.totalPages - 1)) {
      this.loadPage(this.page.pageIndex + 1);
    }
  }

  createUser() {

  }

  editUser(user: UserDto) {

  }
}
