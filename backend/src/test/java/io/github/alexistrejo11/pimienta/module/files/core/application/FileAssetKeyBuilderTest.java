package io.github.alexistrejo11.pimienta.module.files.core.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexistrejo11.pimienta.module.files.core.domain.enums.FileCategory;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FileAssetKeyBuilderTest {

  @Test
  void keyUsesUuidAndKeepsTheOriginalNameOutOfThePath() {
    String key = FileAssetKeyBuilder.buildKey("pimienta/sources", FileCategory.TEMPLATE, null, null, "txt");
    assertTrue(key.startsWith("pimienta/sources/templates/"));
    assertTrue(key.endsWith(".txt"));
    assertFalse(key.contains("invoice"));
    assertFalse(key.contains(".."));
    String name = key.substring(key.lastIndexOf('/') + 1, key.length() - ".txt".length());
    UUID.fromString(name);
  }
}
