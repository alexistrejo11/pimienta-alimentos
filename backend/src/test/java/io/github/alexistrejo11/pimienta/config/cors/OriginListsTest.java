package io.github.alexistrejo11.pimienta.config.cors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class OriginListsTest {

  @Test
  void splitsCommaSeparatedEnvBlobIntoDistinctOrigins() {
    List<String> origins =
        OriginLists.parse(
            List.of("https://pimienta-alimentos.com,https://www.pimienta-alimentos.com"));

    assertThat(origins)
        .containsExactly(
            "https://pimienta-alimentos.com", "https://www.pimienta-alimentos.com");
  }

  @Test
  void trimsWhitespaceAndStripsQuotes() {
    List<String> origins =
        OriginLists.parse(
            List.of(" \"https://pimienta-alimentos.com\" , 'http://localhost:4000' "));

    assertThat(origins)
        .containsExactly("https://pimienta-alimentos.com", "http://localhost:4000");
  }

  @Test
  void keepsAlreadySplitOrigins() {
    List<String> origins =
        OriginLists.parse(
            List.of("https://pimienta-alimentos.com", "http://localhost:4000"));

    assertThat(origins)
        .containsExactly("https://pimienta-alimentos.com", "http://localhost:4000");
  }

  @Test
  void ignoresBlankEntries() {
    assertThat(OriginLists.parse(null)).isEmpty();
    assertThat(OriginLists.parse(List.of())).isEmpty();
    assertThat(OriginLists.parse(List.of("  ", ",", " , "))).isEmpty();
  }
}
