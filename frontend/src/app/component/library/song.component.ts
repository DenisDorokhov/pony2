import {
  AfterViewInit,
  ApplicationRef,
  Component,
  ElementRef,
  EmbeddedViewRef,
  Input,
  OnDestroy,
  OnInit,
  TemplateRef,
  ViewChild
} from '@angular/core';
import {fromEvent, Subscription} from 'rxjs';
import {Playlist, Song} from "../../domain/library.model";
import {PlaybackService} from "../../service/playback.service";
import {TranslateModule, TranslateService} from "@ngx-translate/core";
import {LibraryService} from "../../service/library.service";
import {ScrollingUtils} from "../../utils/scrolling.utils";
import {UnknownSongPipe} from "../../pipe/unknown-song.pipe";
import {UnknownArtistPipe} from "../../pipe/unknown-artist.pipe";
import {resolveAppViewContainerRef} from "../../utils/view.utils";
import {PlaybackEvent, PlaybackState} from "../../service/audio-player.service";
import {UnknownGenrePipe} from "../../pipe/unknown-genre.pipe";
import {PlaylistService} from "../../service/playlist.service";
import {NotificationService} from "../../service/notification.service";
import {NgbModal} from "@ng-bootstrap/ng-bootstrap";
import {PlaylistEditComponent} from "./modal/playlist-edit.component";
import {PlaylistAddSongComponent} from "./modal/playlist-add-song.component";

@Component({
  standalone: true,
  imports: [TranslateModule, UnknownSongPipe, UnknownArtistPipe, UnknownGenrePipe],
  selector: 'pony-song',
  templateUrl: './song.component.html',
  styleUrls: ['./song.component.scss']
})
export class SongComponent implements OnInit, OnDestroy, AfterViewInit {

  PlaybackState = PlaybackState;

  @ViewChild('menuButton') menuButtonElement!: ElementRef;
  @ViewChild('menuContainer') menuContainerElement!: ElementRef;
  @ViewChild('menu', {read: TemplateRef}) menuTemplate!: TemplateRef<unknown>;

  private _song!: Song;

  isMouseOver = false;
  showMenu = false;
  topPlaylists: Playlist[] = [];

  get song(): Song {
    return this._song;
  }

  @Input()
  set song(song: Song) {
    this._song = song;
    this.selected = this.libraryService.selectedSong?.id === this.song.id;
    if (this.playbackService.lastPlaybackEvent.song?.id === this.song.id) {
      this.lastPlaybackEvent = this.playbackService.lastPlaybackEvent;
    } else {
      this.lastPlaybackEvent = undefined;
    }
  }

  @Input() showArtist = false;

  @ViewChild('container') containerElement!: ElementRef;
  selected = false;

  lastPlaybackEvent: PlaybackEvent | undefined;

  private menuEmbeddedViewRef: EmbeddedViewRef<any> | undefined;
  private subscriptions: Subscription[] = [];

  constructor(
    private readonly libraryService: LibraryService,
    private readonly playbackService: PlaybackService,
    private readonly applicationRef: ApplicationRef,
    private readonly playlistService: PlaylistService,
    private readonly notificationService: NotificationService,
    private readonly translateService: TranslateService,
    private readonly modal: NgbModal,
  ) {
  }

  ngOnInit(): void {
    this.subscriptions.push(this.libraryService.observeSelectedSong()
      .subscribe(song => {
        this.selected = song != null && song.id === this.song.id;
        if (this.selected && this.containerElement) {
          this.containerElement.nativeElement.focus();
        }
      }));
    this.subscriptions.push(this.playbackService.observePlaybackEvent()
      .subscribe(playbackEvent => {
        if (playbackEvent.song && playbackEvent.song.id === this.song.id) {
          this.lastPlaybackEvent = playbackEvent;
        } else {
          this.lastPlaybackEvent = undefined;
        }
      }));
    this.subscriptions.push(this.playlistService.observePlaylists()
      .subscribe(() => this.topPlaylists = this.playlistService.getTopPlaylists()));
  }

  ngAfterViewInit(): void {
    if (this.selected) {
      this.containerElement.nativeElement.focus();
    }
    this.subscriptions.push(this.libraryService.observeScrollToSongRequest()
      .subscribe(song => {
        if (song.id === this.song.id) {
          ScrollingUtils.scrollIntoElement(this.containerElement.nativeElement);
          this.libraryService.finishScrollToSong();
        }
      }));
    this.subscriptions.push(fromEvent<KeyboardEvent>(this.containerElement.nativeElement, 'keydown').subscribe(event => {
      if (event.code === 'Enter') {
        this.select();
        this.libraryService.requestSongPlayback(this.song);
        event.preventDefault();
      }
    }));
    this.subscriptions.push(fromEvent(window.document.body, 'mousedown').subscribe(event =>
      this.checkIfClickWithinContainer(event)));
    this.subscriptions.push(fromEvent(window.document.body, 'touchstart').subscribe(event =>
      this.checkIfClickWithinContainer(event)));
    this.subscriptions.push(fromEvent(window.document.body, 'mousewheel').subscribe(() =>
      this.hideMenu()));
  }

