'use client';

import { useState, useCallback, useEffect, useRef } from 'react';
import { cn } from '../lib/utils';
import { Badge } from '../components/ui/badge';
import type { ProjectSummary } from '@cerp/shared-types';
import { apiClient } from '../lib/api-client';

interface ProjectSelectProps {
  _value?: string;
  onChange?: (projectId: string, project: ProjectSummary) => void;
  _onClear?: () => void;
  placeholder?: string;
  className?: string;
  disabled?: boolean;
  required?: boolean;
  label?: string;
  helpText?: string;
  projectId?: string; // alias for value for backward compatibility
  onProjectChange?: (projectId: string, project: ProjectSummary) => void;
}

export function ProjectSelect({
  onChange,
  _onClear,
  placeholder = 'Select a project...',
  className,
  disabled = false,
  required = false,
  label,
  helpText,
}: ProjectSelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedProject, setSelectedProject] = useState<ProjectSummary | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let mounted = true;
    void (async () => {
      setLoading(true);
      try {
        const projectList = await apiClient.get<ProjectSummary[]>('/api/projects');
        if (!mounted) return;
        setProjects(Array.isArray(projectList) ? projectList : []);
      } catch {
        if (mounted) setProjects([]);
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, []);

  // Click outside to close dropdown
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = useCallback((project: ProjectSummary) => {
    setSelectedProject(project);
    onChange?.(project.id, project);
    setIsOpen(false);
  }, [onChange]);

  return (
    <div className={cn('space-y-2', className)} ref={containerRef}>
      {label && (
        <label className="text-sm font-medium leading-none">
          {label}
          {required && <span className="text-destructive ml-1">*</span>}
        </label>
      )}

      {/* Relative wrapper for dropdown positioning */}
      <div className="relative">
        <div
          className={cn(
            'rounded-md border bg-background transition-colors min-h-10',
            isOpen ? 'ring-2 ring-ring' : 'hover:border-muted-foreground/50',
            disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer',
            'border-input'
          )}
          onClick={() => !disabled && setIsOpen(!isOpen)}
        >
          {selectedProject ? (
            <div className="flex items-center px-3 py-2 text-sm">
              <span className="font-medium truncate">{selectedProject.name}</span>
              <span className="ml-2 text-xs text-muted-foreground">({selectedProject.code})</span>
            </div>
          ) : (
            <div className="flex items-center px-3 py-2 text-muted-foreground text-sm">
              <span>{placeholder}</span>
              {required && <span className="text-destructive ml-1">*</span>}
            </div>
          )}
        </div>

        {/* Dropdown - Simple list of projects */}
        {isOpen && (
          <div className="absolute z-50 w-full mt-1 rounded-md border bg-background shadow-lg overflow-hidden animate-in fade-in-0 zoom-in-95">
            {/* Project list */}
            <div className="max-h-60 overflow-y-auto">
            {loading ? (
              <div className="px-3 py-4 text-sm text-muted-foreground text-center">
                Loading projects...
              </div>
            ) : projects.length === 0 ? (
              <div className="px-3 py-4 text-sm text-muted-foreground text-center">
                No projects available
              </div>
            ) : (
              projects.map((project) => (
                <div
                  key={project.id}
                  className="px-3 py-2 text-sm cursor-pointer hover:bg-accent transition-colors"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleSelect(project);
                  }}
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-medium truncate">{project.name}</span>
                        <Badge variant="outline" className="text-xs h-5 shrink-0">
                          {project.code}
                        </Badge>
                      </div>
                      {project.description && (
                        <div className="text-xs text-muted-foreground truncate">
                          {project.description}
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
        )}
      </div>

      {helpText && (
        <p className="text-xs text-muted-foreground">{helpText}</p>
      )}
    </div>
  );
}