package com.sentinelcore.backend.cloud.aws;

import com.sentinelcore.backend.cloud.CloudErrorType;
import com.sentinelcore.backend.cloud.CloudProviderException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.util.Set;

/**
 * Classifies AWS SDK exceptions. Resulting messages include the operation, AWS error code and
 * request id, but never credentials or raw response bodies.
 */
public final class AwsErrorMapper {

    private static final Set<String> ACCESS_DENIED = Set.of(
            "AccessDenied", "AccessDeniedException", "UnauthorizedOperation", "AuthFailure",
            "InvalidClientTokenId", "SignatureDoesNotMatch", "UnrecognizedClientException", "AllAccessDisabled");
    private static final Set<String> EXPIRED = Set.of(
            "ExpiredToken", "ExpiredTokenException", "RequestExpired", "TokenRefreshRequired", "InvalidToken");
    private static final Set<String> THROTTLED = Set.of(
            "Throttling", "ThrottlingException", "RequestLimitExceeded", "SlowDown", "TooManyRequestsException",
            "RequestThrottled", "RequestThrottledException");
    private static final Set<String> NOT_FOUND = Set.of(
            "NoSuchBucket", "InvalidGroup.NotFound", "InvalidGroupId.Malformed");

    private AwsErrorMapper() {
    }

    public static CloudProviderException map(String operation, Exception e) {
        if (e instanceof CloudProviderException cpe) {
            return cpe;
        }
        if (e instanceof ApiCallTimeoutException || e instanceof ApiCallAttemptTimeoutException) {
            return new CloudProviderException(CloudErrorType.TIMEOUT,
                    "AWS " + operation + " timed out", e);
        }
        if (e instanceof AwsServiceException ase) {
            String code = ase.awsErrorDetails() != null ? ase.awsErrorDetails().errorCode() : null;
            String requestId = ase.requestId();
            String suffix = " (" + (code != null ? code : "HTTP " + ase.statusCode())
                    + (requestId != null ? ", request id " + requestId : "") + ")";
            CloudErrorType type;
            if (code != null && ACCESS_DENIED.contains(code)) {
                type = CloudErrorType.ACCESS_DENIED;
            } else if (code != null && EXPIRED.contains(code)) {
                type = CloudErrorType.CREDENTIALS_EXPIRED;
            } else if ((code != null && THROTTLED.contains(code)) || ase.isThrottlingException()) {
                type = CloudErrorType.THROTTLED;
            } else if (code != null && NOT_FOUND.contains(code)) {
                type = CloudErrorType.RESOURCE_NOT_FOUND;
            } else if (ase.statusCode() == 403) {
                type = CloudErrorType.ACCESS_DENIED;
            } else {
                type = CloudErrorType.PROVIDER_ERROR;
            }
            return new CloudProviderException(type, "AWS " + operation + " failed" + suffix, e);
        }
        if (e instanceof SdkClientException sce) {
            String message = sce.getMessage() == null ? "" : sce.getMessage();
            if (message.contains("Unable to load credentials")) {
                return new CloudProviderException(CloudErrorType.NOT_CONFIGURED,
                        "AWS credentials are not available to the platform for " + operation, e);
            }
            return new CloudProviderException(CloudErrorType.PROVIDER_ERROR,
                    "AWS " + operation + " failed: client error contacting AWS", e);
        }
        return new CloudProviderException(CloudErrorType.PROVIDER_ERROR, "AWS " + operation + " failed", e);
    }

    /** Error code of an AWS service exception, or null. */
    static String errorCode(Exception e) {
        if (e instanceof AwsServiceException ase && ase.awsErrorDetails() != null) {
            return ase.awsErrorDetails().errorCode();
        }
        return null;
    }
}
