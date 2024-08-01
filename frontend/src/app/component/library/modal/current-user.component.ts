import {Component, Input, OnInit} from '@angular/core';
import {TranslateModule} from "@ngx-translate/core";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {CommonModule} from "@angular/common";
import {ErrorComponent} from "../../common/error.component";
import {ErrorContainerComponent} from "../../common/error-container.component";
import {FormBuilder, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {LoadingState} from "../../../domain/common.model";
import {LoadingIndicatorComponent} from "../../common/loading-indicator.component";
import {CurrentUserUpdateCommandDto, UserDto} from "../../../domain/user.dto";
import {ErrorDto} from "../../../domain/common.dto";
import {UserService} from "../../../service/user.service";
import {AuthenticationService} from "../../../service/authentication.service";
import {ErrorIndicatorComponent} from "../../common/error-indicator.component";

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, ErrorComponent, ErrorContainerComponent, ReactiveFormsModule, LoadingIndicatorComponent, ErrorIndicatorComponent],
  selector: 'pony-current-user',
  templateUrl: './current-user.component.html',
  styleUrls: ['./current-user.component.scss']
})
export class CurrentUserComponent implements OnInit {

  LoadingState = LoadingState;

  @Input()
  user!: UserDto;

  userForm!: FormGroup;
  error: ErrorDto | undefined;
  loadingState = LoadingState.LOADING;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private formBuilder: FormBuilder,
    private authenticationService: AuthenticationService,
    private userService: UserService
  ) {
    this.userForm = this.formBuilder.group({
      name: '',
      email: '',
      oldPassword: '',
      newPassword: '',
    });
  }

  ngOnInit(): void {
    this.userForm.disable();
    this.authenticationService.authenticate().subscribe({
      next: user => {
        this.userForm.enable();
        this.user = user;
        this.userForm.controls['name'].setValue(this.user.name);
        this.userForm.controls['email'].setValue(this.user.email);
        this.loadingState = LoadingState.LOADED;
      },
      error: () => {
        this.loadingState = LoadingState.ERROR;
      }
    });
  }

  save() {
    const formValue = this.userForm.value;
    const command: CurrentUserUpdateCommandDto = {
      name: formValue.name,
      email: formValue.email,
      oldPassword: formValue.oldPassword,
      newPassword: formValue.newPassword === '' ? undefined : formValue.newPassword,
    };
    this.loadingState = LoadingState.LOADING;
    this.userForm.disable();
    this.userService.updateCurrentUser(command).subscribe({
      next: user => {
        this.loadingState = LoadingState.LOADED;
        this.userForm.enable();
        this.activeModal.close(user);
      },
      error: error => {
        this.error = error;
        this.loadingState = this.error?.code === ErrorDto.Code.VALIDATION ? LoadingState.LOADED : LoadingState.ERROR;
        this.userForm.enable();
      }
    });
  }
}
