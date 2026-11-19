package io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.storage;

import io.github.alexistrejo11.pimienta.module.employees.core.port.output.EmployeeStorageService;
import io.github.alexistrejo11.pimienta.shared.exception.ObjectStorageException;
import io.github.alexistrejo11.pimienta.shared.image.ImageUploadPayload;
import io.github.alexistrejo11.pimienta.shared.infrastructure.image.ImageSanitizer;
import io.github.alexistrejo11.pimienta.shared.storage.S3HttpUrlSupport;
import io.github.alexistrejo11.pimienta.shared.storage.S3WebpUploads;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class EmployeeS3StorageAdapter implements EmployeeStorageService {

  private final Logger log = LoggerFactory.getLogger(EmployeeS3StorageAdapter.class);
  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final String folder;
  private final String bucketName;
  private final ImageSanitizer imageSanitizer;

  public EmployeeS3StorageAdapter(
      @Value("${aws.s3.bucket}") String bucketName,
      @Value("${aws.s3.folder}") String folder,
      S3Client s3Client,
      S3Presigner s3Presigner,
      ImageSanitizer imageSanitizer) {
    this.bucketName = bucketName;
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
    this.folder = folder == null ? "" : folder.strip().replaceAll("/+$", "");
    this.imageSanitizer = imageSanitizer;
  }

  @Override
  public String uploadEmployeePhoto(ImageUploadPayload file) {
    byte[] webp = imageSanitizer.sanitize(file, ImageSanitizer.PEOPLE);
    String key = EmployeeObjectKeys.photo(folder);
    S3WebpUploads.put(s3Client, bucketName, key, webp);
    log.info("Employee photo uploaded: {}", key);
    return key;
  }

  @Override
  public String uploadAttendanceEvidencePhoto(
      ImageUploadPayload file, long employeeId, String evidenceKind) {
    byte[] webp = imageSanitizer.sanitize(file, ImageSanitizer.PEOPLE);
    String key = EmployeeObjectKeys.attendance(folder, employeeId, evidenceKind);
    S3WebpUploads.put(s3Client, bucketName, key, webp);
    log.info("Attendance evidence uploaded: {}", key);
    return key;
  }

  @Override
  public String generatePresignedUrl(String key) {
    try {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(bucketName).key(key).build();
      GetObjectPresignRequest presignRequest =
          GetObjectPresignRequest.builder()
              .signatureDuration(Duration.ofHours(24))
              .getObjectRequest(getObjectRequest)
              .build();
      PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
      return presignedRequest.url().toString();
    } catch (S3Exception | SdkClientException ex) {
      throw new ObjectStorageException("employee presign failed key=" + key, ex);
    }
  }

  @Override
  public void delete(String fileUrlOrKey) {
    String key = S3HttpUrlSupport.objectKeyFromUrlOrKey(fileUrlOrKey, bucketName);
    try {
      s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
      log.info("File deleted: {}", key);
    } catch (S3Exception | SdkClientException ex) {
      throw new ObjectStorageException("employee delete failed key=" + key, ex);
    }
  }

  @Override
  public void deleteQuietly(String fileUrlOrKey) {
    if (fileUrlOrKey == null || fileUrlOrKey.isBlank()) {
      return;
    }
    try {
      delete(fileUrlOrKey);
    } catch (RuntimeException ex) {
      log.warn("employee object delete failed key={}", fileUrlOrKey, ex);
    }
  }
}
