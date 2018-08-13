export interface UserDto {
  id: number;
  creationDate: Date;
  updateDate: Date;
  name: string;
  email: string;
  role: UserDto.Role;
}

export namespace UserDto {
  export enum Role {
    USER = 'USER',
    ADMIN = 'ADMIN',
  }
}
