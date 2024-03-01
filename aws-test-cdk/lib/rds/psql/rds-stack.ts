import * as rds from "aws-cdk-lib/aws-rds";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import { Stack, StackProps, Duration, RemovalPolicy } from "aws-cdk-lib";
import { Secret } from "aws-cdk-lib/aws-secretsmanager";
import { Construct } from "constructs";

export class DatabaseStack extends Stack {
  public readonly psqlInstance: rds.DatabaseInstance;

  constructor(scope: Construct, id: string, props?: StackProps) {
    super(scope, id, props);

    const engine = rds.DatabaseInstanceEngine.postgres({ version: rds.PostgresEngineVersion.VER_15_3 });
    const instanceType = ec2.InstanceType.of(ec2.InstanceClass.T3, ec2.InstanceSize.MICRO);
    const port = 5432;
    const dbName = "teamgeist2";

    const masterUserSecret = new Secret(this, "db-master-user-secret", {
      secretName: "db-master-user-secret",
      description: "Database master user credentials",
      generateSecretString: {
        secretStringTemplate: JSON.stringify({ username: "postgres" }),
        generateStringKey: "password",
        passwordLength: 16,
        excludePunctuation: true,
      },
    });

    const VPC = ec2.Vpc.fromLookup(this, "VpcStack/teamgeist2-test-vpc", { vpcId: "vpc-0ec01250ee0210b5c" });

    const dbSg = new ec2.SecurityGroup(this, "Database-SG", {
      securityGroupName: "Database-SG",
      vpc: VPC,
    });

    dbSg.addIngressRule(
      ec2.Peer.ipv4(VPC.vpcCidrBlock),
      ec2.Port.tcp(port),
      `Allow port ${port} for database connection from only within the VPC (${VPC.vpcId})`,
    );

    this.psqlInstance = new rds.DatabaseInstance(this, "DB-1", {
      vpc: VPC,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      instanceType,
      engine,
      port,
      securityGroups: [dbSg],
      databaseName: dbName,
      credentials: rds.Credentials.fromSecret(masterUserSecret),
      backupRetention: Duration.days(0),
      deleteAutomatedBackups: true,
      removalPolicy: RemovalPolicy.DESTROY,
    });
  }
}
