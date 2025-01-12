import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {CommonModule} from '@angular/common';
import {LargeSongComponent} from './common/large-song.component';
import {ArtistSongs, Song} from '../../../domain/library.model';
import {PlaylistService} from '../../../service/playlist.service';
import {Subscription} from 'rxjs';
import {CdkFixedSizeVirtualScroll, CdkVirtualForOf, CdkVirtualScrollViewport} from '@angular/cdk/scrolling';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {PlaybackService} from '../../../service/playback.service';
import {PlaybackEvent} from '../../../service/audio-player.service';
import {LibraryService} from '../../../service/library.service';
import {NotificationService} from '../../../service/notification.service';

@Component({
  standalone: true,
  imports: [TranslateModule, CommonModule, LargeSongComponent, CdkFixedSizeVirtualScroll, CdkVirtualForOf, CdkVirtualScrollViewport],
  selector: 'pony-artist-likes',
  templateUrl: './artist-likes.component.html',
  styleUrls: ['./artist-likes.component.scss']
})
export class ArtistLikesComponent implements OnInit, OnDestroy {

  readonly rowHeight = LargeSongComponent.HEIGHT;

  @Input()
  artistSongs!: ArtistSongs;

  songs: Song[] = [];
  lastPlaybackEvent: PlaybackEvent | undefined;
  selectedIndex = -1;

  private subscriptions: Subscription[] = [];

  constructor(
    public readonly activeModal: NgbActiveModal,
    private readonly playlistService: PlaylistService,
    private readonly playbackService: PlaybackService,
    private readonly libraryService: LibraryService,
    private readonly notificationService: NotificationService,
    private readonly translateService: TranslateService,
  ) {
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(next => next.unsubscribe());
  }

  ngOnInit(): void {
    this.subscriptions.push(this.playlistService.observeLikePlaylist().subscribe(likePlaylist =>
      this.songs = likePlaylist.songs
        .map(next => next.song)
        .filter(song => song.album.artist.id === this.artistSongs.artist.id)
    ));
    this.subscriptions.push(this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => this.lastPlaybackEvent = playbackEvent));
  }

  switchQueue(index: number) {
    const songs: Song[] = [...this.songs];
    this.artistSongs.albumSongs.forEach(albumSongs =>
      albumSongs.songs.forEach(albumSong => songs.push(albumSong)));
    this.playbackService.switchQueue(songs, index);
  }

  selectIndex(i: number) {
    this.selectedIndex = i;
  }

  onPlaybackClick(index: number) {
    if (this.songs[index].id === this.lastPlaybackEvent?.song?.id) {
      this.playbackService.playOrPause();
    } else {
      this.switchQueue(index);
    }
  }

  goToSong(song: Song) {
    this.libraryService.selectArtistAndMakeDefault(song.album.artist);
    this.libraryService.selectSong(song);
    this.libraryService.requestScrollToSong(song);
    this.activeModal.close();
  }

  onRemovalRequested(song: Song) {
    this.playlistService.unlikeSong(song.id).subscribe({
      error: () => {
        this.notificationService.error(
          this.translateService.instant('artistLikes.unlikeNotificationTitle'),
          this.translateService.instant('artistLikes.unlikeNotificationTextFailure')
        );
      }
    });
  }
}
