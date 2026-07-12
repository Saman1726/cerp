import { Module } from '@nestjs/common';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { PartnersModule } from '../partners/partners.module';

@Module({
  imports: [PartnersModule],
  controllers: [AuthController],
  providers: [AuthService],
})
export class AuthModule {}
