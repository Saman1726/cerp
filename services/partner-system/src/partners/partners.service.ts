import { Injectable, NotFoundException } from '@nestjs/common';
import { Partner, PARTNERS } from './partner.model';

@Injectable()
export class PartnersService {
  findAll(): Partner[] {
    return PARTNERS;
  }

  findOne(id: string): Partner {
    const partner = PARTNERS.find((p) => p.id === id);
    if (!partner) {
      throw new NotFoundException(`Partner ${id} not found`);
    }
    return partner;
  }

  findByCode(code: string): Partner | undefined {
    return PARTNERS.find((p) => p.code === code);
  }
}
