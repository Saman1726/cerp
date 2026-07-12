import { Module } from '@nestjs/common';
import { ProcurementController } from './procurement.controller';
import { ProcurementService } from './procurement.service';

// No TypeOrmModule.forFeature() here on purpose - PurchaseOrder/GoodsReceipt
// are tenant-scoped and must only be reached through TenancyService,
// matching CorePropertiesModule's and ConstructionEngineeringModule's
// convention. Static for now - no consumer-overridable extension point has
// been needed yet.
@Module({
  controllers: [ProcurementController],
  providers: [ProcurementService],
  exports: [ProcurementService],
})
export class ProcurementModule {}
