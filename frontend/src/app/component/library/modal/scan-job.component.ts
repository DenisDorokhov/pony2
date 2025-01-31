import {Component, Input, OnInit} from '@angular/core';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {LogMessageDto, ScanJobDto} from '../../../domain/library.dto';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {formatDuration, formatFileSize} from '../../../utils/format.utils';

@Component({
    imports: [TranslateModule, CommonModule],
    selector: 'pony-scan-job',
    templateUrl: './scan-job.component.html',
    styleUrls: ['./scan-job.component.scss']
})
export class ScanJobComponent implements OnInit {

  ScanJobStatus = ScanJobDto.Status;
  LogMessageLevel = LogMessageDto.Level;

  @Input()
  scanJob!: ScanJobDto;

  result: string | undefined;
  duration: string | undefined;
  songSize: string | undefined;
  exception: string | undefined;

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly translateService: TranslateService,
  ) {
  }

  ngOnInit(): void {
    this.exception = LogMessageDto.extractException(this.scanJob.logMessage);
    if (this.scanJob.scanResult) {
      this.duration = formatDuration(this.scanJob.scanResult!.duration, this.translateService);
      this.songSize = formatFileSize(this.scanJob.scanResult!.songSize, this.translateService);
      const resultEntries: string[] = [];
      if (this.scanJob.scanResult.createdArtistCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.createdArtistCount', {value: this.scanJob.scanResult.createdArtistCount}));
      }
      if (this.scanJob.scanResult.updatedArtistCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.updatedArtistCount', {value: this.scanJob.scanResult.updatedArtistCount}));
      }
      if (this.scanJob.scanResult.deletedArtistCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.deletedArtistCount', {value: this.scanJob.scanResult.deletedArtistCount}));
      }
      if (this.scanJob.scanResult.createdAlbumCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.createdAlbumCount', {value: this.scanJob.scanResult.createdAlbumCount}));
      }
      if (this.scanJob.scanResult.updatedAlbumCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.updatedAlbumCount', {value: this.scanJob.scanResult.updatedAlbumCount}));
      }
      if (this.scanJob.scanResult.deletedAlbumCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.deletedAlbumCount', {value: this.scanJob.scanResult.deletedAlbumCount}));
      }
      if (this.scanJob.scanResult.createdGenreCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.createdGenreCount', {value: this.scanJob.scanResult.createdGenreCount}));
      }
      if (this.scanJob.scanResult.updatedGenreCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.updatedGenreCount', {value: this.scanJob.scanResult.updatedGenreCount}));
      }
      if (this.scanJob.scanResult.deletedGenreCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.deletedGenreCount', {value: this.scanJob.scanResult.deletedGenreCount}));
      }
      if (this.scanJob.scanResult.createdSongCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.createdSongCount', {value: this.scanJob.scanResult.createdSongCount}));
      }
      if (this.scanJob.scanResult.updatedSongCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.updatedSongCount', {value: this.scanJob.scanResult.updatedSongCount}));
      }
      if (this.scanJob.scanResult.deletedSongCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.deletedSongCount', {value: this.scanJob.scanResult.deletedSongCount}));
      }
      if (this.scanJob.scanResult.createdArtworkCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.createdArtworkCount', {value: this.scanJob.scanResult.createdArtworkCount}));
      }
      if (this.scanJob.scanResult.deletedArtworkCount) {
        resultEntries.push(this.translateService.instant('scanJob.scanResult.deletedArtworkCount', {value: this.scanJob.scanResult.deletedArtworkCount}));
      }
      this.result = resultEntries.length > 0 ? resultEntries.join(', ') : 'none';
    }
  }

  protected readonly LogMessageDto = LogMessageDto;
}
