package io.github.alexistrejo11.pimienta.shared.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.alexistrejo11.pimienta.shared.exception.ObjectStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
class S3WebpUploadsTest {

  @Mock private S3Client s3Client;

  @Test
  void putSetsWebpTypeLengthAndImmutableCache() {
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());
    byte[] webp = new byte[] {1, 2, 3, 4};

    S3WebpUploads.put(s3Client, "bucket", "folder/id.webp", webp);

    ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(request.capture(), any(RequestBody.class));
    assertEquals("image/webp", request.getValue().contentType());
    assertEquals(4L, request.getValue().contentLength());
    assertEquals(S3WebpUploads.CACHE_CONTROL, request.getValue().cacheControl());
    assertEquals("folder/id.webp", request.getValue().key());
  }

  @Test
  void putTranslatesS3Exception() {
    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(S3Exception.builder().message("denied").statusCode(403).build());

    org.junit.jupiter.api.Assertions.assertThrows(
        ObjectStorageException.class, () -> S3WebpUploads.put(s3Client, "bucket", "k.webp", new byte[] {1}));
  }
}
