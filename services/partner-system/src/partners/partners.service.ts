import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Partner } from './entities/partner.entity';

@Injectable()
export class PartnersService {
  constructor(@InjectRepository(Partner) private readonly partnerRepo: Repository<Partner>) {}

  findAll(): Promise<Partner[]> {
    return this.partnerRepo.find({ order: { name: 'ASC' } });
  }

  async findOne(id: string): Promise<Partner> {
    const partner = await this.partnerRepo.findOne({ where: { id } });
    if (!partner) {
      throw new NotFoundException(`Partner ${id} not found`);
    }
    return partner;
  }

  findByCode(code: string): Promise<Partner | null> {
    return this.partnerRepo.findOne({ where: { code } });
  }
}
