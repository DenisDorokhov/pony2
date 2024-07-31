import {Component, Input, OnInit} from '@angular/core';
import {TranslateModule} from "@ngx-translate/core";
import {NgbActiveModal} from "@ng-bootstrap/ng-bootstrap";
import {UserCreateCommandDto, UserDto, UserUpdateCommandDto} from "../../../domain/user.dto";
import {FormBuilder, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {ErrorComponent} from "../../common/error.component";
import {ErrorDto} from "../../../domain/common.dto";
import {ErrorContainerComponent} from "../../common/error-container.component";
import {CommonModule} from "@angular/common";
import {UserService} from "../../../service/user.service";
import {Observable} from "rxjs";
import {AuthenticationService} from "../../../service/authentication.service";

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, ReactiveFormsModule, ErrorComponent, ErrorContainerComponent],
  selector: 'pony-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.scss']
})
export class UserComponent implements OnInit {

  @Input()
  user: UserDto | undefined;

  userForm!: FormGroup;
  roles = [UserDto.Role.USER, UserDto.Role.ADMIN];
  error: ErrorDto | undefined;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private formBuilder: FormBuilder,
    private userService: UserService,
    private authenticationService: AuthenticationService
  ) {
  }

  ngOnInit(): void {
    this.userForm = this.formBuilder.group({
      id: this.user?.id ?? undefined,
      name: this.user?.name ?? '',
      email: this.user?.email ?? '',
      role: this.user?.role ?? UserDto.Role.USER,
      password: '',
      newPassword: '',
    });
    if (this.user?.id === this.authenticationService.currentUser!.id) {
      this.userForm.controls['role'].disable();
    }
  }

  save() {
    const formValue = this.userForm.value;
    let observable: Observable<UserDto>;
    if (this.user) {
      const command: UserUpdateCommandDto = {
        id: formValue.id,
        name: formValue.name,
        email: formValue.email,
        newPassword: formValue.newPassword === '' ? undefined : formValue.newPassword,
        role: formValue.role ?? this.user.role
      };
      observable = this.userService.updateUser(command);
    } else {
      const command: UserCreateCommandDto = {
        name: formValue.name,
        email: formValue.email,
        password: formValue.password,
        role: formValue.role
      };
      observable = this.userService.createUser(command);
    }
    observable.subscribe({
      next: () => {
        this.activeModal.close();
      },
      error: error => {
        this.error = error;
      }
    });
  }
}
