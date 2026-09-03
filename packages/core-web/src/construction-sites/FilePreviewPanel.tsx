'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import {
  FileText,
  Image,
  Box,
  Eye,
  Download,
  Loader2,
  Clock,
  CheckCircle2,
  AlertCircle,
  X,
  RefreshCw,
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import type { IfcFileActions, IFCFile } from './types';

interface FilePreviewPanelProps {
  siteId: string | null;
  siteName: string;
  actions: IfcFileActions;
  onClose?: () => void;
}

type FileSelection = {
  file: IFCFile;
  view: 'front' | 'side' | 'top' | 'perspective';
};

export function FilePreviewPanel({ siteId, siteName, actions, onClose }: FilePreviewPanelProps) {
  const [files, setFiles] = useState<IFCFile[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedFile, setSelectedFile] = useState<FileSelection | null>(null);
  const [selectedView, setSelectedView] = useState<'front' | 'side' | 'top' | 'perspective'>('perspective');

  const filesRef = useRef(files);

  // Sync ref outside of render cycle so the polling effects always read
  // the freshest list without triggering re-renders.
  useEffect(() => {
    filesRef.current = files;
  }, [files]);

  /**
   * Fetch a fresh presigned URL from the backend.
   * The backend re-signs the same MinIO object each time, so the URL
   * is always valid regardless of when it was originally generated.
   */
  const getFreshUrl = useCallback(async (
    siteId: string,
    fileId: string,
    urlType: 'preview' | 'render' | 'export' | 'metadata',
  ): Promise<string> => {
    const res = await fetch(`/api/construction-sites/${siteId}/ifc-files/${fileId}/${urlType}`, {
      method: 'GET',
      credentials: 'include',
    });
    if (!res.ok) throw new Error(`Failed to get ${urlType} URL`);
    const data = await res.json();
    return data.url;
  }, []);

  const loadFiles = useCallback(async () => {
    if (!siteId) return;
    setLoading(true);
    try {
      const result = await actions.listFiles(siteId);
      setFiles(result);
    } catch {
      // Silently fail
    } finally {
      setLoading(false);
    }
  }, [actions, siteId]);

  // Poll every 5s while files are still being processed (QUEUED / PROCESSING)
  useEffect(() => {
    const hasActive = filesRef.current.some((f) => f.status === 'QUEUED' || f.status === 'PROCESSING');
    if (!hasActive) return;
    const interval = setInterval(loadFiles, 5000);
    return () => clearInterval(interval);
  }, [loadFiles]);

  // Poll every 15s for completed files so that once preview/render URLs
  // become available (after background processing finishes), they are picked
  // up even though the "active" polling has stopped.
  useEffect(() => {
    if (!siteId) return;
    const interval = setInterval(loadFiles, 15000);
    return () => clearInterval(interval);
  }, [siteId, loadFiles]);

  /* eslint-disable react-hooks/set-state-in-effect */
  // Clearing state here is intentional: when siteId becomes null we must
  // reset files / selection so stale data from the previous site does not
  // leak into the UI.  The loadFiles call is in a separate effect below
  // to keep the "clear" and "load" concerns distinct.
  useEffect(() => {
    if (!siteId) {
      setFiles([]);
      setSelectedFile(null);
    }
  }, [siteId]);

  useEffect(() => {
    if (siteId) {
      loadFiles();
    }
  }, [siteId, loadFiles]);
  /* eslint-enable react-hooks/set-state-in-effect */

  const handleSelectFile = (file: IFCFile, view: 'front' | 'side' | 'top' | 'perspective') => {
    setSelectedFile({ file, view });
    setSelectedView(view);
  };

  if (!siteId) {
    return (
      <div className="flex h-96 flex-col items-center justify-center rounded-lg border border-border bg-card">
        <FileText className="size-10 text-muted-foreground/30" />
        <p className="mt-3 text-sm text-muted-foreground">Select a construction site to preview files</p>
      </div>
    );
  }

  return (
    <div className="flex h-96 flex-col overflow-hidden rounded-lg border border-border bg-card">
      <div className="flex shrink-0 items-center justify-between border-b px-4 py-3">
        <div className="flex items-center gap-2">
          <FileText className="size-4 text-muted-foreground" />
          <h3 className="text-sm font-semibold">IFC Files &mdash; {siteName}</h3>
          <Badge variant="secondary" className="ml-1 text-xs">
            {files.length}
          </Badge>
        </div>
        {onClose ? (
          <Button size="icon" variant="ghost" className="size-7" onClick={onClose}>
            <X className="size-3.5" />
          </Button>
        ) : (
          <Button size="icon" variant="ghost" className="size-7" onClick={loadFiles} disabled={loading}>
            <RefreshCw className={`size-3.5 ${loading ? 'animate-spin' : ''}`} />
          </Button>
        )}
      </div>
      <div className="flex flex-1 overflow-hidden">
        <div className="flex w-60 shrink-0 flex-col border-r overflow-y-auto">
          {files.length === 0 && !loading ? (
            <div className="flex flex-1 flex-col items-center justify-center gap-2 p-4 text-center">
              <FileText className="size-8 text-muted-foreground/40" />
              <p className="text-xs text-muted-foreground">No IFC files uploaded yet.</p>
            </div>
          ) : (
            <div className="flex flex-col">
              {[...files]
                .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
                .map((file) => (
                  <button
                    key={file.id}
                    type="button"
                    onClick={() => handleSelectFile(file, selectedView)}
                    className={`group flex items-start gap-2.5 border-b border-border px-3 py-2.5 text-left hover:bg-muted/50 transition-colors ${
                      selectedFile?.file.id === file.id ? 'bg-muted/80 ring-inset ring-1 ring-primary/30' : ''
                    }`}
                  >
                    <div className="mt-0.5 shrink-0">
                      <FileText className="size-4 text-muted-foreground" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-xs font-medium">{file.fileName}</p>
                      <div className="mt-0.5 flex items-center gap-1.5">
                        <FileStatusBadge status={file.status} />
                      </div>
                    </div>
                  </button>
                ))}
            </div>
          )}
        </div>
        <div className="flex min-w-0 flex-1 flex-col overflow-hidden">
          {selectedFile ? (
            <FilePreviewContent siteId={siteId} file={selectedFile.file} view={selectedFile.view} onViewChange={setSelectedView} getFreshUrl={getFreshUrl} />
          ) : (
            <div className="flex flex-1 items-center justify-center p-8">
              <div className="text-center">
                <Eye className="mx-auto size-10 text-muted-foreground/30" />
                <p className="mt-2 text-sm text-muted-foreground">Select a file to preview</p>
                <p className="mt-1 text-xs text-muted-foreground/60">Upload or import an IFC file to get started</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
function FileStatusBadge({ status }: { status: IFCFile['status'] }) {
  const styles: Record<IFCFile['status'], { bg: string; text: string; icon: React.ReactNode }> = {
    QUEUED: { bg: 'bg-yellow-500/10', text: 'text-yellow-600', icon: <Clock className="size-2.5" /> },
    PROCESSING: { bg: 'bg-blue-500/10', text: 'text-blue-600', icon: <Loader2 className="size-2.5 animate-spin" /> },
    COMPLETED: { bg: 'bg-green-500/10', text: 'text-green-600', icon: <CheckCircle2 className="size-2.5" /> },
    FAILED: { bg: 'bg-red-500/10', text: 'text-red-600', icon: <AlertCircle className="size-2.5" /> },
  };
  const s = styles[status];
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-1.5 py-0.5 text-[10px] font-medium ${s.bg} ${s.text}`}>
      {s.icon}
      {status}
    </span>
  );
}

function FilePreviewContent({
  siteId,
  file,
  view,
  onViewChange,
  getFreshUrl,
}: {
  siteId: string | null;
  file: IFCFile;
  view: 'front' | 'side' | 'top' | 'perspective';
  onViewChange: (v: 'front' | 'side' | 'top' | 'perspective') => void;
  getFreshUrl: (siteId: string, fileId: string, urlType: 'preview' | 'render' | 'export' | 'metadata') => Promise<string>;
}) {
  return (
    <div className="flex h-full flex-col overflow-hidden">
      <div className="flex shrink-0 items-center justify-between border-b px-4 py-2">
        <div className="flex items-center gap-2">
          <span className="text-xs font-medium text-muted-foreground">{file.fileName}</span>
          <FileStatusBadge status={file.status} />
        </div>
        <div className="flex items-center gap-1">
          <span className="text-xs text-muted-foreground mr-1">View:</span>
          {(['perspective', 'front', 'side', 'top'] as const).map((v) => (
            <button
              key={v}
              type="button"
              onClick={() => onViewChange(v)}
              className={`rounded px-2 py-0.5 text-xs font-medium transition-colors ${
                view === v
                  ? 'bg-primary text-primary-foreground'
                  : 'text-muted-foreground hover:bg-muted hover:text-foreground'
              }`}
            >
              {v.charAt(0).toUpperCase() + v.slice(1)}
            </button>
          ))}
        </div>
      </div>
      <div className="flex min-h-0 flex-1 overflow-auto bg-muted/20">
        <PreviewRenderer siteId={siteId} file={file} view={view} />
      </div>
      <div className="flex shrink-0 items-center gap-2 border-t px-4 py-2">
        {file.previewUrl && (
          <Button size="sm" variant="outline"
                  onClick={() => getFreshUrl(siteId!, file.id, 'preview')
                    .then((url: string) => window.open(url, '_blank'))
                    .catch(() => {})}>
            {/* eslint-disable-next-line jsx-a11y/alt-text */}
            <Image aria-hidden="true" className="mr-1.5 size-3" />
            2D Preview
          </Button>
        )}
        {file.blenderRenderUrl && (
          <Button size="sm" variant="outline"
                  onClick={() => getFreshUrl(siteId!, file.id, 'render')
                    .then((url: string) => window.open(url, '_blank'))
                    .catch(() => {})}>
            <Box className="mr-1.5 size-3" />
            3D Render ({view})
          </Button>
        )}
        {file.exportUrl && (
          <Button size="sm" variant="outline"
                  onClick={() => getFreshUrl(siteId!, file.id, 'export')
                    .then((url: string) => window.open(url, '_blank'))
                    .catch(() => {})}>
            <Download className="mr-1.5 size-3" />
            Export 3D Model
          </Button>
        )}
        {file.metadataUrl && (
          <Button size="sm" variant="outline"
                  onClick={() => getFreshUrl(siteId!, file.id, 'metadata')
                    .then((url: string) => window.open(url, '_blank'))
                    .catch(() => {})}>
            <Eye className="mr-1.5 size-3" />
            View Metadata
          </Button>
        )}
      </div>
    </div>
  );
}

function PreviewRenderer({
  siteId,
  file,
  view,
}: {
  siteId: string | null;
  file: IFCFile;
  view: 'front' | 'side' | 'top' | 'perspective';
}) {
  const renderView = view === 'perspective' ? null : view;

  // Fetch fresh presigned URLs so the images never show expired links.
  const [renderUrl, setRenderUrl] = useState<string | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  // Fetch fresh presigned URLs when the file changes or renderView changes.
  // ── Render URL ─────────────────────────────────────────────────────────
  // For specific angles (front / side / top) we only fetch when the stored
  // render view matches the currently selected view.  For perspective (where
  // renderView is null) we always fetch when a Blender render exists.
  useEffect(() => {
    if (!siteId) return;

    const showSpecificRender = renderView !== null && file.blenderRenderView === renderView;
    const showPerspectiveRender = renderView === null && file.blenderRenderUrl;

    if (showSpecificRender || showPerspectiveRender) {
      fetch(`/api/construction-sites/${siteId}/ifc-files/${file.id}/render`, {
        method: 'GET',
        credentials: 'include',
      })
        .then(res => res.ok ? res.json().then(d => d.url) : null)
        .then(url => setRenderUrl(url ?? null))
        .catch(() => {});
    }

    // ── Preview URL ──────────────────────────────────────────────────────
    if (file.previewUrl) {
      fetch(`/api/construction-sites/${siteId}/ifc-files/${file.id}/preview`, {
        method: 'GET',
        credentials: 'include',
      })
        .then(res => res.ok ? res.json().then(d => d.url) : null)
        .then(url => setPreviewUrl(url ?? null))
        .catch(() => {});
    }
  }, [siteId, file.id, file.blenderRenderUrl, file.blenderRenderView, file.previewUrl, renderView]);

  const effectiveRenderUrl = renderUrl ?? file.blenderRenderUrl;
  const effectivePreviewUrl = previewUrl ?? file.previewUrl;

  if (effectiveRenderUrl) {
    return (
      <div className="flex items-center justify-center p-4">
            {/* Remote presigned MinIO URLs — no next.config remotePatterns configured. */}
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={effectiveRenderUrl}
              alt={`Blender render - ${view}`}
          className="max-w-full max-h-[60vh] rounded-lg shadow object-contain"
        />
      </div>
    );
  }

  if (effectivePreviewUrl) {
    return (
      <div className="flex items-center justify-center p-4">
            {/* Remote presigned MinIO URL — no next.config remotePatterns configured. */}
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={effectivePreviewUrl}
              alt="2D Preview"
          className="max-w-full max-h-[60vh] rounded-lg shadow object-contain"
        />
      </div>
    );
  }

  return (
    <div className="flex flex-1 items-center justify-center p-8">
      <div className="text-center">
        {file.status === 'PROCESSING' ? (
          <>
            <Loader2 className="mx-auto size-10 animate-spin text-blue-500" />
            <p className="mt-2 text-sm font-medium">Processing...</p>
            <p className="mt-1 text-xs text-muted-foreground">Preview will appear when generation is complete.</p>
          </>
        ) : file.status === 'QUEUED' ? (
          <>
            <Clock className="mx-auto size-10 text-yellow-500" />
            <p className="mt-2 text-sm font-medium">Queued</p>
            <p className="mt-1 text-xs text-muted-foreground">Waiting to be processed.</p>
          </>
        ) : file.status === 'FAILED' ? (
          <>
            <AlertCircle className="mx-auto size-10 text-red-500" />
            <p className="mt-2 text-sm font-medium text-red-500">Processing Failed</p>
            <p className="mt-1 text-xs text-muted-foreground">Try generating a preview again.</p>
          </>
        ) : (
          <>
            <FileText className="mx-auto size-10 text-muted-foreground/40" />
            <p className="mt-2 text-sm text-muted-foreground">No preview available yet.</p>
            <p className="mt-1 text-xs text-muted-foreground/60">Trigger a 2D Preview or 3D Render to generate preview images.</p>
          </>
        )}
      </div>
    </div>
  );
}