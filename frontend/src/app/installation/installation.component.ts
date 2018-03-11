import {Component} from '@angular/core';
import {FormArray, FormBuilder, FormGroup} from '@angular/forms';
import {Router} from '@angular/router';
import {ErrorDto} from '../core/common/error.dto';
import {InstallationCommandDto} from '../core/installation/installation-command.dto';
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
        console.log(`Version ${installation.version} has been installed.`);
        this.error = null;
        this.router.navigate(['/login'], {skipLocationChange: true});
      },
      (error: ErrorDto) => {
        if (error.code === ErrorDto.Code.VALIDATION) {
          console.log('Validation failed.');
        } else {
          console.log('Installation failed.');
        }
        this.error = error;
      }
    );
  }
}
