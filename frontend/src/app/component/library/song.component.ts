import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  EventEmitter,
  inject,
  input,
  linkedSignal,
  OnInit,
  Output,
  signal,
  ViewChild
} from '@angular/core';
import {takeUntilDestroyed, toSignal} from '@angular/core/rxjs-interop';
import {fromEvent} from 'rxjs';
import {map} from 'rxjs/operators';
import {Playlist, Song} from '../../domain/library.model';
import {PlaybackService} from '../../service/playback.service';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {LibraryService} from '../../service/library.service';
import {ScrollingUtils} from '../../utils/scrolling.utils';
import {UnknownSongPipe} from '../../pipe/unknown-song.pipe';
import {UnknownArtistPipe} from '../../pipe/unknown-artist.pipe';
import {PlaybackEvent, PlaybackState} from '../../service/audio-player.service';
import {UnknownGenrePipe} from '../../pipe/unknown-genre.pipe';
import {PlaylistService} from '../../service/playlist.service';
import {NotificationService} from '../../service/notification.service';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu,
  NgbDropdownToggle,
  NgbModal
} from '@ng-bootstrap/ng-bootstrap';
import {PlaylistEditComponent} from './modal/playlist-edit.component';
import {PlaylistAddSongComponent} from './modal/playlist-add-song.component';
import {PlaylistDto} from '../../domain/library.dto';
import {DropdownCloseOnScrollDirective} from '../common/dropdown-close-on-scroll.directive';

