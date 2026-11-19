package io.github.alexistrejo11.pimienta.shared.storage;

import io.github.alexistrejo11.pimienta.shared.exception.ObjectStorageException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/** Shared put for immutable WebP objects. */
public final class S3WebpUploads {

  public static final String CONTENT_TYPE = "image/webp";
  public static final String CACHE_CONTROL = "private, max-age=31536000, immutable";

  private S3WebpUploads() {}

  public static void put(S3Client client, String bucket, String key, byte[] webp) {
    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(CONTENT_TYPE)
            .contentLength((long) webp.length)
            .cacheControl(CACHE_CONTROL)
            .build();
    try {
      client.putObject(request, RequestBody.fromBytes(webp));
    } catch (S3Exception | SdkClientException ex) {
      throw new ObjectStorageException("s3 put failed key=" + key, ex);
    }
  }
}
