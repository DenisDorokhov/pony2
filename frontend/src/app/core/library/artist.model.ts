export class Artist {

  id: number;
  creationDate: Date;
  updateDate: Date;
  name: string;
  artworkId: number;

  constructor(partial?: Partial<Artist>) {
    Object.assign(this, partial);
  }
}
