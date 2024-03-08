import * as cdk from "aws-cdk-lib";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as servicediscovery from "aws-cdk-lib/aws-servicediscovery";

import { Construct } from "constructs";

export class VpcStack extends cdk.Stack {
  public readonly vpc: ec2.Vpc;
  public readonly privateNamespace: servicediscovery.PrivateDnsNamespace;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    this.vpc = new ec2.Vpc(this, "teamgeist2-test-vpc", {
      maxAzs: 2,
      subnetConfiguration: [
        {
          cidrMask: 24,
          name: "PublicSubnet",
          subnetType: ec2.SubnetType.PUBLIC,
        },
        {
          cidrMask: 24,
          name: "PrivateSubnet",
          subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
        },
      ],
    });

    for (const publicSubnet of this.vpc.publicSubnets) {
      const eip = new ec2.CfnEIP(this, `EIP${publicSubnet.node.id}`, {
        domain: "vpc",
      });

      new ec2.CfnNatGateway(this, `NATGateway${publicSubnet.node.id}`, {
        subnetId: publicSubnet.subnetId,
        allocationId: eip.attrAllocationId,
      });
    }

    this.privateNamespace = new servicediscovery.PrivateDnsNamespace(this, "MyNamespace", {
      name: "teamgeist-ns",
      vpc: this.vpc,
    });
  }
}
