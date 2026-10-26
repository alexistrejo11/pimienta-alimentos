package io.github.alexistrejo11.pimienta.config.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import jakarta.servlet.ServletException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class ClientTimeZoneFilterTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  private ClientTimeZoneFilter filter;

  @BeforeEach
  void setUp() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:i18n/messages");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    source.setDefaultLocale(MessageSourceConfig.DEFAULT_LOCALE);
    filter = new ClientTimeZoneFilter(new ClientErrorMessages(source));
  }

  @AfterEach
  void clearZone() {
    ClientTimeZones.clear();
  }

  @Test
  void validIanaZoneIsBoundForTheRequestAndThenCleared() throws Exception {
    MockHttpServletRequest request = requestWith("America/Tijuana");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<ZoneId> during = new AtomicReference<>();

    filter.doFilter(
        request,
        response,
        (req, res) -> during.set(ClientTimeZones.current()));

    assertEquals(ZoneId.of("America/Tijuana"), during.get());
    assertEquals(ZoneId.of("America/Tijuana"), request.getAttribute(ClientTimeZoneFilter.REQUEST_ZONE));
    assertEquals(ClientTimeZone.FALLBACK, ClientTimeZones.current());
    assertEquals(200, response.getStatus());
  }

  @Test
  void validOffsetIsBound() throws Exception {
    MockHttpServletRequest request = requestWith("-06:00");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<ZoneId> during = new AtomicReference<>();

    filter.doFilter(request, response, (req, res) -> during.set(ClientTimeZones.current()));

    assertEquals(ZoneOffset.of("-06:00"), during.get());
    assertEquals(ClientTimeZone.FALLBACK, ClientTimeZones.current());
  }

  @Test
  void missingHeaderFallsBackToMexicoCity() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR, "trace-missing");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<ZoneId> during = new AtomicReference<>();

    filter.doFilter(request, response, (req, res) -> during.set(ClientTimeZones.current()));

    assertEquals(ClientTimeZone.FALLBACK, during.get());
    assertEquals(ZoneId.of("America/Mexico_City"), during.get());
  }

  @Test
  void blankHeaderFallsBackToMexicoCity() throws Exception {
    MockHttpServletRequest request = requestWith("   ");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<ZoneId> during = new AtomicReference<>();

    filter.doFilter(request, response, (req, res) -> during.set(ClientTimeZones.current()));

    assertEquals(ClientTimeZone.FALLBACK, during.get());
  }

  @Test
  void invalidHeaderReturns400AndDoesNotContinue() throws Exception {
    MockHttpServletRequest request = requestWith("Not/AZone");
    request.setAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR, "trace-bad");
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicBoolean continued = new AtomicBoolean(false);

    filter.doFilter(request, response, (req, res) -> continued.set(true));

    assertFalse(continued.get());
    assertNull(request.getAttribute(ClientTimeZoneFilter.REQUEST_ZONE));
    assertEquals(ClientTimeZone.FALLBACK, ClientTimeZones.current());
    assertEquals(400, response.getStatus());
    JsonNode body = JSON.readTree(response.getContentAsByteArray());
    assertEquals(ErrorCode.INVALID_ARGUMENT.code(), body.get("errorCode").asText());
    assertEquals("Solicitud no válida.", body.get("message").asText());
    assertEquals("trace-bad", body.get("traceId").asText());
  }

  @Test
  void zoneIsClearedWhenTheChainThrows() throws Exception {
    MockHttpServletRequest request = requestWith("UTC");
    MockHttpServletResponse response = new MockHttpServletResponse();

    try {
      filter.doFilter(
          request,
          response,
          (req, res) -> {
            throw new ServletException("boom");
          });
    } catch (ServletException expected) {
      assertEquals("boom", expected.getMessage());
    }

    assertEquals(ClientTimeZone.FALLBACK, ClientTimeZones.current());
  }

  private static MockHttpServletRequest requestWith(String header) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(ClientTimeZone.HEADER, header);
    return request;
  }
}
