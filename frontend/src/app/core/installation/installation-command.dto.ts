export class InstallationCommandDto {
  installationSecret: string;
  libraryFolders: InstallationCommandDto[];
  adminName: string;
  adminEmail: string;
  adminPassword: string;
}

export namespace InstallationCommandDto {
  export class LibraryFolder {
    path: string;
  }
}
