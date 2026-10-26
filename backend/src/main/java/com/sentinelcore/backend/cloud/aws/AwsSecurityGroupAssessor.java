package com.sentinelcore.backend.cloud.aws;

import com.sentinelcore.backend.cloud.CloudModels.*;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

/**
 * Read-only EC2 security group checks for inbound rules open to the internet.
 */
class AwsSecurityGroupAssessor {

    static final String RESOURCE_TYPE = "ec2:security-group";
    private static final int PAGE_SIZE = 200;
    private static final Set<String> WORLD_CIDRS = Set.of("0.0.0.0/0", "::/0");

    /** Ports commonly used for administration or data stores that should not be internet-facing. */
    static final Map<Integer, String> SENSITIVE_PORTS;

    static {
        Map<Integer, String> ports = new TreeMap<>();
        ports.put(22, "SSH");
        ports.put(23, "Telnet");
        ports.put(445, "SMB");
        ports.put(1433, "MSSQL");
        ports.put(1521, "Oracle");
        ports.put(2375, "Docker API");
        ports.put(3306, "MySQL");
        ports.put(3389, "RDP");
        ports.put(5432, "PostgreSQL");
        ports.put(5601, "Kibana");
        ports.put(6379, "Redis");
        ports.put(9200, "Elasticsearch");
        ports.put(27017, "MongoDB");
        SENSITIVE_PORTS = Collections.unmodifiableMap(ports);
    }

    private final Clock clock;

    AwsSecurityGroupAssessor(Clock clock) {
        this.clock = clock;
    }

    static String scope(String region) {
        return "ec2:" + region;
    }

    /** Paginates DescribeSecurityGroups for one region. */
    List<SecurityGroup> listSecurityGroups(Ec2Client ec2, List<String> groupIds) {
        List<SecurityGroup> groups = new ArrayList<>();
        String token = null;
        do {
            DescribeSecurityGroupsRequest.Builder request = DescribeSecurityGroupsRequest.builder();
            if (groupIds != null && !groupIds.isEmpty()) {
                request.groupIds(groupIds); // MaxResults cannot be combined with GroupIds
            } else {
                request.maxResults(PAGE_SIZE);
            }
            if (token != null) {
                request.nextToken(token);
            }
            DescribeSecurityGroupsResponse response = ec2.describeSecurityGroups(request.build());
            groups.addAll(response.securityGroups());
            token = response.nextToken();
        } while (token != null && !token.isEmpty());
        return groups;
    }

    ResourceObservation toResource(String accountId, String region, SecurityGroup group) {
        return new ResourceObservation(RESOURCE_TYPE, arn(region, accountId, group.groupId()), region,
                group.groupName(), scope(region), clock.instant());
    }

    List<FindingObservation> assess(String accountId, String region, SecurityGroup group) {
        Instant now = clock.instant();
        List<FindingObservation> findings = new ArrayList<>();
        String arn = arn(region, accountId, group.groupId());

        for (IpPermission permission : group.ipPermissions()) {
            List<String> worldSources = new ArrayList<>();
            permission.ipRanges().stream().map(IpRange::cidrIp).filter(WORLD_CIDRS::contains).forEach(worldSources::add);
            permission.ipv6Ranges().stream().map(Ipv6Range::cidrIpv6).filter(WORLD_CIDRS::contains).forEach(worldSources::add);
            if (worldSources.isEmpty()) {
                continue;
            }

            String protocol = permission.ipProtocol();
            Integer from = permission.fromPort();
            Integer to = permission.toPort();
            String ruleKey = protocol + ":" + from + "-" + to + ":" + String.join("+", worldSources);

            Map<String, String> evidence = new LinkedHashMap<>();
            evidence.put("groupId", group.groupId());
            evidence.put("groupName", String.valueOf(group.groupName()));
            evidence.put("vpcId", String.valueOf(group.vpcId()));
            evidence.put("ipProtocol", protocol);
            evidence.put("fromPort", String.valueOf(from));
            evidence.put("toPort", String.valueOf(to));
            evidence.put("sources", String.join(",", worldSources));

            if ("-1".equals(protocol)) {
                findings.add(new FindingObservation(
                        "EC2_SG_ALL_TRAFFIC_OPEN", ruleKey,
                        Severity.CRITICAL, RESOURCE_TYPE, arn, region, scope(region),
                        "Security group allows all inbound traffic from the internet: " + group.groupId(),
                        "DescribeSecurityGroups returned an inbound rule with IpProtocol=-1 (all) from " + worldSources + ".",
                        "Every port on every network interface using this group is reachable from the internet.",
                        evidence, "AWS EC2 DescribeSecurityGroups",
                        "Replace the rule with specific ports and trusted source ranges.",
                        null, now));
                continue;
            }

            if (!("tcp".equals(protocol) || "6".equals(protocol)) || from == null || to == null) {
                continue;
            }
            List<String> exposed = new ArrayList<>();
            SENSITIVE_PORTS.forEach((port, name) -> {
                if (port >= from && port <= to) {
                    exposed.add(port + "/" + name);
                }
            });
            if (!exposed.isEmpty()) {
                evidence.put("sensitivePorts", String.join(",", exposed));
                findings.add(new FindingObservation(
                        "EC2_SG_SENSITIVE_PORT_OPEN", ruleKey,
                        Severity.HIGH, RESOURCE_TYPE, arn, region, scope(region),
                        "Security group exposes " + String.join(", ", exposed) + " to the internet: " + group.groupId(),
                        "DescribeSecurityGroups returned an inbound TCP rule for ports " + from + "-" + to
                                + " from " + worldSources + ".",
                        "Administrative or data-store services on these ports are reachable from the internet if a "
                                + "listening instance uses this group.",
                        evidence, "AWS EC2 DescribeSecurityGroups",
                        "Restrict the source to trusted CIDR ranges or use a bastion/SSM Session Manager.",
                        null, now));
            }
        }
        return findings;
    }

    static String arn(String region, String accountId, String groupId) {
        return "arn:aws:ec2:" + region + ":" + accountId + ":security-group/" + groupId;
    }

    static String groupIdFromArn(String arn) {
        int idx = arn == null ? -1 : arn.lastIndexOf("security-group/");
        if (idx < 0) {
            throw new IllegalArgumentException("Not a security group ARN");
        }
        return arn.substring(idx + "security-group/".length());
    }
}
