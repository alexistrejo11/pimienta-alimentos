package io.github.alexistrejo11.pimienta.config.web;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ClientTimeZoneFilter extends OncePerRequestFilter {

  public static final String REQUEST_ZONE = ClientTimeZoneFilter.class.getName() + ".zone";

  private static final Logger log = LoggerFactory.getLogger(ClientTimeZoneFilter.class);

  private final ClientErrorMessages clientErrorMessages;

  public ClientTimeZoneFilter(ClientErrorMessages clientErrorMessages) {
    this.clientErrorMessages = clientErrorMessages;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    ZoneId zone;
    try {
      zone = ClientTimeZone.parse(request.getHeader(ClientTimeZone.HEADER));
    } catch (DateTimeException ex) {
      log.warn("Invalid {} header", ClientTimeZone.HEADER);
      ApiErrorHttpResponseWriter.write(
          request,
          response,
          HttpStatus.BAD_REQUEST,
          ErrorCode.INVALID_ARGUMENT,
          clientErrorMessages.resolve(ErrorCode.INVALID_ARGUMENT));
      return;
    }
    request.setAttribute(REQUEST_ZONE, zone);
    ClientTimeZones.bind(zone);
    try {
      filterChain.doFilter(request, response);
    } finally {
      ClientTimeZones.clear();
    }
  }
}
