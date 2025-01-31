import {Component, OnInit} from '@angular/core';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {ConfigService} from '../../../service/config.service';
import {LoadingState} from '../../../domain/common.model';
import {ConfigDto} from '../../../domain/config.dto';
import {FormArray, FormBuilder, FormGroup, FormsModule, ReactiveFormsModule} from '@angular/forms';
import {ErrorDto} from '../../../domain/common.dto';
import {AutoFocusDirective} from '../../common/auto-focus.directive';
import {ErrorContainerComponent} from '../../common/error-container.component';
import {CommonModule} from '@angular/common';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {LibraryScanService} from '../../../service/library-scan.service';
import {NotificationService} from '../../../service/notification.service';
import {PlaylistService} from '../../../service/playlist.service';
import {LibraryService} from '../../../service/library.service';
import {PlaybackHistoryService} from '../../../service/playback-history.service';

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, ReactiveFormsModule, AutoFocusDirective, ErrorContainerComponent, ErrorIndicatorComponent, LoadingIndicatorComponent, FormsModule],
  selector: 'pony-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit{

  LoadingState = LoadingState;

  config: ConfigDto | undefined;
  showAdvanced = false;

  form: FormGroup;
  formLibraryFolders: FormArray;
  error: ErrorDto | undefined;
  backupPlaylistsFileToRestore: File | undefined;
  backupHistoryFileToRestore: File | undefined;

  primaryLoadingState = LoadingState.LOADING;
  secondaryLoadingState = LoadingState.LOADED;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly configService: ConfigService,
    private readonly formBuilder: FormBuilder,
    private readonly translateService: TranslateService,
    private readonly libraryScanService: LibraryScanService,
    private readonly notificationService: NotificationService,
    private readonly playlistService: PlaylistService,
    private readonly libraryService: LibraryService,
    private readonly playbackHistoryService: PlaybackHistoryService,
  ) {
    this.formLibraryFolders = formBuilder.array([
      formBuilder.group({path: ''})
    ]);
    this.form = formBuilder.group({
      libraryFolders: this.formLibraryFolders,
    });
  }

  ngOnInit(): void {
    this.configService.getConfig().subscribe({
      next: config => {
        this.config = config;
        this.formLibraryFolders.clear();
        this.config.libraryFolders.forEach(next =>
          this.formLibraryFolders.push(this.formBuilder.group({path: next.path})));
        this.primaryLoadingState = LoadingState.LOADED;
      },
      error: () => {
        this.primaryLoadingState = LoadingState.ERROR;
      }
    });
  }

  addLibraryFolder() {
    this.formLibraryFolders.push(this.formBuilder.group({path: ''}));
  }

  removeLibraryFolder(i: number) {
    this.formLibraryFolders.removeAt(i);
  }

  save() {
    const configToSave = this.form.value as ConfigDto;
    this.primaryLoadingState = LoadingState.LOADING;
    this.configService.saveConfig(configToSave).subscribe({
      next: config => {
        this.primaryLoadingState = LoadingState.LOADED;
        if (JSON.stringify(this.config!.libraryFolders) !== JSON.stringify(config.libraryFolders)) {
          this.notificationService.success(
            this.translateService.instant('notification.settingsTitle'),
            this.translateService.instant('notification.settingsUpdatedText')
          );
          if (window.confirm(this.translateService.instant('settings.startScanJobConfirmation'))) {
            this.libraryScanService.startScanJob().subscribe();
          }
        }
        this.activeModal.close(config);
      },
      error: error => {
        this.error = error;
        this.primaryLoadingState = this.error?.code === ErrorDto.Code.VALIDATION ? LoadingState.LOADED : LoadingState.ERROR;
      }
    });
  }

  createPlaylistsBackup() {
    this.secondaryLoadingState = LoadingState.LOADING;
    this.playlistService.backupPlaylists().subscribe({
      next: () => this.secondaryLoadingState = LoadingState.LOADED,
      error: () => this.secondaryLoadingState = LoadingState.ERROR
    });
  }

  restorePlaylistsBackup() {
    this.secondaryLoadingState = LoadingState.LOADING;
    this.playlistService.restorePlaylists(this.backupPlaylistsFileToRestore!).subscribe({
      next: dto => {
        this.secondaryLoadingState = LoadingState.LOADED;
        let message = this.translateService.instant('settings.restorePlaylistsBackupSuccess', { count: dto.userPlaylists.length });
        if (dto.notFoundUserEmails.length > 0) {
          message += '\n' + this.translateService.instant('settings.restorePlaylistsBackupUserEmailsNotFound', { values: dto.notFoundUserEmails.join(', ') });
        }
        if (dto.notFoundSongPaths.length > 0) {
          message += '\n' + this.translateService.instant('settings.restorePlaylistsBackupSongPathsNotFound', { values: dto.notFoundSongPaths.join(', ') });
        }
        window.alert(message);
      },
      error: () => this.secondaryLoadingState = LoadingState.ERROR
    });
  }

  onPlaylistsBackupFileChange(event: Event) {
    const fileList = (event.target as any).files as FileList;
    this.backupPlaylistsFileToRestore = fileList.item(0) ?? undefined;
  }

  createHistoryBackup() {
    this.secondaryLoadingState = LoadingState.LOADING;
    this.playbackHistoryService.backupHistory().subscribe({
      next: () => this.secondaryLoadingState = LoadingState.LOADED,
      error: () => this.secondaryLoadingState = LoadingState.ERROR
    });
  }

  restoreHistoryBackup() {
    this.secondaryLoadingState = LoadingState.LOADING;
    this.playbackHistoryService.restoreHistory(this.backupHistoryFileToRestore!).subscribe({
      next: dto => {
        this.secondaryLoadingState = LoadingState.LOADED;
        let message = this.translateService.instant('settings.restoreHistoryBackupSuccess', { count: dto.restoredSongCount });
        if (dto.notFoundUserEmails.length > 0) {
          message += '\n' + this.translateService.instant('settings.restoreHistoryBackupUserEmailsNotFound', { values: dto.notFoundUserEmails.join(', ') });
        }
        if (dto.notFoundSongCount > 0) {
          message += '\n' + this.translateService.instant('settings.restoreHistoryBackupSongNotFoundCount', { count: dto.notFoundSongCount });
        }
        window.alert(message);
      },
      error: () => this.secondaryLoadingState = LoadingState.ERROR
    });
  }

  onHistoryBackupFileChange(event: Event) {
    const fileList = (event.target as any).files as FileList;
    this.backupHistoryFileToRestore = fileList.item(0) ?? undefined;
  }

  reBuildSearchIndex() {
    this.secondaryLoadingState = LoadingState.LOADING;
    this.libraryService.reBuildSearchIndex().subscribe({
      next: () => {
        this.secondaryLoadingState = LoadingState.LOADED;
        this.notificationService.success(
          this.translateService.instant('notification.settingsTitle'),
          this.translateService.instant('notification.reBuildIndexStartedText')
        );
      },
      error: () => this.secondaryLoadingState = LoadingState.ERROR
    });
  }
}
