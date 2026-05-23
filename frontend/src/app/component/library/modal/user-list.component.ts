import {Component, inject, OnInit} from '@angular/core';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {UserDto, UserPageDto} from '../../../domain/user.dto';
import {CommonModule} from '@angular/common';
import {UserService} from '../../../service/user.service';
import {LoadingState} from '../../../domain/common.model';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {UserComponent} from './user.component';
import {AuthenticationService} from '../../../service/authentication.service';
import {NotificationService} from '../../../service/notification.service';

@Component({
    imports: [TranslateModule, CommonModule, ErrorIndicatorComponent, LoadingIndicatorComponent],
    selector: 'pony-user-list',
    templateUrl: './user-list.component.html',
    styleUrls: ['./user-list.component.scss']
})
export class UserListComponent implements OnInit {

  readonly activeModal = inject(NgbActiveModal);
  readonly authenticationService = inject(AuthenticationService);

  private readonly userService = inject(UserService);
  private readonly modal = inject(NgbModal);
  private readonly translateService = inject(TranslateService);
  private readonly notificationService = inject(NotificationService);

  LoadingState = LoadingState;
  UserRole = UserDto.Role;

  loadingState: LoadingState = LoadingState.LOADING;

  users: UserDto[] = [];
  page: UserPageDto | undefined;
  emptyRowCount = 5;

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
      error: () => {
        this.loadingState = LoadingState.ERROR;
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
      });
    }
  }
}
