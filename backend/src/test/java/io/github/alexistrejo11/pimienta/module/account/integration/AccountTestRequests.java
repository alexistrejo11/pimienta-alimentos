package io.github.alexistrejo11.pimienta.module.account.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public final class AccountTestRequests {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private AccountTestRequests() {}

  public static String registerJson(
      String firstName,
      String lastName,
      String email,
      String phone,
      String password,
      String gender) {
    try {
      ObjectNode n = MAPPER.createObjectNode();
      n.put("firstName", firstName);
      n.put("lastName", lastName);
      n.put("email", email);
      n.put("phone", phone);
      n.put("password", password);
      n.put("gender", gender);
      return MAPPER.writeValueAsString(n);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String validRegisterJson(String email, String phone, String password) {
    return registerJson("Alexis", "Trejo", email, phone, password, "MALE");
  }

  public static String loginJson(String email, String password) {
    try {
      return MAPPER.writeValueAsString(Map.of("email", email, "password", password));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String refreshJson(String refreshToken) {
    try {
      return MAPPER.writeValueAsString(Map.of("refreshToken", refreshToken));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String logoutJson(String refreshToken) {
    try {
      return MAPPER.writeValueAsString(Map.of("refreshToken", refreshToken));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String updateProfileJson(
      String firstName, String lastName, String phone, String gender) {
    try {
      ObjectNode n = MAPPER.createObjectNode();
      n.put("firstName", firstName);
      n.put("lastName", lastName);
      n.put("phone", phone);
      n.put("gender", gender);
      return MAPPER.writeValueAsString(n);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public static MockHttpServletRequestBuilder postJson(String path, String body) {
    return MockMvcRequestBuilders.post(path)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  public static MockHttpServletRequestBuilder getBearer(String path, String accessToken) {
    return MockMvcRequestBuilders.get(path)
        .header("Authorization", "Bearer " + accessToken);
  }

  public static MockHttpServletRequestBuilder patchJsonBearer(
      String path, String accessToken, String body) {
    return MockMvcRequestBuilders.patch(path)
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + accessToken)
        .content(body);
  }

  public static MockHttpServletRequestBuilder putJsonBearer(
      String path, String accessToken, String body) {
    return MockMvcRequestBuilders.put(path)
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + accessToken)
        .content(body);
  }

  public static MockHttpServletRequestBuilder postBearer(String path, String accessToken) {
    return MockMvcRequestBuilders.post(path).header("Authorization", "Bearer " + accessToken);
  }

  public static MockHttpServletRequestBuilder postJsonBearer(
      String path, String accessToken, String body) {
    return MockMvcRequestBuilders.post(path)
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer " + accessToken)
        .content(body);
  }

  public static MockHttpServletRequestBuilder putBearer(String path, String accessToken) {
    return MockMvcRequestBuilders.put(path).header("Authorization", "Bearer " + accessToken);
  }

  public static MockHttpServletRequestBuilder deleteBearer(String path, String accessToken) {
    return MockMvcRequestBuilders.delete(path).header("Authorization", "Bearer " + accessToken);
  }
}
