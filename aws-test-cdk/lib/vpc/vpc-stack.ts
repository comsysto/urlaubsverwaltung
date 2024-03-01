import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from "aws-cdk-lib/aws-ec2";

export class VpcStack extends cdk.Stack {
  public readonly vpc: ec2.Vpc;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    this.vpc = new ec2.Vpc(this, "teamgeist2-test-vpc", {
      maxAzs: 2,
      subnetConfiguration: [
        {
          name: "teamgeist2-test-public",
          subnetType: ec2.SubnetType.PUBLIC,
          
        },
        {
          name: "teamgeist2-test-private",
          subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
        },
      ]
    });

  }
}
