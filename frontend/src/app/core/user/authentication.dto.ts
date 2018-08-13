import {UserDto} from './user.dto';

export interface AuthenticationDto {
  user: UserDto;
  token: string;
}
