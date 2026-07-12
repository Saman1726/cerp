import { IsOptional, IsString, IsUUID, MaxLength, MinLength } from 'class-validator';

export class IssueWorkOrderDto {
  @IsUUID()
  projectId!: string;

  @IsString()
  @MinLength(1)
  @MaxLength(200)
  title!: string;

  @IsOptional()
  @IsString()
  @MaxLength(2000)
  description?: string;

  @IsString()
  @MinLength(1)
  contractorPartnerId!: string;
}
