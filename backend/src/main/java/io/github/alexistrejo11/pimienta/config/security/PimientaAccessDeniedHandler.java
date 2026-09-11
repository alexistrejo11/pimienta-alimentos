package io.github.alexistrejo11.pimienta.config.security;

import io.github.alexistrejo11.pimienta.config.web.ApiErrorHttpResponseWriter;
import io.github.alexistrejo11.pimienta.config.web.ClientErrorMessages;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class PimientaAccessDeniedHandler implements AccessDeniedHandler {

  private static final Logger log = LoggerFactory.getLogger(PimientaAccessDeniedHandler.class);

  private final ClientErrorMessages clientErrorMessages;

  public PimientaAccessDeniedHandler(ClientErrorMessages clientErrorMessages) {
    this.clientErrorMessages = clientErrorMessages;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    log.warn(
        "Forbidden request path={} message={}",
        request.getRequestURI(),
        accessDeniedException.getMessage());
    ApiErrorHttpResponseWriter.write(
        request,
        response,
        HttpStatus.FORBIDDEN,
        ErrorCode.FORBIDDEN,
        clientErrorMessages.resolve(ErrorCode.FORBIDDEN));
  }
}
