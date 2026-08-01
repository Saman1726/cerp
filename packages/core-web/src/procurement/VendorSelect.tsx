'use client';

import { useState, useCallback, useEffect, useRef, useMemo } from 'react';
import { cn } from '../lib/utils';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import type { VendorPartnerSummary } from '@cerp/shared-types';

interface MyVendorResponse {
  vendorId: string;
  id?: string;
  tenantId: string;
  legalName: string;
  status: string;
  city: string | null;
  categories: unknown[];
}

interface VendorSelectProps {
  value?: string[];
  onChange?: (vendorIds: string[], vendors: VendorPartnerSummary[]) => void;
  onClear?: () => void;
  placeholder?: string;
  className?: string;
  disabled?: boolean;
  required?: boolean;
  label?: string;
  helpText?: string;
  mode?: 'single' | 'multi';
}

export function VendorSelect({
  value = [],
  onChange,
  onClear,
  placeholder = 'Select vendors...',
  className,
  disabled = false,
  required = false,
  label,
  helpText,
  mode = 'multi',
}: VendorSelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [vendors, setVendors] = useState<VendorPartnerSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  // Load my vendors (favorited vendors) on mount
  useEffect(() => {
    const loadMyVendors = async () => {
      setLoading(true);
      try {
        const res = await fetch('/api/vendors/my-vendors', {
          headers: {
            'Content-Type': 'application/json',
          },
          cache: 'no-store',
        });

        if (res.ok) {
          const data: MyVendorResponse[] = await res.json();
          // The backend now returns full vendor details directly
          const vendorList: VendorPartnerSummary[] = data.map((v) => ({
            id: v.vendorId ?? v.id ?? '',
            tenantId: v.tenantId,
            legalName: v.legalName ?? 'Unknown Vendor',
            status: (v.status as 'pending' | 'approved' | 'rejected') ?? 'pending',
            city: v.city ?? null,
            categories: (v.categories ?? []) as VendorPartnerSummary['categories'],
          }));
          setVendors(vendorList);
        } else {
          setVendors([]);
        }
      } catch {
        setVendors([]);
      } finally {
        setLoading(false);
      }
    };

    loadMyVendors();
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

  // Sync selected vendors when value changes - derived state from props
  const selectedVendors = useMemo(() => {
    if (value.length === 0) {
      return [] as VendorPartnerSummary[];
    }
    return vendors.filter((v) => value.includes(v.id));
  }, [value, vendors]);

  const handleToggle = useCallback((vendor: VendorPartnerSummary) => {
    const isSelected = selectedVendors.some((v) => v.id === vendor.id);
    let newSelected: VendorPartnerSummary[];

    if (isSelected) {
      newSelected = selectedVendors.filter((v) => v.id !== vendor.id);
    } else {
      if (mode === 'single') {
        newSelected = [vendor];
      } else {
        newSelected = [...selectedVendors, vendor];
      }
    }

    onChange?.(
      newSelected.map((v) => v.id),
      newSelected
    );
  }, [selectedVendors, onChange, mode]);

  const handleClear = useCallback(() => {
    onClear?.();
    onChange?.([], []);
  }, [onClear, onChange]);

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
            selectedVendors.length > 0 ? 'border-primary' : 'border-input'
          )}
          onClick={() => !disabled && setIsOpen(!isOpen)}
        >
          {selectedVendors.length > 0 ? (
            <div className="flex items-center flex-wrap gap-1 px-3 py-2">
              {selectedVendors.map((vendor) => (
                <Badge
                  key={vendor.id}
                  variant="secondary"
                  className="cursor-pointer hover:bg-secondary/80"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleToggle(vendor);
                  }}
                >
                  {vendor.legalName}
                  <span className="ml-1 text-xs">×</span>
                </Badge>
              ))}
              {selectedVendors.length > 2 && (
                <span className="text-xs text-muted-foreground ml-1">
                  +{selectedVendors.length - 2} more
                </span>
              )}
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="h-5 w-5 p-0 hover:bg-transparent ml-1"
                onClick={(e) => {
                  e.stopPropagation();
                  handleClear();
                }}
              >
                ×
              </Button>
            </div>
          ) : (
            <div className="flex items-center px-3 py-2 text-muted-foreground text-sm">
              <span>{placeholder}</span>
              {required && (
                <span className="text-destructive ml-1">*</span>
              )}
            </div>
          )}
        </div>

        {/* Dropdown - Simple list of My Vendors (favorited) */}
        {isOpen && (
          <div className="absolute z-50 w-full mt-1 rounded-md border bg-background shadow-lg overflow-hidden animate-in fade-in-0 zoom-in-95">
            {/* Vendor list */}
            <div className="max-h-60 overflow-y-auto">
              {loading ? (
                <div className="px-3 py-4 text-sm text-muted-foreground text-center">
                  Loading my vendors...
                </div>
              ) : vendors.length === 0 ? (
                <div className="px-3 py-4 text-sm text-muted-foreground text-center">
                  No favorited vendors. Add vendors to your favorites list.
                </div>
              ) : (
                vendors.map((vendor) => {
                  const isSelected = selectedVendors.some((v) => v.id === vendor.id);
                  return (
                    <div
                      key={vendor.id}
                      className={cn(
                        'px-3 py-2 text-sm cursor-pointer hover:bg-accent transition-colors',
                        isSelected && 'bg-accent'
                      )}
                      onClick={(e) => {
                        e.stopPropagation();
                        handleToggle(vendor);
                      }}
                    >
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <span className="font-medium truncate">{vendor.legalName}</span>
                          {vendor.status === 'approved' && (
                            <Badge variant="outline" className="text-xs h-4 px-1">
                              Approved
                            </Badge>
                          )}
                        </div>
                        {vendor.city && (
                          <div className="text-xs text-muted-foreground truncate">
                            {vendor.city}
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })
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