import { Module } from '@nestjs/common';
import { CoreModule } from '@cerp/core-api';
import { PartnersModule } from './partners/partners.module';

// The reference app: zero overrides of core's own extension points. See
// examples/demo-overlay/api for a version of this same file with those
// wired in. PartnersModule is a separate, additive integration with the
// external Partner System (services/partner-system) - not part of the
// core/overlay extension mechanism at all.
@Module({
  imports: [CoreModule.forRoot({}), PartnersModule],
})
export class AppModule {}
