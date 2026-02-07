import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  QueryList,
  ViewChildren
} from '@angular/core';
import {AlbumSongs, Playlist, Song} from '../../domain/library.model';
import {SongListComponent} from './song-list.component';
import {TranslateModule, TranslateService} from '@ngx-translate/core';
import {ImageLoaderComponent} from '../common/image-loader.component';
import {CommonModule} from '@angular/common';
import {LibraryService} from '../../service/library.service';
import {fromEvent, Subscription} from 'rxjs';
import {UnknownAlbumPipe} from '../../pipe/unknown-album.pipe';
import {ArtworkComponent} from './modal/artwork.component';
import {
  NgbDropdown,
  NgbDropdownButtonItem,
  NgbDropdownItem,
  NgbDropdownMenu, NgbDropdownToggle,
  NgbModal
} from '@ng-bootstrap/ng-bootstrap';
import {formatDuration} from '../../utils/format.utils';
import {UnknownArtistPipe} from '../../pipe/unknown-artist.pipe';
import {shouldShowNewIndicator} from '../../utils/indicator.utils';
import {InstallationService} from '../../service/installation.service';
import {PlaylistDto} from '../../domain/library.dto';
import {PlaylistService} from '../../service/playlist.service';
import {PlaylistAddSongComponent} from './modal/playlist-add-song.component';
import {PlaylistEditComponent} from './modal/playlist-edit.component';
import {PlaybackService} from '../../service/playback.service';
import {NotificationService} from '../../service/notification.service';

interface Disc {
  discNumber: number | undefined;
  showArtist: boolean;
  songs: Song[];
}

function compareDiscs(disc1: Disc, disc2: Disc) {
  if ((disc1.discNumber ?? 0) > (disc2.discNumber ?? 0)) {
    return 1;
  } else {
    return -1;
  }
}

function nullSafeNormalizedEquals(value1: string | undefined, value2: string | undefined): boolean {
  if (value1 == null && value2 == null) {
    return true;
  }
  if (value1 == null || value2 == null) {
    return false;
  }
  const normalizedValue1 = value1.trim().toLowerCase();
  const normalizedValue2 = value2.trim().toLowerCase();
  return normalizedValue1 === normalizedValue2;
}

@Component({
  imports: [CommonModule, TranslateModule, ImageLoaderComponent, SongListComponent, UnknownAlbumPipe, UnknownArtistPipe, NgbDropdown, NgbDropdownButtonItem, NgbDropdownItem, NgbDropdownMenu, NgbDropdownToggle],
    selector: 'pony-album',
    templateUrl: './album.component.html',
    styleUrls: ['./album.component.scss']
})
export class AlbumComponent implements OnInit, OnDestroy, OnChanges, AfterViewInit {

  @Input() albumSongs!: AlbumSongs;

  discs: Disc[] = [];
  duration: string | undefined;
  showNewIndicator = false;
  topPlaylists: Playlist[] = [];

  @ViewChildren(NgbDropdown) optionsDropDowns!: QueryList<NgbDropdown>;

  private subscriptions: Subscription[] = [];

  constructor(
    private readonly libraryService: LibraryService,
    private readonly translateService: TranslateService,
    private readonly installationService: InstallationService,
    private readonly playlistService: PlaylistService,
    private readonly playbackService: PlaybackService,
    private readonly notificationService: NotificationService,
    private readonly modal: NgbModal,
    private rootElement: ElementRef
  ) {
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }

  ngOnInit(): void {
    this.subscriptions.push(this.libraryService.observeScrollToAlbumRequest()
      .subscribe(album => {
        if (album.id === this.albumSongs.album.id) {
          const song = this.discs[0].songs[0];
          this.libraryService.selectSong(song);
          this.libraryService.requestScrollToSong(song);
          this.libraryService.finishScrollToAlbum();
        }
      }));
    this.subscriptions.push(this.playlistService.observePlaylists()
      .subscribe(() => this.topPlaylists = this.playlistService.getTopPlaylists(PlaylistDto.Type.NORMAL)));
  }

