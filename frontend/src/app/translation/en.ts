export default {
  noSongTitle: 'Pony | Music Streamer',
  songTitlePrefix: 'Pony | ',
  songTitleBody: '{{artistName}} - {{songName}} | ',
  installation: {
    mainHeader: 'Installation',
    libraryFoldersLabel: 'Library Folders:',
    libraryFoldersPlaceholder: 'Enter full folder path',
    adminNameLabel: 'Admin name:',
    adminNamePlaceholder: 'Enter name',
    adminEmailLabel: 'Admin email:',
    adminEmailPlaceholder: 'Enter email',
    adminPasswordLabel: 'Admin password:',
    adminPasswordPlaceholder: 'Enter password',
    repeatAdminPasswordLabel: 'Repeat admin password:',
    repeatAdminPasswordPlaceholder: 'Repeat password',
    installationSecretLabel: 'Installation secret:',
    installationSecretPlaceholder: 'Get from file ~/.pony3/installationSecret.txt',
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
      refreshButton: 'Refresh',
      systemButton: 'System',
      settingsButton: 'Settings',
      scanningButton: 'Scanning',
      logButton: 'Log',
      usersButton: 'Users',
    },
    log: {
      header: 'Log',
    },
    scanning: {
      header: 'Scanning',
    },
    settings: {
      header: 'Settings',
    },
    userList: {
      header: 'Users',
    },
    album: {
      discLabel: 'Disc {{discNumber}}',
      unknownLabel: 'Unknown'
    },
    artist: {
      unknownLabel: 'Unknown'
    },
    song: {
      unknownLabel: 'Unknown'
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
    playbackFailed: 'Playback failed.',
    windowCloseConfirmation: 'Playback will stop after closing the window. Are you sure?',
  },
  scanning: {
    statusLabel: 'Status:',
    progressLabel: 'Progress:',
    startScanButton: 'Start Scan',
    startDateColumn: 'Start Date',
    updateDateColumn: 'Update Date',
    statusColumn: 'Status',
    lastMessageColumn: 'Last Message',
    scanJobProgress: {
      inactiveLabel: 'inactive',
      errorLabel: 'could not start scan job :-(',
      startingLabel: 'starting...',
      preparingLabel: 'preparing...',
      searchingMediaLabel: 'searching media...',
      cleaningSongsLabel: 'cleaning songs...',
      cleaningSongsWithProgressLabel: 'cleaning songs: {{percentage}}% ({{itemsComplete}} of {{itemsTotal}} files processed)...',
      cleaningArtworksLabel: 'cleaning artworks...',
      cleaningArtworksWithProgressLabel: 'cleaning artworks: {{percentage}}% ({{itemsComplete}} of {{itemsTotal}} files processed)...',
      importingLabel: 'importing songs...',
      importingWithProgressLabel: 'importing songs: {{percentage}}% ({{itemsComplete}} of {{itemsTotal}} files processed)...',
      searchingArtworksLabel: 'searching artworks...',
      searchingArtworksWithProgressLabel: 'searching artworks: {{percentage}}% ({{itemsComplete}} of {{itemsTotal}} files processed)...',
    },
  },
  userList: {
    creationDateColumn: 'Creation Date',
    updateDateColumn: 'Update Date',
    nameColumn: 'Name',
    emailColumn: 'Email',
    roleColumn: 'Role',
    actionsColumn: 'Actions',
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
    passwordLabel: 'Password:',
    roleLabel: 'Role:',
    newPasswordPlaceholder: 'Leave empty to keep old password'
  },
  currentUser: {
    header: 'My Profile',
    nameLabel: 'Name:',
    emailLabel: 'Email:',
    oldPasswordLabel: 'Old Password:',
    newPasswordLabel: 'New password:',
  },
  shared: {
    errorsHeader: 'Errors',
    loadingIndicatorLabel: 'Loading...',
    errorIndicatorLabel: 'Operation failed!',
    previousPageButton: '&laquo; Previous',
    nextPageButton: 'Next &raquo;',
    dateTimeFormat: 'yyyy-MM-dd hh:mm:ss',
    currentPageLabel: 'Page {{pageIndex}} of {{totalPages}}',
    creationDateLabel: 'Creation Date:',
    updateDateLabel: 'Update Date:',
    cancelButton: 'Cancel',
    saveButton: 'Save',
    scanJob: {
      startingStatus: 'STARTING',
      startedStatus: 'STARTED',
      completeStatus: 'COMPLETE',
      failedStatus: 'FAILED',
      interruptedStatus: 'INTERRUPTED',
    },
  },
  notification: {
    authenticationErrorTitle: 'Authentication Error',
    authenticationErrorText: 'Please sign in.',
    authorizationErrorTitle: 'Authorization Error',
    authorizationErrorText: 'Access denied.',
    scanJobTitle: 'Scan Job',
    scanJobStartedText: 'Running...',
    scanJobInterruptedText: 'Interrupted!',
    scanJobFinishedText: 'Complete!',
    scanJobFailedText: 'Failed!',
  },
  fieldViolation: {
    // Localized field violation messages can be defined here (code-message pairs).
  },
  error: {
    // Localized error messages can be defined here (code-message pairs).
  }
};
