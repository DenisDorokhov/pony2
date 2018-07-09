import {Component, OnDestroy, OnInit} from '@angular/core';
import 'rxjs/add/operator/do';
import {Subscription} from 'rxjs/Subscription';
import {LoadingState} from '../core/common/loading-state';
import {ArtistSongsDto} from '../core/library/artist-songs.dto';
import {ArtistDto} from '../core/library/artist.dto';
import {LibraryService} from '../core/library/library.service';

@Component({
  selector: 'pony-album-list',
  templateUrl: './album-list.component.html',
  styleUrls: ['./album-list.component.scss']
})
export class AlbumListComponent implements OnInit, OnDestroy {

  LoadingState = LoadingState;

  loadingState = LoadingState.LOADING;
  artistSongs: ArtistSongsDto;

  private selectedArtistSubscription: Subscription;
  private artistSongsSubscription: Subscription;

  constructor(private libraryService: LibraryService) {
  }

  ngOnInit(): void {
    this.selectedArtistSubscription = this.libraryService.selectedArtist
      .subscribe(artist => this.loadArtistSongs(artist));
  }

  ngOnDestroy(): void {
    this.selectedArtistSubscription.unsubscribe();
  }

  private loadArtistSongs(artist: ArtistDto) {
    console.log(`Loading albums of artist ${artist.id} -> '${artist.name}'...`);
    this.loadingState = LoadingState.LOADING;
    if (this.artistSongsSubscription) {
      this.artistSongsSubscription.unsubscribe();
    }
    this.artistSongsSubscription = this.libraryService.getArtistSongs(artist.id)
      .subscribe(
        artistSongs => {
          this.artistSongs = artistSongs;
          this.loadingState = LoadingState.LOADED;
          console.log(`${artistSongs.albums.length} albums have been loaded for artist ${artist.id} -> '${artist.name}'.`);
        },
        error => {
          this.loadingState = LoadingState.ERROR;
          console.log(`Could not load albums of artist ${artist.id} -> '${artist.name}': "${error.message}".`);
        }
      );
  }
}
