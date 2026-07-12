# Hexagonal architecture (Ports & Adapters)

Bounded-context-map.md decides *what* the modules are. This document decides
*how each one is laid out internally* so business logic never depends on
NestJS, TypeORM, or any specific transport - only the reverse.

## The rule

**Dependencies point inward, always: `infrastructure → application → domain`.**
`domain/` never imports anything from `application/` or `infrastructure/`, and
never imports a framework package. If a file under `domain/` needs `@nestjs/*`
or `typeorm`, that's the signal something is in the wrong layer.

## Folder template

Every bounded context module (existing or planned) follows this shape:

```
<module>/
  domain/
    <aggregate>.ts                  # aggregate root - a plain class, no decorators
    entities/
      <entity>.ts
    value-objects/
      <name>.vo.ts
    events/
      <name>.event.ts                # plain data classes/interfaces
    <aggregate>.domain-service.ts    # only when logic spans multiple aggregates in this context

  application/
    use-cases/
      <verb-noun>.use-case.ts         # implements an inbound port; orchestrates domain + outbound ports
    ports/
      in/
        <verb-noun>.port.ts           # inbound port interface - what an adapter calls
      out/
        <aggregate>-repository.port.ts
        <external-system>-gateway.port.ts
    projections/
      <read-model>.projector.ts       # event listener that updates a read-model table (see hydration-and-projection.md)

  infrastructure/
    adapters/
      in/
        http/
          <aggregate>.controller.ts   # NestJS controller - translates HTTP <-> use-case input/output
        events/
          <consumer>.listener.ts       # @OnEvent handlers that call a use case in reaction to another context's event
      out/
        persistence/
          <aggregate>.repository.ts    # implements the repository port using TypeORM
          <aggregate>.mapper.ts         # translates ORM entity <-> domain aggregate (the Hydration step)
        gateways/
          <external-system>.gateway.ts  # implements a gateway port (HTTP client, SDK wrapper, etc.)
    persistence/
      entities/
        <aggregate>.orm-entity.ts       # @Entity()-decorated class, shaped for the database, NOT the same class as the domain aggregate
      migrations/
    read-models/
      <read-model>.orm-entity.ts        # denormalized projection table - a second, independent persistence shape

  <module>.module.ts                    # NestJS wiring: binds each port interface to its concrete adapter via DI tokens
```

## Why the domain aggregate and the ORM entity are two different classes

This is the detail most likely to get collapsed under time pressure, so it's
worth stating why it isn't optional here: an ORM entity is shaped by the
database (foreign keys, column types, nullable columns for partially-filled
drafts). A domain aggregate is shaped by business behavior (methods that
enforce invariants, value objects, a constructor that can't produce an invalid
instance). Merging them means either the domain model bends to fit the schema
(anemic domain model - all the actual validation ends up scattered across
services) or the schema bends to fit the domain (fighting the ORM). Keeping
them separate, connected by an explicit `Mapper`, is what makes `domain/`
independently unit-testable with zero database and zero DI container - you
`new` an aggregate and call a method on it in a plain Jest test.

## Ports, concretely

An **inbound port** is the interface an adapter calls to make something
happen - named from the caller's perspective:

```ts
// application/ports/in/create-project.port.ts
export interface CreateProjectPort {
  execute(command: CreateProjectCommand): Promise<ProjectId>;
}
```

An **outbound port** is the interface the application layer depends on to do
something it can't do itself - named from the application layer's
perspective, with the concrete implementation living in `infrastructure/`:

```ts
// application/ports/out/project-repository.port.ts
export interface ProjectRepositoryPort {
  findById(id: ProjectId): Promise<Project | null>;
  save(project: Project): Promise<void>;
}
```

```ts
// infrastructure/adapters/out/persistence/project.repository.ts
@Injectable()
export class TypeOrmProjectRepository implements ProjectRepositoryPort {
  async findById(id: ProjectId) {
    const row = await this.orm.findOneBy({ id: id.value });
    return row ? ProjectMapper.toDomain(row) : null;
  }
  async save(project: Project) {
    await this.orm.save(ProjectMapper.toPersistence(project));
  }
}
```

Binding happens once, in the module file, exactly like the existing
`CorePropertiesModule.forRoot({ validatorClass })` DI-token-override pattern
already does for `PROPERTY_VALIDATOR`:

```ts
// project-delivery.module.ts
@Module({
  providers: [
    { provide: PROJECT_REPOSITORY_PORT, useClass: TypeOrmProjectRepository },
    { provide: CREATE_PROJECT_PORT, useClass: CreateProjectUseCase },
  ],
})
export class ProjectDeliveryModule {}
```

This is precisely why ports & adapters composes so naturally with the
existing core/overlay architecture (see
`docs/creating-a-new-overlay.md`): an overlay swapping in a custom
`ProjectRepositoryPort` implementation (a different database, a legacy system
adapter) or a custom `CreateProjectPort` (extra validation) is just another
`useClass` override at the same seam already used for
`CorePropertiesModule.forRoot()`.

## Worked example: today's Properties module, named against this template

`packages/core-api/src/properties` doesn't use these folder names yet, but the
concepts already map cleanly, which is the best evidence the pattern fits this
codebase rather than fighting it:

| Template concept | Today's equivalent |
|---|---|
| Domain aggregate | `Property` entity class |
| Inbound port | Implicit - `PropertiesController` calls `PropertiesService` directly |
| Outbound port | `PROPERTY_VALIDATOR` token + `PropertyValidator` interface |
| Adapter (outbound) | `DefaultPropertyValidator`, or an overlay's `DemoPropertyValidator` |
| Domain event | `PROPERTY_CREATED_EVENT` |
| Projector | `examples/demo-overlay/api`'s `DemoApprovalModule`'s `@OnEvent('property.created')` listener |

The gap between today's code and the full template is the missing explicit
`ProjectRepositoryPort` interface (today `PropertiesService` calls
`TenancyService.getEntityManager()` directly) and the missing domain/ORM
entity split (`Property` is currently both at once). Closing that gap is
exactly the kind of change this document set is meant to guide when Project
Delivery gets built out for real - not a mandate to refactor Properties today.
