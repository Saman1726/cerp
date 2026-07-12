import { DynamicModule, Module, Provider, Type } from '@nestjs/common';
import { PropertiesController } from './properties.controller';
import { PropertiesService } from './properties.service';
import { DefaultPropertyValidator, PROPERTY_VALIDATOR, PropertyValidator } from './properties.tokens';

export interface CorePropertiesModuleOptions {
  /** Plain class - the common case. Nest resolves it via the module's own DI container. */
  validatorClass?: Type<PropertyValidator>;
  /** Escape hatch for a useFactory/async provider instead of a plain class. */
  validatorProvider?: Provider;
}

// No TypeOrmModule.forFeature([Property]) here on purpose - Property is
// tenant-scoped and must only be reached through TenancyService.
@Module({})
export class CorePropertiesModule {
  static forRoot(options: CorePropertiesModuleOptions = {}): DynamicModule {
    return {
      module: CorePropertiesModule,
      controllers: [PropertiesController],
      providers: [
        PropertiesService,
        options.validatorProvider ?? {
          provide: PROPERTY_VALIDATOR,
          useClass: options.validatorClass ?? DefaultPropertyValidator,
        },
      ],
      exports: [PropertiesService],
    };
  }
}
