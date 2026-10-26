package com.sentinelcore.backend.cloud.aws;

import com.sentinelcore.backend.cloud.CloudModels.AccountContext;
import com.sentinelcore.backend.cloud.CloudModels.AuthMode;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.utils.SdkAutoCloseable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds SDK clients using the standard AWS credential chain for the platform identity and, for
 * ASSUME_ROLE accounts, short-lived STS credentials. No long-lived keys are read from the database.
 */
@Component
public class DefaultAwsClientFactory implements AwsClientFactory, DisposableBean {

    private final AwsSettings settings;
    private final AwsCredentialsProvider platformCredentials;
    // One auto-refreshing assume-role provider per (role, external id) to avoid an STS call per request
    private final Map<String, StsAssumeRoleCredentialsProvider> assumeRoleProviders = new ConcurrentHashMap<>();
    private final Map<String, StsClient> assumeRoleStsClients = new ConcurrentHashMap<>();

    public DefaultAwsClientFactory(AwsSettings settings) {
        this.settings = settings;
        this.platformCredentials = DefaultCredentialsProvider.builder().build();
    }

    @Override
    public StsClient sts(AccountContext account) {
        return StsClient.builder()
                .region(Region.of(settings.getDefaultRegion()))
                .credentialsProvider(credentialsFor(account))
                .overrideConfiguration(overrideConfiguration())
                .build();
    }

    @Override
    public S3Client s3(AccountContext account) {
        return S3Client.builder()
                .region(Region.of(settings.getDefaultRegion()))
                .crossRegionAccessEnabled(true)
                .credentialsProvider(credentialsFor(account))
                .overrideConfiguration(overrideConfiguration())
                .build();
    }

    @Override
    public Ec2Client ec2(AccountContext account, String region) {
        return Ec2Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsFor(account))
                .overrideConfiguration(overrideConfiguration())
                .build();
    }

    private ClientOverrideConfiguration overrideConfiguration() {
        // Bounded timeouts; STANDARD retry mode backs off on throttling and transient errors
        return ClientOverrideConfiguration.builder()
                .apiCallTimeout(settings.getApiCallTimeout())
                .apiCallAttemptTimeout(settings.getApiCallAttemptTimeout())
                .retryStrategy(RetryMode.STANDARD)
                .build();
    }

    private AwsCredentialsProvider credentialsFor(AccountContext account) {
        if (account.authMode() != AuthMode.ASSUME_ROLE) {
            return platformCredentials;
        }
        String key = account.roleArn() + "|" + (account.externalId() == null ? "" : account.externalId());
        return assumeRoleProviders.computeIfAbsent(key, k -> {
            StsClient baseSts = StsClient.builder()
                    .region(Region.of(settings.getDefaultRegion()))
                    .credentialsProvider(platformCredentials)
                    .overrideConfiguration(overrideConfiguration())
                    .build();
            assumeRoleStsClients.put(k, baseSts);

            AssumeRoleRequest.Builder request = AssumeRoleRequest.builder()
                    .roleArn(account.roleArn())
                    .roleSessionName("sentinelcore-" + account.externalAccountId())
                    .durationSeconds(settings.getAssumeRoleSessionSeconds());
            if (StringUtils.hasText(account.externalId())) {
                request.externalId(account.externalId());
            }
            return StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(baseSts)
                    .refreshRequest(request.build())
                    .build();
        });
    }

    @Override
    public void destroy() {
        assumeRoleProviders.values().forEach(SdkAutoCloseable::close);
        assumeRoleStsClients.values().forEach(SdkAutoCloseable::close);
        if (platformCredentials instanceof SdkAutoCloseable closeable) {
            closeable.close();
        }
    }
}
