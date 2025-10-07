import {Injectable} from '@angular/core';
import {TranslateService} from '@ngx-translate/core';
import {Song} from '../domain/library.model';
import {from, Observable, of} from 'rxjs';
import {tap} from 'rxjs/operators';
import {isMobileBrowser} from '../utils/mobile.utils';
import {TokenStorageService} from './token-storage.service';

@Injectable({
  providedIn: 'root'
})
export class BrowserNotificationService {

  private appInForeground = true;

  constructor(
    private translateService: TranslateService,
    private tokenStorageService: TokenStorageService,
  ) {
    window.addEventListener('blur', () => {
      this.appInForeground = false;
    });
    window.addEventListener('focus', () => {
      this.appInForeground = true;
    });
  }

  requestPermission(): Observable<NotificationPermission> {
    if (isMobileBrowser()) {
      return of('default');
    }
    return from(Notification.requestPermission()).pipe(
      tap(permission => {
        if (permission !== 'granted') {
          console.info('Notifications disabled.');
        } else {
          console.info('Notification permission granted.');
        }
      })
    );
  }

  showSongNotification(song: Song) {
    if (this.appInForeground) {
      return;
    }
    if (isMobileBrowser()) {
      console.debug('Not showing browser notification: mobile browser.');
      return;
    }
    const artistName = song.artistName ?? this.translateService.instant('library.artist.unknownLabel');
    const songName = song.name ?? this.translateService.instant('library.song.unknownLabel');
    let albumName = song.album.name ?? this.translateService.instant('library.album.unknownLabel');
    if (song.album.year) {
      albumName += ' (' + song.album.year + ')';
    }
    Notification.requestPermission().then(permission => {
      if (permission === 'granted') {
        let absoluteArtworkUrl: string | undefined = undefined;
        if (song.album.smallArtworkUrl) {
          const artworkUrl = song.album.smallArtworkUrl + '?apiKey=' + (this.tokenStorageService.staticToken ?? '');
          absoluteArtworkUrl = new URL(artworkUrl, document.baseURI).href;
        }
        const notification = new Notification(this.translateService.instant('player.songTitle', {artistName, songName}),
          {
            icon: absoluteArtworkUrl,
            body: albumName
          });
        notification.onclick = () => {
          window.parent.parent.focus();
        };
      }
    });
  }
}