@Component({
    imports: [TranslateModule, UnknownSongPipe, UnknownArtistPipe, UnknownGenrePipe, NgbDropdown, NgbDropdownButtonItem, NgbDropdownItem, NgbDropdownMenu, NgbDropdownToggle, DropdownCloseOnScrollDirective],
    selector: 'pony-song',
    templateUrl: './song.component.html',
    styleUrls: ['./song.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class SongComponent implements OnInit, AfterViewInit {

  PlaybackState = PlaybackState;

  private readonly libraryService = inject(LibraryService);
  private readonly playbackService = inject(PlaybackService);
  private readonly playlistService = inject(PlaylistService);
  private readonly notificationService = inject(NotificationService);
  private readonly translateService = inject(TranslateService);
  private readonly modal = inject(NgbModal);
  private readonly destroyRef = inject(DestroyRef);

  readonly song = input.required<Song>();
  readonly showArtist = input(false);

  @Output() switchQueueRequested = new EventEmitter<Song>();

  @ViewChild('container') containerElement!: ElementRef;
  @ViewChild('menuDropdown') menuDropdown!: NgbDropdown;

  private readonly selectedSong = signal(this.libraryService.selectedSong);
  private readonly playbackEvent = toSignal(this.playbackService.observePlaybackEvent(), {requireSync: true});
  private readonly likePlaylist = toSignal(this.playlistService.observeLikePlaylist(), {
    requireSync: true,
    // Every playlist notification must reset the optimistic like state.
    equal: () => false,
  });
  private readonly artists = toSignal(this.libraryService.observeArtists(), {requireSync: true});

  readonly isMouseOver = signal(false);
  readonly topPlaylists = toSignal(this.playlistService.observePlaylists().pipe(
    map(() => this.playlistService.getTopPlaylists(PlaylistDto.Type.NORMAL)),
  ), {requireSync: true});
  readonly isLikedSong = linkedSignal({
    source: () => ({song: this.song(), likePlaylist: this.likePlaylist()}),
    computation: ({song}) => this.playlistService.isLikedSong(song.id),
  });
  readonly isTopTrack = computed(() => {
    this.artists();
    return this.libraryService.isTopTrack(this.song());
  });
  readonly selected = computed(() => this.selectedSong()?.id === this.song().id);
  readonly lastPlaybackEvent = computed<PlaybackEvent | undefined>(() => {
    const playbackEvent = this.playbackEvent();
    return playbackEvent.song?.id === this.song().id ? playbackEvent : undefined;
  });

  ngOnInit(): void {
    this.libraryService.observeSelectedSong()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(songSelection => {
        this.selectedSong.set(songSelection?.song);
        if (this.selected()) {
          if (songSelection?.play) {
            if (this.song().id !== this.playbackService.currentSong?.id) {
              this.libraryService.requestSongPlayback(this.song());
            } else if (this.playbackService.currentPlaybackState !== PlaybackState.PLAYING) {
              this.playbackService.playOrPause();
            }
          }
          if (this.containerElement && !this.menuDropdown?.isOpen()) {
            this.containerElement.nativeElement.focus();
          }
          this.libraryService.finishSongSelection();
        }
      });
  }

  ngAfterViewInit(): void {
    if (this.selected()) {
      this.containerElement.nativeElement.focus();
    }
    this.libraryService.observeScrollToSongRequest()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(request => {
        if (request.song.id === this.song().id) {
          ScrollingUtils.scrollIntoElement(this.containerElement.nativeElement);
          this.libraryService.finishScrollToSong();
        }
      });
    fromEvent<KeyboardEvent>(this.containerElement.nativeElement, 'keydown')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(event => {
        if (event.code === 'Enter') {
          this.select();
          this.libraryService.requestSongPlayback(this.song());
          event.preventDefault();
        }
      });
  }

  select(event?: MouseEvent) {
    if (event?.altKey) {
      event.preventDefault();
      return;
    }
    this.libraryService.selectSong(this.song());
  }

  onDoubleClick(event: MouseEvent) {
    let checkElement: Element | undefined = event.target as Element;
    let buttonClick = false;
    do {
      buttonClick = checkElement?.tagName === 'BUTTON';
      checkElement = checkElement.parentNode as Element;
    } while (!buttonClick && checkElement);
    if (!buttonClick && !event.altKey) {
      this.libraryService.requestSongPlayback(this.song());
    }
  }

  onMouseDown(event: MouseEvent) {
    // Disable text selection on double click.
    if (event.detail > 1 && !event.altKey) {
      event.preventDefault();
    }
  }

  onMouseMove() {
    this.isMouseOver.set(true);
  }

  onMouseLeave() {
    this.isMouseOver.set(false);
  }

  playNext() {
    this.playbackService.playNext(this.song());
  }

  addToQueue() {
    this.playbackService.addToQueue(this.song());
  }

  createQueue() {
    this.playbackService.createQueue(this.song());
  }

  switchQueue() {
    this.switchQueueRequested.emit(this.song());
  }

  onPlaybackClick() {
    if (this.lastPlaybackEvent()?.song?.id === this.song().id) {
      this.playbackService.playOrPause();
    } else {
      this.libraryService.requestSongPlayback(this.song());
    }
  }

  selectOrCreatePlaylist() {
    if (this.topPlaylists().length > 0) {
      const modalRef = this.modal.open(PlaylistAddSongComponent);
      const playlistAddSongComponent: PlaylistAddSongComponent = modalRef.componentInstance;
      playlistAddSongComponent.songs = [this.song()];
    } else {
      const modalRef = this.modal.open(PlaylistEditComponent);
      const playlistEditComponent: PlaylistEditComponent = modalRef.componentInstance;
      playlistEditComponent.songs = [this.song()];
    }
  }

  addToPlaylist(playlist: Playlist) {
    this.playlistService.addSongToPlaylist(playlist.id, this.song().id).subscribe({
      error: () => this.notificationService.error(
        this.translateService.instant('library.song.addToPlaylistNotificationTitle'),
        this.translateService.instant('library.song.addToPlaylistNotificationTextFailure'),
      ),
    });
  }

  onLikeClick() {
    if (this.isLikedSong()) {
      this.isLikedSong.set(false);
      this.playlistService.unlikeSong(this.song().id).subscribe({
        error: () => {
          this.isLikedSong.set(true);
          this.notificationService.error(
            this.translateService.instant('library.song.unlikeNotificationTitle'),
            this.translateService.instant('library.song.unlikeNotificationTextFailure')
          );
        }
      });
    } else {
      this.isLikedSong.set(true);
      this.playlistService.likeSong(this.song().id).subscribe({
        error: () => {
          this.isLikedSong.set(false);
          this.notificationService.error(
            this.translateService.instant('library.song.likeNotificationTitle'),
            this.translateService.instant('library.song.likeNotificationTextFailure')
          );
        }
      });
    }
  }

  download() {
    this.libraryService.downloadSong(this.song().id);
  }
}
