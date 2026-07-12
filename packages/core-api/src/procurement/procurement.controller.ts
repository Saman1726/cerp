import { Body, Controller, Get, Param, Post } from '@nestjs/common';
import { ProcurementService } from './procurement.service';
import { RaisePurchaseOrderDto } from './dto/raise-purchase-order.dto';
import { RecordGoodsReceiptDto } from './dto/record-goods-receipt.dto';

@Controller('purchase-orders')
export class ProcurementController {
  constructor(private readonly procurementService: ProcurementService) {}

  @Get()
  findAll() {
    return this.procurementService.findAll();
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.procurementService.findOne(id);
  }

  @Post()
  raise(@Body() dto: RaisePurchaseOrderDto) {
    return this.procurementService.raise(dto);
  }

  @Post(':id/receipts')
  recordGoodsReceipt(@Param('id') id: string, @Body() dto: RecordGoodsReceiptDto) {
    return this.procurementService.recordGoodsReceipt(id, dto);
  }
}
