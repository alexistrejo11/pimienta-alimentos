package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.storage;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output.OperatingExpenseReceiptStorage;
import io.github.alexistrejo11.pimienta.shared.storage.S3PresignService;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3OperatingExpenseReceiptStorage implements OperatingExpenseReceiptStorage {

  private static final Logger log = LoggerFactory.getLogger(S3OperatingExpenseReceiptStorage.class);
  private static final Duration PRESIGN_TTL = Duration.ofHours(1);

  private final S3Client s3Client;
  private final S3PresignService s3PresignService;
  private final String bucketName;
  private final String folder;

  public S3OperatingExpenseReceiptStorage(
      S3Client s3Client,
      S3PresignService s3PresignService,
      @Value("${aws.s3.bucket}") String bucketName,
      @Value("${aws.s3.folder}") String folder) {
    this.s3Client = s3Client;
    this.s3PresignService = s3PresignService;
    this.bucketName = bucketName;
    this.folder = folder;
  }

  @Override
  public String storeJpeg(byte[] jpeg, long headquarterId, LocalDate occurredOn) {
    String key = OperatingExpenseObjectKeys.build(folder, headquarterId, occurredOn, UUID.randomUUID());
    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType("image/jpeg")
            .contentLength((long) jpeg.length)
            .build();
    s3Client.putObject(request, RequestBody.fromBytes(jpeg));
    log.info("expense receipt stored key={}", key);
    return key;
  }

  @Override
  public void deleteQuietly(String key) {
    if (key == null || key.isBlank()) {
      return;
    }
    try {
      s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
      log.info("expense receipt deleted key={}", key);
    } catch (RuntimeException ex) {
      log.warn("expense receipt delete failed key={}", key, ex);
    }
  }

  @Override
  public String presign(String key) {
    return s3PresignService.presign(key, PRESIGN_TTL);
  }
}
