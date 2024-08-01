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

export interface UserUpdateCommandDto {
  id: string;
  name: string;
  email: string;
  newPassword: string;
  role: UserDto.Role;
}

export interface UserCreateCommandDto {
  name: string;
  email: string;
  password: string;
  role: UserDto.Role;
}

export interface CurrentUserUpdateCommandDto {
  name: string;
  email: string;
  oldPassword: string;
  newPassword: string;
}
