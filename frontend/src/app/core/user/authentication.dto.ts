import {UserDto} from './user.dto';

export class AuthenticationDto {
  user: UserDto;
  token: string;
}
