export interface LibraryFolderDto {
  path: string;
}

export interface ConfigDto {
  updateDate: string | undefined;
  libraryFolders: LibraryFolderDto[];
  discoveryEnabled: boolean;
  llmUrl: string | undefined;
  llmModel: string | undefined;
  llmApiKey: string | undefined;
}
