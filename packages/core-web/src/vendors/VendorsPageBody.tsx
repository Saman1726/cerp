'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { Search, X, Star, Users } from 'lucide-react';
import { Card, CardContent } from '../components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Modal } from '../components/ui/modal';
import { PageHeader } from '../shell/PageHeader';
import type { Vendor } from './types';

interface MyVendor {
  id: string;
  userId: string;
  vendorId: string;
  createdAt: string;
}

function initialFetchVendors(): { vendors: Vendor[]; loading: boolean } {
  return { vendors: [], loading: true };
}

export function VendorsPageBody() {
  const [data, setData] = useState<{ vendors: Vendor[]; loading: boolean }>(initialFetchVendors);
  const [category, setCategory] = useState('');
  const [name, setName] = useState('');
  const [city, setCity] = useState('');
  const [myVendors, setMyVendors] = useState<MyVendor[]>([]);
  const [favoriteIds, setFavoriteIds] = useState<Set<string>>(new Set());
  const [myVendorsOpen, setMyVendorsOpen] = useState(false);
  const [favoriting, setFavoriting] = useState<string | null>(null);
  const hasFetched = useRef(false);

  const fetchMyVendors = useCallback(async () => {
    try {
      const response = await fetch('/api/vendors/my-vendors');
      if (response.ok) {
        const data: MyVendor[] = await response.json();
        setMyVendors(data);
        const ids = new Set<string>(data.map((v) => v.vendorId));
        setFavoriteIds(ids);
      }
    } catch (error) {
      console.error('Failed to fetch my vendors:', error);
    }
  }, []);

  const fetchVendors = useCallback(async () => {
    try {
      const params = new URLSearchParams();
      if (category) params.set('category', category);
      if (name) params.set('name', name);
      if (city) params.set('city', city);
      const response = await fetch(`/api/vendors/proxy?${params.toString()}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch vendors: ${response.status}`);
      }
      const result: Vendor[] = await response.json();
      // Mark vendors as favorite based on favoriteIds
      const vendorsWithFavorite = result.map((v) => ({
        ...v,
        isFavorite: favoriteIds.has(v.id),
      }));
      setData({ vendors: vendorsWithFavorite, loading: false });
    } catch (error) {
      console.error('Failed to fetch vendors:', error);
      setData({ vendors: [], loading: false });
    }
  }, [category, name, city, favoriteIds]);

  // Run initial fetch on mount — fetch myVendors first, then fetch vendors
  useEffect(() => {
    if (hasFetched.current) return;
    hasFetched.current = true;
    fetchMyVendors();
    fetchVendors();
  }, []);

  // Re-fetch vendors after myVendors loads (favoriteIds becomes non-empty) to update favorite states
  useEffect(() => {
    if (hasFetched.current && favoriteIds.size > 0) {
      fetchVendors();
    }
  }, [favoriteIds]);

  const handleSearch = () => {
    fetchVendors();
  };

  const handleClear = () => {
    setCategory('');
    setName('');
    setCity('');
  };

  const toggleFavorite = async (vendorId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const isCurrentlyFavorite = favoriteIds.has(vendorId);
    const previousIds = new Set(favoriteIds);

    // Optimistic update
    const newIds = new Set(favoriteIds);
    if (isCurrentlyFavorite) {
      newIds.delete(vendorId);
    } else {
      newIds.add(vendorId);
    }
    setFavoriteIds(newIds);
    setFavoriting(vendorId);

    // Update the vendor list with the new favorite state
    setData((prev) => ({
      ...prev,
      vendors: prev.vendors.map((v) =>
        v.id === vendorId ? { ...v, isFavorite: !isCurrentlyFavorite } : v
      ),
    }));

    try {
      if (isCurrentlyFavorite) {
        await fetch(`/api/vendors/${vendorId}/favorite`, { method: 'DELETE' });
      } else {
        await fetch(`/api/vendors/${vendorId}/favorite`, { method: 'POST' });
      }
      // Refresh my vendors list
      await fetchMyVendors();
    } catch (error) {
      console.error('Failed to toggle favorite:', error);
      // Revert optimistic update on error
      setFavoriteIds(previousIds);
      setData((prev) => ({
        ...prev,
        vendors: prev.vendors.map((v) =>
          v.id === vendorId ? { ...v, isFavorite: isCurrentlyFavorite } : v
        ),
      }));
    } finally {
      setFavoriting(null);
    }
  };

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <PageHeader
          title="Vendors"
          description="Search and manage your vendor directory by category, name, or city."
        />
        <Button
          variant="outline"
          onClick={() => setMyVendorsOpen(true)}
          className="gap-2"
        >
          <Users className="h-4 w-4" />
          My Vendors ({myVendors.length})
        </Button>
      </div>

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
                  <TableHead className="w-12"></TableHead>
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
                    <TableCell colSpan={6} className="text-center text-muted-foreground py-8">
                      No vendors found. Adjust your search criteria or add new vendors.
                    </TableCell>
                  </TableRow>
                ) : (
                  data.vendors.map((vendor) => (
                    <TableRow key={vendor.id}>
                      <TableCell>
                        <Button
                          variant="ghost"
                          size="sm"
                          className={`p-1 h-auto ${
                            vendor.isFavorite ? 'text-yellow-500' : 'text-muted-foreground'
                          }`}
                          onClick={(e) => toggleFavorite(vendor.id, e)}
                          disabled={favoriting !== null}
                        >
                          <Star
                            className={`h-4 w-4 ${
                              vendor.isFavorite ? 'fill-current' : ''
                            }`}
                          />
                        </Button>
                      </TableCell>
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

      {/* My Vendors Popup */}
      <Modal
        open={myVendorsOpen}
        onClose={() => setMyVendorsOpen(false)}
        title="My Vendors"
      >
        <div className="space-y-2">
          {myVendors.length === 0 ? (
            <p className="py-4 text-center text-muted-foreground">
              You have not favorited any vendors yet. Click the star icon next to any vendor to add them to your list.
            </p>
          ) : (
            <div className="space-y-2">
              {myVendors.map((mv) => {
                // Try to find the vendor details from the current data
                const vendorDetail = data.vendors.find((v) => v.id === mv.vendorId);
                return (
                  <div
                    key={mv.id}
                    className="flex items-center justify-between rounded border p-3"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="font-medium truncate">{vendorDetail?.legalName ?? 'Unknown Vendor'}</p>
                      <p className="text-sm text-muted-foreground">
                        {vendorDetail?.categories?.join(', ') ?? 'No categories'}
                      </p>
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-yellow-500 shrink-0 ml-2"
                      onClick={(e) => toggleFavorite(mv.vendorId, e)}
                    >
                      <Star className="h-4 w-4 fill-current" />
                    </Button>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}