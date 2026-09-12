package io.github.alexistrejo11.pimienta.module.headquarter.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDateTime;

/** Managed sale category used by a headquarter POS catalog. */
public class PosSaleCategory extends BaseDomain<Long> {
  private Long headquarterId;
  private String name;
  private int displayOrder;
  private boolean active;

  private PosSaleCategory() {}

  public static PosSaleCategory create(long headquarterId, String name, int displayOrder) {
    PosSaleCategory category = new PosSaleCategory();
    category.id = 0L;
    category.headquarterId = headquarterId;
    category.name = name.strip();
    category.displayOrder = displayOrder;
    category.active = true;
    category.createdAt = LocalDateTime.now();
    category.updatedAt = category.createdAt;
    category.version = 0L;
    return category;
  }

  public Long getHeadquarterId() { return headquarterId; }
  public String getName() { return name; }
  public int getDisplayOrder() { return displayOrder; }
  public boolean isActive() { return active; }

  public void rename(String value) { name = value.strip(); updatedAt = LocalDateTime.now(); }
  public void reorder(int value) { displayOrder = value; updatedAt = LocalDateTime.now(); }
  public void archive() { active = false; updatedAt = LocalDateTime.now(); }
}
