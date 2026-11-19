package io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmployeeObjectKeysTest {

  @Test
  void keysAreUuidWebpAndOmitTheOriginalName() {
    String photo = EmployeeObjectKeys.photo("employees/");
    String evidence = EmployeeObjectKeys.attendance("employees", 15, "check-in");

    assertTrue(photo.startsWith("employees/employee-photos/"));
    assertTrue(photo.endsWith(".webp"));
    assertTrue(evidence.startsWith("employees/attendance-evidence/15/check-in/"));
    assertTrue(evidence.endsWith(".webp"));
    assertFalse(photo.contains("ticket"));
    assertFalse(evidence.contains(".."));
    UUID.fromString(photo.substring(photo.lastIndexOf('/') + 1, photo.length() - ".webp".length()));
    UUID.fromString(evidence.substring(evidence.lastIndexOf('/') + 1, evidence.length() - ".webp".length()));
  }
}
