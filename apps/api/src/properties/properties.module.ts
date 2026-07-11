import { Module } from '@nestjs/common';
import { PropertiesController } from './properties.controller';
import { PropertiesService } from './properties.service';

// No TypeOrmModule.forFeature([Property]) here on purpose - Property is
// tenant-scoped and must only be reached through TenancyService.
@Module({
  controllers: [PropertiesController],
  providers: [PropertiesService],
})
export class PropertiesModule {}
