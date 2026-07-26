'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { Search, X } from 'lucide-react';
import { Card, CardContent } from '../components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { PageHeader } from '../shell/PageHeader';
import type { Vendor } from './types';

function initialFetchVendors(): { vendors: Vendor[]; loading: boolean } {
  return { vendors: [], loading: true };
}

export function VendorsPageBody() {
  const [data, setData] = useState<{ vendors: Vendor[]; loading: boolean }>(initialFetchVendors);
  const [category, setCategory] = useState('');
  const [name, setName] = useState('');
  const [city, setCity] = useState('');
  const hasFetched = useRef(false);

  const fetchVendors = useCallback(async () => {
    try {
      const params = new URLSearchParams();
      if (category) params.set('category', category);
      if (name) params.set('name', name);
      if (city) params.set('city', city);
      // Use Next.js API proxy - it handles reading the httpOnly session cookie
      // server-side and forwarding the Authorization header to the backend
      const response = await fetch(`/api/vendors/proxy?${params.toString()}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch vendors: ${response.status}`);
      }
      const result = await response.json();
      setData({ vendors: result, loading: false });
    } catch (error) {
      console.error('Failed to fetch vendors:', error);
      setData({ vendors: [], loading: false });
    }
  }, [category, name, city]);

  // Run initial fetch on mount
  useEffect(() => {
    if (hasFetched.current) return;
    hasFetched.current = true;
    fetchVendors();
  }, [fetchVendors]);

  const handleSearch = () => {
    fetchVendors();
  };

  const handleClear = () => {
    setCategory('');
    setName('');
    setCity('');
  };

  return (
    <div>
      <PageHeader
        title="Vendors"
        description="Search and manage your vendor directory by category, name, or city."
      />

      {/* Search Filters */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="flex items-end gap-4">
            <div className="flex-1">
              <label className="text-sm font-medium text-muted-foreground">Category</label>
              <Input
                placeholder="Filter by category..."
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              />
            </div>
            <div className="flex-1">
              <label className="text-sm font-medium text-muted-foreground">Name</label>
              <Input
                placeholder="Filter by name..."
                value={name}
                onChange={(e) => setName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              />
            </div>
            <div className="flex-1">
              <label className="text-sm font-medium text-muted-foreground">City</label>
              <Input
                placeholder="Filter by city..."
                value={city}
                onChange={(e) => setCity(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              />
            </div>
            <Button onClick={handleSearch} className="gap-2">
              <Search className="h-4 w-4" />
              Search
            </Button>
            <Button variant="outline" onClick={handleClear} className="gap-2 shrink-0">
              <X className="h-4 w-4" />
              Clear
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Vendor Table */}
      <Card>
        <CardContent className="p-0">
          {data.loading ? (
            <div className="flex items-center justify-center py-8">
              <p className="text-muted-foreground">Loading vendors...</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>City</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Rating</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.vendors.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center text-muted-foreground py-8">
                      No vendors found. Adjust your search criteria or add new vendors.
                    </TableCell>
                  </TableRow>
                ) : (
                  data.vendors.map((vendor) => (
                    <TableRow key={vendor.id}>
                      <TableCell className="font-medium">{vendor.legalName}</TableCell>
                      <TableCell>
                        <div className="flex flex-wrap gap-1">
                          {vendor.categories.map((cat) => (
                            <Badge key={cat} variant="secondary" className="text-xs">
                              {cat}
                            </Badge>
                          ))}
                        </div>
                      </TableCell>
                      <TableCell className="text-muted-foreground">{vendor.city ?? '—'}</TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            vendor.status === 'approved'
                              ? 'success'
                              : vendor.status === 'rejected'
                                ? 'destructive'
                                : 'secondary'
                          }
                        >
                          {vendor.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {vendor.ratingAvg != null ? `${vendor.ratingAvg.toFixed(1)} / 5` : '—'}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}