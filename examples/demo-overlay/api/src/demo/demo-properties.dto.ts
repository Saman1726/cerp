import { IsOptional, IsString, MaxLength } from 'class-validator';
import { CreatePropertyDto } from '@cerp/core-api';

export class CreateDemoPropertyDto extends CreatePropertyDto {
  @IsOptional()
  @IsString()
  @MaxLength(2000)
  inspectorNotes?: string;
}
