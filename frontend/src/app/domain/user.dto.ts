import {PageDto} from "./common.dto";

export interface UserDto {
  id: string;
  creationDate: string;
  updateDate: string | undefined;
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

export interface UserPageDto extends PageDto {
  users: UserDto[];
}
