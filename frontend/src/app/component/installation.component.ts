import {Component} from '@angular/core';
import {FormArray, FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {ErrorDto} from '../domain/common.dto';
import {InstallationService} from '../service/installation.service';
import {Router} from '@angular/router';
import {InstallationCommandDto} from '../domain/installation.dto';
import {ErrorComponent} from './common/error.component';
import {ErrorContainerComponent} from './common/error-container.component';
import {TranslateModule} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {AutoFocusDirective} from './common/auto-focus.directive';
import {mergeMap} from 'rxjs';
import {AuthenticationService, Credentials} from '../service/authentication.service';

@Component({
    imports: [CommonModule, ReactiveFormsModule, TranslateModule, AutoFocusDirective, ErrorComponent, ErrorContainerComponent],
    selector: 'pony-installation',
    templateUrl: './installation.component.html',
    styleUrl: './installation.component.scss'
})
export class InstallationComponent {

  form: FormGroup;
  error: ErrorDto | undefined;

  get libraryFolders(): FormArray {
    return this.form.get('libraryFolders') as FormArray;
  }

  constructor(
    private installationService: InstallationService,
    private authenticationService: AuthenticationService,
    private formBuilder: FormBuilder,
    private router: Router
  ) {
    this.form = formBuilder.group({
      installationSecret: '',
      libraryFolders: formBuilder.array([
        formBuilder.group({path: ''})
      ]),
      adminName: '',
      adminEmail: '',
      adminPassword: '',
      repeatAdminPassword: '',
    });
  }

  addLibraryFolder() {
    this.libraryFolders.push(this.formBuilder.group({path: ''}));
  }

  removeLibraryFolder(i: number) {
    this.libraryFolders.removeAt(i);
  }

  install() {
    const installationCommand = this.form.value as InstallationCommandDto;
    installationCommand.startScanJobAfterInstallation = true;
    this.installationService.install(installationCommand).pipe(
      mergeMap(() => {
        this.error = undefined;
        return this.authenticationService.authenticate({
          email: installationCommand.adminEmail,
          password: installationCommand.adminPassword,
        } as Credentials);
      })
    ).subscribe({
      next: () => this.router.navigate(['/library'], {replaceUrl: true}),
      error: error => this.error = error
    });
  }
}
