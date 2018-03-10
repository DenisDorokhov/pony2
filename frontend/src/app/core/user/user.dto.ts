export class UserDto {
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
