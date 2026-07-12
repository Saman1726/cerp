import { Injectable, UnauthorizedException } from '@nestjs/common';
import { PartnersService } from '../partners/partners.service';
import { PARTNER_USERS } from './partner-user.model';
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
  constructor(private readonly partnersService: PartnersService) {}

  login(dto: PartnerLoginDto): PartnerLoginResult {
    const user = PARTNER_USERS.find(
      (u) => u.username === dto.username && u.password === dto.password && u.partnerCode === dto.partnerCode,
    );
    if (!user) {
      throw new UnauthorizedException('Invalid partner code, username or password');
    }

    const partner = this.partnersService.findByCode(user.partnerCode);
    if (!partner || partner.status !== 'active') {
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
