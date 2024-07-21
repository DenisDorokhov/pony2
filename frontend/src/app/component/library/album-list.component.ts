import {Component, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs';
import {AlbumSongs, Artist, ArtistSongs, Song} from "../../domain/library.model";
import {LibraryService, LibraryState} from "../../service/library.service";
import {PlaybackService} from "../../service/playback.service";
import {StaticPlaylist} from "../../domain/playlist.model";
import {LoadingState} from "../../domain/common.model";
import Logger from "js-logger";
import {TranslateModule} from "@ngx-translate/core";
import {LoadingIndicatorComponent} from "../common/loading-indicator.component";
import {ErrorIndicatorComponent} from "../common/error-indicator.component";
import {NoContentIndicatorComponent} from "../common/no-content-indicator.component";
import {AlbumComponent} from "./album.component";
import {CommonModule} from "@angular/common";

@Component({
  standalone: true,
  imports: [CommonModule, TranslateModule, LoadingIndicatorComponent, ErrorIndicatorComponent, NoContentIndicatorComponent, AlbumComponent],
  selector: 'pony-album-list',
  templateUrl: './album-list.component.html',
  styleUrls: ['./album-list.component.scss']
})
export class AlbumListComponent implements OnInit, OnDestroy {

  LoadingState = LoadingState;

  loadingState = LoadingState.LOADING;
  artistSongs!: ArtistSongs;

  private artistSongsSubscription: Subscription | undefined;
  private refreshRequestSubscription: Subscription | undefined;
  private selectedArtistSubscription: Subscription | undefined;
  private libraryStateSubscription: Subscription | undefined;
  private songPlaybackRequestSubscription: Subscription | undefined;

  private static compareAlbumSongs(albumSongs1: AlbumSongs, albumSongs2: AlbumSongs): number {
    if ((albumSongs1.album.year ?? 0) < (albumSongs2.album.year ?? 0)) {
      return 1;
    }
    if ((albumSongs1.album.year ?? 0) > (albumSongs2.album.year ?? 0)) {
      return -1;
    }
    return 0;
  }

  constructor(private libraryService: LibraryService, private playbackService: PlaybackService) {
  }

  ngOnInit(): void {
    this.refreshRequestSubscription = this.libraryService.observeRefreshRequest()
      .subscribe(() => {
        if (this.libraryService.selectedArtist) {
          this.loadArtistSongs(this.libraryService.selectedArtist, true);
        }
      });
    this.selectedArtistSubscription = this.libraryService.observeSelectedArtist()
      .subscribe(artist => {
        if (artist) {
          this.loadArtistSongs(artist);
        }
      });
    this.libraryStateSubscription = this.libraryService.observeLibraryState()
      .subscribe(libraryState => {
        if (libraryState === LibraryState.EMPTY) {
          this.loadingState = LoadingState.EMPTY;
        }
      });
    this.songPlaybackRequestSubscription = this.libraryService.observeSongPlaybackRequest()
      .subscribe(song => {
        if (this.artistSongs) {

          const songs: Song[] = [];
          this.artistSongs.albumSongs.forEach(albumSongs =>
            albumSongs.songs.forEach(albumSong => songs.push(albumSong)));

          const playlist = new StaticPlaylist(songs);
          if (song) {
            playlist.switchToSong(song.id);
          }
          this.playbackService.switchPlaylist(playlist);
        }
      });
  }

  ngOnDestroy(): void {
    this.artistSongsSubscription?.unsubscribe();
    this.refreshRequestSubscription?.unsubscribe();
    this.selectedArtistSubscription?.unsubscribe();
    this.libraryStateSubscription?.unsubscribe();
    this.songPlaybackRequestSubscription?.unsubscribe();
  }

  trackByIndex(index: number): number {
    return index;
  }

  private loadArtistSongs(artist: Artist, refreshing: boolean = false) {
    if (refreshing) {
      Logger.info(`Refreshing albums of artist ${artist.id} -> '${artist.name}'...`);
    } else {
      Logger.info(`Loading albums of artist ${artist.id} -> '${artist.name}'...`);
      this.loadingState = LoadingState.LOADING;
    }
    if (this.artistSongsSubscription) {
      this.artistSongsSubscription.unsubscribe();
    }
    this.artistSongsSubscription = this.libraryService.getArtistSongs(artist.id)
      .subscribe({
        next: artistSongs => {
          this.artistSongs = artistSongs;
          this.artistSongs.albumSongs.sort(AlbumListComponent.compareAlbumSongs);
          this.loadingState = LoadingState.LOADED;
          Logger.info(`${artistSongs.albumSongs.length} albums have been loaded for artist ${artist.id} -> '${artist.name}'.`);
        },
        error: error => {
          if (!refreshing) {
            this.loadingState = LoadingState.ERROR;
          }
          Logger.error(`Could not load albums of artist ${artist.id} -> '${artist.name}': "${error.message}".`);
        }
      });
  }
}
