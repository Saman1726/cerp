import { Module } from '@nestjs/common';
import { DemoPropertiesController } from './demo-properties.controller';
import { DemoPropertiesService } from './demo-properties.service';
import { DemoApprovalListener } from './demo-approval.listener';

@Module({
  controllers: [DemoPropertiesController],
  providers: [DemoPropertiesService, DemoApprovalListener],
})
export class DemoModule {}
