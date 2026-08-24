'use client';

import { useState, useCallback } from 'react';
import {
  FileText,
  Image,
  Box,
  Trash2,
  Download,
  Eye,
  MoreHorizontal,
  Loader2,
  AlertCircle,
  CheckCircle2,
  Clock,
} from 'lucide-react';
import { Button } from '../components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';
import type { IfcFileActions, IFCFile } from './types';

interface IfcFileManagerProps {
  siteId: string;
  siteName: string;
  actions: IfcFileActions;
}

export function IfcFileManager({ siteId, siteName, actions }: IfcFileManagerProps) {
  const [files, setFiles] = useState<IFCFile[]>([]);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [renderView, setRenderView] = useState<string>('perspective');

  const loadFiles = useCallback(async () => {
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

  return (
    <div className="mt-4 border-t pt-4">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-sm font-semibold text-muted-foreground flex items-center gap-2">
          <FileText className="size-4" />
          IFC Files for <span className="text-foreground">{siteName}</span>
        </h3>
        <Button size="sm" variant="outline" onClick={loadFiles} disabled={loading}>
          {loading ? <Loader2 className="size-3 animate-spin" /> : null}
          Refresh
        </Button>
      </div>

      {files.length === 0 && !loading && (
        <p className="text-xs text-muted-foreground py-4 text-center">
          No IFC files uploaded yet. Use the action above to get started.
        </p>
      )}

      <div className="space-y-2">
        {files.map((file) => (
          <IfcFileCard
            key={file.id}
            file={file}
            actionLoading={actionLoading}
            onAction={async (action) => {
              setActionLoading(action);
              try {
                if (action === 'preview') {
                  await actions.triggerPreview(siteId, file.id);
                } else if (action === 'render-front') {
                  await actions.triggerBlenderRender(siteId, file.id, 'front');
                } else if (action === 'render-side') {
                  await actions.triggerBlenderRender(siteId, file.id, 'side');
                } else if (action === 'render-top') {
                  await actions.triggerBlenderRender(siteId, file.id, 'top');
                } else if (action === 'render-perspective') {
                  await actions.triggerBlenderRender(siteId, file.id, 'perspective');
                } else if (action === 'delete') {
                  const fd = new FormData();
                  fd.set('siteId', siteId);
                  fd.set('fileId', file.id);
                  await actions.deleteFile(fd);
                  loadFiles();
                }
              } finally {
                setActionLoading(null);
                loadFiles();
              }
            }}
            renderView={renderView}
            onRenderViewChange={setRenderView}
          />
        ))}
      </div>
    </div>
  );
}

type FileStatus = IFCFile['status'];

function statusBadge(status: FileStatus) {
  const styles: Record<FileStatus, { bg: string; text: string; icon: React.ReactNode }> = {
    QUEUED: { bg: 'bg-yellow-500/10', text: 'text-yellow-600', icon: <Clock className="size-3" /> },
    PROCESSING: { bg: 'bg-blue-500/10', text: 'text-blue-600', icon: <Loader2 className="size-3 animate-spin" /> },
    COMPLETED: { bg: 'bg-green-500/10', text: 'text-green-600', icon: <CheckCircle2 className="size-3" /> },
    FAILED: { bg: 'bg-red-500/10', text: 'text-red-600', icon: <AlertCircle className="size-3" /> },
  };
  const s = styles[status];
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${s.bg} ${s.text}`}>
      {s.icon}
      {status}
    </span>
  );
}

interface IfcFileCardProps {
  file: IFCFile;
  actionLoading: string | null;
  onAction: (action: string) => Promise<void>;
  renderView: string;
  onRenderViewChange: (view: string) => void;
}

function IfcFileCard({ file, actionLoading, onAction, renderView, onRenderViewChange }: IfcFileCardProps) {
  const sizeMB = file.fileSize ? `${(file.fileSize / 1024 / 1024).toFixed(1)} MB` : '\u2014';
  const date = file.createdAt ? new Date(file.createdAt).toLocaleDateString() : '\u2014';

  return (
    <div className="flex items-center justify-between rounded-lg border bg-card px-3 py-2.5 text-sm">
      <div className="flex min-w-0 flex-1 items-center gap-3">
        <FileText className="size-4 shrink-0 text-muted-foreground" />
        <div className="min-w-0">
          <p className="truncate font-medium">{file.fileName}</p>
          <p className="text-xs text-muted-foreground">{sizeMB} &middot; {date}</p>
        </div>
      </div>

      <div className="flex shrink-0 items-center gap-2">
        {statusBadge(file.status)}

        <select
          value={renderView}
          onChange={(e) => onRenderViewChange(e.target.value)}
          className="rounded border bg-transparent text-xs text-muted-foreground focus:border-primary focus:outline-none"
          aria-label="Render view"
        >
          <option value="perspective">Perspective</option>
          <option value="front">Front</option>
          <option value="side">Side</option>
          <option value="top">Top</option>
        </select>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button size="icon" variant="ghost" className="size-7">
              <MoreHorizontal className="size-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => onAction('preview')} disabled={actionLoading !== null}>
              <Image className="mr-2 size-3.5" alt="" />
              2D Preview
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => onAction('render-front')} disabled={actionLoading !== null}>
              <Box className="mr-2 size-3.5" />
              3D Render - Front
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onAction('render-side')} disabled={actionLoading !== null}>
              <Box className="mr-2 size-3.5" />
              3D Render - Side
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onAction('render-top')} disabled={actionLoading !== null}>
              <Box className="mr-2 size-3.5" />
              3D Render - Top
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => onAction('render-perspective')} disabled={actionLoading !== null}>
              <Box className="mr-2 size-3.5" />
              3D Render - Perspective
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            {file.previewUrl && (
              <DropdownMenuItem asChild>
                <a href={file.previewUrl} target="_blank" rel="noopener noreferrer">
                  <Eye className="mr-2 size-3.5" />
                  View Preview
                </a>
              </DropdownMenuItem>
            )}
            {file.exportUrl && (
              <DropdownMenuItem asChild>
                <a href={file.exportUrl} target="_blank" rel="noopener noreferrer">
                  <Download className="mr-2 size-3.5" />
                  Export 3D Model
                </a>
              </DropdownMenuItem>
            )}
            {file.metadataUrl && (
              <DropdownMenuItem asChild>
                <a href={file.metadataUrl} target="_blank" rel="noopener noreferrer">
                  <Eye className="mr-2 size-3.5" />
                  View Metadata
                </a>
              </DropdownMenuItem>
            )}
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={() => onAction('delete')} disabled={actionLoading !== null} className="text-destructive focus:text-destructive">
              <Trash2 className="mr-2 size-3.5" />
              Delete
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        {actionLoading !== null && <Loader2 className="size-3 animate-spin text-muted-foreground" />}
      </div>
    </div>
  );
}
