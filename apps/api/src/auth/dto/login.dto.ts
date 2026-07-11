import { IsEmail, IsString, Matches, MaxLength, MinLength } from 'class-validator';
import type { LoginRequest } from '@cerp/shared-types';

export class LoginDto implements LoginRequest {
  @IsString()
  @MinLength(2)
  @MaxLength(32)
  @Matches(/^[a-z][a-z0-9-]*$/, {
    message: 'tenantSlug must be lowercase letters, numbers and hyphens',
  })
  tenantSlug!: string;

  @IsEmail()
  email!: string;

  @IsString()
  @MinLength(8)
  password!: string;
}
