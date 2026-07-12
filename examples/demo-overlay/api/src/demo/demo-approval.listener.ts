import { Injectable } from '@nestjs/common';
import { OnEvent } from '@nestjs/event-emitter';
import { PROPERTY_CREATED_EVENT, TenancyService, type PropertyCreatedEvent } from '@cerp/core-api';
import { DemoPropertyAudit } from './entities/demo-property-audit.entity';

// Demonstrates the event-hook extension point: reacts to something core did
// (a property was created) without altering core's behavior or core needing
// to know this listener - or this whole overlay - exists.
@Injectable()
export class DemoApprovalListener {
  constructor(private readonly tenancyService: TenancyService) {}

  @OnEvent(PROPERTY_CREATED_EVENT)
  async onPropertyCreated(payload: PropertyCreatedEvent): Promise<void> {
    const manager = await this.tenancyService.getEntityManager();
    await manager.save(DemoPropertyAudit, {
      propertyId: payload.property.id,
      eventType: PROPERTY_CREATED_EVENT,
    });
  }
}
