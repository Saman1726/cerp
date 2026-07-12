import { Inject, Injectable, NotFoundException } from '@nestjs/common';
import { EventEmitter2 } from '@nestjs/event-emitter';
import { TenancyService } from '../tenancy/tenancy.service';
import { Property } from './entities/property.entity';
import { CreatePropertyDto } from './dto/create-property.dto';
import { UpdatePropertyDto } from './dto/update-property.dto';
import { PROPERTY_VALIDATOR, PropertyValidator } from './properties.tokens';
import { PROPERTY_CREATED_EVENT } from './events/property-events';

@Injectable()
export class PropertiesService {
  constructor(
    private readonly tenancyService: TenancyService,
    @Inject(PROPERTY_VALIDATOR) private readonly validator: PropertyValidator,
    private readonly events: EventEmitter2,
  ) {}

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
    const tenantContext = this.tenancyService.requireTenantContext();
    await this.validator.validateCreate(dto, tenantContext);

    const manager = await this.tenancyService.getEntityManager();
    const property = await manager.save(Property, dto);

    // emitAsync, not emit: TenancyCleanupInterceptor releases this request's
    // tenant-scoped connection as soon as the response resolves. A listener
    // that calls getEntityManager() must run to completion before that
    // happens, or it risks racing against - or reusing after release - a
    // connection another request's since-rebound search_path now owns.
    await this.events.emitAsync(PROPERTY_CREATED_EVENT, { property, tenantContext });
    return property;
  }

  async update(id: string, dto: UpdatePropertyDto): Promise<Property> {
    const tenantContext = this.tenancyService.requireTenantContext();
    await this.validator.validateUpdate(id, dto, tenantContext);

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
