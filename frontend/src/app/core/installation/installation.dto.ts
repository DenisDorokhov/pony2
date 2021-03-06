export interface InstallationDto {
  creationDate: Date;
  updateDate: Date | undefined;
  version: string;
}

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

export interface InstallationStatusDto {
  installed: boolean;
}
