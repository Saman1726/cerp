import 'dotenv/config';
import { DataSource } from 'typeorm';
import { Partner } from '../partners/entities/partner.entity';
import { PartnerUser } from '../auth/entities/partner-user.entity';
import { migrations } from './migrations';

// TypeORM's CLI requires exactly one DataSource export from this file - a
// named export alongside the default (even pointing at the same instance)
// makes it refuse to load it.
export default new DataSource({
  type: 'postgres',
  url: process.env.DATABASE_URL,
  entities: [Partner, PartnerUser],
  migrations,
  synchronize: false,
});
