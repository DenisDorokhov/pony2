export interface InstallationCommandDto {
  installationSecret: string;
  libraryFolders: InstallationCommandDto[];
  adminName: string;
  adminEmail: string;
  adminPassword: string;
}

export namespace InstallationCommandDto {
  export interface LibraryFolder {
    path: string;
  }
}
