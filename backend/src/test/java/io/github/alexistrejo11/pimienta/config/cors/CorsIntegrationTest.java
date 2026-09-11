package io.github.alexistrejo11.pimienta.config.cors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class CorsIntegrationTest {

  private static final String PRODUCTION_WEB_ORIGIN = "https://pimienta-alimentos.com";

  @Autowired
  private MockMvc mockMvc;

  @Test
  void preflight_fromProductionWebOrigin_allowsLoginPost() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, PRODUCTION_WEB_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PRODUCTION_WEB_ORIGIN))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  void loginPost_fromProductionWebOrigin_includesAllowOrigin() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, PRODUCTION_WEB_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"cors-it@mail.com\",\"password\":\"x\"}"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PRODUCTION_WEB_ORIGIN));
  }

  @Test
  void preflight_fromUnknownOrigin_isRejected() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
        .andExpect(status().isForbidden());
  }
}
