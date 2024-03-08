import * as acm from "aws-cdk-lib/aws-certificatemanager";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as ecs from "aws-cdk-lib/aws-ecs";
import * as elbv2 from "aws-cdk-lib/aws-elasticloadbalancingv2";
import * as servicediscovery from "aws-cdk-lib/aws-servicediscovery";
import * as route53 from "aws-cdk-lib/aws-route53";
import * as targets from "aws-cdk-lib/aws-route53-targets";

import { StackProps, Stack } from "aws-cdk-lib";
import { Construct } from "constructs";

interface KeycloakProps extends StackProps {
  vpc: ec2.Vpc;
  privateDnsNamespace: servicediscovery.PrivateDnsNamespace;
}

export class KeycloakStack extends Stack {
  constructor(scope: Construct, id: string, props: KeycloakProps) {
    super(scope, id, props);

    const { vpc, privateDnsNamespace } = props;

    const hostedZone = route53.HostedZone.fromLookup(this, "comsysto.com", {
      domainName: "comsysto.com",
    });

    const domainCertificate = new acm.Certificate(this, "keycloak-certificate", {
      domainName: "keycloak.comsysto.com",
      validation: acm.CertificateValidation.fromDns(hostedZone),
    });

    const cluster = new ecs.Cluster(this, "teamgeist-cluster", { vpc });

    const albSecurityGroup = new ec2.SecurityGroup(this, "AlbSecurityGroup", {
      vpc,
      allowAllOutbound: true,
    });

    albSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(443), "Allow inbound HTTPS traffic");

    const alb = new elbv2.ApplicationLoadBalancer(this, "teamgeist2-test-lb", {
      vpc,
      internetFacing: true,
      vpcSubnets: { subnets: vpc.publicSubnets },
      securityGroup: albSecurityGroup,
    });

    const httpsListener = alb.addListener("HttpsListener", {
      port: 443,
      open: true,
      certificates: [domainCertificate],
    });

    const keycloakTaskDef = new ecs.FargateTaskDefinition(this, "KeycloakTask", {
      memoryLimitMiB: 512,
      cpu: 256,
    });

    keycloakTaskDef.addContainer("KeycloakContainer", {
      image: ecs.ContainerImage.fromRegistry("quay.io/keycloak/keycloak:23.0.7"),
      command: ["start-dev"],
      environment: {
        KC_PROXY: "edge",
        KEYCLOAK_ADMIN: "admin",
        KEYCLOAK_ADMIN_PASSWORD: "MFvLLP1PYZdzfEB",
        KC_HOSTNAME_STRICT_HTTPS: "false",
        KC_HEALTH_ENABLED: "true",
        KC_HOSTNAME_URL: "https://keycloak.comsysto.com",
        KC_HOSTNAME_ADMIN: "keycloak.comsysto.com",
        KC_HTTP_PORT: "8090",
      },
      portMappings: [{ containerPort: 8090 }],
      logging: new ecs.AwsLogDriver({ streamPrefix: "Keycloak" }),
    });

    const keycloakSecurityGroup = new ec2.SecurityGroup(this, "KeycloakSecurityGroup", {
      vpc,
    });
    keycloakSecurityGroup.addIngressRule(albSecurityGroup, ec2.Port.tcp(8090), "Allow inbound traffic from ALB");

    const keycloakService = new ecs.FargateService(this, "KeycloakService", {
      cluster,
      taskDefinition: keycloakTaskDef,
      assignPublicIp: false,
      vpcSubnets: { subnets: vpc.privateSubnets },
      cloudMapOptions: {
        name: "keycloak",
        cloudMapNamespace: privateDnsNamespace,
      },
      securityGroups: [keycloakSecurityGroup],
    });

    httpsListener.addTargets("KeycloakService", {
      port: 8090,
      protocol: elbv2.ApplicationProtocol.HTTP,
      targets: [keycloakService],
    });

    new route53.ARecord(this, "KeycloakDNSRecord", {
        zone: hostedZone,
        target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(alb)),
        recordName: "keycloak",
    });
  }
}
