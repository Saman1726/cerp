import { Module } from '@nestjs/common';
import { CoreModule } from '@cerp/core-api';
import { DemoModule } from './demo/demo.module';
import { DemoPropertyValidator } from './demo/demo-property-validator';
import { DemoPropertyExtension } from './demo/entities/demo-property-extension.entity';
import { DemoPropertyAudit } from './demo/entities/demo-property-audit.entity';

// This is the file a real customer overlay's app.module.ts would look like:
// CoreModule.forRoot({}) plus exactly the extension points this customer
// needs, plus its own additional module (DemoModule). Compare with
// apps/api/src/app.module.ts, which passes no options at all.
@Module({
  imports: [
    CoreModule.forRoot({
      database: { extraEntities: [DemoPropertyExtension, DemoPropertyAudit] },
      properties: { validatorClass: DemoPropertyValidator },
    }),
    DemoModule,
  ],
})
export class AppModule {}
