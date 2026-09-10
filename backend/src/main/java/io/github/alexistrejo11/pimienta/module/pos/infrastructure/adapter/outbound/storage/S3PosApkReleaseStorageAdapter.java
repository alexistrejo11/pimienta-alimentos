package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.outbound.storage;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosApkManifest;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosApkReleaseStoragePort;
import io.github.alexistrejo11.pimienta.shared.storage.S3PresignService;
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class S3PosApkReleaseStorageAdapter implements PosApkReleaseStoragePort {

  private static final Logger log = LoggerFactory.getLogger(S3PosApkReleaseStorageAdapter.class);

  private final S3Client s3Client;
  private final S3PresignService s3PresignService;
  private final ObjectMapper objectMapper;
  private final String bucketName;
  private final String defaultApkKey;

  public S3PosApkReleaseStorageAdapter(
      S3Client s3Client,
      S3PresignService s3PresignService,
      ObjectMapper objectMapper,
      @Value("${aws.s3.bucket}") String bucketName,
      @Value("${aws.s3.pos-apk-key}") String defaultApkKey) {
    this.s3Client = s3Client;
    this.s3PresignService = s3PresignService;
    this.objectMapper = objectMapper;
    this.bucketName = bucketName;
    this.defaultApkKey = defaultApkKey;
  }

  @Override
  public Optional<PosApkManifest> readManifest(String manifestKey) {
    try (InputStream in =
        s3Client.getObject(
            GetObjectRequest.builder().bucket(bucketName).key(manifestKey).build())) {
      JsonNode root = objectMapper.readTree(in);
      String versionName = textOrEmpty(root, "versionName");
      int versionCode = root.path("versionCode").asInt(0);
      String s3Key = textOrEmpty(root, "s3Key");
      if (!StringUtils.hasText(versionName) || versionCode <= 0) {
        log.warn("POS APK manifest incomplete key={}", manifestKey);
        return Optional.empty();
      }
      if (!StringUtils.hasText(s3Key)) {
        s3Key = defaultApkKey;
      }
      return Optional.of(new PosApkManifest(versionName, versionCode, s3Key));
    } catch (NoSuchKeyException e) {
      log.info("POS APK manifest missing key={}", manifestKey);
      return Optional.empty();
    } catch (S3Exception e) {
      if (e.statusCode() == 404) {
        log.info("POS APK manifest missing key={}", manifestKey);
        return Optional.empty();
      }
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Failed to read POS APK manifest: " + manifestKey, e);
    }
  }

  @Override
  public String presignedDownloadUrl(String s3Key) {
    return s3PresignService.presign24h(s3Key);
  }

  private static String textOrEmpty(JsonNode root, String field) {
    JsonNode node = root.get(field);
    return node == null || node.isNull() ? "" : node.asText("").trim();
  }
}
