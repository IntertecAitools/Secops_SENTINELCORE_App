# Cloud Security (AWS) — Setup and Operations

SentinelCore connects to AWS accounts **read-only** to build an inventory and detect security
misconfigurations, and supports **one** allowlisted remediation through an approval workflow.
Azure and Google Cloud are modelled (`CloudProvider`) but **not implemented**; the API returns
HTTP 501 for them.

## What is implemented

| Capability | AWS APIs | Notes |
| --- | --- | --- |
| Connection check | `sts:GetCallerIdentity` | Fails if the credentials resolve to a different account than registered |
| S3 inventory | `ListBuckets` (paginated), `GetBucketLocation` | Bucket region recorded |
| S3 Block Public Access check | `GetPublicAccessBlock` | `S3_PUBLIC_ACCESS_BLOCK_DISABLED` (MEDIUM). Account-level BPA is **not** evaluated |
| S3 public policy | `GetBucketPolicyStatus` | `S3_BUCKET_POLICY_PUBLIC` (CRITICAL) unless bucket `RestrictPublicBuckets` is on |
| S3 public ACL | `GetBucketAcl` | `S3_BUCKET_ACL_PUBLIC` (HIGH) unless bucket `IgnorePublicAcls` is on |
| S3 default encryption | `GetBucketEncryption` | `S3_DEFAULT_ENCRYPTION_MISSING` (LOW) |
| Security groups | `DescribeSecurityGroups` (paginated, per region) | `EC2_SG_ALL_TRAFFIC_OPEN` (CRITICAL), `EC2_SG_SENSITIVE_PORT_OPEN` (HIGH) for 0.0.0.0/0 or ::/0 on SSH, RDP, DB and similar ports |
| Remediation | `PutPublicAccessBlock` | `S3_BLOCK_PUBLIC_ACCESS`: enables all four bucket-level settings |

Not implemented: IAM policy analysis, AWS Organizations discovery, other services, provider
dry-run (S3 has none; previews are labelled *local validation*).

Every finding records the account, resource ARN, region, type, severity, **observed fact** (what the
API returned), **inferred risk** (interpretation), key/value evidence, source API, first-detected,
last-observed and resolution details.

## Credentials model

* No access keys are entered in the UI or stored in PostgreSQL. `cloud_accounts` holds only
  metadata: account id, auth mode, role ARN, optional STS ExternalId (write-only via API), regions.
* The backend's own identity comes from the **standard AWS credential provider chain**: environment,
  web identity (EKS IRSA / ECS task role), IAM Identity Center/SSO profile, or instance profile.
* `ASSUME_ROLE` accounts: the backend calls `sts:AssumeRole` for short-lived (default 15 min)
  credentials, with ExternalId if configured. `PLATFORM_DEFAULT` uses the backend identity directly
  (only for the account the backend runs in).
* Never use root credentials.

## IAM permissions by operation

### Read-only profile (required for verify, scan, preview, verification)

Attach to the role SentinelCore assumes in each target account (e.g. `SentinelCoreReadOnly`):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "S3Inventory",
      "Effect": "Allow",
      "Action": ["s3:ListAllMyBuckets", "s3:GetBucketLocation"],
      "Resource": "*"
    },
    {
      "Sid": "S3ConfigurationRead",
      "Effect": "Allow",
      "Action": [
        "s3:GetBucketPublicAccessBlock",
        "s3:GetBucketPolicyStatus",
        "s3:GetBucketAcl",
        "s3:GetEncryptionConfiguration"
      ],
      "Resource": "arn:aws:s3:::*"
    },
    {
      "Sid": "SecurityGroupsRead",
      "Effect": "Allow",
      "Action": "ec2:DescribeSecurityGroups",
      "Resource": "*"
    }
  ]
}
```

`sts:GetCallerIdentity` needs no permission.

### Remediation profile (only if execution will be enabled)

Keep this separate and attach it only when you intend to allow remediation. Scope `Resource` to
specific buckets where possible:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "S3BlockPublicAccess",
      "Effect": "Allow",
      "Action": "s3:PutBucketPublicAccessBlock",
      "Resource": "arn:aws:s3:::*"
    }
  ]
}
```

