import {Component, Input, OnChanges, OnDestroy, OnInit, SimpleChanges} from '@angular/core';
import {AlbumSongs, Song} from "../../domain/library.model";
import {SongListComponent} from "./song-list.component";
import {TranslateModule} from "@ngx-translate/core";
import {ImageLoaderComponent} from "../common/image-loader.component";
import {CommonModule} from "@angular/common";
import {LibraryService} from "../../service/library.service";
import {Subscription} from "rxjs";
import {UnknownAlbumPipe} from "../../pipe/unknown-album.pipe";

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
  standalone: true,
  imports: [CommonModule, TranslateModule, ImageLoaderComponent, SongListComponent, UnknownAlbumPipe],
  selector: 'pony-album',
  templateUrl: './album.component.html',
  styleUrls: ['./album.component.scss']
})
export class AlbumComponent implements OnInit, OnDestroy, OnChanges {

  @Input() albumSongs!: AlbumSongs;

  discs: Disc[] = [];

  private scrollToAlbumRequestSubscription: Subscription | undefined;

  constructor(
    private readonly libraryService: LibraryService
  ) {
  }

  ngOnDestroy(): void {
    this.scrollToAlbumRequestSubscription?.unsubscribe();
  }

  ngOnInit(): void {
    this.scrollToAlbumRequestSubscription = this.libraryService.observeScrollToAlbumRequest()
      .subscribe(album => {
        if (album.id === this.albumSongs.album.id) {
          var song = this.discs[0].songs[0];
          this.libraryService.selectSong(song);
          this.libraryService.startScrollToSong(song);
          this.libraryService.finishScrollToAlbum();
        }
      });
  }

  ngOnChanges(changes: SimpleChanges) {
    this.splitAlbumsIntoDiscs();
  }

  download() {
    window.open(`/api/file/export/album/${this.albumSongs.album.id}`, '_blank', '');
  }

  trackByIndex(index: number): number {
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
          disc.showArtist = !nullSafeNormalizedEquals(song.artistName, this.albumSongs.album.artist.name);
        }
      });
      this.discs.push(disc);
    });
    this.discs.sort(compareDiscs);
  }
}
