import {LibraryFolder} from './library-folder.model';

export class InstallationCommand {
  installationSecret: string;
  libraryFolders: [LibraryFolder];
  adminName: string;
  adminEmail: string;
  adminPassword: string;
}
