package io.github.alexistrejo11.pimienta.shared.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends PimientaException {

  public ForbiddenException(String clientMessage, Map<String, Object> context, String logDetails) {
    this(ErrorCode.FORBIDDEN, clientMessage, context, logDetails);
  }

  public ForbiddenException(
      ErrorCode errorCode, String clientMessage, Map<String, Object> context, String logDetails) {
    super(errorCode, HttpStatus.FORBIDDEN, clientMessage, context, logDetails, null);
  }
}
