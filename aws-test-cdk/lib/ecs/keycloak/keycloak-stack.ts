import * as ecs from "aws-cdk-lib/aws-ecs";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as route53 from "aws-cdk-lib/aws-route53";
import * as acm from "aws-cdk-lib/aws-certificatemanager";
import * as elbv2 from "aws-cdk-lib/aws-elasticloadbalancingv2";
import * as targets from "aws-cdk-lib/aws-route53-targets";
import * as servicediscovery from "aws-cdk-lib/aws-servicediscovery";
import { Stack, StackProps } from "aws-cdk-lib";
import { Construct } from "constructs";

interface KeycloakProps extends StackProps {
  vpc: ec2.Vpc;
}

export class KeycloakStack extends Stack {
  public readonly dnsNamespace: servicediscovery.PrivateDnsNamespace;
  public readonly sg: ec2.SecurityGroup;

  constructor(scope: Construct, id: string, props: KeycloakProps) {
    super(scope, id, props);

    const { vpc } = props;

    const hostedZone = route53.HostedZone.fromLookup(this, "comsysto.com", {
      domainName: "comsysto.com",
    });

    const certificate = new acm.Certificate(this, "keycloak-certificate", {
      domainName: "keycloak.comsysto.com",
      validation: acm.CertificateValidation.fromDns(hostedZone),
    });

    const cluster = new ecs.Cluster(this, "teamgeist-cluster", { vpc });
    this.dnsNamespace = new servicediscovery.PrivateDnsNamespace(this, "MyNamespace", {
      name: "teamgeist-ns",
      vpc,
    });

    const lb = new elbv2.ApplicationLoadBalancer(this, "teamgeist2-test-lb", {
      vpc,
      internetFacing: true,
      vpcSubnets: { subnets: vpc.publicSubnets },
    });

    const httpsListener = lb.addListener("HttpsListener", {
      port: 443,
      open: true,
      certificates: [certificate],
    });

    const keycloakTaskDef = new ecs.FargateTaskDefinition(this, "KeycloakTask", {
      memoryLimitMiB: 512,
      cpu: 256,
    });

    keycloakTaskDef.addContainer("KeycloakContainer", {
      image: ecs.ContainerImage.fromRegistry("quay.io/keycloak/keycloak:23.0.7"),
      command: ["start-dev", "--hostname-debug=true"],
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
    const keycloakService = new ecs.FargateService(this, "KeycloakService", {
      cluster,
      taskDefinition: keycloakTaskDef,
      vpcSubnets: {
        subnets: vpc.publicSubnets,
      },
      assignPublicIp: true,
      cloudMapOptions: {
        name: "keycloak",
        cloudMapNamespace: this.dnsNamespace,
      },
    });

    this.sg = new ec2.SecurityGroup(this, "KeycloakSg", {
      vpc,
      allowAllOutbound: true,
    });
    this.sg.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(8090));
    keycloakService.connections.addSecurityGroup(this.sg);
    httpsListener.addTargets("KeycloakService", {
      port: 8090,
      protocol: elbv2.ApplicationProtocol.HTTP,
      targets: [keycloakService],
    });


    new route53.ARecord(this, "KeycloakDNSRecord", {
      zone: hostedZone,
      target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(lb)),
      recordName: "keycloak",
    });
  }
}
