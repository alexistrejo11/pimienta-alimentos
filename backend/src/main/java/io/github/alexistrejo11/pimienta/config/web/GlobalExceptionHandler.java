package io.github.alexistrejo11.pimienta.config.web;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.PimientaException;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final ClientErrorMessages clientErrorMessages;

  public GlobalExceptionHandler(ClientErrorMessages clientErrorMessages) {
    this.clientErrorMessages = clientErrorMessages;
  }

  @ExceptionHandler(S3Exception.class)
  public ResponseEntity<ApiErrorResponse> handleS3Exception(
      S3Exception e, HttpServletRequest request) {
    String traceId = traceId(request);
    String awsCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;
    log.warn(
        "S3 error traceId={} awsErrorCode={} statusCode={}",
        traceId,
        awsCode,
        e.statusCode(),
        e);
    if ("AccessDenied".equals(awsCode)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(
              ApiErrorResponse.of(
                  ErrorCode.FORBIDDEN,
                  clientErrorMessages.resolve("OBJECT_STORAGE_ACCESS_DENIED", null),
                  traceId,
                  awsCode != null ? Map.of("awsErrorCode", awsCode) : null));
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiErrorResponse.of(
                ErrorCode.OBJECT_STORAGE_ERROR,
                clientErrorMessages.resolve(ErrorCode.OBJECT_STORAGE_ERROR),
                traceId,
                awsCode != null ? Map.of("awsErrorCode", awsCode) : null));
  }

  @ExceptionHandler(PimientaException.class)
  public ResponseEntity<ApiErrorResponse> handlePimienta(
      PimientaException ex, HttpServletRequest request) {
    String traceId = traceId(request);
    log.warn(
        "Application error traceId={} code={} status={} logDetails={}",
        traceId,
        ex.errorCode().code(),
        ex.httpStatus().value(),
        ex.logDetails());
    String message = clientErrorMessages.resolve(ex.errorCode(), ex.getMessage());
    return ResponseEntity.status(ex.httpStatus())
        .body(ApiErrorResponse.from(ex, traceId, message));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String traceId = traceId(request);
    List<FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
        .collect(Collectors.toList());
    log.warn(
        "Validation failed traceId={} fieldCount={} details={}",
        traceId,
        fields.size(),
        fields);
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.ofValidation(
                ErrorCode.VALIDATION_FAILED,
                clientErrorMessages.resolve(ErrorCode.VALIDATION_FAILED),
                traceId,
                fields));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    String traceId = traceId(request);
    List<FieldError> fields = ex.getConstraintViolations().stream()
        .map(this::toFieldError)
        .collect(Collectors.toList());
    log.warn("Constraint violation traceId={} violations={}", traceId, fields);
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.ofValidation(
                ErrorCode.CONSTRAINT_VIOLATION,
                clientErrorMessages.resolve(ErrorCode.CONSTRAINT_VIOLATION),
                traceId,
                fields));
  }

  private FieldError toFieldError(ConstraintViolation<?> v) {
    String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "unknown";
    return new FieldError(path, v.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    String traceId = traceId(request);
    log.warn(
        "Malformed payload traceId={} rootCause={}",
        traceId,
        ex.getMostSpecificCause().getMessage(),
        ex);
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.of(
                ErrorCode.MALFORMED_PAYLOAD,
                clientErrorMessages.resolve(ErrorCode.MALFORMED_PAYLOAD),
                traceId,
                null));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiErrorResponse> handleUnsupportedMedia(
      HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
    String traceId = traceId(request);
    log.warn(
        "Unsupported media traceId={} supported={} contentType={}",
        traceId,
        ex.getSupportedMediaTypes(),
        ex.getContentType());
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(
            ApiErrorResponse.of(
                ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                clientErrorMessages.resolve(ErrorCode.UNSUPPORTED_MEDIA_TYPE),
                traceId,
                Map.of("supportedTypes", ex.getSupportedMediaTypes().toString())));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    String traceId = traceId(request);
    log.warn(
        "Method not allowed traceId={} method={} supported={}",
        traceId,
        ex.getMethod(),
        ex.getSupportedHttpMethods());
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(
            ApiErrorResponse.of(
                ErrorCode.METHOD_NOT_ALLOWED,
                clientErrorMessages.resolve(ErrorCode.METHOD_NOT_ALLOWED),
                traceId,
                Map.of("supportedMethods", String.valueOf(ex.getSupportedHttpMethods()))));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    String traceId = traceId(request);
    String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
    log.warn(
        "Type mismatch traceId={} param={} value={} expectedType={}",
        traceId,
        ex.getName(),
        ex.getValue(),
        expected);
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.of(
                ErrorCode.TYPE_MISMATCH,
                clientErrorMessages.resolve(ErrorCode.TYPE_MISMATCH),
                traceId,
                Map.of("parameter", ex.getName(), "expectedType", expected)));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiErrorResponse> handleMissingParam(
      MissingServletRequestParameterException ex, HttpServletRequest request) {
    String traceId = traceId(request);
    log.warn(
        "Missing parameter traceId={} name={} type={}",
        traceId,
        ex.getParameterName(),
        ex.getParameterType());
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.of(
                ErrorCode.MISSING_PARAMETER,
                clientErrorMessages.resolve(ErrorCode.MISSING_PARAMETER),
                traceId,
                Map.of("parameter", ex.getParameterName())));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    String traceId = traceId(request);
    log.warn("Illegal argument traceId={} message={}", traceId, ex.getMessage(), ex);
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.of(
                ErrorCode.INVALID_ARGUMENT,
                clientErrorMessages.resolve(ErrorCode.INVALID_ARGUMENT),
                traceId,
                null));
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalState(
      IllegalStateException ex, HttpServletRequest request) {
    String traceId = traceId(request);
    log.warn("Illegal state traceId={} message={}", traceId, ex.getMessage(), ex);
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.of(
                ErrorCode.INVALID_ARGUMENT,
                clientErrorMessages.resolve("REQUEST_COULD_NOT_BE_PROCESSED", null),
                traceId,
                null));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
      NoResourceFoundException ex, HttpServletRequest request) {
    String traceId = traceId(request);
    log.warn("No static resource traceId={} path={}", traceId, ex.getResourcePath());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            ApiErrorResponse.of(
                ErrorCode.RESOURCE_NOT_FOUND,
                clientErrorMessages.resolve(ErrorCode.RESOURCE_NOT_FOUND),
                traceId,
                null));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> handleResponseStatus(
      ResponseStatusException ex, HttpServletRequest request) {
    String traceId = traceId(request);
    int statusValue = ex.getStatusCode().value();
    HttpStatus status = HttpStatus.resolve(statusValue);
    if (status == null) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    log.warn(
        "ResponseStatusException traceId={} status={} reason={}",
        traceId,
        statusValue,
        ex.getReason());
    ErrorCode code = switch (statusValue) {
      case 400 -> ErrorCode.INVALID_ARGUMENT;
      case 401 -> ErrorCode.UNAUTHORIZED;
      case 403 -> ErrorCode.FORBIDDEN;
      case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
      case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
      case 409 -> ErrorCode.CONFLICT;
      case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
      default -> status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.INVALID_ARGUMENT;
    };
    String safeMessage = switch (statusValue) {
      case 400 -> clientErrorMessages.resolve(ErrorCode.INVALID_ARGUMENT);
      case 401 -> clientErrorMessages.resolve(ErrorCode.UNAUTHORIZED);
      case 403 -> clientErrorMessages.resolve(ErrorCode.FORBIDDEN);
      case 404 -> clientErrorMessages.resolve(ErrorCode.RESOURCE_NOT_FOUND);
      case 405 -> clientErrorMessages.resolve(ErrorCode.METHOD_NOT_ALLOWED);
      case 409 -> clientErrorMessages.resolve(ErrorCode.CONFLICT);
      case 415 -> clientErrorMessages.resolve(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
      default -> status.is5xxServerError()
          ? clientErrorMessages.resolve(ErrorCode.INTERNAL_ERROR)
          : clientErrorMessages.resolve("REQUEST_COULD_NOT_BE_PROCESSED", null);
    };
    return ResponseEntity.status(status).body(ApiErrorResponse.of(code, safeMessage, traceId, null));
  }

  @ExceptionHandler({
    org.springframework.security.access.AccessDeniedException.class,
    org.springframework.security.authorization.AuthorizationDeniedException.class
  })
  public ResponseEntity<ApiErrorResponse> handleAccessDenied(
      Exception ex, HttpServletRequest request) {
    String traceId = traceId(request);
    log.warn("Forbidden traceId={} message={}", traceId, ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            ApiErrorResponse.of(
                ErrorCode.FORBIDDEN,
                clientErrorMessages.resolve(ErrorCode.FORBIDDEN),
                traceId,
                null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnhandled(
      Exception ex, HttpServletRequest request) {
    String traceId = traceId(request);
    log.error("Unhandled error traceId={}", traceId, ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ApiErrorResponse.of(
                ErrorCode.INTERNAL_ERROR,
                clientErrorMessages.resolve(ErrorCode.INTERNAL_ERROR),
                traceId,
                null));
  }

  private static String traceId(HttpServletRequest request) {
    return TraceIds.from(request);
  }
}
