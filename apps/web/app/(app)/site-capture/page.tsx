import { SiteCaptureForm } from '@cerp/core-web/site-capture';

interface SiteCapturePageProps {
  searchParams: Promise<{ projectId?: string; subProjectId?: string }>;
}

export default async function SiteCapturePage({ searchParams }: SiteCapturePageProps) {
  const { projectId, subProjectId } = await searchParams;

  if (!projectId) {
    return (
      <div className="p-6 text-sm text-muted-foreground">
        Open this page from a project (e.g. <code>/site-capture?projectId=…</code>) to start capturing photos.
      </div>
    );
  }

  return (
    <div className="flex justify-center p-6">
      <SiteCaptureForm projectId={projectId} subProjectId={subProjectId} />
    </div>
  );
}
