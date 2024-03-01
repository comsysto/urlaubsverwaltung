#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { VpcStack } from '../lib/vpc/vpc-stack';
import { DatabaseStack } from '../lib/rds/psql/rds-stack';
import { KeycloakStack } from '../lib/ecs/keycloak/keycloak-stack';
import { TeamgeistStack } from '../lib/ecs/teamgeist/teamgeist-stack';

const app = new cdk.App();
const env = {
  account: "169119119606",
  region: "eu-north-1"
}
const VpcStc = new VpcStack(app, 'VpcStack', { env: env});
const dbStc = new DatabaseStack(app, 'DatabaseStack', {
  env: env,
});
const kc = new KeycloakStack(app, 'KeycloakStack', {
  vpc: VpcStc.vpc,
  env: env,
});
new TeamgeistStack(app, "TeamgeistStack", {
  vpc: VpcStc.vpc,
  db: dbStc.psqlInstance,
  namespace: kc.dnsNamespace,
  env: env,
})