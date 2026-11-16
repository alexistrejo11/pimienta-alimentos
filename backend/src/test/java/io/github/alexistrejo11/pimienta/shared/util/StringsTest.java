package io.github.alexistrejo11.pimienta.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class StringsTest {

  @Test
  void blankToNullTreatsNullBlankAndSurroundingWhitespace() {
    assertNull(Strings.blankToNull(null));
    assertNull(Strings.blankToNull(""));
    assertNull(Strings.blankToNull("   "));
    assertNull(Strings.blankToNull("\u2003"));
    assertEquals("hola", Strings.blankToNull("  hola  "));
    assertEquals("hola", Strings.blankToNull("\u2003hola\u2003"));
    // Java isBlank/strip ignore NO-BREAK SPACE (U+00A0); it is not Character.isWhitespace.
    assertEquals("\u00A0", Strings.blankToNull("\u00A0"));
    assertEquals("\u00A0hola", Strings.blankToNull("\u00A0hola\u2003"));
  }

  @Test
  void nullToEmptyTreatsNullBlankAndSurroundingWhitespace() {
    assertEquals("", Strings.nullToEmpty(null));
    assertEquals("", Strings.nullToEmpty(""));
    assertEquals("", Strings.nullToEmpty("   "));
    assertEquals("", Strings.nullToEmpty("\u2003"));
    assertEquals("hola", Strings.nullToEmpty("  hola  "));
    assertEquals("hola", Strings.nullToEmpty("\u2003hola\u2003"));
    assertEquals("\u00A0", Strings.nullToEmpty("\u00A0"));
    assertEquals("\u00A0hola", Strings.nullToEmpty("\u00A0hola\u2003"));
  }
}
