package io.github.alexistrejo11.pimienta.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Lightweight architecture checks. ArchUnit is not on the classpath; these walk {@code src/main/java}
 * so {@code ./mvnw test} fails if the invariants regress.
 */
class ArchitectureRulesTest {

  private static final Path SOURCE = Path.of("src/main/java");
  private static final Pattern PRIVATE_STRING_HELPER =
      Pattern.compile(
          "private\\s+(?:static\\s+)?String\\s+(blankToNull|nullToEmpty|blankToEmpty)\\s*\\(");

  @Test
  void domainDoesNotImportFrameworks() throws IOException {
    List<String> violations = new ArrayList<>();
    walkJava(
        path -> {
          if (!isDomain(path)) {
            return;
          }
          String text = Files.readString(path);
          for (String prefix :
              List.of(
                  "org.springframework.",
                  "jakarta.persistence.",
                  "org.hibernate.",
                  "com.fasterxml.jackson.")) {
            if (text.contains("import " + prefix)) {
              violations.add(path + " imports " + prefix);
            }
          }
        });
    assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
  }

  @Test
  void productionCodeDoesNotImportLombok() throws IOException {
    List<String> violations = new ArrayList<>();
    walkJava(
        path -> {
          String text = Files.readString(path);
          if (text.contains("import lombok.")) {
            violations.add(path + " imports lombok");
          }
        });
    assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
  }

  @Test
  void stringHelpersAreNotRedefinedOutsideStrings() throws IOException {
    List<String> violations = new ArrayList<>();
    walkJava(
        path -> {
          if (path.endsWith(Path.of("shared", "util", "Strings.java"))) {
            return;
          }
          String text = Files.readString(path);
          if (PRIVATE_STRING_HELPER.matcher(text).find()) {
            violations.add(path + " declares a private blankToNull/nullToEmpty/blankToEmpty");
          }
        });
    assertTrue(violations.isEmpty(), () -> String.join("\n", violations));
  }

  private static boolean isDomain(Path path) {
    return path.toString().contains("/core/domain/") || path.toString().contains("\\core\\domain\\");
  }

  @FunctionalInterface
  private interface IoConsumer {
    void accept(Path path) throws IOException;
  }

  private static void walkJava(IoConsumer visitor) throws IOException {
    try (Stream<Path> files = Files.walk(SOURCE)) {
      for (Path path : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        visitor.accept(path);
      }
    }
  }
}
