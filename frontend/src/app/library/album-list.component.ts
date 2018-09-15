import {Component, OnDestroy, OnInit} from '@angular/core';
import 'rxjs/add/operator/do';
import * as Logger from 'js-logger';
import {Subscription} from 'rxjs/Subscription';
import {LoadingState} from '../core/common/common.model';
import {AlbumSongs, Artist, ArtistSongs, Song} from '../core/library/library.model';
import {LibraryService, LibraryState} from '../core/library/library.service';
import {PlaybackService} from '../core/library/playback.service';
import {StaticPlaylist} from '../core/library/playlist.model';

@Component({
  selector: 'pony-album-list',
  templateUrl: './album-list.component.html',
  styleUrls: ['./album-list.component.scss']
})
export class AlbumListComponent implements OnInit, OnDestroy {

  LoadingState = LoadingState;

  loadingState = LoadingState.LOADING;
  artistSongs: ArtistSongs;

  private selectedArtistSubscription: Subscription;
  private artistSongsSubscription: Subscription;
  private libraryStateSubscription: Subscription;
  private songPlaybackRequestSubscription: Subscription;

  private static compareAlbumSongs(albumSongs1: AlbumSongs, albumSongs2: AlbumSongs): number {
    if (albumSongs1.album.year < albumSongs2.album.year) {
      return 1;
    }
    if (albumSongs1.album.year > albumSongs2.album.year) {
      return -1;
    }
    return 0;
  }

  constructor(private libraryService: LibraryService, private playbackService: PlaybackService) {
  }

  ngOnInit(): void {
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
    this.selectedArtistSubscription.unsubscribe();
    this.libraryStateSubscription.unsubscribe();
    this.songPlaybackRequestSubscription.unsubscribe();
  }

  private loadArtistSongs(artist: Artist) {
    Logger.info(`Loading albums of artist ${artist.id} -> '${artist.name}'...`);
    this.loadingState = LoadingState.LOADING;
    if (this.artistSongsSubscription) {
      this.artistSongsSubscription.unsubscribe();
    }
    this.artistSongsSubscription = this.libraryService.getArtistSongs(artist.id)
      .subscribe(
        artistSongs => {
          this.artistSongs = artistSongs;
          this.artistSongs.albumSongs.sort(AlbumListComponent.compareAlbumSongs);
          this.loadingState = LoadingState.LOADED;
          Logger.info(`${artistSongs.albumSongs.length} albums have been loaded for artist ${artist.id} -> '${artist.name}'.`);
        },
        error => {
          this.loadingState = LoadingState.ERROR;
          Logger.error(`Could not load albums of artist ${artist.id} -> '${artist.name}': "${error.message}".`);
        }
      );
  }
}
