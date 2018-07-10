export class Artist {

  id: number;
  creationDate: Date;
  updateDate: Date;
  name: string;
  artwork: number;

  constructor(partial?: Partial<Artist>) {
    Object.assign(this, partial);
  }
}
