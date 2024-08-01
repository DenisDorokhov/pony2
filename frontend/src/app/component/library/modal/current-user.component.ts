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

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, ErrorComponent, ErrorContainerComponent, ReactiveFormsModule, LoadingIndicatorComponent],
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
  loadingState = LoadingState.LOADED;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private formBuilder: FormBuilder,
    private userService: UserService
  ) {
  }

  ngOnInit(): void {
    this.userForm = this.formBuilder.group({
      name: this.user.name,
      email: this.user.email,
      oldPassword: '',
      newPassword: '',
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
        this.loadingState = LoadingState.LOADED;
        this.userForm.enable();
        this.error = error;
      }
    });
  }
}
