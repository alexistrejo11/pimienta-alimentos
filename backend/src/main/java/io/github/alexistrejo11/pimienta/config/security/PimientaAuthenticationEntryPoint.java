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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class PimientaAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private static final Logger log = LoggerFactory.getLogger(PimientaAuthenticationEntryPoint.class);

  private final ClientErrorMessages clientErrorMessages;

  public PimientaAuthenticationEntryPoint(ClientErrorMessages clientErrorMessages) {
    this.clientErrorMessages = clientErrorMessages;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {
    log.warn(
        "Unauthorized request path={} message={}",
        request.getRequestURI(),
        authException.getMessage());
    ApiErrorHttpResponseWriter.write(
        request,
        response,
        HttpStatus.UNAUTHORIZED,
        ErrorCode.UNAUTHORIZED,
        clientErrorMessages.resolve(ErrorCode.UNAUTHORIZED));
  }
}