  private checkIfClickWithinContainer(event: Event) {
    if (this.menuContainerElement?.nativeElement) {
      let checkElement: Node | null = event.target as Node;
      let clickWithinContainer = false;
      do {
        clickWithinContainer = this.menuContainerElement.nativeElement === checkElement;
        checkElement = (checkElement as Node).parentNode;
      } while (!clickWithinContainer && checkElement);
      if (!clickWithinContainer) {
        this.hideMenu();
      }
    }
  }

  private hideMenu(): void {
    this.menuEmbeddedViewRef?.destroy();
    this.menuEmbeddedViewRef = undefined;
    this.showMenu = false;
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(next => next.unsubscribe());
  }

  select() {
    this.libraryService.selectSong(this.song);
  }

  onDoubleClick(event: MouseEvent) {
    let checkElement: Element | undefined = event.target as Element;
    let buttonClick = false;
    do {
      buttonClick = checkElement?.tagName === 'BUTTON';
      checkElement = checkElement.parentNode as Element;
    } while (!buttonClick && checkElement);
    if (!buttonClick) {
      this.libraryService.requestSongPlayback(this.song);
    }
  }

  onMouseDown(event: MouseEvent) {
    // Disable text selection on double click.
    if (event.detail > 1) {
      event.preventDefault();
    }
  }

  onMouseMove() {
    this.isMouseOver = true;
  }

  onMouseLeave() {
    this.isMouseOver = false;
  }

  onMenuClick() {
    // Here we improvise instead of using standard Bootstrap dropdown functionality, as it will be affected by column layout.
    this.showMenu = true;
    const viewContainerRef = resolveAppViewContainerRef(this.applicationRef);
    this.menuEmbeddedViewRef = viewContainerRef.createEmbeddedView(this.menuTemplate);
    const menu = this.menuEmbeddedViewRef.rootNodes[0] as HTMLElement;
    menu.style.visibility = 'hidden';
    setTimeout(() => this.adjustMenuPosition());
  }

  private adjustMenuPosition() {
    const menuContainer = this.menuContainerElement.nativeElement as HTMLElement;
    const menuContainerRect = menuContainer.getBoundingClientRect();
    const menuButton = this.menuButtonElement.nativeElement as HTMLElement;
    const menuButtonRect = menuButton.getBoundingClientRect();
    const menu = this.menuEmbeddedViewRef!.rootNodes[0] as HTMLElement;
    if (menuButtonRect.top + menuButtonRect.height + menuContainerRect.height > window.innerHeight) {
      const menuTop = menuButtonRect.top - menuContainerRect.height;
      menu.style.top = menuTop + 'px';
    } else {
      const menuTop = menuButtonRect.top + menuButtonRect.height;
      menu.style.top = menuTop + 'px';
    }
    const menuLeft = menuButtonRect.left - menuContainerRect.width + menuButtonRect.width;
    menu.style.left = menuLeft + 'px';
    menu.style.visibility = 'visible';
  }

  playNext() {
    this.playbackService.playNext(this.song);
    this.hideMenu();
  }

  addToQueue() {
    this.playbackService.addToQueue(this.song);
    this.hideMenu();
  }

  createQueue() {
    this.playbackService.createQueue(this.song);
    this.hideMenu();
  }

  onPlaybackClick() {
    if (this.lastPlaybackEvent?.song?.id === this.song.id) {
      this.playbackService.playOrPause();
    } else {
      this.libraryService.requestSongPlayback(this.song);
    }
  }

  selectOrCreatePlaylist() {
    this.hideMenu();
    if (this.topPlaylists.length > 0) {
      const modalRef = this.modal.open(PlaylistAddSongComponent);
      const playlistAddSongComponent: PlaylistAddSongComponent = modalRef.componentInstance;
      playlistAddSongComponent.song = this.song;
    } else {
      const modalRef = this.modal.open(PlaylistEditComponent);
      const playlistEditComponent: PlaylistEditComponent = modalRef.componentInstance;
      playlistEditComponent.songs = [this.song];
    }
  }

  addToPlaylist(playlist: Playlist) {
    this.hideMenu();
    this.playlistService.addToPlaylist(playlist.id, this.song.id).subscribe({
      error: () => this.notificationService.error(
        this.translateService.instant('library.song.addToPlaylistNotificationTitle'),
        this.translateService.instant('library.song.addToPlaylistNotificationTextFailure'),
      ),
    });
  }
}
