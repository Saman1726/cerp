package io.cerp.document.config;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Two separate clients on purpose. `s3Client` talks to storage over the
 * Docker-internal network (bucket/CORS bootstrap, any server-side reads) -
 * cheap and doesn't depend on the host's network being reachable from inside
 * a container. `s3Presigner` only ever produces a signed URL string (no
 * network call of its own) and must be built against the LAN-facing endpoint,
 * since that URL is handed to the browser - which could be this laptop or a
 * phone on the same Wi-Fi, never something that can resolve Docker's internal
 * hostnames.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public S3Client s3Client(StorageProperties props) {
        return S3Client.builder()
            .httpClient(UrlConnectionHttpClient.create())
            .endpointOverride(URI.create(props.endpoint()))
            .region(Region.of(props.region()))
            .credentialsProvider(credentials(props))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            // Recent SDK versions default to proactively attaching flexible-
            // checksum headers ("WHEN_SUPPORTED"), which MinIO's S3 API
            // doesn't recognize for several operations (501 Not Implemented
            // on plain ListBuckets, notably). Only send/expect them when the
            // operation actually requires it.
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
            .build();
    }

    @Bean
    public S3Presigner s3Presigner(StorageProperties props) {
        return S3Presigner.builder()
            .endpointOverride(URI.create(props.publicEndpoint()))
            .region(Region.of(props.region()))
            .credentialsProvider(credentials(props))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    }

    private StaticCredentialsProvider credentials(StorageProperties props) {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKey(), props.secretKey()));
    }
}
