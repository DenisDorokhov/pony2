export default {
  noSongTitle: 'Pony | Music Streamer',
  songTitlePrefix: 'Pony | ',
  songTitleBody: '{{artistName}} - {{songName}} | ',
  installation: {
    mainHeader: 'Installation',
    libraryFoldersLabel: 'Library Folders:',
    libraryFoldersPlaceholder: 'Enter full folder path',
    adminNameLabel: 'Admin name:',
    adminEmailLabel: 'Admin email:',
    adminPasswordLabel: 'Admin password:',
    repeatAdminPasswordLabel: 'Repeat admin password:',
    installationSecretLabel: 'Installation secret:',
    installationSecretPlaceholder: 'Get from file ~/.pony2/installationSecret.txt',
    installButton: 'Install',
  },
  login: {
    mainHeader: 'Sign In',
    emailLabel: 'Email:',
    passwordLabel: 'Password:',
    loginButton: 'Login',
  },
  library: {
    toolbar: {
      settingsButton: 'Settings',
      scanningButton: 'Scanning',
      logButton: 'Log',
      usersButton: 'Users',
      profileButton: 'Profile',
      logOutButton: 'Logout',
      refreshButton: 'Re-scan',
      queueButton: 'Queue',
      playlistsButton: 'Playlists',
      historyButton: 'History',
      normalModeButton: 'Normal',
      repeatAllModeButton: 'Repeat all',
      repeatOneModeButton: 'Repeat one',
      shuffleModeButton: 'Shuffle',
      radioModeButton: 'Radio',
    },
    genre: {
      unknownLabel: 'Unknown Genre'
    },
    album: {
      counterHeader: '{{albumCount}} albums, {{songCount}} songs',
      discLabel: 'Disc {{discNumber}}',
      unknownLabel: 'Unknown Album'
    },
    artist: {
      unknownLabel: 'Unknown Artist'
    },
    song: {
      unknownLabel: 'Unknown Song',
      playNext: 'Play next',
      addToQueue: 'Add to queue',
      createQueue: 'Create queue',
      selectOrCreatePlaylist: 'Add to playlist',
      addToPlaylist: 'Add to "{{name}}"',
      addToPlaylistNotificationTitle: 'Playlists',
      addToPlaylistNotificationTextSuccess: 'Song added to playlist.',
      addToPlaylistNotificationTextFailure: 'Could not add song to playlist!',
      likedSongButtonTooltip: 'Delete song from favorites',
      notLikedSongButtonTooltip: 'Add song to favorites',
      likeNotificationTitle: 'Playlists',
      likeNotificationTextFailure: 'Could not add song to favorites!',
      unlikeNotificationTitle: 'Playlists',
      unlikeNotificationTextFailure: 'Could not remove song from favorites!',
    },
    noMusicLabel: 'Your library is empty :-(',
    scanStatistics: {
      counts: '{{songCount}} songs, {{artistCount}} artists, {{albumCount}} albums, {{artworkCount}} artworks',
      sizeGigabytes: '{{size}} GB',
      sizeMegabytes: '{{size}} MB',
      date: 'Last scan: {{date}}',
      githubLinkLabel: 'Pony on GitHub',
    },
  },
  player: {
    noSongTitle: 'Pony - Music Streamer',
    songTitle: '{{artistName}} - {{songName}}',
    likedSongButtonTooltip: 'Delete song from favorites',
    notLikedSongButtonTooltip: 'Add song to favorites',
    likeNotificationTitle: 'Playlists',
    likeNotificationTextFailure: 'Could not add song to favorites!',
    unlikeNotificationTitle: 'Playlists',
    unlikeNotificationTextFailure: 'Could not remove song from favorites!',
  },
  queue: {
    header: 'Playback Queue',
    emptyQueueLabel: 'Your playback queue is currently empty.',
    scrollToCurrentSongButton: 'Show current song',
    sizeLabel: '{{songCount}} songs ({{duration}})'
  },
  largeSong: {
    goToSongButton: 'Go to song',
    removeSongButton: 'Remove from queue',
  },
  playlistEdit: {
    editHeader: 'Edit Playlist',
    createHeader: 'Create Playlist',
    nameLabel: 'Title:',
  },
  playlistAddSong: {
    header: 'Select Playlist',
    saveButton: 'Add Song',
    createPlaylistButton: 'New Playlist',
    notificationTitle: 'Playlists',
    notificationTextSuccess: 'Song added to playlist.',
    notificationTextFailure: 'Could not add song to playlist!',
  },
  history: {
    header: 'Playback History',
    noHistoryLabel: 'No songs played yet :-(',
    statisticsLabel: 'Showing {{count}} or {{totalCount}} total songs played',
  },
  playlist: {
    header: 'Playlists',
    noPlaylistsLabel: 'No playlists found.',
    noSongsLabel: 'Playlist is empty.',
    createButton: 'New Playlist',
    deleteButton: 'Delete',
    deletionConfirmation: 'Are you sure to delete selected playlist?',
    notificationTitle: 'Playlists',
    notificationTextDeletionSuccess: 'Playlist deleted.',
    notificationTextDeletionFailure: 'Could not delete selected playlist!',
  },
  scanning: {
    header: 'Scanning',
    statusLabel: 'Status:',
    progressLabel: 'Progress:',
    startScanButton: 'Re-Scan',
    startDateColumn: 'Start Date',
    updateDateColumn: 'Update Date',
    statusColumn: 'Status',
    lastMessageColumn: 'Last Message',
    detailsColumn: '',
    detailsButton: 'Details',
    scanJobProgress: {
      inactiveLabel: 'inactive',
      errorLabel: 'could not start scan job :-(',
      startingLabel: 'starting...',
      preparingLabel: 'preparing...',
      searchingMediaLabel: 'searching media...',
      cleaningSongsLabel: 'looking for deleted songs...',
      cleaningSongsWithProgressLabel: 'cleaning songs: {{percentage}}% ({{itemsComplete}} of {{itemsTotal}} files processed)...',
      cleaningArtworksLabel: 'looking for deleted artworks...',
      cleaningArtworksWithProgressLabel: 'cleaning artworks: {{percentage}}% ({{itemsComplete}} of {{itemsTotal}} files processed)...',
      importingLabel: 'importing songs...',
      importingWithProgressLabel: 'importing songs: {{percentage}}% ({{itemsComplete}} of {{itemsTotal}} files processed)...',
      searchingArtworksLabel: 'searching artworks...',
      searchingArtworksWithProgressLabel: 'searching artworks: {{percentage}}% ({{itemsComplete}} of {{itemsTotal}} files processed)...',
    },
  },
  scanJob: {
    header: 'Scan Job',
    statusLabel: 'Status:',
    logMessageLabel: 'Log message:',
    targetPathsLabel: 'Target folders:',
    failedPathsLabel: 'Failed folders:',
    processedAudioFilesLabel: 'Processed audio files:',
    scanResultLabel: 'Updates:',
    scanResult: {
      createdArtistCount: 'created {{value}} artists',
      updatedArtistCount: 'updated {{value}} artists',
      deletedArtistCount: 'deleted {{value}} artists',
      createdAlbumCount: 'created {{value}} albums',
      updatedAlbumCount: 'updated {{value}} albums',
      deletedAlbumCount: 'deleted {{value}} albums',
      createdGenreCount: 'created {{value}} genres',
      updatedGenreCount: 'updated {{value}} genres',
      deletedGenreCount: 'deleted {{value}} genres',
      createdSongCount: 'created {{value}} songs',
      updatedSongCount: 'updated {{value}} songs',
      deletedSongCount: 'deleted {{value}} songs',
      createdArtworkCount: 'created {{value}} artworks',
      deletedArtworkCount: 'deleted {{value}} artworks',
    },
    countsLabel: 'Counts:',
    counts: '{{artistCount}} artists,\n' +
      '{{albumCount}} albums, \n' +
      '{{songCount}} songs, \n' +
      '{{artworkCount}} artworks, \n' +
      '{{genreCount}} genres',
    durationLabel: 'Duration:',
    sizeLabel: 'Audio files size:',
  },
  userList: {
    header: 'Users',
    creationDateColumn: 'Creation Date',
    updateDateColumn: 'Update Date',
    nameColumn: 'Name',
    emailColumn: 'Email',
    roleColumn: 'Role',
    editButton: 'Edit',
    deleteButton: 'Delete',
    createUserButton: 'Create User',
    deletionConfirmation: 'Are you sure to delete this user?',
    userDeletionNotificationTitle: 'User Deletion',
    userDeletionNotificationFailed: 'Failed!',
  },
  user: {
    editHeader: 'Edit User',
    createHeader: 'Create User',
    nameLabel: 'Name:',
    emailLabel: 'Email:',
    newPasswordLabel: 'New password:',
    repeatNewPasswordLabel: 'Repeat new password:',
    passwordLabel: 'Password:',
    repeatPasswordLabel: 'Repeat password:',
    roleLabel: 'Role:',
    newPasswordPlaceholder: 'Leave empty to keep old password',
  },
  currentUser: {
    header: 'My Profile',
    nameLabel: 'Name:',
    emailLabel: 'Email:',
    oldPasswordLabel: 'Old password:',
    newPasswordLabel: 'New password:',
    newPasswordPlaceholder: 'Leave empty to keep old password',
    repeatNewPasswordLabel: 'Repeat new password:',
  },
  settings: {
    header: 'Settings',
    libraryFoldersLabel: 'Library Folders:',
    libraryFoldersPlaceholder: 'Enter full folder path',
    startScanJobConfirmation: 'Library folders updated. Do you want to start scan now?',
  },
  log: {
    header: 'Log',
    dateColumn: 'Date',
    levelColumn: 'Level',
    messageColumn: 'Message',
    levelFilterLabel: 'Level:',
  },
  shared: {
    errorsHeader: 'Errors',
    loadingIndicatorLabel: 'Loading...',
    errorIndicatorLabel: 'Operation failed!',
    previousPageButton: '&laquo; Previous',
    nextPageButton: 'Next &raquo;',
    dateTimeFormat: 'yyyy-MM-dd H:mm:ss',
    currentPageLabel: 'Page {{pageIndex}} of {{totalPages}}',
    creationDateLabel: 'Creation Date:',
    updateDateLabel: 'Update Date:',
    closeButton: 'Close',
    cancelButton: 'Cancel',
    saveButton: 'Save',
    duration: {
      days: '{{value}} days',
      hours: '{{value}} hours',
      minutes: '{{value}} minutes',
      seconds: '{{value}} seconds',
    },
    scanJob: {
      startingStatus: 'STARTING',
      startedStatus: 'STARTED',
      completeStatus: 'COMPLETE',
      failedStatus: 'FAILED',
      interruptedStatus: 'INTERRUPTED',
    },
  },
  fastSearch: {
    searchPlaceholder: 'Search (Ctrl+Shift+F)',
    foundSongsHeader: 'Songs',
    foundArtistsHeader: 'Artists',
    foundAlbumsHeader: 'Albums',
  },
  notification: {
    authenticationErrorTitle: 'Authentication Error',
    authenticationErrorText: 'Please sign in.',
    authorizationErrorTitle: 'Authorization Error',
    authorizationErrorText: 'Access denied.',
    scanJobTitle: 'Scan Job',
    scanJobInterruptedText: 'Interrupted!',
    scanJobFinishedText: 'Complete!',
    scanJobFailedText: 'Failed!',
    scanJobAlreadyRunningText: 'Failed!',
    settingsTitle: 'Settings',
    settingsUpdatedText: 'Updated!',
  },
  fieldViolation: {
    // Localized field violation messages can be defined here (code-message pairs).
  },
  error: {
    // Localized error messages can be defined here (code-message pairs).
  }
};
