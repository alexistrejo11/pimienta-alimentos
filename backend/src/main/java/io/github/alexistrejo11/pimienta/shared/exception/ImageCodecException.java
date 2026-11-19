package io.github.alexistrejo11.pimienta.shared.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * The WebP codec is unavailable. This is a server setup failure, not a bad upload.
 * {@code GlobalExceptionHandler} maps it to HTTP 500 via {@link PimientaException}.
 */
public class ImageCodecException extends PimientaException {

  public ImageCodecException(String logDetails, Throwable cause) {
    super(
        ErrorCode.INTERNAL_ERROR,
        HttpStatus.INTERNAL_SERVER_ERROR,
        "No se pudo procesar la imagen.",
        Map.of(),
        logDetails,
        cause);
  }
}
