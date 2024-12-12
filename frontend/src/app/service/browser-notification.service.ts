import {Injectable} from "@angular/core";
import {TranslateService} from "@ngx-translate/core";
import {Song} from "../domain/library.model";

@Injectable({
  providedIn: 'root'
})
export class BrowserNotificationService {

  private appInForeground = true;

  constructor(
    private translateService: TranslateService,
  ) {
    document.addEventListener("visibilitychange", () =>
      this.appInForeground = !document.hidden);
  }

  showSongNotification(song: Song) {
    if (this.appInForeground) {
      return;
    }
    const artistName = song.artistName ?? this.translateService.instant('library.artist.unknownLabel');
    const songName = song.name ?? this.translateService.instant('library.song.unknownLabel');
    let albumName = song.album.name ?? this.translateService.instant('library.album.unknownLabel');
    if (song.album.year) {
      albumName += ' (' + song.album.year + ')';
    }
    Notification.requestPermission().then(status => {
      if (status !== 'granted') {
        console.info('Browser notifications disabled.');
      }
      const notification = new Notification(this.translateService.instant('player.noSongTitle'),
        {
          icon: song.album.largeArtworkUrl,
          body: this.translateService.instant('player.songTitle', {artistName, songName}) + '\n' +
            albumName
        });
      notification.onclick = () => {
        window.parent.parent.focus();
      };
    });
  }
}
