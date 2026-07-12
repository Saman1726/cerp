import { IsOptional, IsString, MaxLength } from 'class-validator';

export class RecordGoodsReceiptDto {
  @IsOptional()
  @IsString()
  @MaxLength(2000)
  notes?: string;
}
