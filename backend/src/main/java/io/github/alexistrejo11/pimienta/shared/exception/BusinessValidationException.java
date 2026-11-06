package io.github.alexistrejo11.pimienta.shared.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Business rule or domain validation failure (HTTP 400), distinct from Jakarta bean validation. */
public class BusinessValidationException extends PimientaException {

  public BusinessValidationException(
      String clientMessage, Map<String, Object> context, String logDetails) {
    this(ErrorCode.VALIDATION_FAILED, clientMessage, context, logDetails);
  }

  public BusinessValidationException(
      ErrorCode errorCode, String clientMessage, Map<String, Object> context, String logDetails) {
    super(errorCode, HttpStatus.BAD_REQUEST, clientMessage, context, logDetails, null);
  }
}
