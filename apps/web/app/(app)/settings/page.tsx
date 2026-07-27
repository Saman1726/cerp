import { Settings, User, Building2, Store } from 'lucide-react';
import { ComingSoon } from '@cerp/core-web/components';
import { PageHeader } from '@cerp/core-web/shell';
import Link from 'next/link';
import { Card, CardContent } from '@cerp/core-web/components/ui/card';

export default function SettingsPage() {
  return (
    <div>
      <PageHeader title="Settings" description="Organisation, team, and account settings." />

      {/* Quick Links */}
      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Link href="/settings/organisation" className="block">
          <Card className="transition-shadow hover:shadow-md">
            <CardContent className="flex items-center gap-3 pt-5">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-100 text-blue-600">
                <Building2 className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm font-medium">Organisation</p>
                <p className="text-xs text-muted-foreground">Company settings</p>
              </div>
            </CardContent>
          </Card>
        </Link>

        <Link href="/settings/profile" className="block">
          <Card className="transition-shadow hover:shadow-md">
            <CardContent className="flex items-center gap-3 pt-5">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-green-100 text-green-600">
                <User className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm font-medium">My Profile</p>
                <p className="text-xs text-muted-foreground">Personal info</p>
              </div>
            </CardContent>
          </Card>
        </Link>

        <Link href="/settings/vendor-profile" className="block">
          <Card className="transition-shadow hover:shadow-md">
            <CardContent className="flex items-center gap-3 pt-5">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-purple-100 text-purple-600">
                <Store className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm font-medium">Vendor Profile</p>
                <p className="text-xs text-muted-foreground">Business categories</p>
              </div>
            </CardContent>
          </Card>
        </Link>
      </div>

      <div className="mt-6">
        <h2 className="mb-2 text-sm font-medium text-muted-foreground">Sections</h2>
        <ul className="list-disc pl-6">
          <li>
            <Link href="/settings/organisation" className="text-primary underline underline-offset-4">
              Organisation
            </Link>
          </li>
        </ul>
      </div>
      <ComingSoon
        icon={Settings}
        title="Settings module coming soon"
        description="Team management, billing, and organisation preferences will live here."
      />
    </div>
  );
}
