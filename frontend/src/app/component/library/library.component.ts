import {Component, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {Subscription} from 'rxjs';
import {ScanStatisticsDto} from '../../domain/library.dto';
import {AuthenticationService} from '../../service/authentication.service';
import {LibraryScanService} from '../../service/library-scan.service';
import {CommonModule} from '@angular/common';
import {PlayerComponent} from './player.component';
import {ToolbarComponent} from './toolbar.component';
import {ArtistListComponent} from './artist-list.component';
import {AlbumListComponent} from './album-list.component';
import {formatDuration, formatFileSize} from '../../utils/format.utils';
import {BrowserNotificationService} from '../../service/browser-notification.service';

@Component({
    imports: [TranslateModule, CommonModule, PlayerComponent, ToolbarComponent, ArtistListComponent, AlbumListComponent],
    selector: 'pony-library',
    templateUrl: './library.component.html',
    styleUrls: ['./library.component.scss']
})
export class LibraryComponent implements OnInit, OnDestroy {

  private loggedOutSubscription: Subscription | undefined;
  private scanStatisticsSubscription: Subscription | undefined;

  scanStatistics: ScanStatisticsDto | undefined | null;
  size: string | undefined;
  duration: string | undefined;

  constructor(
    private router: Router,
    private authenticationService: AuthenticationService,
    private libraryScanService: LibraryScanService,
    private translateService: TranslateService,
    private browserNotificationService: BrowserNotificationService,
  ) {
  }

  ngOnInit(): void {
    this.loggedOutSubscription = this.authenticationService.observeLogout()
      .subscribe(user => {
        console.info(`User ${user?.email} has been logged out.`);
        this.router.navigate(['/login'], {replaceUrl: true});
      });
    this.scanStatisticsSubscription = this.libraryScanService.observeScanStatistics()
      .subscribe(scanStatistics => {
        this.scanStatistics = scanStatistics;
        this.size = this.calculateSize();
        this.duration = this.calculateDuration();
      });
    this.libraryScanService.updateScanStatistics().subscribe();
    this.browserNotificationService.requestPermission().subscribe();
  }

  ngOnDestroy(): void {
    this.loggedOutSubscription?.unsubscribe();
    this.scanStatisticsSubscription?.unsubscribe();
  }

  private calculateSize(): string | undefined {
    return this.scanStatistics ? formatFileSize(this.scanStatistics.songSize, this.translateService) : undefined;
  }

  private calculateDuration(): string | undefined {
    return this.scanStatistics ? formatDuration(this.scanStatistics.duration, this.translateService) : undefined;
  }
}
