import {Component, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs';
import {AlbumSongs, Artist, ArtistSongs, PlaylistSongs, Song} from '../../domain/library.model';
import {LibraryService, LibraryState} from '../../service/library.service';
import {PlaybackService} from '../../service/playback.service';
import {LoadingState} from '../../domain/common.model';
import {TranslateModule} from '@ngx-translate/core';
import {LoadingIndicatorComponent} from '../common/loading-indicator.component';
import {ErrorIndicatorComponent} from '../common/error-indicator.component';
import {NoContentIndicatorComponent} from '../common/no-content-indicator.component';
import {AlbumComponent} from './album.component';
import {CommonModule} from '@angular/common';
import {UnknownArtistPipe} from '../../pipe/unknown-artist.pipe';
import {PlaylistService} from '../../service/playlist.service';

@Component({
  standalone: true,
  imports: [CommonModule, TranslateModule, LoadingIndicatorComponent, ErrorIndicatorComponent, NoContentIndicatorComponent, AlbumComponent, UnknownArtistPipe],
  selector: 'pony-album-list',
  templateUrl: './album-list.component.html',
  styleUrls: ['./album-list.component.scss']
})
export class AlbumListComponent implements OnInit, OnDestroy {

  LoadingState = LoadingState;

  loadingState = LoadingState.LOADING;
  artistSongs!: ArtistSongs;

  albumCount = 0;
  songCount = 0;
  likeCount = 0;

  private likePlaylist: PlaylistSongs | undefined;
  private artistSongsSubscription: Subscription | undefined;
  private subscriptions: Subscription[] = [];

  constructor(
    private readonly libraryService: LibraryService,
    private readonly playbackService: PlaybackService,
    private readonly playlistService: PlaylistService,
  ) {
  }

  ngOnInit(): void {
    this.subscriptions.push(this.libraryService.observeRefreshRequest()
      .subscribe(() => {
        if (this.libraryService.selectedArtist) {
          this.loadArtistSongs(this.libraryService.selectedArtist, true);
        }
      }));
    this.subscriptions.push(this.libraryService.observeSelectedArtist()
      .subscribe(artist => {
        if (artist) {
          this.loadArtistSongs(artist);
        }
      }));
    this.subscriptions.push(this.libraryService.observeLibraryState()
      .subscribe(libraryState => {
        if (libraryState === LibraryState.EMPTY) {
          this.loadingState = LoadingState.EMPTY;
        }
      }));
    this.subscriptions.push(this.libraryService.observeSongPlaybackRequest()
      .subscribe(song => {
        if (this.artistSongs) {

          const songs: Song[] = [];
          this.artistSongs.albumSongs.forEach(albumSongs =>
            albumSongs.songs.forEach(albumSong => songs.push(albumSong)));

          let index = 0;
          if (song) {
            const targetIndex = songs.findIndex(nextSong => nextSong.id === song.id);
            if (targetIndex >= 0) {
              index = targetIndex;
            }
          }
          this.playbackService.switchQueue(songs, index);
        }
      }));
    this.subscriptions.push(this.playlistService.observeLikePlaylist().subscribe(likePlaylist => {
      this.likePlaylist = likePlaylist;
      this.countLikes();
    }));
  }

  private countLikes() {
    this.likeCount = this.likePlaylist?.songs
      .map(next => next.song)
      .filter(song => song.album.artist.id === this.artistSongs.artist.id)
      .length ?? 0;
  }

  ngOnDestroy(): void {
    this.artistSongsSubscription?.unsubscribe();
    this.subscriptions.forEach(next => next.unsubscribe());
  }

  trackByIndex(index: number) {
    return index;
  }

  private loadArtistSongs(artist: Artist, refreshing = false) {
    if (refreshing) {
      console.info(`Refreshing albums of artist ${artist.id} -> '${artist.name}'...`);
    } else {
      console.info(`Loading albums of artist ${artist.id} -> '${artist.name}'...`);
      this.loadingState = LoadingState.LOADING;
    }
    if (this.artistSongsSubscription) {
      this.artistSongsSubscription.unsubscribe();
    }
    this.artistSongsSubscription = this.libraryService.getArtistSongs(artist.id)
      .subscribe({
        next: artistSongs => {
          this.albumCount = 0;
          this.songCount = 0;
          this.artistSongs = artistSongs;
          this.artistSongs.albumSongs.sort(AlbumSongs.compare);
          this.artistSongs.albumSongs.forEach(album => {
            this.albumCount++;
            album.songs.sort(Song.compare);
            album.songs.forEach(() => this.songCount++);
          });
          this.countLikes();
          this.loadingState = LoadingState.LOADED;
          console.info(`${artistSongs.albumSongs.length} albums have been loaded for artist ${artist.id} -> '${artist.name}'.`);
        },
        error: error => {
          if (!refreshing) {
            this.loadingState = LoadingState.ERROR;
          }
          console.error(`Could not load albums of artist ${artist.id} -> '${artist.name}': "${error.message}".`);
        }
      });
  }
}
