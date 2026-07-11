import { CallHandler, ExecutionContext, Injectable, NestInterceptor } from '@nestjs/common';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { TenancyService } from './tenancy.service';

// Releases the request's tenant-scoped connection (if one was ever acquired)
// after the response is sent or the request errors. Deliberately an
// interceptor (ordinary singleton) rather than request-scoped DI, so the rest
// of the app never pays the request-scope tax - see TenancyService.
@Injectable()
export class TenancyCleanupInterceptor implements NestInterceptor {
  constructor(private readonly tenancyService: TenancyService) {}

  intercept(_context: ExecutionContext, next: CallHandler): Observable<unknown> {
    return next.handle().pipe(finalize(() => void this.tenancyService.releaseConnection()));
  }
}
