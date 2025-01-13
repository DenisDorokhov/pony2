import {Component, Input, OnInit} from '@angular/core';
import {TranslateModule} from '@ngx-translate/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {UserCreateCommandDto, UserDto, UserUpdateCommandDto} from '../../../domain/user.dto';
import {FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {ErrorDto} from '../../../domain/common.dto';
import {ErrorContainerComponent} from '../../common/error-container.component';
import {CommonModule} from '@angular/common';
import {UserService} from '../../../service/user.service';
import {Observable} from 'rxjs';
import {AuthenticationService} from '../../../service/authentication.service';
import {LoadingState} from '../../../domain/common.model';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, ReactiveFormsModule, ErrorContainerComponent, LoadingIndicatorComponent, ErrorIndicatorComponent],
  selector: 'pony-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.scss']
})
export class UserComponent implements OnInit {

  LoadingState = LoadingState;
  UserDto = UserDto;

  @Input()
  user: UserDto | undefined;

  form!: FormGroup;
  error: ErrorDto | undefined;
  loadingState = LoadingState.LOADED;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private formBuilder: FormBuilder,
    private userService: UserService,
    private authenticationService: AuthenticationService
  ) {
  }

  ngOnInit(): void {
    this.form = this.formBuilder.group({
      id: this.user?.id ?? undefined,
      name: this.user?.name ?? '',
      email: this.user?.email ?? '',
      role: this.user?.role ?? UserDto.Role.USER,
      password: '',
      repeatPassword: '',
      newPassword: '',
      repeatNewPassword: '',
    });
    if (this.user?.id === this.authenticationService.currentUser!.id) {
      this.form.controls['role'].disable();
    }
  }

  save() {
    const formValue = this.form.value;
    let observable: Observable<UserDto>;
    if (this.user) {
      const command: UserUpdateCommandDto = {
        id: formValue.id,
        name: formValue.name,
        email: formValue.email,
        newPassword: formValue.newPassword === '' ? undefined : formValue.newPassword,
        repeatNewPassword: formValue.repeatNewPassword === '' ? undefined : formValue.repeatNewPassword,
        role: formValue.role ?? this.user.role
      };
      observable = this.userService.updateUser(command);
    } else {
      const command: UserCreateCommandDto = {
        name: formValue.name,
        email: formValue.email,
        password: formValue.password,
        repeatPassword: formValue.repeatPassword === '' ? undefined : formValue.repeatPassword,
        role: formValue.role
      };
      observable = this.userService.createUser(command);
    }
    this.loadingState = LoadingState.LOADING;
    observable.subscribe({
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
}
