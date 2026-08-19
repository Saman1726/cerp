import { ChevronDown, ChevronRight } from 'lucide-react';
import * as React from 'react';

interface CollapsibleSectionProps {
  title: string;
  description?: string;
  defaultOpen?: boolean;
  children: React.ReactNode;
  className?: string;
}

const CollapsibleSection: React.FC<CollapsibleSectionProps> = ({
  title,
  description,
  defaultOpen = false,
  children,
  className = '',
}) => {
  const [open, setOpen] = React.useState(defaultOpen);

  return (
    <div className={`border border-gray-200 rounded-lg ${className}`}>
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="flex items-center justify-between w-full px-4 py-3 text-left hover:bg-gray-50 rounded-t-lg transition-colors"
      >
        <div className="flex-1">
          <div className="font-medium text-gray-900">{title}</div>
          {description && (
            <div className="text-sm text-gray-500 mt-0.5">{description}</div>
          )}
        </div>
        {open ? (
          <ChevronDown className="w-4 h-4 text-gray-400 ml-2" />
        ) : (
          <ChevronRight className="w-4 h-4 text-gray-400 ml-2" />
        )}
      </button>
      {open && <div className="px-4 pb-4 border-t border-gray-100">{children}</div>}
    </div>
  );
};

export { CollapsibleSection };