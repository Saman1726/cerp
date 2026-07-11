import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { AppModule } from './app.module';
import { AllExceptionsFilter } from './common/filters/all-exceptions.filter';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  const configService = app.get(ConfigService);

  // No CORS here: the browser never calls this API directly. The Next.js app
  // is a BFF - its server-side code calls this API and holds the session
  // cookie itself. See apps/web/lib/api-client.ts.
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
  app.useGlobalFilters(new AllExceptionsFilter());

  const port = configService.get<number>('port') ?? 3000;
  await app.listen(port);
}

void bootstrap();
