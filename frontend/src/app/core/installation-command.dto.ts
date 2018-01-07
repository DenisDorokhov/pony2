class LibraryFolder {
  path: string;
}

export class InstallationCommandDto {

  static LibraryFolder = LibraryFolder;

  installationSecret: string;
  libraryFolders: LibraryFolder[];
  adminName: string;
  adminEmail: string;
  adminPassword: string;
}
