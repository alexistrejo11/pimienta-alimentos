package io.github.alexistrejo11.pimienta.module.operatingexpense;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.storage.OperatingExpenseObjectKeys;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperatingExpenseObjectKeysTest {

  @Test
  void prefixesFolderAndUsesOccurredMonth() {
    UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
    assertEquals(
        "test-employees/operating-expenses/4/2026-10/" + id + ".jpg",
        OperatingExpenseObjectKeys.build("test-employees/", 4, LocalDate.of(2026, 10, 14), id));
  }
}
