export interface LibraryFolderDto {
  path: string;
}

export interface ConfigDto {
  libraryFolders: LibraryFolderDto[];
}
