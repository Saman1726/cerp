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
 * Three clients on purpose:
 *
 * <ul>
 *   <li>{@code s3Client} — talks to storage over the Docker-internal network
 *       (bucket/CORS bootstrap, any server-side reads). Cheap and doesn't depend
 *       on the host's network being reachable from inside a container.</li>
 *   <li>{@code internalS3Presigner} — signs against the Docker-internal endpoint
 *       for server-to-server presigned URLs (e.g. project-service uploading a
 *       preview PNG from inside the container). The {@code serverUploadUrl} in
 *       {@code PresignResponse} carries this host.</li>
 *   <li>{@code publicS3Presigner} — signs against the public endpoint so the
 *       generated signature is valid for requests arriving at the public host.
 *       Used for download URLs and the {@code uploadUrl} browser-facing URL in
 *       {@code PresignResponse}. No host-rewriting is needed.</li>
 * </ul>
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

    /**
     * Presigner for server-to-server operations: signs against the Docker-internal
     * endpoint. The returned server upload URL is used by other services uploading
     * directly from inside the container (e.g. project-service uploading a preview
     * PNG to MinIO).
     */
    @Bean
    public S3Presigner internalS3Presigner(StorageProperties props) {
        return S3Presigner.builder()
            .endpointOverride(URI.create(props.endpoint()))
            .region(Region.of(props.region()))
            .credentialsProvider(credentials(props))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    }

    /**
     * Presigner for browser-facing URLs: signs against the public endpoint so the
     * generated signature is valid for requests arriving at the public host. No
     * host-rewriting is needed — the signature already matches the host the browser
     * will send.
     */
    @Bean
    public S3Presigner publicS3Presigner(StorageProperties props) {
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
