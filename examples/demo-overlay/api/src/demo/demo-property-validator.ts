import { BadRequestException, Injectable } from '@nestjs/common';
import type { CreatePropertyDto, PropertyValidator, UpdatePropertyDto } from '@cerp/core-api';

const POSTAL_CODE_SUFFIX = /\d{5}$/;

// Demonstrates the DI-override extension point: a customer-specific rule
// layered on top of core's Property module via CorePropertiesModule.forRoot,
// with zero changes to core's own source.
@Injectable()
export class DemoPropertyValidator implements PropertyValidator {
  validateCreate(dto: CreatePropertyDto): void {
    this.assertPostalCode(dto.address);
  }

  validateUpdate(_id: string, dto: UpdatePropertyDto): void {
    if (dto.address) {
      this.assertPostalCode(dto.address);
    }
  }

  private assertPostalCode(address: string): void {
    if (!POSTAL_CODE_SUFFIX.test(address)) {
      throw new BadRequestException('Address must end in a 5-digit postal code for this demo tenant');
    }
  }
}
