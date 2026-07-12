import { Body, Controller, Get, Post } from '@nestjs/common';
import { DemoPropertiesService } from './demo-properties.service';
import { CreateDemoPropertyDto } from './demo-properties.dto';

@Controller('demo/properties')
export class DemoPropertiesController {
  constructor(private readonly demoPropertiesService: DemoPropertiesService) {}

  @Get()
  findAll() {
    return this.demoPropertiesService.findAllExtended();
  }

  @Post()
  create(@Body() dto: CreateDemoPropertyDto) {
    return this.demoPropertiesService.createExtended(dto);
  }
}
