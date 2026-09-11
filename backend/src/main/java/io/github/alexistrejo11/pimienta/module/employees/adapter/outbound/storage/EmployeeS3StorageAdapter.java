package io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.storage;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import io.github.alexistrejo11.pimienta.module.employees.core.port.output.EmployeeStorageService;
import io.github.alexistrejo11.pimienta.shared.storage.S3HttpUrlSupport;

@Service
public class EmployeeS3StorageAdapter implements EmployeeStorageService {
  private static final String EMPLOYEE_PHOTOS_PREFIX = "employee-photos";
  private static final String ATTENDANCE_EVIDENCE_PREFIX = "attendance-evidence";

  private final Logger log = LoggerFactory.getLogger(EmployeeS3StorageAdapter.class);
  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final String folder;
  private final String bucketName;
  private final ImageProcessor imageProcessor;

  public EmployeeS3StorageAdapter(
      @Value("${aws.s3.bucket}") String bucketName,
      @Value("${aws.s3.folder}") String folder,
      S3Client s3Client,
      S3Presigner s3Presigner,
      ImageProcessor imageProcessor) {
    this.bucketName = bucketName;
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
    this.folder = folder == null ? "" : folder.strip().replaceAll("/+$", "");
    this.imageProcessor = imageProcessor;
  }

  private String objectKey(String relativePathWithoutLeadingSlash) {
    if (folder.isBlank()) {
      return relativePathWithoutLeadingSlash;
    }
    return folder + "/" + relativePathWithoutLeadingSlash;
  }

  @Override
  public String uploadEmployeePhoto(MultipartFile file, String displayNameSegment) {
    try {
      validateImage(file);
      byte[] processedImage = imageProcessor.proccessImage(file);
      String ext = fileExtension(file);
      String stem =
          sanitizeSegment(displayNameSegment)
              + "_"
              + System.currentTimeMillis()
              + "_"
              + ThreadLocalRandom.current().nextInt(10_000);
      String key = objectKey(EMPLOYEE_PHOTOS_PREFIX + "/" + stem + "." + ext);
      putBytes(key, file, processedImage);
      log.info("Employee photo uploaded: {}", key);
      return key;
    } catch (IllegalArgumentException | S3Exception e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Error uploading image", e);
    }
  }

  @Override
  public String uploadAttendanceEvidencePhoto(
      MultipartFile file, long employeeId, String displayNameSegment, String evidenceKind) {
    try {
      validateImage(file);
      byte[] processedImage = imageProcessor.proccessImage(file);
      String ext = fileExtension(file);
      String kind = sanitizeEvidenceKind(evidenceKind);
      String stem =
          employeeId
              + "_"
              + kind
              + "_"
              + sanitizeSegment(displayNameSegment)
              + "_"
              + System.currentTimeMillis()
              + "_"
              + ThreadLocalRandom.current().nextInt(10_000);
      String key = objectKey(ATTENDANCE_EVIDENCE_PREFIX + "/" + stem + "." + ext);
      putBytes(key, file, processedImage);
      log.info("Attendance evidence uploaded: {}", key);
      return key;
    } catch (IllegalArgumentException | S3Exception e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Error uploading attendance evidence", e);
    }
  }

  private void putBytes(String key, MultipartFile file, byte[] processedImage) {
    String contentType = imageProcessor.outputContentType(file.getContentType());
    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .contentLength((long) processedImage.length)
            .metadata(
                Map.of(
                    "original-name",
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "",
                    "uploaded-at",
                    Instant.now().toString()))
            .build();
    s3Client.putObject(request, RequestBody.fromBytes(processedImage));
  }

  @Override
  public String generatePresignedUrl(String key) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(key).build();

    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofHours(24))
            .getObjectRequest(getObjectRequest)
            .build();

    PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
    return presignedRequest.url().toString();
  }

  private void validateImage(MultipartFile file) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    long maxBytes = 2L * 1024 * 1024;
    if (file.getSize() > maxBytes) {
      throw new IllegalArgumentException("Image must be at most 2 MB");
    }

    String contentType = file.getContentType();
    if (contentType == null) {
      throw new IllegalArgumentException("File must have a Content-Type");
    }
    String ct = contentType.toLowerCase().strip();
    if (!ct.equals("image/jpeg") && !ct.equals("image/jpg") && !ct.equals("image/png")) {
      throw new IllegalArgumentException("Only JPEG and PNG images are allowed");
    }

    String originalName = file.getOriginalFilename();
    if (originalName == null || originalName.isBlank()) {
      throw new IllegalArgumentException("File must have a name");
    }
    int dot = originalName.lastIndexOf('.');
    if (dot < 0 || dot == originalName.length() - 1) {
      throw new IllegalArgumentException("File must have a file extension");
    }
    String extension = originalName.substring(dot + 1).toLowerCase();

    if (!Arrays.asList("jpg", "jpeg", "png").contains(extension)) {
      throw new IllegalArgumentException("Only .jpg, .jpeg and .png extensions are allowed");
    }
  }

  private static String fileExtension(MultipartFile file) {
    String originalName = file.getOriginalFilename();
    int dot = originalName.lastIndexOf('.');
    return originalName.substring(dot + 1).toLowerCase();
  }

  private static String sanitizeSegment(String segment) {
    if (segment == null || segment.isBlank()) {
      return "unknown";
    }
    return segment.strip().replaceAll("[^a-zA-Z0-9._-]", "_").replaceAll("_{2,}", "_");
  }

  private static String sanitizeEvidenceKind(String evidenceKind) {
    if (evidenceKind == null || evidenceKind.isBlank()) {
      return "evidence";
    }
    return evidenceKind.strip().toLowerCase().replaceAll("[^a-z0-9-]+", "-").replaceAll("-{2,}", "-");
  }

  @Override
  public void delete(String fileUrlOrKey) {
    try {
      String key = S3HttpUrlSupport.objectKeyFromUrlOrKey(fileUrlOrKey, bucketName);
      DeleteObjectRequest request =
          DeleteObjectRequest.builder().bucket(bucketName).key(key).build();

      s3Client.deleteObject(request);
      log.info("File deleted: {}", key);

    } catch (IllegalArgumentException | S3Exception e) {
      throw e;
    } catch (Exception e) {
      log.error("Error deleting file from S3", e);
      throw new RuntimeException("Error al eliminar imagen", e);
    }
  }
}
