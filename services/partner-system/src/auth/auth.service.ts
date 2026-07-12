import { Injectable, UnauthorizedException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import * as argon2 from 'argon2';
import { PartnersService } from '../partners/partners.service';
import { PartnerUser } from './entities/partner-user.entity';
import { PartnerLoginDto } from './dto/partner-login.dto';

export interface PartnerLoginResult {
  partnerId: string;
  partnerCode: string;
  partnerName: string;
  username: string;
  role: string;
}

@Injectable()
export class AuthService {
  constructor(
    private readonly partnersService: PartnersService,
    @InjectRepository(PartnerUser) private readonly partnerUserRepo: Repository<PartnerUser>,
  ) {}

  async login(dto: PartnerLoginDto): Promise<PartnerLoginResult> {
    const partner = await this.partnersService.findByCode(dto.partnerCode);
    const user = partner
      ? await this.partnerUserRepo.findOne({ where: { username: dto.username, partnerId: partner.id } })
      : null;

    if (!partner || !user || !(await argon2.verify(user.passwordHash, dto.password))) {
      throw new UnauthorizedException('Invalid partner code, username or password');
    }
    if (partner.status !== 'active') {
      throw new UnauthorizedException('Partner is not active');
    }

    return {
      partnerId: partner.id,
      partnerCode: partner.code,
      partnerName: partner.name,
      username: user.username,
      role: user.role,
    };
  }
}
