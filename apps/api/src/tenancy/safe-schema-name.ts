const SAFE_SCHEMA_NAME = /^[a-z][a-z0-9_]{0,62}$/;

// search_path can't be parameterized like a normal query argument, so every
// schema name that reaches a raw SQL/connection-options string must be
// checked against this allow-list first.
export function assertSafeSchemaName(schemaName: string): void {
  if (!SAFE_SCHEMA_NAME.test(schemaName)) {
    throw new Error(`Refusing to use unsafe schema name: ${schemaName}`);
  }
}
