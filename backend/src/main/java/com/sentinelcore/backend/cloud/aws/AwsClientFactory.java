package com.sentinelcore.backend.cloud.aws;

import com.sentinelcore.backend.cloud.CloudModels.AccountContext;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * Creates AWS SDK clients scoped to one account. Callers own and must close the returned clients.
 * Abstracted so tests can supply mocked clients without AWS credentials.
 */
public interface AwsClientFactory {

    /** STS client acting as the account identity (assumed role or platform default). */
    StsClient sts(AccountContext account);

    /** S3 client acting as the account identity, with cross-region bucket access enabled. */
    S3Client s3(AccountContext account);

    /** EC2 client for one region acting as the account identity. */
    Ec2Client ec2(AccountContext account, String region);
}