  ngAfterViewInit(): void {
    this.subscriptions.push(fromEvent((this.rootElement.nativeElement as HTMLElement).parentElement as any, 'scroll').subscribe(() =>
      this.closeOptionsDropDowns()));
    this.subscriptions.push(fromEvent((this.rootElement.nativeElement as HTMLElement).parentElement as any, 'touchstart').subscribe(() =>
      this.closeOptionsDropDowns()));
  }

  private closeOptionsDropDowns() {
    this.optionsDropDowns.toArray().forEach(dropDown => {
      dropDown.close();
    });
  }

  ngOnChanges() {
    this.splitAlbumsIntoDiscs();
    this.duration = formatDuration(this.albumSongs.songs.reduce((result: number, song: Song) => result + song.duration, 0), this.translateService);
    this.showNewIndicator = shouldShowNewIndicator(this.albumSongs.album.updateDate, this.installationService.installationStatus) ||
      shouldShowNewIndicator(this.albumSongs.album.creationDate, this.installationService.installationStatus);
  }

  download() {
    window.open(`/api/file/export/album/${this.albumSongs.album.id}`, '_blank', '');
  }

  trackByIndex(index: number) {
    return index;
  }

  private splitAlbumsIntoDiscs() {

    const discToSongs = new Map<number, Song[]>();
    this.albumSongs.songs.forEach(song => {
      const discNumber = song.discNumber && song.discNumber !== 0 ? song.discNumber : 1;
      let discSongs = discToSongs.get(discNumber);
      if (!discSongs) {
        discSongs = [];
      }
      discSongs.push(song);
      discToSongs.set(discNumber, discSongs);
    });

    this.discs = [];
    discToSongs.forEach((discSongs, discNumber) => {
      const disc: Disc = {
        discNumber: (discNumber !== 1 || discToSongs.size !== 1 ? discNumber : undefined),
        showArtist: false,
        songs: discSongs
      };
      disc.songs.sort(Song.compare);
      disc.songs.forEach(song => {
        if (!disc.showArtist) {
          disc.showArtist = song.artistName !== undefined && !nullSafeNormalizedEquals(song.artistName, this.albumSongs.album.artist.name);
        }
      });
      this.discs.push(disc);
    });
    this.discs.sort(compareDiscs);
  }

  openArtwork() {
    if (this.albumSongs.album.largeArtworkUrl) {
      const modalRef = this.modal.open(ArtworkComponent, {size: '400px'});
      const userComponent: ArtworkComponent = modalRef.componentInstance;
      userComponent.url = this.albumSongs.album.largeArtworkUrl;
    }
  }

  playNext() {
    this.playbackService.playListNext(this.albumSongs.songs);
  }

  addToQueue() {
    this.playbackService.addListToQueue(this.albumSongs.songs);
  }

  createQueue() {
    this.playbackService.createListQueue(this.albumSongs.songs);
  }

  addToPlaylist(playlist: Playlist) {
    this.playlistService.addSongsToPlaylist(playlist.id, this.albumSongs.songs.map(song => song.id)).subscribe({
      error: () => this.notificationService.error(
        this.translateService.instant('library.song.addToPlaylistNotificationTitle'),
        this.translateService.instant('library.song.addToPlaylistNotificationTextFailure'),
      ),
    });
  }

  selectOrCreatePlaylist() {
    if (this.topPlaylists.length > 0) {
      const modalRef = this.modal.open(PlaylistAddSongComponent);
      const playlistAddSongComponent: PlaylistAddSongComponent = modalRef.componentInstance;
      playlistAddSongComponent.songs = this.albumSongs.songs;
    } else {
      const modalRef = this.modal.open(PlaylistEditComponent);
      const playlistEditComponent: PlaylistEditComponent = modalRef.componentInstance;
      playlistEditComponent.songs = this.albumSongs.songs;
    }
  }
}
