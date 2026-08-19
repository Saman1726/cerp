'use client';

import { useState, useMemo } from 'react';
import { Check, ChevronDown, ChevronUp } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Card, CardContent } from '../components/ui/card';
import type { VendorCategory, VendorCategoryGroup } from '@cerp/shared-types';

interface CategorySelectorProps {
  categories: VendorCategory[];
  selectedCategoryIds: string[];
  onChange: (selectedIds: string[]) => void;
  disabled?: boolean;
  error?: string;
}

export function CategorySelector({
  categories,
  selectedCategoryIds,
  onChange,
  disabled = false,
  error,
}: CategorySelectorProps) {
  const [expandedGroups, setExpandedGroups] = useState<Record<string, boolean>>({});

  // Group categories by groupId
  const groupedCategories = useMemo(() => {
    const groups: Record<string, VendorCategoryGroup> = {};
    const uncategorized: VendorCategory[] = [];

    for (const cat of categories) {
      if (cat.groupId != null) {
        let group = groups[cat.groupId];
        if (!group) {
          group = {
            id: cat.groupId,
            name: cat.groupName ?? 'Uncategorized',
            description: null,
            displayOrder: 0,
            isActive: true,
            activeCategoryCount: 0,
            categories: [],
          };
          groups[cat.groupId] = group;
        }
        group.categories.push(cat);
      } else {
        uncategorized.push(cat);
      }
    }

    // Sort groups by displayOrder
    const sortedGroups = Object.values(groups).sort((a, b) => a.displayOrder - b.displayOrder);
    for (const group of sortedGroups) {
      group.categories.sort((a, b) => a.displayOrder - b.displayOrder);
    }

    // Sort uncategorized
    uncategorized.sort((a, b) => a.displayOrder - b.displayOrder);

    return { groups: sortedGroups, uncategorized };
  }, [categories]);

  const toggleCategory = (categoryId: string) => {
    if (selectedCategoryIds.includes(categoryId)) {
      onChange(selectedCategoryIds.filter((id) => id !== categoryId));
    } else {
      onChange([...selectedCategoryIds, categoryId]);
    }
  };

  const toggleGroup = (groupId: string) => {
    const group = groupedCategories.groups.find((g) => g.id === groupId);
    if (!group) return;

    const groupCategoryIds = group.categories.map((c) => c.id);
    const allSelected = groupCategoryIds.every((id) => selectedCategoryIds.includes(id));

    if (allSelected) {
      // Deselect all in group
      const remaining = selectedCategoryIds.filter((id) => !groupCategoryIds.includes(id));
      onChange(remaining);
    } else {
      // Select all in group
      onChange([...new Set([...selectedCategoryIds, ...groupCategoryIds])]);
    }
  };

  const toggleExpandGroup = (groupId: string) => {
    setExpandedGroups((prev) => ({ ...prev, [groupId]: !prev[groupId] }));
  };

  const selectAll = () => {
    onChange(categories.map((c) => c.id));
  };

  const clearAll = () => {
    onChange([]);
  };

  const isGroupSelected = (group: VendorCategoryGroup) =>
    group.categories.every((c) => selectedCategoryIds.includes(c.id));

  const isPartiallySelected = (group: VendorCategoryGroup) => {
    const selectedCount = group.categories.filter((c) => selectedCategoryIds.includes(c.id)).length;
    return selectedCount > 0 && selectedCount < group.categories.length;
  };

  if (categories.length === 0) return null;

  return (
    <div className="space-y-4">
      {/* Header with select all/clear all */}
      <div className="flex items-center justify-between">
        <span className="text-sm text-muted-foreground">
          {selectedCategoryIds.length} of {categories.length} selected
        </span>
        <div className="flex gap-2">
          <Button type="button" variant="ghost" size="sm" onClick={selectAll}>
            Select All
          </Button>
          <Button type="button" variant="ghost" size="sm" onClick={clearAll}>
            Clear All
          </Button>
        </div>
      </div>

      {/* Error message */}
      {error && <p className="text-sm text-destructive">{error}</p>}

      {/* Grouped categories */}
      <div className="space-y-2">
        {groupedCategories.groups.map((group) => (
          <Card key={group.id} className="overflow-hidden">
            <CardContent className="p-0">
              {/* Group header */}
              <div
                className="w-full flex items-center justify-between p-3 hover:bg-muted/50 transition-colors"
                onClick={() => toggleExpandGroup(group.id)}
              >
                <div className="flex items-center gap-2">
                  <span
                    role="checkbox"
                    aria-checked={isGroupSelected(group)}
                    tabIndex={0}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.stopPropagation();
                        e.preventDefault();
                        toggleGroup(group.id);
                      }
                    }}
                    onClick={(e) => {
                      e.stopPropagation();
                      toggleGroup(group.id);
                    }}
                    className={`flex items-center justify-center w-5 h-5 border rounded-sm transition-colors cursor-pointer ${
                      disabled ? 'opacity-50 cursor-not-allowed' : ''
                    }`}
                  >
                    {isGroupSelected(group) && <Check className="h-3 w-3" />}
                    {isPartiallySelected(group) && <div className="h-3 w-3 bg-muted-foreground rounded-sm" />}
                  </span>
                  <span className="font-medium text-sm">{group.name}</span>
                </div>
                {expandedGroups[group.id] ? (
                  <ChevronUp className="h-4 w-4 text-muted-foreground" />
                ) : (
                  <ChevronDown className="h-4 w-4 text-muted-foreground" />
                )}
              </div>

              {/* Categories in group */}
              {expandedGroups[group.id] && (
                <div className="px-3 pb-3 pl-10 flex flex-wrap gap-2">
                  {group.categories.map((cat) => (
                    <button
                      key={cat.id}
                      type="button"
                      className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-colors border ${
                        selectedCategoryIds.includes(cat.id)
                          ? 'bg-primary text-primary-foreground border-primary'
                          : 'bg-background text-foreground border-border hover:bg-muted'
                      } ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
                      onClick={() => toggleCategory(cat.id)}
                      disabled={disabled}
                    >
                      {selectedCategoryIds.includes(cat.id) && <Check className="h-3 w-3" />}
                      {cat.name}
                    </button>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        ))}

        {/* Uncategorized categories */}
        {groupedCategories.uncategorized.length > 0 && (
          <Card>
            <CardContent className="p-3">
              <div className="flex flex-wrap gap-2">
                {groupedCategories.uncategorized.map((cat) => (
                  <button
                    key={cat.id}
                    type="button"
                    className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-colors border ${
                      selectedCategoryIds.includes(cat.id)
                        ? 'bg-primary text-primary-foreground border-primary'
                        : 'bg-background text-foreground border-border hover:bg-muted'
                    } ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
                    onClick={() => toggleCategory(cat.id)}
                    disabled={disabled}
                  >
                    {selectedCategoryIds.includes(cat.id) && <Check className="h-3 w-3" />}
                    {cat.name}
                  </button>
                ))}
              </div>
            </CardContent>
          </Card>
        )}
      </div>

      {/* Selected preview */}
      {selectedCategoryIds.length > 0 && (
        <div className="pt-2 border-t">
          <p className="text-sm text-muted-foreground mb-2">Selected categories:</p>
          <div className="flex flex-wrap gap-1">
            {categories
              .filter((c) => selectedCategoryIds.includes(c.id))
              .map((cat) => (
                <Badge key={cat.id} variant="secondary" className="text-xs">
                  {cat.name}
                </Badge>
              ))}
          </div>
        </div>
      )}
    </div>
  );
}