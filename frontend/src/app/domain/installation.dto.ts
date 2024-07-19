export interface InstallationDto {
  creationDate: string;
  updateDate: string | undefined;
  version: string;
}

export interface InstallationCommandDto {
  installationSecret: string;
  libraryFolders: InstallationCommandDto.LibraryFolder[];
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
