import {Component} from '@angular/core';
import {FormArray, FormBuilder, FormGroup} from '@angular/forms';
import {Router} from '@angular/router';
import * as Logger from 'js-logger';
import {ErrorDto} from '../core/common/common.dto';
import {InstallationCommandDto} from '../core/installation/installation.dto';
import {InstallationService} from '../core/installation/installation.service';

@Component({
  selector: 'pony-installation',
  templateUrl: './installation.component.html',
  styleUrls: ['./installation.component.scss']
})
export class InstallationComponent {

  installationForm: FormGroup;
  error: ErrorDto;

  fieldHasViolation = ErrorDto.fieldHasViolation;

  get libraryFolders(): FormArray {
    return this.installationForm.get('libraryFolders') as FormArray;
  }

  constructor(private installationService: InstallationService,
              private formBuilder: FormBuilder,
              private router: Router) {
    this.installationForm = formBuilder.group({
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
    const installationCommand = <InstallationCommandDto>this.installationForm.value;
    this.installationService.install(installationCommand).subscribe(
      installation => {
        Logger.info(`Version ${installation.version} has been installed.`);
        this.error = null;
        this.router.navigate(['/login'], {replaceUrl: true});
      },
      (error: ErrorDto) => {
        if (error.code === ErrorDto.Code.VALIDATION) {
          Logger.error('Validation failed.');
        } else {
          Logger.error(`Installation failed: "${error.message}".`);
        }
        this.error = error;
      }
    );
  }
}
