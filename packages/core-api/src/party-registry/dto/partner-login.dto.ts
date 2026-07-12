import { IsString, MinLength } from 'class-validator';

export class PartnerLoginDto {
  @IsString()
  partnerCode!: string;

  @IsString()
  username!: string;

  @IsString()
  @MinLength(1)
  password!: string;
}
