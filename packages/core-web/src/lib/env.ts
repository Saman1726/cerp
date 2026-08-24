// Each URL prefers a non-prefixed override first, e.g. PROJECT_SERVICE_URL.
// That var is only ever visible server-side (Next strips non-NEXT_PUBLIC_
// vars from the client bundle), so this is how a container gives its own
// server-side code a different address (Docker-internal hostname) than
// whatever NEXT_PUBLIC_* value the browser bundle bakes in for the actual
// client - needed once server and browser aren't the same network anymore,
// e.g. testing from a phone against a locally-run backend.
export const API_URL = process.env.API_URL ?? process.env.NEXT_PUBLIC_API_URL ?? 'http://api-java:8080';
export const PROJECT_SERVICE_URL = process.env.PROJECT_SERVICE_URL ?? process.env.NEXT_PUBLIC_PROJECT_SERVICE_URL ?? 'http://project-service-java:8085';
export const WORKFLOW_SERVICE_URL = process.env.WORKFLOW_SERVICE_URL ?? process.env.NEXT_PUBLIC_WORKFLOW_SERVICE_URL ?? 'http://workflow-service-java:8089';
export const VENDOR_SERVICE_URL = process.env.VENDOR_SERVICE_URL ?? process.env.NEXT_PUBLIC_VENDOR_SERVICE_URL ?? 'http://vendor-service-java:8085';
export const IDENTITY_SERVICE_URL = process.env.IDENTITY_SERVICE_URL ?? process.env.NEXT_PUBLIC_IDENTITY_SERVICE_URL ?? 'http://identity-service-java:8090';
// Only ever called from Next.js Route Handlers (server-side), never directly
// from the browser - the browser talks to the presigned storage URL
// directly instead - so this one has no real client-side use yet, but keeps
// the same override shape as the others for consistency.
export const DOCUMENT_SERVICE_URL = process.env.DOCUMENT_SERVICE_URL ?? process.env.NEXT_PUBLIC_DOCUMENT_SERVICE_URL ?? 'http://document-service-java:8092';
export const IFC_RENDERER_URL = process.env.IFC_RENDERER_URL ?? process.env.NEXT_PUBLIC_IFC_RENDERER_URL ?? 'http://ifc-renderer-python:8093';

export const ENV = {
  API_URL,
  PROJECT_SERVICE_URL,
  WORKFLOW_SERVICE_URL,
  VENDOR_SERVICE_URL,
  IDENTITY_SERVICE_URL,
  DOCUMENT_SERVICE_URL,
  IFC_RENDERER_URL,
};
