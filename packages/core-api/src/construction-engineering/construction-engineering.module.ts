import { Module } from '@nestjs/common';
import { ConstructionEngineeringController } from './construction-engineering.controller';
import { ConstructionEngineeringService } from './construction-engineering.service';

// No TypeOrmModule.forFeature([WorkOrder]) here on purpose - WorkOrder is
// tenant-scoped and must only be reached through TenancyService, matching
// CorePropertiesModule's own convention. Static (not forRoot()) for now -
// no consumer-overridable extension point has been needed yet; add one the
// same way CorePropertiesModule's validatorClass works if that changes.
@Module({
  controllers: [ConstructionEngineeringController],
  providers: [ConstructionEngineeringService],
  exports: [ConstructionEngineeringService],
})
export class ConstructionEngineeringModule {}
