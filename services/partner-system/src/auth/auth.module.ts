import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { PartnersModule } from '../partners/partners.module';
import { PartnerUser } from './entities/partner-user.entity';

@Module({
  imports: [PartnersModule, TypeOrmModule.forFeature([PartnerUser])],
  controllers: [AuthController],
  providers: [AuthService],
})
export class AuthModule {}
