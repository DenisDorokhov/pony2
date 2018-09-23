export default {
  noSongTitle: 'Pony | Music Streamer',
  songTitlePrefix: 'Pony | ',
  songTitleBody: '{{ artistName }} - {{ songName }} | ',
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
      refreshButton: 'Refresh',
      systemButton: 'System',
      settingsButton: 'Settings',
      scanningButton: 'Scanning',
      logButton: 'Log',
      usersButton: 'Users',
    },
    currentUser: {
      header: 'My Profile',
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
    songTitle: '{{ artistName }} - {{ songName }}',
    playbackFailed: 'Playback failed.',
    windowCloseConfirmation: 'Playback will stop after closing the window. Are you sure?',
  },
  shared: {
    errorsHeader: 'Errors',
    loadingIndicatorLabel: 'Loading...',
    errorIndicatorLabel: 'Loading failed!',
  },
  fieldViolation: {
    // Localized field violation messages can be defined here (code-message pairs).
  },
  error: {
    // Localized error messages can be defined here (code-message pairs).
  }
};
