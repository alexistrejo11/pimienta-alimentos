package io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output;

import java.time.LocalDate;

public interface OperatingExpenseReceiptStorage {

  /** Stores a processed JPEG and returns the private object key. */
  String storeJpeg(byte[] jpeg, long headquarterId, LocalDate occurredOn);

  /** Deletes an object. Failures are logged and do not propagate. */
  void deleteQuietly(String key);

  /** One-hour presigned GET URL. */
  String presign(String key);
}
