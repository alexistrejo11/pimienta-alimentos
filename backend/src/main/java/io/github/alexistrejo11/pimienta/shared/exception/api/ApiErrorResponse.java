package io.github.alexistrejo11.pimienta.shared.exception.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.PimientaException;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    String errorCode,
    String message,
    String traceId,
    Map<String, Object> context,
    List<FieldError> fieldErrors) {

  public static ApiErrorResponse from(PimientaException ex, String traceId) {
    return from(ex, traceId, ex.getMessage());
  }

  /** Prefer a localized {@code clientMessage} from the presentation layer. */
  public static ApiErrorResponse from(PimientaException ex, String traceId, String clientMessage) {
    Map<String, Object> ctx = ex.context().isEmpty() ? null : ex.context();
    return new ApiErrorResponse(ex.errorCode().code(), clientMessage, traceId, ctx, null);
  }

  public static ApiErrorResponse of(
      ErrorCode errorCode, String safeMessage, String traceId, Map<String, Object> context) {
    Map<String, Object> ctx = context == null || context.isEmpty() ? null : context;
    return new ApiErrorResponse(errorCode.code(), safeMessage, traceId, ctx, null);
  }

  public static ApiErrorResponse ofValidation(
      ErrorCode errorCode,
      String safeMessage,
      String traceId,
      List<FieldError> fieldErrors) {
    return new ApiErrorResponse(errorCode.code(), safeMessage, traceId, null, fieldErrors);
  }
}
