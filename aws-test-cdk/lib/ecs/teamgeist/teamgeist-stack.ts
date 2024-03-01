import * as ecs from "aws-cdk-lib/aws-ecs";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as route53 from "aws-cdk-lib/aws-route53";
import * as acm from "aws-cdk-lib/aws-certificatemanager";
import * as elbv2 from "aws-cdk-lib/aws-elasticloadbalancingv2";
import * as targets from "aws-cdk-lib/aws-route53-targets";
import * as rds from "aws-cdk-lib/aws-rds";
import * as servicediscovery from "aws-cdk-lib/aws-servicediscovery";
import { Duration } from "aws-cdk-lib"; 


import { Stack, StackProps } from "aws-cdk-lib";
import { Construct } from "constructs";

interface TeamgeistProps extends StackProps {
  vpc: ec2.Vpc;
  db: rds.DatabaseInstance;
  namespace: servicediscovery.PrivateDnsNamespace
}

export class TeamgeistStack extends Stack {
  constructor(scope: Construct, id: string, props: TeamgeistProps) {
    super(scope, id, props);

    const { vpc, db, namespace } = props;

    const hostedZone = route53.HostedZone.fromLookup(this, "comsysto.com", {
      domainName: "comsysto.com",
    });

    const certificate = new acm.Certificate(this, "teamgeist-certificate", {
      domainName: "teamgeist-demo.comsysto.com",
      validation: acm.CertificateValidation.fromDns(hostedZone),
    });

    const cluster = new ecs.Cluster(this, "teamgeist-cluster.2", { vpc });

    const lb = new elbv2.ApplicationLoadBalancer(this, "teamgeist2-lb", {
      vpc,
      internetFacing: true,
      vpcSubnets: { subnets: vpc.publicSubnets },
    });

    const httpsListener = lb.addListener("HttpsListener", {
      port: 443,
      open: true,
      certificates: [certificate],
    });

    const teamgeistTaskDef = new ecs.FargateTaskDefinition(this, "TeamgeistTask", {
      memoryLimitMiB: 2048,
      cpu: 1024,
    });
    teamgeistTaskDef.addContainer("TeamgeistContainer", {
      image: ecs.ContainerImage.fromRegistry("docker.io/urlaubsverwaltung/urlaubsverwaltung:5.0.2"),
      environment: {
        SERVER_PORT: "8080",
        SPRING_DATASOURCE_URL: `jdbc:postgresql://${db.instanceEndpoint.hostname}:5432/teamgeist2`,
        SPRING_DATASOURCE_USERNAME: db.secret!.secretValueFromJson("username").unsafeUnwrap().toString(),
        SPRING_DATASOURCE_PASSWORD: db.secret!.secretValueFromJson("password").unsafeUnwrap().toString(),
        SPRING_DATASOURCE_DRIVER_CLASS_NAME: "org.postgresql.Driver",
        UV_MAIL_FROM: "urlaubsverwaltung@example.org",
        UV_MAIL_FROMDISPLAYNAME: "urlaubsverwaltung",
        UV_MAIL_REPLYTO: "urlaubsverwaltung@example.org",
        UV_MAIL_REPLYTODISPLAYNAME: "urlaubsverwaltung",
        UV_MAIL_APPLICATIONURL: "http://localhost:8080",
        SPRING_MAIL_HOST: "localhost",
        SPRING_MAIL_PORT: "1025",
        UV_CALENDAR_ORGANIZER: "organizer@example.org",
        SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_DEFAULT_CLIENT_ID: "urlaubsverwaltung",
        SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_DEFAULT_CLIENT_SECRET: "urlaubsverwaltung-secret",
        SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_DEFAULT_CLIENT_NAME: "urlaubsverwaltung",
        SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_DEFAULT_PROVIDER: "default",
        SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_DEFAULT_SCOPE: "openid,profile,email,roles",
        SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_DEFAULT_AUTHORIZATION_GRANT_TYPE: "authorization_code",
        SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_DEFAULT_REDIRECT_URI:
          "https://teamgeist-demo.comsysto.com/login/oauth2/code/{registrationId}",
        SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_DEFAULT_ISSUER_URI: "https://keycloak.comsysto.com/realms/urlaubsverwaltung",
        SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: "https://keycloak.comsysto.com/realms/urlaubsverwaltung",
      },
      portMappings: [{ containerPort: 8080 }],
      logging: new ecs.AwsLogDriver({ streamPrefix: "Teamgeist" }),
    });

    const teamgeistSg = new ec2.SecurityGroup(this, "TeamgeistSg", {
      vpc,
      allowAllOutbound: true,
    });
    teamgeistSg.addIngressRule(ec2.Peer.ipv4('10.0.0.0/8'), ec2.Port.tcp(8080));

    const teamgeistService = new ecs.FargateService(this, "TeamgeistService", {
      cluster,
      taskDefinition: teamgeistTaskDef,
      vpcSubnets: {
        subnets: vpc.publicSubnets,
      },
      assignPublicIp: true,
      cloudMapOptions: {
        name: "teamgeist",
        cloudMapNamespace: namespace,
      },
    })

    teamgeistService.connections.addSecurityGroup(teamgeistSg);
    httpsListener.addTargets("TeamgeistService", {
      port: 8080,
      protocol: elbv2.ApplicationProtocol.HTTP,
      targets: [teamgeistService],
      healthCheck: {
        path: "/",
        interval: Duration.seconds(30),
        timeout: Duration.seconds(5),
        port: "8080",
        healthyHttpCodes: "200,302",
      },
    });

    new route53.ARecord(this, "TeamgeistDNSRecord", {
      zone: hostedZone,
      target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(lb)),
      recordName: "teamgeist-demo",
    });
  }
}
