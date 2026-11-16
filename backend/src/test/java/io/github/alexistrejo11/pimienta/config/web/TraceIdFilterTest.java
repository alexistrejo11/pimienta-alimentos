package io.github.alexistrejo11.pimienta.config.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

  private static final Pattern CANONICAL_UUID =
      Pattern.compile(
          "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  private final TraceIdFilter filter = new TraceIdFilter();

  @Test
  void validUuidIsEchoedOnTheResponseAndStoredOnTheRequest() throws Exception {
    String incoming = "550e8400-e29b-41d4-a716-446655440000";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(TraceIdFilter.TRACE_ID_HEADER, incoming);
    MockHttpServletResponse response = new MockHttpServletResponse();
    AtomicReference<String> mdcDuring = new AtomicReference<>();

    filter.doFilter(
        request,
        response,
        (req, res) -> mdcDuring.set(MDC.get("traceId")));

    assertEquals(incoming, response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
    assertEquals(incoming, request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR));
    assertEquals(incoming, mdcDuring.get());
    assertNull(MDC.get("traceId"));
  }

  @Test
  void trimmedValidUuidIsAccepted() throws Exception {
    String incoming = "550e8400-e29b-41d4-a716-446655440000";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "  " + incoming + "  ");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    assertEquals(incoming, response.getHeader(TraceIdFilter.TRACE_ID_HEADER));
  }

  @Test
  void invalidHeaderIsReplacedWithAGeneratedUuid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(TraceIdFilter.TRACE_ID_HEADER, "not-a-uuid");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    String echoed = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
    assertNotEquals("not-a-uuid", echoed);
    assertTrue(CANONICAL_UUID.matcher(echoed).matches());
    UUID.fromString(echoed);
  }

  @Test
  void missingHeaderMintsAUuid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> {});

    String echoed = response.getHeader(TraceIdFilter.TRACE_ID_HEADER);
    assertTrue(CANONICAL_UUID.matcher(echoed).matches());
    assertEquals(echoed, request.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTR));
    assertNull(MDC.get("traceId"));
  }
}