### Trust policy on the target role

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": { "AWS": "arn:aws:iam::<PLATFORM_ACCOUNT_ID>:role/<SentinelCoreBackendRole>" },
      "Action": "sts:AssumeRole",
      "Condition": { "StringEquals": { "sts:ExternalId": "<random-shared-value>" } }
    }
  ]
}
```

The backend identity needs `sts:AssumeRole` on each target role ARN.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `APP_CLOUD_AWS_ENABLED` | `false` | Master switch. When false, no AWS API is called (verify/scan return 503) |
| `APP_CLOUD_AWS_DEFAULT_REGION` | `us-east-1` | STS/S3 client region, and EC2 region when an account lists none |
| `APP_CLOUD_AWS_API_CALL_TIMEOUT_SECONDS` | `30` | Total time per API call including retries |
| `APP_CLOUD_AWS_API_CALL_ATTEMPT_TIMEOUT_SECONDS` | `10` | Per-attempt timeout |
| `APP_CLOUD_AWS_ASSUME_ROLE_SESSION_SECONDS` | `900` | STS session length (900–3600) |
| `APP_CLOUD_REMEDIATION_EXECUTION_ENABLED` | `false` | Allows remediation execution. When false, requests can be proposed/approved but execution returns 503 |
| `AWS_PROFILE`, `AWS_REGION`, … | — | Standard AWS SDK settings for the backend identity |

Retries use the SDK STANDARD mode (backoff on throttling/transient errors). Access-denied, expired
credentials, throttling and timeouts are classified and shown to the user without credentials.

## Connecting a test account

1. Use a **non-production** AWS account. Create the `SentinelCoreReadOnly` role with the read-only
   profile and trust policy above.
2. Give the backend an identity (e.g. `aws sso login --profile sentinelcore-dev`, then start the
   backend with `AWS_PROFILE=sentinelcore-dev APP_CLOUD_AWS_ENABLED=true`).
3. In the UI: **Security → Cloud Security → Accounts → Connect Account**. Enter the 12-digit
   account id, choose *Assume IAM role*, paste the role ARN and ExternalId, list regions.
4. Click **Verify connection**. Status becomes `CONNECTED`, or `FAILED` with the classified error.
5. Click **Run scan**. Results: `SUCCEEDED`, `PARTIAL` (some scopes failed; their findings are
   neither added nor resolved) or `FAILED`.

## Reviewing findings

**Findings** tab: filter by account/severity/status, open a finding for evidence. Scans are
idempotent: each finding has a fingerprint (type + resource + rule); re-scans update it, a finding
not observed by a **complete** scope is marked `RESOLVED` (never deleted), and it is reopened if the
condition returns. **Re-check Resource** re-evaluates a single resource.

## Remediation workflow

```
PROPOSED → VALIDATED → PENDING_APPROVAL → APPROVED → EXECUTING → SUCCEEDED → VERIFIED
                          ↘ REJECTED   ↘ CANCELLED            ↘ FAILED      ↘ VERIFICATION_FAILED
                                                              ↘ OUTCOME_UNKNOWN (verify before acting)
```

1. **Request** (`REMEDIATION_REQUEST`): from an open finding with a supported action. The server
   takes the target from the finding, validates the action against the allowlist and finding type,
   refuses a second active request for the same finding, and requires an `Idempotency-Key` header
   (the UI sends one per dialog; repeating it returns the same request).
2. **Preview**: reads current state and shows the planned change, labelled *Local validation*.
3. **Approve/Reject** (`REMEDIATION_APPROVE`): must be a **different user** than the requester. The
   approver submits the request hash they reviewed; the approval is bound to that hash, and the
   server recomputes it from stored fields at approval and execution time.
4. **Execute** (`REMEDIATION_EXECUTE`): requires execution enabled, status `APPROVED` (or `FAILED`
   for a retry), the finding still open, and the bucket name typed as confirmation. An optimistic
   lock ensures one executor wins. Timeouts produce `OUTCOME_UNKNOWN`, not success.
5. **Verify**: runs automatically after execution (and on demand). Only when AWS reports all four
   Block Public Access settings enabled does the request become `VERIFIED` and the finding `RESOLVED`.

Every step is written to the audit log with actor, tenant scope, account, resource, hash prefix and
provider request id (for CloudTrail correlation).

## Roles

| Role | Cloud permissions |
| --- | --- |
| SUPER_ADMIN | all |
| ADMIN | view, manage accounts, scan, request, approve |
| SECURITY_ANALYST | view, scan, request |
| VULNERABILITY_MANAGER | view, request, approve, execute |
| AUDITOR | view |
| others | none |

## Testing

All automated tests use mocked AWS SDK clients; no AWS credentials or network access are needed.
Do not run remediation against a real account without explicit authorization.
