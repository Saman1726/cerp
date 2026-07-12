import { Body, Controller, HttpCode, HttpStatus, Post } from '@nestjs/common';
import { Public } from '@cerp/core-api';
import { PartnersService } from './partners.service';
import { PartnerLoginDto } from './dto/partner-login.dto';

// Partners aren't cerp tenant users - this delegates credential
// verification entirely to the external Partner System, unrelated to cerp's
// own tenant/JWT auth.
@Controller('auth')
export class PartnerAuthController {
  constructor(private readonly partnersService: PartnersService) {}

  @Public()
  @HttpCode(HttpStatus.OK)
  @Post('partner-login')
  partnerLogin(@Body() dto: PartnerLoginDto) {
    return this.partnersService.login(dto.partnerCode, dto.username, dto.password);
  }
}
