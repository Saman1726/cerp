import 'dotenv/config';
import * as argon2 from 'argon2';
import dataSource from './data-source';
import { Partner } from '../partners/entities/partner.entity';
import { PartnerUser } from '../auth/entities/partner-user.entity';

const PARTNERS_SEED = [
  { code: 'ACME-SUPPLY', name: 'Acme Supply Co.', type: 'supplier' as const, contactEmail: 'contact@acmesupply.test' },
  { code: 'BUILDRIGHT', name: 'BuildRight Contractors', type: 'contractor' as const, contactEmail: 'info@buildright.test' },
  { code: 'CITYREALTY', name: 'City Realty Group', type: 'real-estate-agent' as const, contactEmail: 'hello@cityrealty.test' },
];

const PARTNER_USERS_SEED = [
  { username: 'jane', password: 'partner123', partnerCode: 'ACME-SUPPLY', role: 'account-manager' },
  { username: 'bob', password: 'partner123', partnerCode: 'BUILDRIGHT', role: 'site-lead' },
];

// Idempotent: safe to re-run against an already-seeded database, matching
// how apps/api's tenant provisioning script is designed.
async function seed(): Promise<void> {
  await dataSource.initialize();
  const partnerRepo = dataSource.getRepository(Partner);
  const userRepo = dataSource.getRepository(PartnerUser);

  const partnersByCode = new Map<string, Partner>();
  for (const p of PARTNERS_SEED) {
    let partner = await partnerRepo.findOne({ where: { code: p.code } });
    if (!partner) {
      partner = await partnerRepo.save(partnerRepo.create(p));
      console.log(`Seeded partner ${p.code}`);
    }
    partnersByCode.set(p.code, partner);
  }

  for (const u of PARTNER_USERS_SEED) {
    const partner = partnersByCode.get(u.partnerCode);
    if (!partner) continue;
    const existing = await userRepo.findOne({ where: { username: u.username, partnerId: partner.id } });
    if (!existing) {
      const passwordHash = await argon2.hash(u.password);
      await userRepo.save(
        userRepo.create({ username: u.username, passwordHash, partnerId: partner.id, role: u.role }),
      );
      console.log(`Seeded partner user ${u.username}`);
    }
  }

  await dataSource.destroy();
}

seed().catch((err: unknown) => {
  console.error(err);
  process.exit(1);
});
