import { Injectable } from '@nestjs/common';
import type { TenantContext } from '../tenancy/interfaces/tenant-context.interface';
import type { CreatePropertyDto } from './dto/create-property.dto';
import type { UpdatePropertyDto } from './dto/update-property.dto';

export const PROPERTY_VALIDATOR = Symbol('PROPERTY_VALIDATOR');

// An interface is erased at compile time, so consumers overriding this must
// still provide a concrete class - Nest resolves the token, not the shape.
export interface PropertyValidator {
  validateCreate(dto: CreatePropertyDto, ctx: TenantContext): void | Promise<void>;
  validateUpdate(id: string, dto: UpdatePropertyDto, ctx: TenantContext): void | Promise<void>;
}

@Injectable()
export class DefaultPropertyValidator implements PropertyValidator {
  validateCreate(): void {}
  validateUpdate(): void {}
}
