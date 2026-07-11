import { Injectable, NotFoundException } from '@nestjs/common';
import { TenancyService } from '../tenancy/tenancy.service';
import { Property } from './entities/property.entity';
import { CreatePropertyDto } from './dto/create-property.dto';
import { UpdatePropertyDto } from './dto/update-property.dto';

@Injectable()
export class PropertiesService {
  constructor(private readonly tenancyService: TenancyService) {}

  async findAll(): Promise<Property[]> {
    const manager = await this.tenancyService.getEntityManager();
    return manager.find(Property, { order: { createdAt: 'DESC' } });
  }

  async findOne(id: string): Promise<Property> {
    const manager = await this.tenancyService.getEntityManager();
    const property = await manager.findOne(Property, { where: { id } });
    if (!property) {
      throw new NotFoundException(`Property ${id} not found`);
    }
    return property;
  }

  async create(dto: CreatePropertyDto): Promise<Property> {
    const manager = await this.tenancyService.getEntityManager();
    return manager.save(Property, dto);
  }

  async update(id: string, dto: UpdatePropertyDto): Promise<Property> {
    const manager = await this.tenancyService.getEntityManager();
    await this.findOne(id);
    await manager.update(Property, { id }, dto);
    return this.findOne(id);
  }

  async remove(id: string): Promise<void> {
    const manager = await this.tenancyService.getEntityManager();
    await this.findOne(id);
    await manager.delete(Property, { id });
  }
}
