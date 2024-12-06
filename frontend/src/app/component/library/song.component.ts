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
import {Song} from "../../domain/library.model";
import {PlaybackService} from "../../service/playback.service";
import {TranslateModule} from "@ngx-translate/core";
import {LibraryService} from "../../service/library.service";
import {ScrollingUtils} from "../../utils/scrolling.utils";
import {UnknownSongPipe} from "../../pipe/unknown-song.pipe";
import {UnknownArtistPipe} from "../../pipe/unknown-artist.pipe";
import {resolveAppViewContainerRef} from "../../utils/view.utils";
import {PlaybackState} from "../../service/audio-player.service";

@Component({
  standalone: true,
  imports: [TranslateModule, UnknownSongPipe, UnknownArtistPipe],
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

  showMenuButton = false;
  showMenu = false;

  get song(): Song {
    return this._song;
  }

  @Input()
  set song(song: Song) {
    this._song = song;
    this.selected = this.libraryService.selectedSong?.id === this.song.id;
    if (this.playbackService.lastPlaybackEvent.song?.id === this.song.id) {
      this.playbackState = this.playbackService.lastPlaybackEvent.state;
    } else {
      this.playbackState = undefined;
    }
  }

  @Input() showArtist = false;

  @ViewChild('container') containerElement!: ElementRef;
  selected = false;

  playbackState: PlaybackState | undefined;

  private menuEmbeddedViewRef: EmbeddedViewRef<any> | undefined;
  private subscriptions: Subscription[] = [];

  constructor(
    private libraryService: LibraryService,
    private playbackService: PlaybackService,
    private applicationRef: ApplicationRef,
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
          this.playbackState = playbackEvent.state;
        } else {
          this.playbackState = undefined;
        }
      }));
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

  play() {
    this.libraryService.requestSongPlayback(this.song);
  }

  onMouseDown(event: MouseEvent) {
    // Disable text selection on double click.
    if (event.detail > 1) {
      event.preventDefault();
    }
  }

  onMouseMove() {
    this.showMenuButton = true;
  }

  onMouseLeave() {
    this.showMenuButton = false;
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
}
