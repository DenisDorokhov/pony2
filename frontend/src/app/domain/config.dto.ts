export interface LibraryFolderDto {
  path: string;
}

export interface ConfigDto {
  updateDate: string;
  libraryFolders: LibraryFolderDto[];
}
