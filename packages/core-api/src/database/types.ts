import type { Type } from '@nestjs/common';
import type { MigrationInterface } from 'typeorm';

export type EntityClass = Type<object>;
export type MigrationClass = Type<MigrationInterface>;
