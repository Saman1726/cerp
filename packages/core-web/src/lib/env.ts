export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://api-java:8080';
export const PROJECT_SERVICE_URL = process.env.NEXT_PUBLIC_PROJECT_SERVICE_URL ?? 'http://project-service-java:8085';
export const WORKFLOW_SERVICE_URL = process.env.NEXT_PUBLIC_WORKFLOW_SERVICE_URL ?? 'http://workflow-service-java:8089';
export const VENDOR_SERVICE_URL = process.env.NEXT_PUBLIC_VENDOR_SERVICE_URL ?? 'http://vendor-service-java:8085';
export const IDENTITY_SERVICE_URL = process.env.NEXT_PUBLIC_IDENTITY_SERVICE_URL ?? 'http://identity-service-java:8090';

export const ENV = {
  API_URL,
  PROJECT_SERVICE_URL,
  WORKFLOW_SERVICE_URL,
  VENDOR_SERVICE_URL,
  IDENTITY_SERVICE_URL,
};
