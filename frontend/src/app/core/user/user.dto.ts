export interface UserDto {
  id: string;
  creationDate: Date;
  updateDate: Date | undefined;
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

export interface AuthenticationDto {
  user: UserDto;
  accessToken: string;
  staticToken: string;
}
