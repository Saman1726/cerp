import { openDB, type IDBPDatabase } from 'idb';

export type QueuedItemStatus = 'PENDING' | 'FAILED';

/**
 * Fields every offline-queued upload needs, regardless of feature. Extend this with
 * feature-specific fields (e.g. `projectId`, `kind`) in the feature's own queue module.
 */
export interface QueuedItemEnvelope {
  /** Client-generated UUID; doubles as the idempotency key once it reaches the backend. */
  clientId: string;
  originalFileName: string;
  /** Content type of the already-compressed blob (e.g. image/webp). */
  contentType: string;
  byteSize: number;
  blob: Blob;
  status: QueuedItemStatus;
  attempts: number;
  lastError?: string;
  createdAt: string;
  updatedAt: string;
}

/** After this many failed attempts an item stops auto-retrying and needs a manual nudge. */
export const MAX_SYNC_ATTEMPTS = 8;

export interface OfflineQueueConfig {
  /** Each feature gets its own IndexedDB database, so a bug in one queue can't jam another. */
  dbName: string;
  storeName: string;
  dbVersion?: number;
}

export interface OfflineQueue<TItem extends QueuedItemEnvelope> {
  enqueue(
    input: Omit<
      TItem,
      'clientId' | 'status' | 'attempts' | 'lastError' | 'createdAt' | 'updatedAt'
    >,
  ): Promise<TItem>;
  list(): Promise<TItem[]>;
  remove(clientId: string): Promise<void>;
  markFailed(clientId: string, error: string): Promise<void>;
}

// crypto.randomUUID() only exists in secure contexts (HTTPS, or localhost) -
// on a plain-HTTP LAN address (e.g. a phone testing against a laptop's dev
// server over Wi-Fi) it's simply absent, and calling it throws before this
// function ever reaches IndexedDB. getRandomValues() has no such
// restriction, so build a v4 UUID from it directly instead of relying on
// randomUUID() being there.
function generateClientId(): string {
  if (typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6]! & 0x0f) | 0x40;
  bytes[8] = (bytes[8]! & 0x3f) | 0x80;
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

/**
 * Creates an IndexedDB-backed offline upload queue. Each call owns its own database (per
 * `config.dbName`) so independent features (e.g. Site Capture vs. workflow step attachments)
 * can't corrupt or block each other's queues.
 */
export function createOfflineQueue<TItem extends QueuedItemEnvelope>(
  config: OfflineQueueConfig,
): OfflineQueue<TItem> {
  let dbPromise: Promise<IDBPDatabase> | null = null;

  function getDB(): Promise<IDBPDatabase> {
    if (!dbPromise) {
      dbPromise = openDB(config.dbName, config.dbVersion ?? 1, {
        upgrade(db) {
          if (!db.objectStoreNames.contains(config.storeName)) {
            db.createObjectStore(config.storeName, { keyPath: 'clientId' });
          }
        },
      });
    }
    return dbPromise;
  }

  return {
    async enqueue(input) {
      const now = new Date().toISOString();
      const record = {
        ...input,
        clientId: generateClientId(),
        status: 'PENDING',
        attempts: 0,
        createdAt: now,
        updatedAt: now,
      } as unknown as TItem;
      const db = await getDB();
      await db.put(config.storeName, record);
      return record;
    },

    async list() {
      const db = await getDB();
      const all = (await db.getAll(config.storeName)) as TItem[];
      return all.sort((a, b) => a.createdAt.localeCompare(b.createdAt));
    },

    async remove(clientId) {
      const db = await getDB();
      await db.delete(config.storeName, clientId);
    },

    async markFailed(clientId, error) {
      const db = await getDB();
      const tx = db.transaction(config.storeName, 'readwrite');
      const existing = (await tx.store.get(clientId)) as TItem | undefined;
      if (existing) {
        existing.status = 'FAILED';
        existing.attempts += 1;
        existing.lastError = error;
        existing.updatedAt = new Date().toISOString();
        await tx.store.put(existing);
      }
      await tx.done;
    },
  };
}
