# cerp target architecture

This folder documents the **target module structure** for cerp's domain layer -
where the system is heading, not what's implemented today. It is documentation
only: no code lives under `docs/architecture/`, and nothing here is wired into
the build. Treat it as the reference a future implementation (by a human or an
agent) should follow when a new module gets built out for real.

Three patterns compose to define that structure:

1. **Domain-Driven Design (DDD)** - decides *what the modules are* and *how
   they relate*. See [bounded-context-map.md](./bounded-context-map.md).
2. **Hexagonal Architecture (Ports & Adapters)** - decides *how each module is
   laid out internally* so the business logic stays independent of NestJS,
   TypeORM, or any specific transport. See
   [hexagonal-architecture.md](./hexagonal-architecture.md).
3. **Hydration & Projection (a CQRS-lite read/write split)** - decides *how a
   module loads its own state to handle a command* and *how other modules
   consume its outcomes without querying its tables directly*. See
   [hydration-and-projection.md](./hydration-and-projection.md).

The module catalog itself is split by subdomain classification:

- [modules/core-subdomain.md](./modules/core-subdomain.md) - the
  differentiating, revenue-driving contexts (Land Acquisition, Legal &
  Compliance, Project Delivery, Construction Engineering, Sales, Leasing,
  Facility Management).
- [modules/supporting-subdomain.md](./modules/supporting-subdomain.md) -
  necessary but not differentiating (CRM & Leads, Procurement, Partner
  Directory, Cost & Invoicing, Bookkeeping & Ledger, HR & Payroll, Expense &
  Reimbursement, Asset Management).
- [modules/generic-and-platform.md](./modules/generic-and-platform.md) -
  commodity capabilities and cross-cutting infrastructure (Document
  Management, Reporting & Analytics, Identity & Access, Tenancy & Platform).

## Methodology and sources

cerp's own scope statement is "an ERP for Properties and Construction." Rather
than invent a module list from scratch, this document set is modeled on
**In4Suite® ERP** (by In4Velocity Systems) - a real, shipping ERP built for
exactly that industry (real estate developers, construction firms,
infrastructure businesses), with 16 published modules covering the full
land-to-lease lifecycle. In4Suite was researched via public product and
review-site pages (see Sources below); its module *names and scope* are used
as the grounding for the bounded-context map. The specific aggregates,
entities, value objects, domain events, and ports documented in the module
catalogs are **this project's own DDD design applied to that scope** - not a
reverse-engineering of In4Suite's actual internal data model or source code,
which is proprietary and was not accessed.

Sources:
- [In4Suite® ERP - official product page](https://www.in4velocity.com/in4suite-erp/)
- [In4Suite Real Estate ERP - SoftwareSuggest](https://www.softwaresuggest.com/in4suite-real-estate-erp)

## How to read the catalog entries

Every module in `modules/*.md` follows the same template:

| Field | Meaning |
|---|---|
| **Subdomain** | Core / Supporting / Generic (DDD strategic classification - see the context map for what this implies about build-vs-buy and investment priority) |
| **Purpose** | One or two sentences, in plain language |
| **Status** | `existing` (already scaffolded in this repo, cited by path), `planned` (designed here, not yet built), or `deferred` (out of scope until a real need appears) |
| **Aggregate root(s)** | The entity/entities that own transactional consistency boundaries |
| **Entities & value objects** | The rest of the domain model |
| **Domain events** | What this module publishes for others to react to or project |
| **Inbound ports** | Use-case interfaces adapters call into (HTTP controllers, event consumers, jobs) |
| **Outbound ports** | Interfaces this module's application layer depends on (repositories, gateways to other modules or external systems) |
| **Hydration strategy** | State-based or event-sourced - see hydration-and-projection.md |
| **Relationships** | Which other bounded contexts this one talks to, and via which DDD context-mapping pattern |

## Where this repo already matches the target

Three pieces of the current codebase are worked examples of these patterns,
referenced throughout the catalogs rather than repeated:

- **`packages/core-api/src/properties`** - today's simplified stand-in for
  what the catalog calls `Project Delivery` / `Unit` inventory. Its
  `CorePropertiesModule.forRoot({ validatorClass })` DI-override is the
  existing example of a port with a swappable adapter.
- **`property.created` event + `examples/demo-overlay/api`'s
  `@OnEvent('property.created')` listener** - the existing, working precursor
  to the Projection pattern described in hydration-and-projection.md.
- **`services/partner-system` + `apps/api/src/partners`** - the existing,
  working example of an external system (a separate deployable) integrated
  through an outbound gateway port, exactly the shape the catalog calls
  `Partner Directory`.
