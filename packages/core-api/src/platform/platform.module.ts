import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Tenant } from './entities/tenant.entity';
import { PlatformAdmin } from './entities/platform-admin.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Tenant, PlatformAdmin])],
  exports: [TypeOrmModule],
})
export class PlatformModule {}
