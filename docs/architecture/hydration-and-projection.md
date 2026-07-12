# Hydration & Projection

Hexagonal architecture says a repository port's shape is
`findById(id): Promise<Aggregate | null>` and `save(aggregate): Promise<void>`,
and hides *how* that happens behind the port. This document is about that
"how" - specifically, a CQRS-lite split: a **write side** that hydrates an
aggregate to run a command against it, and a **read side** that projects the
outcomes into denormalized views, so cross-context reads never depend on
another context's write-side tables.

## Hydration (write side)

**Hydration** is reconstructing an in-memory, behavior-rich aggregate from
persisted state before a command mutates it. There are two strategies, and
the choice is made **per bounded context**, not globally - the repository
port's interface is identical either way, so callers never know which one a
given context uses.

### State-based hydration (the default)

Load the current row(s), map to the domain aggregate via a `Mapper`, call a
method on the aggregate (which enforces invariants and records any domain
events internally), map back, save. This is the right default for most
contexts - Project Delivery, Sales, Leasing, Procurement, HR & Payroll,
Facility Management, and everything else in the catalog unless stated
otherwise - because the business requirement is "what's true right now,"
and a normal audit/updated-at column is enough.

```ts
async execute(cmd: RecordPaymentCommand) {
  const booking = await this.bookingRepo.findById(cmd.bookingId);   // hydrate
  if (!booking) throw new NotFoundError();
  booking.recordPayment(cmd.amount, cmd.receivedAt);                // mutate + records event internally
  await this.bookingRepo.save(booking);                             // persist new state
  await this.events.emitAsync(booking.pullDomainEvents());          // publish for projectors
}
```

### Event-sourced hydration (Bookkeeping & Ledger only, for now)

Rebuild the aggregate's current state by replaying its full ordered event
stream (`apply(event)` for each past event) instead of reading a snapshot row.
This is deliberately reserved for **Bookkeeping & Ledger**, because that
context's actual business requirement - an immutable, fully-reconstructable
audit trail where corrections happen via reversing entries, never edits - is
what event sourcing is *for*. Modeling the ledger as an event stream isn't
extra ceremony there; it's the natural shape of the domain. Forcing every
other context into that shape *would* be extra ceremony, which is why it
isn't the default.

```ts
async findById(id: JournalId): Promise<JournalEntry | null> {
  const events = await this.eventStore.readStream(`journal-${id.value}`);
  if (events.length === 0) return null;
  return JournalEntry.hydrate(events);   // replays apply(event) for each, in order
}
```

A snapshot (periodic materialized state alongside the stream, so replay only
covers events since the last snapshot) can be added later purely as a
performance optimization once a journal's event count makes full replay slow
- that's an infrastructure detail behind the same port, not a domain-model
change.

### Why the port hides this choice

```ts
export interface JournalRepositoryPort {
  findById(id: JournalId): Promise<JournalEntry | null>;
  save(entry: JournalEntry): Promise<void>;
}
```

Application-layer use cases depend only on this interface. Whether
`findById` replays an event stream or reads a snapshot row is invisible to
every caller - which is the entire point of putting hydration strategy behind
a hexagonal port instead of leaking it into use-case code.

## Projection (read side)

**Projection** is building a denormalized, query-optimized read model by
consuming domain events, kept independent of the write-side aggregate's own
storage shape. This is what makes cross-context queries (dashboards, reports)
cheap: they read a small precomputed table, not a live join/aggregation
across every other context's write-side rows.

### The mechanism

1. A use case, after `save()`ing an aggregate, publishes the domain events the
   aggregate recorded (today: `EventEmitter2.emitAsync()`, per the existing
   `property.created` → `emitAsync` fix already made in this codebase so a
   listener never races the request-scoped connection teardown).
2. A **Projector** - an event listener owned by the *consuming* context, not
   the publishing one - updates one read-model table per event it cares
   about. `examples/demo-overlay/api`'s `DemoApprovalModule` listening for
   `property.created` and writing a `demo_property_audit` row is already
   exactly this pattern, just not yet named "Projector" in the code.
3. Projectors must be **idempotent** - safe to run twice on the same event -
   since at-least-once delivery is the realistic guarantee once this moves
   past in-process `EventEmitter2`.

### Cross-context projections: Reporting & Analytics

Reporting & Analytics (see `modules/generic-and-platform.md`) is a bounded
context with **no aggregates and no write side of its own** - it exists
purely to hydrate projections from every other context's published events.
The Dashboard page's stat cards, built this session, are the first real
instance: `GET /properties` and `GET /partners` are called live today because
the counts are cheap at current data volumes, but the pattern this document
recommends for real scale is a `PortfolioDashboardProjection` table -
incrementally updated as `PropertyCreated`, `UnitBooked`, `LeaseSigned`, etc.
arrive - so a dashboard load becomes one indexed `SELECT`, not a live count
across every tenant's full properties/partners/leases tables. This is exactly
the failure mode flagged when this system's scaling requirements were
discussed: a PM/investor dashboard aggregating across thousands of active
projects live, on every page load, does not survive real volume; a
precomputed, incrementally-updated projection does.

### The dual-write problem, and why an outbox is the recommended evolution

Calling `save(aggregate)` and then `emitAsync(events)` as two separate steps
has a real failure mode: if the process crashes between them, the aggregate's
new state persists but the event never fires, so every projection silently
drifts out of sync forever. `EventEmitter2` in-process is fine for now (single
process, low volume, acceptable for the current stage of this project), but
the documented recommendation for when projections must survive crashes and
restarts without dropping events is the **transactional outbox pattern**:
write the domain event row to an `outbox` table in the *same database
transaction* as the aggregate's own save, then a separate relay process reads
`outbox` and publishes to the real event bus, deleting/marking rows once
delivered. This turns "save aggregate, then maybe publish" into a single
atomic write, with publishing as an at-least-once background concern instead
of a same-request race.

## Summary: which contexts use which strategy

| Context | Hydration | Notes |
|---|---|---|
| Bookkeeping & Ledger | Event-sourced | Immutability + audit trail is the actual business requirement |
| Every other context in the catalog | State-based | Current-state CRUD-shaped consistency is sufficient |
| Reporting & Analytics | N/A (no write side) | Pure projection consumer of every other context's events |
