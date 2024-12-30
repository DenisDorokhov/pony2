import {Component, OnInit} from '@angular/core';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {ConfigService} from '../../../service/config.service';
import {LoadingState} from '../../../domain/common.model';
import {ConfigDto} from '../../../domain/config.dto';
import {FormArray, FormBuilder, FormGroup, ReactiveFormsModule} from '@angular/forms';
import {ErrorDto} from '../../../domain/common.dto';
import {AutoFocusDirective} from '../../common/auto-focus.directive';
import {ErrorContainerComponent} from '../../common/error-container.component';
import {CommonModule} from '@angular/common';
import {ErrorIndicatorComponent} from '../../common/error-indicator.component';
import {LoadingIndicatorComponent} from '../../common/loading-indicator.component';
import {LibraryScanService} from '../../../service/library-scan.service';
import {NotificationService} from '../../../service/notification.service';
import {PlaylistService} from '../../../service/playlist.service';

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, ReactiveFormsModule, AutoFocusDirective, ErrorContainerComponent, ErrorIndicatorComponent, LoadingIndicatorComponent],
  selector: 'pony-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit{

  LoadingState = LoadingState;

  config: ConfigDto | undefined;

  form: FormGroup;
  formLibraryFolders: FormArray;
  error: ErrorDto | undefined;
  backupFileToRestore: File | undefined;

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

  createBackup() {
    this.secondaryLoadingState = LoadingState.LOADING;
    this.playlistService.backupPlaylists().subscribe({
      next: () => this.secondaryLoadingState = LoadingState.LOADED,
      error: () => this.secondaryLoadingState = LoadingState.ERROR
    });
  }

  restoreBackup() {
    this.secondaryLoadingState = LoadingState.LOADING;
    this.playlistService.restorePlaylists(this.backupFileToRestore!).subscribe({
      next: dto => {
        this.secondaryLoadingState = LoadingState.LOADED;
        let message = this.translateService.instant('settings.restoreBackupSuccess', { count: dto.userPlaylists.length });
        if (dto.notFoundUserEmails.length > 0) {
          message += '\n' + this.translateService.instant('settings.restoreBackupUserEmailsNotFound', { values: dto.notFoundUserEmails.join(', ') });
        }
        if (dto.notFoundSongPaths.length > 0) {
          message += '\n' + this.translateService.instant('settings.restoreBackupSongPathsNotFound', { values: dto.notFoundSongPaths.join(', ') });
        }
        window.alert(message);
      },
      error: () => this.secondaryLoadingState = LoadingState.ERROR
    });
  }

  onBackupFileChange(event: Event) {
    const fileList = (event.target as any).files as FileList;
    this.backupFileToRestore = fileList.item(0) ?? undefined;
  }
}
