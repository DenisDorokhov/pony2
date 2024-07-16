import {Component} from "@angular/core";
import {FormArray, FormBuilder, FormGroup, ReactiveFormsModule} from "@angular/forms";
import {ErrorDto} from "../domain/common.dto";
import {InstallationService} from "../service/installation.service";
import {Router} from "@angular/router";
import {InstallationCommandDto} from "../domain/installation.dto";
import Logger from "js-logger";
import {ErrorComponent} from "./common/error.component";
import {ErrorContainerComponent} from "./common/error-container.component";
import {TranslateModule} from "@ngx-translate/core";
import {CommonModule} from "@angular/common";
import {AutoFocusDirective} from "./common/auto-focus.directive";

@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslateModule, AutoFocusDirective, ErrorComponent, ErrorContainerComponent],
  selector: 'pony-installation',
  templateUrl: './installation.component.html',
  styleUrl: './installation.component.scss'
})
export class InstallationComponent {

  installationForm: FormGroup;
  error: ErrorDto | undefined;

  fieldHasViolation = ErrorDto.fieldHasViolation;

  get libraryFolders(): FormArray {
    return this.installationForm.get('libraryFolders') as FormArray;
  }

  constructor(
    private installationService: InstallationService,
    private formBuilder: FormBuilder,
    private router: Router
  ) {
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
        this.error = undefined;
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
