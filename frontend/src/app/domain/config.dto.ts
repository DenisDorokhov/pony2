export interface LibraryFolderDto {
  path: string;
}

export interface ConfigDto {
  updateDate: string | undefined;
  libraryFolders: LibraryFolderDto[];
  llmUrl: string | undefined;
  llmModel: string | undefined;
  llmApiKey: string | undefined;
}
