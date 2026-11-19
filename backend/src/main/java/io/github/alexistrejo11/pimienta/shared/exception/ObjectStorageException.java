package io.github.alexistrejo11.pimienta.shared.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Object storage failed. Adapters translate SDK exceptions into this type. */
public class ObjectStorageException extends PimientaException {

  public ObjectStorageException(String logDetails, Throwable cause) {
    super(
        ErrorCode.OBJECT_STORAGE_ERROR,
        HttpStatus.INTERNAL_SERVER_ERROR,
        "No se pudo completar la operación de almacenamiento.",
        Map.of(),
        logDetails,
        cause);
  }
}
