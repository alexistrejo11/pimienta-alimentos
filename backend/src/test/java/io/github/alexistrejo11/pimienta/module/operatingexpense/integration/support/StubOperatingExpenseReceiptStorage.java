package io.github.alexistrejo11.pimienta.module.operatingexpense.integration.support;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output.OperatingExpenseReceiptStorage;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.storage.OperatingExpenseObjectKeys;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class StubOperatingExpenseReceiptStorage implements OperatingExpenseReceiptStorage {

  private final String folder;
  private final ConcurrentHashMap<String, byte[]> objects = new ConcurrentHashMap<>();
  private final List<String> deleted = new CopyOnWriteArrayList<>();

  public StubOperatingExpenseReceiptStorage(String folder) {
    this.folder = folder;
  }

  public void clear() {
    objects.clear();
    deleted.clear();
  }

  public boolean contains(String key) {
    return objects.containsKey(key);
  }

  public List<String> deletedKeys() {
    return List.copyOf(deleted);
  }

  @Override
  public String storeJpeg(byte[] jpeg, long headquarterId, LocalDate occurredOn) {
    String key = OperatingExpenseObjectKeys.build(folder, headquarterId, occurredOn, UUID.randomUUID());
    objects.put(key, jpeg);
    return key;
  }

  @Override
  public void deleteQuietly(String key) {
    deleted.add(key);
    objects.remove(key);
  }

  @Override
  public String presign(String key) {
    return "https://s3.test/" + key + "?sig=1";
  }
}
