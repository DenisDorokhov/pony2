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

  form: FormGroup;
  error: ErrorDto | undefined;

  get libraryFolders(): FormArray {
    return this.form.get('libraryFolders') as FormArray;
  }

  constructor(
    private installationService: InstallationService,
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
    const installationCommand = <InstallationCommandDto>this.form.value;
    installationCommand.startScanJobAfterInstallation = true;
    this.installationService.install(installationCommand).subscribe({
      next: installation => {
        Logger.info(`Version ${installation.version} has been installed.`);
        this.error = undefined;
        this.router.navigate(['/login'], {replaceUrl: true});
      },
      error: error => {
        this.error = error;
      }
    });
  }
}
