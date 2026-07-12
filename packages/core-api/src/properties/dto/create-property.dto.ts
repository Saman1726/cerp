import { IsString, MaxLength, MinLength } from 'class-validator';
import type { CreatePropertyRequest } from '@cerp/shared-types';

export class CreatePropertyDto implements CreatePropertyRequest {
  @IsString()
  @MinLength(1)
  @MaxLength(200)
  name!: string;

  @IsString()
  @MinLength(1)
  @MaxLength(500)
  address!: string;
}
