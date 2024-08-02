import {Component, OnInit} from '@angular/core';
import {TranslateModule, TranslateService} from "@ngx-translate/core";
import {NgbActiveModal, NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {UserDto, UserPageDto} from "../../../domain/user.dto";
import {CommonModule} from "@angular/common";
import {UserService} from "../../../service/user.service";
import {LoadingState} from "../../../domain/common.model";
import Logger from "js-logger";
import {ErrorIndicatorComponent} from "../../common/error-indicator.component";
import {LoadingIndicatorComponent} from "../../common/loading-indicator.component";
import {UserComponent} from "./user.component";
import {AuthenticationService} from "../../../service/authentication.service";
import {NotificationService} from "../../../service/notification.service";

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
    public readonly activeModal: NgbActiveModal,
    public readonly authenticationService: AuthenticationService,
    private readonly userService: UserService,
    private readonly modal: NgbModal,
    private readonly translateService: TranslateService,
    private readonly notificationService: NotificationService,
  ) {}

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
    this.modal.open(UserComponent).closed.subscribe(user => {
      if (user) {
        this.loadPage();
      }
    });
  }

  editUser(user: UserDto) {
    const modalRef = this.modal.open(UserComponent);
    const userComponent: UserComponent = modalRef.componentInstance;
    userComponent.user = user;
    modalRef.closed.subscribe(user => {
      if (user) {
        this.loadPage();
        if (user.id === this.authenticationService.currentUser!.id) {
          this.authenticationService.authenticate().subscribe();
        }
      }
    });
  }

  deleteUser(user: UserDto) {
    if (window.confirm(this.translateService.instant('userList.deletionConfirmation'))) {
      this.loadingState = LoadingState.LOADING;
      this.userService.deleteUser(user.id).subscribe({
        next: () => {
          this.loadingState = LoadingState.LOADED;
          this.loadPage();
        },
        error: () => {
          this.loadingState = LoadingState.LOADED;
          this.notificationService.error(
            this.translateService.instant('userList.userDeletionNotificationTitle'),
            this.translateService.instant('userList.userDeletionNotificationFailed'),
          );
        }
      })
    }
  }
}
