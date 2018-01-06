import {Component} from '@angular/core';
import {FormArray, FormBuilder, FormGroup} from '@angular/forms';
import {InstallationCommand} from './installation-command.model';
import {LibraryFolder} from './library-folder.model';

@Component({
  selector: 'pony-installation',
  templateUrl: './installation.component.html',
  styleUrls: ['./installation.component.scss']
})
export class InstallationComponent {

  installationForm: FormGroup;

  get libraryFolders(): FormArray {
    return this.installationForm.get('libraryFolders') as FormArray;
  }

  constructor(private formBuilder: FormBuilder) {
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
    const installationCommand = this.buildInstallationCommand();
    console.log('To be implemented.');
  }

  private buildInstallationCommand(): InstallationCommand {
    const formValue = this.installationForm.value;
    return <InstallationCommand>{
      installationSecret: formValue.installationSecret,
      libraryFolders: formValue.libraryFolders.map(value => <LibraryFolder>{path: value.path}),
      adminName: formValue.adminName,
      adminEmail: formValue.adminEmail,
      adminPassword: formValue.adminPassword,
    };
  }
}
