import { Injectable } from '@nestjs/common';
import { PropertiesService, Property, TenancyService } from '@cerp/core-api';
import { DemoPropertyExtension } from './entities/demo-property-extension.entity';
import { CreateDemoPropertyDto } from './demo-properties.dto';

export interface ExtendedProperty extends Property {
  inspectorNotes: string | null;
  complianceStatus: string;
}

// Composes core's PropertiesService with an overlay-owned satellite table -
// core's own /properties endpoint stays byte-for-byte unchanged; this is a
// wholly additive endpoint the demo overlay adds on its own.
@Injectable()
export class DemoPropertiesService {
  constructor(
    private readonly propertiesService: PropertiesService,
    private readonly tenancyService: TenancyService,
  ) {}

  async findAllExtended(): Promise<ExtendedProperty[]> {
    const properties = await this.propertiesService.findAll();
    const manager = await this.tenancyService.getEntityManager();
    const extensions = await manager.find(DemoPropertyExtension);
    const byPropertyId = new Map(extensions.map((ext) => [ext.propertyId, ext]));

    return properties.map((property) => ({
      ...property,
      inspectorNotes: byPropertyId.get(property.id)?.inspectorNotes ?? null,
      complianceStatus: byPropertyId.get(property.id)?.complianceStatus ?? 'pending',
    }));
  }

  async createExtended(dto: CreateDemoPropertyDto): Promise<ExtendedProperty> {
    const property = await this.propertiesService.create(dto);

    const manager = await this.tenancyService.getEntityManager();
    const extension = await manager.save(DemoPropertyExtension, {
      propertyId: property.id,
      inspectorNotes: dto.inspectorNotes ?? null,
    });

    return {
      ...property,
      inspectorNotes: extension.inspectorNotes,
      complianceStatus: extension.complianceStatus,
    };
  }
}
