import { Body, Controller, Get, Param, Patch, Post } from '@nestjs/common';
import { ConstructionEngineeringService } from './construction-engineering.service';
import { IssueWorkOrderDto } from './dto/issue-work-order.dto';

@Controller('work-orders')
export class ConstructionEngineeringController {
  constructor(private readonly constructionEngineeringService: ConstructionEngineeringService) {}

  @Get()
  findAll() {
    return this.constructionEngineeringService.findAll();
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.constructionEngineeringService.findOne(id);
  }

  @Post()
  issue(@Body() dto: IssueWorkOrderDto) {
    return this.constructionEngineeringService.issue(dto);
  }

  @Patch(':id/complete')
  complete(@Param('id') id: string) {
    return this.constructionEngineeringService.complete(id);
  }
}
