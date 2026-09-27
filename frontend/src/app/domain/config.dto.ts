export interface LibraryFolderDto {
  path: string;
}

export interface ConfigDto {
  updateDate: string;
  libraryFolders: LibraryFolderDto[];
  llmUrl: string | null;
  llmApiKey: string | null;
}
