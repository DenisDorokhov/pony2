export default {
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
    installationSecretPlaceholder: 'Get from file ~/.pony2/installationSecret',
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
    noMusicLabel: 'Your library is empty :-(',
    album: {
      discLabel: 'Disc {{discNumber}}',
      unknownLabel: 'Unknown'
    },
    artist: {
      unknownLabel: 'Unknown'
    },
    song: {
      unknownLabel: 'Unknown'
    }
  },
  player: {
    noSongTitle: 'Pony - Music Streamer',
    songTitle: '{{ artistName }} - {{ songName }}',
    playbackFailed: 'Playback failed.',
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
