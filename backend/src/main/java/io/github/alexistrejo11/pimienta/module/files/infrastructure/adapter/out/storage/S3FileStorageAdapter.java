package io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.out.storage;

import io.github.alexistrejo11.pimienta.module.files.core.port.output.FileStoragePort;
import io.github.alexistrejo11.pimienta.shared.exception.ObjectStorageException;
import io.github.alexistrejo11.pimienta.shared.storage.S3PresignService;
import io.github.alexistrejo11.pimienta.shared.storage.S3WebpUploads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class S3FileStorageAdapter implements FileStoragePort {

  private static final Logger log = LoggerFactory.getLogger(S3FileStorageAdapter.class);

  private final S3Client s3Client;
  private final S3PresignService s3PresignService;
  private final String bucketName;

  public S3FileStorageAdapter(
      S3Client s3Client,
      S3PresignService s3PresignService,
      @Value("${aws.s3.bucket}") String bucketName) {
    this.s3Client = s3Client;
    this.s3PresignService = s3PresignService;
    this.bucketName = bucketName;
  }

  @Override
  public void store(byte[] content, String contentType, String s3Key) {
    String type =
        contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .contentType(type)
            .contentLength((long) content.length)
            .cacheControl(S3WebpUploads.CACHE_CONTROL)
            .build();
    try {
      s3Client.putObject(request, RequestBody.fromBytes(content));
      log.info("file stored bucket={} key={}", bucketName, s3Key);
    } catch (S3Exception | SdkClientException ex) {
      throw new ObjectStorageException("file put failed key=" + s3Key, ex);
    }
  }

  @Override
  public void delete(String s3Key) {
    try {
      s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(s3Key).build());
      log.info("file deleted bucket={} key={}", bucketName, s3Key);
    } catch (S3Exception | SdkClientException ex) {
      log.warn("S3 delete failed key={}", s3Key, ex);
    }
  }

  @Override
  public void deleteQuietly(String s3Key) {
    if (s3Key == null || s3Key.isBlank()) {
      return;
    }
    delete(s3Key);
  }

  @Override
  public String presignedDownloadUrl(String s3Key) {
    try {
      return s3PresignService.presign24h(s3Key);
    } catch (S3Exception | SdkClientException ex) {
      throw new ObjectStorageException("file presign failed key=" + s3Key, ex);
    }
  }
}
