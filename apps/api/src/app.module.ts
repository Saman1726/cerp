import { Module } from '@nestjs/common';
import { CoreModule } from '@cerp/core-api';

// The reference app: zero overrides of core's own extension points. See
// examples/demo-overlay/api for a version of this same file with those
// wired in. Partner System integration (GET /partners, POST
// /auth/partner-login) now comes from CoreModule itself via
// PartyRegistryModule, since Construction Engineering needs to resolve
// contractors too - not just this app's own Partners page.
@Module({
  imports: [CoreModule.forRoot({})],
})
export class AppModule {}
