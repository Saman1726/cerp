import { openDB, type DBSchema, type IDBPDatabase } from 'idb';
import type { SiteCaptureKind } from '@cerp/shared-types';

export type QueuedCaptureStatus = 'PENDING' | 'FAILED';

export interface QueuedCapture {
  /** Client-generated UUID; doubles as the idempotency key once it reaches the backend. */
  clientId: string;
  projectId: string;
  subProjectId?: string;
  kind: SiteCaptureKind;
  notes?: string;
  /** ISO timestamp taken at capture time, not upload time. */
  capturedAt: string;
  originalFileName: string;
  /** Content type of the already-compressed blob (e.g. image/webp). */
  contentType: string;
  byteSize: number;
  blob: Blob;
  status: QueuedCaptureStatus;
  attempts: number;
  lastError?: string;
  createdAt: string;
  updatedAt: string;
}

export type NewQueuedCapture = Omit<
  QueuedCapture,
  'clientId' | 'status' | 'attempts' | 'lastError' | 'createdAt' | 'updatedAt'
>;

/** After this many failed attempts a capture stops auto-retrying and needs a manual nudge. */
export const MAX_SYNC_ATTEMPTS = 8;

interface SiteCaptureDB extends DBSchema {
  captures: {
    key: string;
    value: QueuedCapture;
  };
}

const DB_NAME = 'cerp-site-capture';
const DB_VERSION = 1;
const STORE_NAME = 'captures';

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

let dbPromise: Promise<IDBPDatabase<SiteCaptureDB>> | null = null;

function getDB(): Promise<IDBPDatabase<SiteCaptureDB>> {
  if (!dbPromise) {
    dbPromise = openDB<SiteCaptureDB>(DB_NAME, DB_VERSION, {
      upgrade(db) {
        db.createObjectStore(STORE_NAME, { keyPath: 'clientId' });
      },
    });
  }
  return dbPromise;
}

/** Persists a captured photo + form metadata locally and returns the queued record. */
export async function enqueueCapture(input: NewQueuedCapture): Promise<QueuedCapture> {
  const now = new Date().toISOString();
  const record: QueuedCapture = {
    ...input,
    clientId: generateClientId(),
    status: 'PENDING',
    attempts: 0,
    createdAt: now,
    updatedAt: now,
  };
  const db = await getDB();
  await db.put(STORE_NAME, record);
  return record;
}

/** All captures still waiting to reach the backend, oldest first. */
export async function listQueuedCaptures(): Promise<QueuedCapture[]> {
  const db = await getDB();
  const all = await db.getAll(STORE_NAME);
  return all.sort((a, b) => a.createdAt.localeCompare(b.createdAt));
}

export async function removeQueuedCapture(clientId: string): Promise<void> {
  const db = await getDB();
  await db.delete(STORE_NAME, clientId);
}

export async function markCaptureFailed(clientId: string, error: string): Promise<void> {
  const db = await getDB();
  const tx = db.transaction(STORE_NAME, 'readwrite');
  const existing = await tx.store.get(clientId);
  if (existing) {
    existing.status = 'FAILED';
    existing.attempts += 1;
    existing.lastError = error;
    existing.updatedAt = new Date().toISOString();
    await tx.store.put(existing);
  }
  await tx.done;
}
