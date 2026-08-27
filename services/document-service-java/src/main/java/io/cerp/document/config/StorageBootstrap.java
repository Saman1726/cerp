package io.cerp.document.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

/**
 * Creates the capture bucket on startup, with a short retry loop - MinIO's
 * container can still be initializing when this service's own container
 * starts (compose only waits for it to *start*, not to be ready to accept S3
 * calls yet).
 *
 * <p>CORS is configured server-wide on the MinIO instance via
 * {@code mc admin config set local api cors_allow_origin=*}, so browser
 * requests to presigned URLs (e.g. opening an IFC preview image) are not
 * blocked. (Note: the current MinIO version does not implement
 * PutBucketCors via the S3 API.)
 */
@Component
public class StorageBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StorageBootstrap.class);
    private static final int MAX_ATTEMPTS = 10;
    private static final long RETRY_DELAY_MS = 2000;

    private final S3Client s3Client;
    private final StorageProperties props;

    public StorageBootstrap(S3Client s3Client, StorageProperties props) {
        this.s3Client = s3Client;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ensureBucket();
                log.info("Storage bucket '{}' ready at {}", props.bucket(), props.endpoint());
                return;
            } catch (Exception e) {
                log.warn("Storage not ready yet (attempt {}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage());
                sleep();
            }
        }
        log.error("Giving up preparing storage bucket '{}' after {} attempts", props.bucket(), MAX_ATTEMPTS);
    }

    private void ensureBucket() {
        boolean exists = s3Client.listBuckets().buckets().stream()
            .anyMatch(b -> b.name().equals(props.bucket()));
        if (!exists) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(props.bucket()).build());
        }
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
