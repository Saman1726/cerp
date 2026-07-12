import { IsString, IsUUID, MaxLength, MinLength } from 'class-validator';

export class RaisePurchaseOrderDto {
  @IsUUID()
  projectId!: string;

  @IsString()
  @MinLength(1)
  vendorPartnerId!: string;

  @IsString()
  @MinLength(1)
  @MaxLength(500)
  description!: string;
}
