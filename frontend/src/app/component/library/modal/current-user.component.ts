import {Component, OnInit} from '@angular/core';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {NgbActiveModal, NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {CommonModule} from '@angular/common';
import {ErrorContainerComponent} from '../../common/error-container.component';
import {FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {LoadingState} from '../../../domain/common.model';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {CurrentUserUpdateCommandDto, UserDto} from '../../../domain/user.dto';
import {ErrorDto} from '../../../domain/common.dto';
import {UserService} from '../../../service/user.service';
import {AuthenticationService} from '../../../service/authentication.service';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {NotificationService} from '../../../service/notification.service';
import {OpenSubsonicApiKeyComponent} from './open-subsonic-api-key.component';

@Component({
  imports: [TranslateModule, CommonModule, ErrorContainerComponent, ReactiveFormsModule, LoadingIndicatorComponent, ErrorIndicatorComponent],
  selector: 'pony-current-user',
  templateUrl: './current-user.component.html',
  styleUrls: ['./current-user.component.scss']
})
export class CurrentUserComponent implements OnInit {

  LoadingState = LoadingState;

  user!: UserDto;

  form: FormGroup;
  error: ErrorDto | undefined;
  loadingState = LoadingState.LOADING;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private formBuilder: FormBuilder,
    private authenticationService: AuthenticationService,
    private userService: UserService,
    private notificationService: NotificationService,
    private translateService: TranslateService,
    private readonly modal: NgbModal,
  ) {
    this.form = this.formBuilder.group({
      name: '',
      email: '',
      oldPassword: '',
      newPassword: '',
      repeatNewPassword: '',
    });
  }

  ngOnInit(): void {
    this.authenticationService.authenticate().subscribe({
      next: user => {
        this.user = user;
        this.form.controls['name'].setValue(this.user.name);
        this.form.controls['email'].setValue(this.user.email);
        this.loadingState = LoadingState.LOADED;
      },
      error: () => {
        this.loadingState = LoadingState.ERROR;
      }
    });
  }

  save() {
    const formValue = this.form.value;
    const command: CurrentUserUpdateCommandDto = {
      name: formValue.name,
      email: formValue.email,
      oldPassword: formValue.oldPassword,
      newPassword: formValue.newPassword === '' ? undefined : formValue.newPassword,
      repeatNewPassword: formValue.repeatNewPassword === '' ? undefined : formValue.repeatNewPassword,
    };
    this.loadingState = LoadingState.LOADING;
    this.userService.updateCurrentUser(command).subscribe({
      next: user => {
        this.loadingState = LoadingState.LOADED;
        this.activeModal.close(user);
      },
      error: error => {
        this.error = error;
        this.loadingState = this.error?.code === ErrorDto.Code.VALIDATION ? LoadingState.LOADED : LoadingState.ERROR;
      }
    });
  }

  generateOpenSubsonicApiKey() {
    this.userService.generateCurrentUserOpenSubsonicApiKey().subscribe({
      next: apiKey => {
        const modalRef = this.modal.open(OpenSubsonicApiKeyComponent, {size: '400px'});
        const userComponent: OpenSubsonicApiKeyComponent = modalRef.componentInstance;
        userComponent.apiKey = apiKey.value;
      },
      error: () => {
        this.notificationService.error(
          this.translateService.instant('notification.generateApiKeyTitle'),
          this.translateService.instant('notification.generateApiKeyErrorText')
        );
      }
    });
  }
}
