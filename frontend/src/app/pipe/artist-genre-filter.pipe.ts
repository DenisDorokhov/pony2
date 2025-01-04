import {Pipe, PipeTransform} from '@angular/core';
import {Artist, Genre} from '../domain/library.model';

@Pipe({
  standalone: true,
  name: 'ponyArtistGenreFilter'
})
export class ArtistGenreFilterPipe implements PipeTransform {

  transform(artists: Artist[], genre: Genre | undefined): any {
    return genre ? artists.filter(artist => artist.genres.findIndex(artistGenre => artistGenre.id === genre.id) >= 0) : artists;
  }
}
